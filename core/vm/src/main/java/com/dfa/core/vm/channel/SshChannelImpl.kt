package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationError
import com.dfa.core.vm.communication.CommunicationState
import com.dfa.core.vm.communication.ConnectionInfo
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SSH通道实现
 *
 * 使用JSch库实现SSH通信通道，支持命令执行、文件传输和端口转发功能。
 * 该实现支持密码认证和密钥认证两种方式。
 *
 * ## 功能特性
 * - 远程命令执行（同步和交互式）
 * - 文件上传和下载（SFTP）
 * - 本地和远程端口转发
 * - 动态端口转发（SOCKS代理）
 * - Shell会话管理
 * - 自动重连机制
 *
 * ## 使用示例
 * ```kotlin
 * val config = SshChannelConfig(
 *     host = "192.168.1.100",
 *     port = 22,
 *     authMethod = SshAuthMethod.Password("user", "password")
 * )
 * val result = sshChannel.connect(config)
 * if (result.isSuccess) {
 *     val cmdResult = sshChannel.executeCommand("ls -la")
 * }
 * ```
 *
 * @constructor 创建SSH通道实例
 * @author DFA Team
 * @since 1.0.0
 */
@Singleton
class SshChannelImpl @Inject constructor() : SshChannel {

    // ==================== 通道基础属性 ====================

    override val channelId: String = UUID.randomUUID().toString()
    override val channelType: ChannelType = ChannelType.SSH
    override val host: String
        get() = _config?.host ?: ""
    override val sshPort: Int
        get() = _config?.port ?: SshChannelConfig.DEFAULT_SSH_PORT
    override val username: String
        get() = _config?.username ?: ""

    // ==================== 内部状态 ====================

    private var _config: SshChannelConfig? = null
    private var _session: Session? = null
    private var _jsch: JSch? = null
    private var _receiveJob: Job? = null
    private var _reconnectJob: Job? = null
    private var _activeTunnels = mutableMapOf<String, com.jcraft.jsch.Channel>()
    private var _activeShells = mutableMapOf<String, SshShellImpl>()

    // ==================== 状态流 ====================

