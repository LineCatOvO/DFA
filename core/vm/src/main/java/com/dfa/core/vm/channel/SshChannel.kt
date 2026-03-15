package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationChannel
import com.dfa.core.vm.communication.CommunicationState
import com.dfa.core.vm.communication.ConnectionInfo
import kotlinx.coroutines.flow.Flow

/**
 * SSH通道接口
 *
 * 专门用于SSH通信的接口扩展，提供命令执行、文件传输和端口转发功能
 */
interface SshChannel : CommunicationChannel {

    /**
     * SSH服务器主机地址
     */
    val host: String

    /**
     * SSH服务器端口
     */
    val sshPort: Int

    /**
     * 当前登录用户名
     */
    val username: String

    /**
     * SSH会话状态流
     */
    val sessionState: Flow<SshSessionState>

    /**
     * 执行远程命令
     *
     * @param command 要执行的命令
     * @param timeoutMs 超时时间（毫秒）
     * @return 命令执行结果
     */
    suspend fun executeCommand(
        command: String,
        timeoutMs: Long = ChannelConfig.DEFAULT_TIMEOUT_MS
    ): Result<SshCommandResult>

    /**
     * 执行远程命令（带环境变量）
     *
     * @param command 要执行的命令
     * @param environment 环境变量
     * @param timeoutMs 超时时间（毫秒）
     * @return 命令执行结果
     */
    suspend fun executeCommand(
        command: String,
        environment: Map<String, String>,
        timeoutMs: Long = ChannelConfig.DEFAULT_TIMEOUT_MS
    ): Result<SshCommandResult>

    /**
     * 执行交互式命令
     *
     * @param command 要执行的命令
     * @param input 输入流
     * @param timeoutMs 超时时间（毫秒）
     * @return 命令执行结果
     */
    suspend fun executeInteractiveCommand(
        command: String,
        input: Flow<ByteArray>,
        timeoutMs: Long = ChannelConfig.DEFAULT_TIMEOUT_MS
    ): Result<SshCommandResult>

    /**
     * 上传文件
     *
     * @param localPath 本地文件路径
     * @param remotePath 远程文件路径
     * @param progress 进度回调（可选）
     * @return 上传结果
     */
    suspend fun uploadFile(
        localPath: String,
        remotePath: String,
        progress: ((transferred: Long, total: Long) -> Unit)? = null
    ): Result<FileTransferResult>

    /**
     * 上传文件（从字节数组）
     *
     * @param data 文件数据
     * @param remotePath 远程文件路径
     * @param progress 进度回调（可选）
     * @return 上传结果
     */
    suspend fun uploadFile(
        data: ByteArray,
        remotePath: String,
        progress: ((transferred: Long, total: Long) -> Unit)? = null
    ): Result<FileTransferResult>

    /**
     * 下载文件
     *
     * @param remotePath 远程文件路径
     * @param localPath 本地文件路径
     * @param progress 进度回调（可选）
     * @return 下载结果
     */
    suspend fun downloadFile(
        remotePath: String,
        localPath: String,
        progress: ((transferred: Long, total: Long) -> Unit)? = null
    ): Result<FileTransferResult>

    /**
     * 下载文件（到字节数组）
     *
     * @param remotePath 远程文件路径
     * @param progress 进度回调（可选）
     * @return 下载结果（包含文件数据）
     */
    suspend fun downloadFile(
        remotePath: String,
        progress: ((transferred: Long, total: Long) -> Unit)? = null
    ): Result<FileDownloadResult>

    /**
     * 创建端口转发隧道（本地转发）
     *
     * 将本地端口转发到远程主机端口
     *
     * @param localPort 本地端口
     * @param remoteHost 远程主机地址
     * @param remotePort 远程端口
     * @return 隧道信息
     */
    suspend fun createLocalTunnel(
        localPort: Int,
        remoteHost: String,
        remotePort: Int
    ): Result<SshTunnel>

    /**
     * 创建端口转发隧道（远程转发）
     *
     * 将远程端口转发到本地主机端口
     *
     * @param remotePort 远程端口
     * @param localHost 本地主机地址
     * @param localPort 本地端口
     * @return 隧道信息
     */
    suspend fun createRemoteTunnel(
        remotePort: Int,
        localHost: String,
        localPort: Int
    ): Result<SshTunnel>

    /**
     * 创建动态端口转发（SOCKS代理）
     *
     * @param localPort 本地端口
     * @return 隧道信息
     */
    suspend fun createDynamicTunnel(
        localPort: Int
    ): Result<SshTunnel>