    private val _state = MutableStateFlow(CommunicationState.DISCONNECTED)
    private val _connectionInfo = MutableStateFlow(
        ConnectionInfo(
            channelId = channelId,
            type = channelType,
            state = CommunicationState.DISCONNECTED
        )
    )
    private val _receiveData = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64)
    private val _sessionState = MutableStateFlow(
        SshSessionState(
            isConnected = false,
            isAuthenticated = false
        )
    )

    private val scope = CoroutineScope(Dispatchers.IO)

    // ==================== 公开属性 ====================

    override val state: StateFlow<CommunicationState> = _state.asStateFlow()
    override val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo.asStateFlow()
    override val receiveData: Flow<ByteArray> = _receiveData.asSharedFlow()
    override val sessionState: Flow<SshSessionState> = _sessionState.asStateFlow()

    // ==================== 连接管理 ====================

    /**
     * 连接SSH服务器
     *
     * @param config 通道配置，必须是SshChannelConfig类型
     * @return 连接结果，成功返回ConnectionInfo，失败返回异常
     */
    override suspend fun connect(config: ChannelConfig): Result<ConnectionInfo> {
        if (config.type != ChannelType.SSH) {
            return Result.failure(
                CommunicationError.ConfigurationError("Invalid channel type: ${config.type}")
            )
        }

        val sshConfig = config as? SshChannelConfig
            ?: return Result.failure(
                CommunicationError.ConfigurationError("Config must be SshChannelConfig")
            )

        if (!sshConfig.validate()) {
            return Result.failure(
                CommunicationError.ConfigurationError("Invalid SSH configuration")
            )
        }

        return try {
            _state.value = CommunicationState.CONNECTING
            updateConnectionInfo { it.copy(state = CommunicationState.CONNECTING) }

            _config = sshConfig

            // 初始化JSch
            _jsch = JSch()

            // 配置认证方式
            setupAuthentication(sshConfig)

            // 创建会话
            val session = _jsch!!.getSession(sshConfig.username, sshConfig.host, sshConfig.port)
            configureSession(session, sshConfig)

            // 连接
            session.connect(sshConfig.timeoutConfig.connectionTimeoutMs.toInt())
            _session = session

            // 更新状态
            _state.value = CommunicationState.CONNECTED
            _sessionState.value = SshSessionState(
                isConnected = true,
                isAuthenticated = true,
                sessionId = sessionId,
                serverVersion = session.serverVersion,
                clientVersion = JSch.VERSION,
                activeChannels = 0,
                activeTunnels = 0
            )
            updateConnectionInfo {
                it.copy(
                    state = CommunicationState.CONNECTED,
                    connectedAt = System.currentTimeMillis()
                )
            }

            // 启动心跳
            startKeepAlive(sshConfig)

            Result.success(_connectionInfo.value)
        } catch (e: Exception) {
            val error = CommunicationError.ConnectionError("Failed to connect: ${e.message}")
            handleError(error)
            Result.failure(error)
        }
    }

    /**
     * 断开SSH连接
     *
     * @return 断开结果
     */
    override suspend fun disconnect(): Result<Unit> {
        return try {
            // 关闭所有Shell会话
            _activeShells.values.forEach { it.close() }
            _activeShells.clear()

            // 关闭所有隧道
            _activeTunnels.values.forEach { it.disconnect() }
            _activeTunnels.clear()

            // 停止后台任务
            stopReceiveLoop()
            stopReconnectLoop()

            // 断开会话
            _session?.disconnect()
            _session = null
            _jsch = null

            // 更新状态
            _state.value = CommunicationState.DISCONNECTED
            _sessionState.value = SshSessionState(
                isConnected = false,
                isAuthenticated = false
            )
            updateConnectionInfo {
                it.copy(
                    state = CommunicationState.DISCONNECTED,
                    connectedAt = null
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            val error = CommunicationError.ChannelError("Failed to disconnect: ${e.message}")
            Result.failure(error)
        }
    }

    /**
     * 发送数据
     *
     * SSH通道不支持直接发送原始数据，请使用executeCommand方法
     *
     * @param data 要发送的数据
     * @return 发送结果
     */
    override suspend fun send(data: ByteArray): Result<Unit> {
        return Result.failure(
            CommunicationError.ChannelError("SSH channel does not support raw data sending. Use executeCommand instead.")
        )
    }

    /**
     * 发送数据（带超时）
     *
     * SSH通道不支持直接发送原始数据，请使用executeCommand方法
     *
     * @param data 要发送的数据
     * @param timeoutMs 超时时间
     * @return 发送结果
     */
    override suspend fun send(data: ByteArray, timeoutMs: Long): Result<Unit> {
        return send(data)
    }

    /**
     * 检查是否已连接
     *
     * @return 是否已连接
     */
    override fun isConnected(): Boolean {
        return _state.value == CommunicationState.CONNECTED &&
               _session?.isConnected == true
    }

    /**
     * 获取当前连接信息
     *
     * @return 连接信息
     */
    override fun getConnectionInfo(): ConnectionInfo = _connectionInfo.value

    /**
     * 释放资源
     */
    override suspend fun release() {
        disconnect()
    }

    // ==================== 命令执行 ====================

    /**
     * 执行远程命令
     *
     * @param command 要执行的命令
     * @param timeoutMs 超时时间（毫秒）
     * @return 命令执行结果
     */
    override suspend fun executeCommand(
        command: String,
        timeoutMs: Long
    ): Result<SshCommandResult> {
        return executeCommand(command, emptyMap(), timeoutMs)
    }

    /**
     * 执行远程命令（带环境变量）
     *
     * @param command 要执行的命令
     * @param environment 环境变量
     * @param timeoutMs 超时时间（毫秒）
     * @return 命令执行结果
     */
    override suspend fun executeCommand(
        command: String,
        environment: Map<String, String>,
        timeoutMs: Long
    ): Result<SshCommandResult> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        return try {
            withTimeout(timeoutMs) {
                val startTime = System.currentTimeMillis()
                val session = _session!!

                // 创建执行通道
                val channel = session.openChannel("exec") as ChannelExec
                channel.command = command

                // 设置环境变量
                environment.forEach { (key, value) ->
                    channel.setEnv(key, value)
                }

                // 获取输出流
                val stdoutStream = ByteArrayOutputStream()
                val stderrStream = ByteArrayOutputStream()
                channel.outputStream = stdoutStream
                channel.errStream = stderrStream

                // 执行命令
                channel.connect(_config?.timeoutConfig?.connectionTimeoutMs?.toInt() ?: 30000)

                // 等待命令完成
                while (!channel.isClosed) {
                    delay(50)
                }

                val executionTime = System.currentTimeMillis() - startTime
                val result = SshCommandResult(
                    command = command,
                    exitCode = channel.exitStatus,
                    stdout = stdoutStream.toString("UTF-8"),
                    stderr = stderrStream.toString("UTF-8"),
                    executionTimeMs = executionTime,
                    signal = channel.exitSignal
                )

                channel.disconnect()

                // 更新统计
                updateConnectionInfo {
                    it.copy(
                        bytesSent = it.bytesSent + command.toByteArray().size,
                        bytesReceived = it.bytesReceived + result.output.toByteArray().size,
                        lastActivityAt = System.currentTimeMillis()
                    )
                }

                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Command execution failed: ${e.message}"))
        }
    }

    /**
     * 执行交互式命令
     *
     * @param command 要执行的命令
     * @param input 输入流
     * @param timeoutMs 超时时间（毫秒）
     * @return 命令执行结果
     */
    override suspend fun executeInteractiveCommand(
        command: String,
        input: Flow<ByteArray>,
        timeoutMs: Long
    ): Result<SshCommandResult> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        return try {
            withTimeout(timeoutMs) {
                val startTime = System.currentTimeMillis()
                val session = _session!!

                // 创建执行通道
                val channel = session.openChannel("exec") as ChannelExec
                channel.command = command
                channel.setPty(true) // 分配伪终端

                // 获取流
                val outputStream = channel.outputStream
                val stdoutStream = ByteArrayOutputStream()
                val stderrStream = ByteArrayOutputStream()
                channel.outputStream = stdoutStream
                channel.errStream = stderrStream

                // 连接
                channel.connect(_config?.timeoutConfig?.connectionTimeoutMs?.toInt() ?: 30000)

                // 发送输入
                val inputJob = scope.launch {
                    input.collect { data ->
                        outputStream.write(data)
                        outputStream.flush()
                    }
                }

                // 等待命令完成
                while (!channel.isClosed) {
                    delay(50)
                }

                inputJob.cancel()

                val executionTime = System.currentTimeMillis() - startTime
                val result = SshCommandResult(
                    command = command,
                    exitCode = channel.exitStatus,
                    stdout = stdoutStream.toString("UTF-8"),
                    stderr = stderrStream.toString("UTF-8"),
                    executionTimeMs = executionTime,
                    signal = channel.exitSignal
                )

                channel.disconnect()

                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Interactive command failed: ${e.message}"))
        }
    }

    // ==================== 文件传输 ====================

    /**
     * 上传文件
     *
     * @param localPath 本地文件路径
     * @param remotePath 远程文件路径
     * @param progress 进度回调
     * @return 上传结果
     */
    override suspend fun uploadFile(
        localPath: String,
        remotePath: String,
        progress: ((transferred: Long, total: Long) -> Unit)?
    ): Result<FileTransferResult> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        val localFile = File(localPath)
        if (!localFile.exists()) {
            return Result.failure(CommunicationError.ChannelError("Local file not found: $localPath"))
        }

        return try {
            val startTime = System.currentTimeMillis()
            val session = _session!!

            // 创建SFTP通道
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(_config?.timeoutConfig?.connectionTimeoutMs?.toInt() ?: 30000)

            val totalBytes = localFile.length()
            var transferredBytes = 0L

            // 使用进度监控器
            val monitor = object : com.jcraft.jsch.SftpProgressMonitor {
                override fun init(op: Int, src: String, dest: String, max: Long) {
                    // 初始化
                }

                override fun count(count: Long): Boolean {
                    transferredBytes += count
                    progress?.invoke(transferredBytes, totalBytes)
                    return true
                }

                override fun end() {
                    // 完成
                }
            }

            // 上传文件
            val inputStream = FileInputStream(localFile)
            channel.put(inputStream, remotePath, monitor, ChannelSftp.OVERWRITE)
            inputStream.close()

            val transferTime = System.currentTimeMillis() - startTime
            val result = FileTransferResult(
                sourcePath = localPath,
                destinationPath = remotePath,
                bytesTransferred = totalBytes,
                transferTimeMs = transferTime,
                averageSpeed = if (transferTime > 0) totalBytes * 1000 / transferTime else 0
            )

            channel.disconnect()

            Result.success(result)
        } catch (e: SftpException) {
            Result.failure(CommunicationError.ChannelError("SFTP upload failed: ${e.message} (id: ${e.id})"))
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("File upload failed: ${e.message}"))
        }
    }

    /**
     * 上传文件（从字节数组）
     *
     * @param data 文件数据
     * @param remotePath 远程文件路径
     * @param progress 进度回调
     * @return 上传结果
     */
    override suspend fun uploadFile(
        data: ByteArray,
        remotePath: String,
        progress: ((transferred: Long, total: Long) -> Unit)?
    ): Result<FileTransferResult> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        return try {
            val startTime = System.currentTimeMillis()
            val session = _session!!

            // 创建SFTP通道
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(_config?.timeoutConfig?.connectionTimeoutMs?.toInt() ?: 30000)

            val totalBytes = data.size.toLong()

            // 使用进度监控器
            val monitor = object : com.jcraft.jsch.SftpProgressMonitor {
                private var transferred = 0L
                override fun init(op: Int, src: String, dest: String, max: Long) {}
                override fun count(count: Long): Boolean {
                    transferred += count
                    progress?.invoke(transferred, totalBytes)
                    return true
                }
                override fun end() {}
            }

            // 上传
            val inputStream = data.inputStream()
            channel.put(inputStream, remotePath, monitor, ChannelSftp.OVERWRITE)
            inputStream.close()

            val transferTime = System.currentTimeMillis() - startTime
            val result = FileTransferResult(
                sourcePath = "<memory>",
                destinationPath = remotePath,
                bytesTransferred = totalBytes,
                transferTimeMs = transferTime,
                averageSpeed = if (transferTime > 0) totalBytes * 1000 / transferTime else 0
            )

            channel.disconnect()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("File upload failed: ${e.message}"))
        }
    }

    /**
     * 下载文件
     *
     * @param remotePath 远程文件路径
     * @param localPath 本地文件路径
     * @param progress 进度回调
     * @return 下载结果
     */
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String,
        progress: ((transferred: Long, total: Long) -> Unit)?
    ): Result<FileTransferResult> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        return try {
            val startTime = System.currentTimeMillis()
            val session = _session!!

            // 创建SFTP通道
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(_config?.timeoutConfig?.connectionTimeoutMs?.toInt() ?: 30000)

            // 获取文件大小
            val attrs = channel.stat(remotePath)
            val totalBytes = attrs.size

            // 创建本地文件
            val localFile = File(localPath)
            localFile.parentFile?.mkdirs()

            var transferredBytes = 0L

            // 使用进度监控器
            val monitor = object : com.jcraft.jsch.SftpProgressMonitor {
                override fun init(op: Int, src: String, dest: String, max: Long) {}
                override fun count(count: Long): Boolean {
                    transferredBytes += count
                    progress?.invoke(transferredBytes, totalBytes)
                    return true
                }
                override fun end() {}
            }

            // 下载文件
            val outputStream = FileOutputStream(localFile)
            channel.get(remotePath, outputStream, monitor)
            outputStream.close()

            val transferTime = System.currentTimeMillis() - startTime
            val result = FileTransferResult(
                sourcePath = remotePath,
                destinationPath = localPath,
                bytesTransferred = totalBytes,
                transferTimeMs = transferTime,
                averageSpeed = if (transferTime > 0) totalBytes * 1000 / transferTime else 0
            )

            channel.disconnect()

            Result.success(result)
        } catch (e: SftpException) {
            Result.failure(CommunicationError.ChannelError("SFTP download failed: ${e.message} (id: ${e.id})"))
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("File download failed: ${e.message}"))
        }
    }

    /**
     * 下载文件（到字节数组）
     *
     * @param remotePath 远程文件路径
     * @param progress 进度回调
     * @return 下载结果（包含文件数据）
     */
    override suspend fun downloadFile(
        remotePath: String,
        progress: ((transferred: Long, total: Long) -> Unit)?
    ): Result<FileDownloadResult> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        return try {
            val startTime = System.currentTimeMillis()
            val session = _session!!

            // 创建SFTP通道
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(_config?.timeoutConfig?.connectionTimeoutMs?.toInt() ?: 30000)

            // 获取文件大小
            val attrs = channel.stat(remotePath)
            val totalBytes = attrs.size

            var transferredBytes = 0L

            // 使用进度监控器
            val monitor = object : com.jcraft.jsch.SftpProgressMonitor {
                override fun init(op: Int, src: String, dest: String, max: Long) {}
                override fun count(count: Long): Boolean {
                    transferredBytes += count
                    progress?.invoke(transferredBytes, totalBytes)
                    return true
                }
                override fun end() {}
            }

            // 下载到内存
            val outputStream = ByteArrayOutputStream()
            channel.get(remotePath, outputStream, monitor)
            val data = outputStream.toByteArray()

            val transferTime = System.currentTimeMillis() - startTime
            val result = FileDownloadResult(
                remotePath = remotePath,
                data = data,
                bytesTransferred = data.size.toLong(),
                transferTimeMs = transferTime,
                averageSpeed = if (transferTime > 0) data.size * 1000L / transferTime else 0
            )

            channel.disconnect()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("File download failed: ${e.message}"))
        }
    }

    // ==================== 端口转发 ====================

    /**
     * 创建本地端口转发隧道
     *
     * 将本地端口转发到远程主机端口
     *
     * @param localPort 本地端口
     * @param remoteHost 远程主机地址
     * @param remotePort 远程端口
     * @return 隧道信息
     */
    override suspend fun createLocalTunnel(
        localPort: Int,
        remoteHost: String,
        remotePort: Int
    ): Result<SshTunnel> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        return try {
            val session = _session!!
            val tunnelId = UUID.randomUUID().toString()

            // 设置本地端口转发
            session.setPortForwardingL(localPort, remoteHost, remotePort)

            val tunnel = SshTunnel(
                tunnelId = tunnelId,
                type = SshTunnelType.LOCAL,
                localHost = "127.0.0.1",
                localPort = localPort,
                remoteHost = remoteHost,
                remotePort = remotePort,
                createdAt = System.currentTimeMillis()
            )

            _activeTunnels[tunnelId] = session.openChannel("direct-tcpip")
            updateSessionState { it.copy(activeTunnels = it.activeTunnels + 1) }

            Result.success(tunnel)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to create local tunnel: ${e.message}"))
        }
    }

    /**
     * 创建远程端口转发隧道
     *
     * 将远程端口转发到本地主机端口
     *
     * @param remotePort 远程端口
     * @param localHost 本地主机地址
     * @param localPort 本地端口
     * @return 隧道信息
     */
    override suspend fun createRemoteTunnel(
        remotePort: Int,
        localHost: String,
        localPort: Int
    ): Result<SshTunnel> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        return try {
            val session = _session!!
            val tunnelId = UUID.randomUUID().toString()

            // 设置远程端口转发
            session.setPortForwardingR(remotePort, localHost, localPort)

            val tunnel = SshTunnel(
                tunnelId = tunnelId,
                type = SshTunnelType.REMOTE,
                localHost = localHost,
                localPort = localPort,
                remoteHost = null,
                remotePort = remotePort,
                createdAt = System.currentTimeMillis()
            )

            _activeTunnels[tunnelId] = session.openChannel("forwarded-tcpip")
            updateSessionState { it.copy(activeTunnels = it.activeTunnels + 1) }

            Result.success(tunnel)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to create remote tunnel: ${e.message}"))
        }
    }

    /**
     * 创建动态端口转发（SOCKS代理）
     *
     * @param localPort 本地端口
     * @return 隧道信息
     */
    override suspend fun createDynamicTunnel(localPort: Int): Result<SshTunnel> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        return try {
            val session = _session!!
            val tunnelId = UUID.randomUUID().toString()

            // 设置动态端口转发（SOCKS代理）
            session.setPortForwardingD(localPort)

            val tunnel = SshTunnel(
                tunnelId = tunnelId,
                type = SshTunnelType.DYNAMIC,
                localHost = "127.0.0.1",
                localPort = localPort,
                remoteHost = null,
                remotePort = null,
                createdAt = System.currentTimeMillis()
            )

            _activeTunnels[tunnelId] = session.openChannel("dynamic")
            updateSessionState { it.copy(activeTunnels = it.activeTunnels + 1) }

            Result.success(tunnel)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to create dynamic tunnel: ${e.message}"))
        }
    }

    /**
     * 关闭端口转发隧道
     *
     * @param tunnelId 隧道ID
     * @return 关闭结果
     */
    override suspend fun closeTunnel(tunnelId: String): Result<Unit> {
        val channel = _activeTunnels.remove(tunnelId)
            ?: return Result.failure(CommunicationError.ChannelError("Tunnel not found: $tunnelId"))

        return try {
            channel.disconnect()
            updateSessionState { it.copy(activeTunnels = it.activeTunnels - 1) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to close tunnel: ${e.message}"))
        }
    }

    /**
     * 获取所有活动隧道
     *
     * @return 隧道列表
     */
    override suspend fun getActiveTunnels(): List<SshTunnel> {
        return _activeTunnels.entries.map { (id, channel) ->
            SshTunnel(
                tunnelId = id,
                type = SshTunnelType.LOCAL, // 简化处理
                localHost = "127.0.0.1",
                localPort = 0,
                remoteHost = null,
                remotePort = null,
                isActive = channel.isConnected
            )
        }
    }

    // ==================== Shell会话 ====================

    /**
     * 创建Shell会话
     *
     * @param terminalType 终端类型
     * @param cols 列数
     * @param rows 行数
     * @return Shell会话
     */
    override suspend fun createShell(
        terminalType: String,
        cols: Int,
        rows: Int
    ): Result<SshShell> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("SSH session not connected"))
        }

        return try {
            val session = _session!!
            val shellId = UUID.randomUUID().toString()

            // 创建Shell通道
            val channel = session.openChannel("shell") as ChannelShell
            channel.ptyType = terminalType
            channel.setPtySize(cols, rows, cols * 8, rows * 8)

            val shell = SshShellImpl(
                shellId = shellId,
                terminalType = terminalType,
                channel = channel,
                scope = scope
            )

            channel.connect(_config?.timeoutConfig?.connectionTimeoutMs?.toInt() ?: 30000)
            _activeShells[shellId] = shell
            updateSessionState { it.copy(activeChannels = it.activeChannels + 1) }

            Result.success(shell)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to create shell: ${e.message}"))
        }
    }

    // ==================== 服务器信息 ====================

    /**
     * 获取服务器指纹
     *
     * @return 服务器指纹信息
     */
    override suspend fun getServerFingerprint(): Result<SshServerFingerprint> {
        val session = _session
            ?: return Result.failure(CommunicationError.ChannelError("SSH session not connected"))

        return try {
            val hostKey = session.hostKey
            Result.success(
                SshServerFingerprint(
                    host = host,
                    port = sshPort,
                    keyType = hostKey.type,
                    fingerprint = hostKey.fingerPrint,
                    fingerprintAlgorithm = "SHA256"
                )
            )
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to get server fingerprint: ${e.message}"))
        }
    }

    /**
     * 检查服务器是否可达
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否可达
     */
    override suspend fun isServerReachable(timeoutMs: Long): Boolean {
        return try {
            withTimeout(timeoutMs) {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(host, sshPort), timeoutMs.toInt())
                socket.close()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取SSH连接信息
     *
     * @return SSH连接信息
     */
    override fun getSshConnectionInfo(): SshConnectionInfo {
        val session = _session
        return SshConnectionInfo(
            host = host,
            port = sshPort,
            username = username,
            authMethod = getAuthMethodName(),
            serverVersion = session?.serverVersion,
            sessionId = sessionId,
            connectedAt = _connectionInfo.value.connectedAt,
            lastActivityAt = _connectionInfo.value.lastActivityAt
        )
    }

    // ==================== 私有方法 ====================

    private val sessionId: String?
        get() = _session?.let { Integer.toHexString(it.hashCode()) }

    private fun setupAuthentication(config: SshChannelConfig) {
        when (val auth = config.authMethod) {
            is SshAuthMethod.Password -> {
                // 密码认证在session配置中设置
            }
            is SshAuthMethod.PublicKey -> {
                // 添加私钥
                if (auth.passphrase != null) {
                    _jsch?.addIdentity(
                        "key-${UUID.randomUUID()}",
                        auth.privateKey.toByteArray(),
                        null,
                        auth.passphrase.toByteArray()
                    )
                } else {
                    _jsch?.addIdentity(
                        "key-${UUID.randomUUID()}",
                        auth.privateKey.toByteArray(),
                        null,
                        null
                    )
                }
            }
            is SshAuthMethod.PublicKeyFile -> {
                // 从文件添加私钥
                if (auth.passphrase != null) {
                    _jsch?.addIdentity(auth.privateKeyPath, auth.passphrase)
                } else {
                    _jsch?.addIdentity(auth.privateKeyPath)
                }
            }
            is SshAuthMethod.KeyboardInteractive -> {
                // 键盘交互认证需要特殊处理
            }
        }
    }

    private fun configureSession(session: Session, config: SshChannelConfig) {
        // 设置密码（如果是密码认证）
        when (val auth = config.authMethod) {
            is SshAuthMethod.Password -> {
                session.password = auth.password
            }
            else -> {
                // 密钥认证已在setupAuthentication中配置
            }
        }

        // 配置属性
        val properties = Properties()

        // 严格主机密钥检查
        if (!config.securityConfig.strictHostKeyChecking) {
            properties["StrictHostKeyChecking"] = "no"
        }

        // 已知主机文件
        config.securityConfig.knownHostsPath?.let {
            _jsch?.knownHosts = it
        }

        // 首选算法
        if (config.securityConfig.preferredKeyExchangeAlgorithms.isNotEmpty()) {
            properties["kex"] = config.securityConfig.preferredKeyExchangeAlgorithms.joinToString(",")
        }
        if (config.securityConfig.preferredCipherAlgorithms.isNotEmpty()) {
            properties["cipher.s2c"] = config.securityConfig.preferredCipherAlgorithms.joinToString(",")
            properties["cipher.c2s"] = config.securityConfig.preferredCipherAlgorithms.joinToString(",")
        }
        if (config.securityConfig.preferredMacAlgorithms.isNotEmpty()) {
            properties["mac.s2c"] = config.securityConfig.preferredMacAlgorithms.joinToString(",")
            properties["mac.c2s"] = config.securityConfig.preferredMacAlgorithms.joinToString(",")
        }

        // 压缩
        if (config.securityConfig.compressionEnabled) {
            properties["compression.s2c"] = "zlib@openssh.com,zlib"
            properties["compression.c2s"] = "zlib@openssh.com,zlib"
        }

        session.setConfig(properties)

        // 设置超时
        session.timeout = config.timeoutConfig.connectionTimeoutMs.toInt()
    }

    private fun startKeepAlive(config: SshChannelConfig) {
        _receiveJob?.cancel()
        _receiveJob = scope.launch {
            while (isActive && isConnected()) {
                delay(config.timeoutConfig.keepAliveIntervalMs)
                try {
                    _session?.sendKeepAliveMsg()
                } catch (e: Exception) {
                    // 心跳失败，可能连接已断开
                    if (_config?.enableReconnect == true) {
                        startReconnectLoop()
                    }
                    break
                }
            }
        }
    }

    private fun stopReceiveLoop() {
        _receiveJob?.cancel()
        _receiveJob = null
    }

    private fun startReconnectLoop() {
        val config = _config ?: return
        if (!config.enableReconnect) return

        _state.value = CommunicationState.RECONNECTING
        updateConnectionInfo { it.copy(state = CommunicationState.RECONNECTING) }

        _reconnectJob?.cancel()
        _reconnectJob = scope.launch {
            var attempts = 0
            val maxAttempts = config.reconnectConfig.maxReconnectAttempts

            while (isActive && attempts < maxAttempts) {
                val delayMs = (config.reconnectConfig.reconnectDelayMs *
                    Math.pow(config.reconnectConfig.backoffMultiplier, attempts.toDouble())).toLong()
                    .coerceAtMost(config.reconnectConfig.maxReconnectDelayMs)

                delay(delayMs)
                attempts++

                try {
                    disconnect()
                    connect(config)
                    if (isConnected()) {
                        return@launch
                    }
                } catch (e: Exception) {
                    // 继续重试
                }
            }

            // 重连失败
            handleError(CommunicationError.ConnectionError("Reconnect failed after $attempts attempts"))
        }
    }

    private fun stopReconnectLoop() {
        _reconnectJob?.cancel()
        _reconnectJob = null
    }

    private fun handleError(error: CommunicationError) {
        _state.value = CommunicationState.ERROR
        _sessionState.value = SshSessionState(
            isConnected = false,
            isAuthenticated = false
        )
        updateConnectionInfo {
            it.copy(
                state = CommunicationState.ERROR,
                errorMessage = error.message
            )
        }
    }

    private inline fun updateConnectionInfo(update: (ConnectionInfo) -> ConnectionInfo) {
        _connectionInfo.value = update(_connectionInfo.value)
    }

    private inline fun updateSessionState(update: (SshSessionState) -> SshSessionState) {
        _sessionState.value = update(_sessionState.value)
    }

    private fun getAuthMethodName(): String {
        return when (_config?.authMethod) {
            is SshAuthMethod.Password -> "password"
            is SshAuthMethod.PublicKey -> "publickey"
            is SshAuthMethod.PublicKeyFile -> "publickey-file"
            is SshAuthMethod.KeyboardInteractive -> "keyboard-interactive"
            null -> "unknown"
        }
    }
}

/**
 * SSH Shell会话实现
 *
 * @property shellId Shell ID
 * @property terminalType 终端类型
 * @property channel JSch Shell通道
 * @property scope 协程作用域
 */
private class SshShellImpl(
    override val shellId: String,
    override val terminalType: String,
    private val channel: ChannelShell,
    private val scope: CoroutineScope
) : SshShell {

    private val _output = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64)
    private var _isOpen = true
    private var _receiveJob: Job? = null

    override val output: Flow<ByteArray> = _output
    override val isOpen: Boolean
        get() = _isOpen && channel.isConnected

    init {
        startOutputLoop()
    }

    private fun startOutputLoop() {
        _receiveJob = scope.launch {
            val buffer = ByteArray(4096)
            val inputStream = channel.inputStream

            while (isActive && channel.isConnected) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        _output.emit(buffer.copyOf(bytesRead))
                    } else if (bytesRead == -1) {
                        break
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        break
                    }
                }
            }
        }
    }

    override suspend fun sendInput(data: ByteArray): Result<Unit> {
        if (!isOpen) {
            return Result.failure(CommunicationError.ChannelError("Shell is not open"))
        }

        return try {
            channel.outputStream.write(data)
            channel.outputStream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to send input: ${e.message}"))
        }
    }

    override suspend fun sendInput(text: String): Result<Unit> {
        return sendInput(text.toByteArray(Charsets.UTF_8))
    }

    override suspend fun resize(cols: Int, rows: Int, width: Int, height: Int): Result<Unit> {
        if (!isOpen) {
            return Result.failure(CommunicationError.ChannelError("Shell is not open"))
        }

        return try {
            channel.setPtySize(cols, rows, width, height)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to resize: ${e.message}"))
        }
    }

    override suspend fun close() {
        _receiveJob?.cancel()
        channel.disconnect()
        _isOpen = false
    }
}