    /**
     * 关闭端口转发隧道
     *
     * @param tunnelId 隧道ID
     * @return 关闭结果
     */
    suspend fun closeTunnel(tunnelId: String): Result<Unit>

    /**
     * 获取所有活动隧道
     *
     * @return 隧道列表
     */
    suspend fun getActiveTunnels(): List<SshTunnel>

    /**
     * 创建Shell会话
     *
     * @param terminalType 终端类型
     * @param cols 列数
     * @param rows 行数
     * @return Shell会话
     */
    suspend fun createShell(
        terminalType: String = "xterm-256color",
        cols: Int = 80,
        rows: Int = 24
    ): Result<SshShell>

    /**
     * 获取服务器指纹
     *
     * @return 服务器指纹信息
     */
    suspend fun getServerFingerprint(): Result<SshServerFingerprint>

    /**
     * 检查服务器是否可达
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否可达
     */
    suspend fun isServerReachable(timeoutMs: Long = 5000): Boolean

    /**
     * 获取SSH连接信息
     *
     * @return SSH连接信息
     */
    fun getSshConnectionInfo(): SshConnectionInfo
}

/**
 * SSH会话状态
 */
data class SshSessionState(
    val isConnected: Boolean = false,
    val isAuthenticated: Boolean = false,
    val sessionId: String? = null,
    val serverVersion: String? = null,
    val clientVersion: String? = null,
    val activeChannels: Int = 0,
    val activeTunnels: Int = 0,
    val lastActivityAt: Long = System.currentTimeMillis()
)

/**
 * SSH命令执行结果
 */
data class SshCommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long,
    val signal: String? = null
) {
    val isSuccess: Boolean
        get() = exitCode == 0

    val output: String
        get() = stdout + stderr
}

/**
 * 文件传输结果
 */
data class FileTransferResult(
    val sourcePath: String,
    val destinationPath: String,
    val bytesTransferred: Long,
    val transferTimeMs: Long,
    val averageSpeed: Long // bytes per second
)

/**
 * 文件下载结果
 */
data class FileDownloadResult(
    val remotePath: String,
    val data: ByteArray,
    val bytesTransferred: Long,
    val transferTimeMs: Long,
    val averageSpeed: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileDownloadResult) return false
        return remotePath == other.remotePath &&
               data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = remotePath.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * SSH隧道类型
 */
enum class SshTunnelType {
    LOCAL,      // 本地端口转发 (-L)
    REMOTE,     // 远程端口转发 (-R)
    DYNAMIC     // 动态端口转发/SOCKS代理 (-D)
}

/**
 * SSH隧道信息
 */
data class SshTunnel(
    val tunnelId: String,
    val type: SshTunnelType,
    val localHost: String,
    val localPort: Int,
    val remoteHost: String?,
    val remotePort: Int?,
    val createdAt: Long = System.currentTimeMillis(),
    val bytesTransferred: Long = 0,
    val isActive: Boolean = true
)

/**
 * SSH Shell会话
 */
interface SshShell {
    /**
     * Shell ID
     */
    val shellId: String

    /**
     * 终端类型
     */
    val terminalType: String

    /**
     * 输出流
     */
    val output: Flow<ByteArray>

    /**
     * 是否已打开
     */
    val isOpen: Boolean

    /**
     * 发送输入
     *
     * @param data 输入数据
     * @return 发送结果
     */
    suspend fun sendInput(data: ByteArray): Result<Unit>

    /**
     * 发送输入（字符串）
     *
     * @param text 输入文本
     * @return 发送结果
     */
    suspend fun sendInput(text: String): Result<Unit>

    /**
     * 调整终端大小
     *
     * @param cols 列数
     * @param rows 行数
     * @param width 像素宽度
     * @param height 像素高度
     * @return 调整结果
     */
    suspend fun resize(
        cols: Int,
        rows: Int,
        width: Int = 0,
        height: Int = 0
    ): Result<Unit>

    /**
     * 关闭Shell
     */
    suspend fun close()
}

/**
 * SSH服务器指纹
 */
data class SshServerFingerprint(
    val host: String,
    val port: Int,
    val keyType: String,
    val fingerprint: String,
    val fingerprintAlgorithm: String = "SHA256"
)

/**
 * SSH连接信息
 */
data class SshConnectionInfo(
    val host: String,
    val port: Int,
    val username: String,
    val authMethod: String,
    val serverVersion: String? = null,
    val sessionId: String? = null,
    val connectedAt: Long? = null,
    val lastActivityAt: Long = System.currentTimeMillis()
)