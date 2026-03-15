package com.dfa.core.vm.channel

import android.net.LocalSocket
import android.net.LocalSocketAddress
import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationError
import com.dfa.core.vm.communication.CommunicationState
import com.dfa.core.vm.communication.ConnectionInfo
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
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket通道实现
 *
 * 支持Unix Domain Socket和TCP Socket的通信通道实现。
 * 继承自SocketChannel接口，提供跨平台的Socket通信能力。
 *
 * ## 功能特性
 * - Unix Domain Socket支持（Android API 21+，使用LocalSocket）
 * - TCP Socket支持
 * - 异步数据发送/接收
 * - 连接管理和自动重连
 * - 消息监听器回调
 * - Socket选项配置
 *
 * ## 使用示例
 * ```kotlin
 * // Unix Domain Socket
 * val unixConfig = SocketConfig.unixDefault(path = "/tmp/dfa.sock")
 * val unixResult = socketChannel.connect(unixConfig)
 *
 * // TCP Socket
 * val tcpConfig = SocketConfig.tcpDefault(host = "127.0.0.1", port = 8080)
 * val tcpResult = socketChannel.connect(tcpConfig)
 *
 * // 发送数据
 * socketChannel.send("Hello".toByteArray())
 *
 * // 设置消息监听器
 * socketChannel.setMessageListener(object : SocketChannel.MessageListener {
 *     override fun onMessage(data: ByteArray) {
 *         println("Received: ${String(data)}")
 *     }
 * })
 * ```
 *
 * @constructor 创建Socket通道实例
 * @author DFA Team
 * @since 1.0.0
 */
@Singleton
class SocketChannelImpl @Inject constructor() : SocketChannel {

    // ==================== 通道基础属性 ====================

    override val channelId: String = UUID.randomUUID().toString()
    override val channelType: ChannelType
        get() = when (_config?.socketType) {
            is SocketType.Unix -> ChannelType.SOCKET_UNIX
            is SocketType.Tcp -> ChannelType.SOCKET_TCP
            else -> ChannelType.SOCKET_UNIX
        }

    override val socketType: SocketType
        get() = _config?.socketType ?: SocketType.Unix(SocketType.Unix.DEFAULT_PATH)

    override val socketAddress: String
        get() = _config?.getConnectionAddress() ?: ""

    // ==================== 内部状态 ====================

    private var _config: SocketConfig? = null
    private var _tcpSocket: Socket? = null
    private var _localSocket: LocalSocket? = null
    private var _inputStream: java.io.InputStream? = null
    private var _outputStream: java.io.OutputStream? = null
    private var _receiveJob: Job? = null
    private var _reconnectJob: Job? = null
    private var _messageListener: SocketChannel.MessageListener? = null

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
    private val _socketOptions = MutableStateFlow(SocketOptions.DEFAULT)
    private val _channelState = MutableStateFlow(SocketChannelState.EMPTY)

    private val scope = CoroutineScope(Dispatchers.IO)

    // ==================== 公开属性 ====================

    override val state: StateFlow<CommunicationState> = _state.asStateFlow()
    override val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo.asStateFlow()
    override val receiveData: Flow<ByteArray> = _receiveData.asSharedFlow()
    override val socketOptions: StateFlow<SocketOptions> = _socketOptions.asStateFlow()

    // ==================== 连接管理 ====================

    /**
     * 连接Socket通道
     *
     * 根据配置的Socket类型（Unix Domain Socket或TCP Socket）建立连接。
     *
     * @param config 通道配置，必须是SocketConfig类型
     * @return 连接结果，成功返回ConnectionInfo，失败返回异常
     */
    override suspend fun connect(config: ChannelConfig): Result<ConnectionInfo> {
        val socketConfig = config as? SocketConfig
            ?: return Result.failure(
                CommunicationError.ConfigurationError("Config must be SocketConfig")
            )

        if (!socketConfig.validateConfig()) {
            return Result.failure(
                CommunicationError.ConfigurationError("Invalid Socket configuration")
            )
        }

        return try {
            _state.value = CommunicationState.CONNECTING
            updateConnectionInfo { it.copy(state = CommunicationState.CONNECTING) }

            _config = socketConfig

            // 根据Socket类型连接
            when (socketConfig.socketType) {
                is SocketType.Unix -> connectUnixSocket(socketConfig)
                is SocketType.Tcp -> connectTcpSocket(socketConfig)
            }

            // 应用Socket选项
            applySocketOptions(socketConfig)

            // 启动接收循环
            startReceiveLoop()

            // 更新状态
            _state.value = CommunicationState.CONNECTED
            _channelState.value = _channelState.value.copy(
                isConnected = true,
                isConnecting = false,
                localAddress = getLocalAddress(),
                remoteAddress = getRemoteAddress()
            )
            updateConnectionInfo {
                it.copy(
                    type = channelType,
                    state = CommunicationState.CONNECTED,
                    connectedAt = System.currentTimeMillis()
                )
            }

            // 通知监听器
            _messageListener?.onStateChanged(CommunicationState.CONNECTED)

            Result.success(_connectionInfo.value)
        } catch (e: CommunicationError) {
            handleError(e)
            Result.failure(e)
        } catch (e: Exception) {
            val error = CommunicationError.ConnectionError("Failed to connect: ${e.message}")
            handleError(error)
            Result.failure(error)
        }
    }

    /**
     * 断开Socket连接
     *
     * 关闭所有流和Socket连接，释放资源。
     *
     * @return 断开结果
     */
    override suspend fun disconnect(): Result<Unit> {
        return try {
            stopReceiveLoop()
            stopReconnectLoop()

            // 关闭流
            _outputStream?.close()
            _inputStream?.close()

            // 关闭Socket
            _tcpSocket?.close()
            _localSocket?.close()

            // 清空引用
            _outputStream = null
            _inputStream = null
            _tcpSocket = null
            _localSocket = null

            // 更新状态
            _state.value = CommunicationState.DISCONNECTED
            _channelState.value = SocketChannelState.EMPTY
            updateConnectionInfo {
                it.copy(
                    state = CommunicationState.DISCONNECTED,
                    connectedAt = null
                )
            }

            // 通知监听器
            _messageListener?.onStateChanged(CommunicationState.DISCONNECTED)

            Result.success(Unit)
        } catch (e: Exception) {
            val error = CommunicationError.ChannelError("Failed to disconnect: ${e.message}")
            Result.failure(error)
        }
    }

    // ==================== 数据发送/接收 ====================

    /**
     * 发送数据
     *
     * 通过Socket发送字节数据。
     *
     * @param data 要发送的数据字节数组
     * @return 发送结果
     */
    override suspend fun send(data: ByteArray): Result<Unit> {
        val config = _config ?: return Result.failure(
            CommunicationError.ChannelError("Channel not connected")
        )
        return send(data, config.timeoutMs)
    }

    /**
     * 发送数据（带超时）
     *
     * 通过Socket发送字节数据，支持超时控制。
     *
     * @param data 要发送的数据字节数组
     * @param timeoutMs 超时时间（毫秒）
     * @return 发送结果
     */
    override suspend fun send(data: ByteArray, timeoutMs: Long): Result<Unit> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("Channel not connected"))
        }

        return try {
            withTimeout(timeoutMs) {
                _outputStream?.write(data)
                _outputStream?.flush()
            }

            // 更新统计
            val now = System.currentTimeMillis()
            _channelState.value = _channelState.value.copy(
                bytesSent = _channelState.value.bytesSent + data.size,
                messagesSent = _channelState.value.messagesSent + 1,
                lastSentAt = now
            )
            updateConnectionInfo {
                it.copy(
                    bytesSent = it.bytesSent + data.size,
                    lastActivityAt = now
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            val error = CommunicationError.ChannelError("Failed to send data: ${e.message}")
            _messageListener?.onError(error)
            Result.failure(error)
        }
    }

    /**
     * 接收数据
     *
     * 从Socket接收数据，支持超时控制。
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 接收到的数据，超时或失败返回null
     */
    override suspend fun receive(timeoutMs: Long): Result<ByteArray?> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("Channel not connected"))
        }

        return try {
            val buffer = ByteBuffer.allocate(_config?.bufferSize ?: ChannelConfig.DEFAULT_BUFFER_SIZE)
            val byteArray = ByteArray(buffer.capacity())

            withTimeout(timeoutMs) {
                val bytesRead = _inputStream?.read(byteArray) ?: -1
                if (bytesRead > 0) {
                    val data = byteArray.copyOf(bytesRead)

                    // 更新统计
                    val now = System.currentTimeMillis()
                    _channelState.value = _channelState.value.copy(
                        bytesReceived = _channelState.value.bytesReceived + bytesRead,
                        messagesReceived = _channelState.value.messagesReceived + 1,
                        lastReceivedAt = now
                    )
                    updateConnectionInfo {
                        it.copy(
                            bytesReceived = it.bytesReceived + bytesRead,
                            lastActivityAt = now
                        )
                    }

                    Result.success(data)
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            val error = CommunicationError.ChannelError("Failed to receive data: ${e.message}")
            _messageListener?.onError(error)
            Result.failure(error)
        }
    }

    // ==================== 连接状态查询 ====================

    /**
     * 检查是否已连接
     *
     * @return 是否已连接
     */
    override fun isConnected(): Boolean {
        return when {
            _tcpSocket != null -> _state.value == CommunicationState.CONNECTED &&
                    _tcpSocket?.isConnected == true &&
                    _tcpSocket?.isClosed == false
            _localSocket != null -> _state.value == CommunicationState.CONNECTED &&
                    _localSocket?.isConnected == true
            else -> false
        }
    }

    /**
     * 获取当前连接信息
     *
     * @return 连接信息
     */
    override fun getConnectionInfo(): ConnectionInfo = _connectionInfo.value

    // ==================== 消息监听器 ====================

    /**
     * 设置消息监听器
     *
     * 用于异步接收消息通知。
     *
     * @param listener 消息监听器回调
     */
    override fun setMessageListener(listener: SocketChannel.MessageListener?) {
        _messageListener = listener
    }

    // ==================== Socket选项管理 ====================

    /**
     * 设置Socket选项
     *
     * @param options Socket选项配置
     * @return 设置结果
     */
    override suspend fun setSocketOptions(options: SocketOptions): Result<Unit> {
        return try {
            _tcpSocket?.apply {
                keepAlive = options.keepAlive
                tcpNoDelay = options.tcpNoDelay
                sendBufferSize = options.sendBufferSize
                receiveBufferSize = options.receiveBufferSize
                soTimeout = options.soTimeout
                reuseAddress = options.reuseAddress
                oobInline = options.oobInline
                if (options.soLinger >= 0) {
                    setSoLinger(true, options.soLinger)
                }
                trafficClass = options.trafficClass
            }

            _socketOptions.value = options
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to set socket options: ${e.message}"))
        }
    }

    /**
     * 获取当前Socket选项
     *
     * @return 当前Socket选项
     */
    override fun getSocketOptions(): SocketOptions = _socketOptions.value

    /**
     * 设置读取超时
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 设置结果
     */
    override suspend fun setReadTimeout(timeoutMs: Long): Result<Unit> {
        return try {
            _tcpSocket?.soTimeout = timeoutMs.toInt()
            _socketOptions.value = _socketOptions.value.copy(soTimeout = timeoutMs.toInt())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to set read timeout: ${e.message}"))
        }
    }

    /**
     * 设置写入超时
     *
     * 注意：Java Socket不支持直接设置写入超时，此方法保留用于未来扩展。
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 设置结果
     */
    override suspend fun setWriteTimeout(timeoutMs: Long): Result<Unit> {
        // Java Socket不直接支持写入超时，记录配置供后续使用
        return Result.success(Unit)
    }

    /**
     * 刷新发送缓冲区
     *
     * @return 刷新结果
     */
    override suspend fun flush(): Result<Unit> {
        return try {
            _outputStream?.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to flush: ${e.message}"))
        }
    }

    /**
     * 检查Socket是否可达
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否可达
     */
    override suspend fun isReachable(timeoutMs: Long): Boolean {
        return try {
            when (val type = _config?.socketType) {
                is SocketType.Tcp -> {
                    val testSocket = Socket()
                    testSocket.connect(InetSocketAddress(type.host, type.port), timeoutMs.toInt())
                    testSocket.close()
                    true
                }
                is SocketType.Unix -> {
                    File(type.path).exists()
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取本地Socket地址
     *
     * @return 本地地址字符串，未连接时返回null
     */
    override fun getLocalAddress(): String? {
        return when {
            _tcpSocket != null -> {
                val localAddr = _tcpSocket?.localSocketAddress
                localAddr?.toString()
            }
            _localSocket != null -> {
                _localSocket?.localSocketAddress?.name
            }
            else -> null
        }
    }

    /**
     * 获取远程Socket地址
     *
     * @return 远程地址字符串，未连接时返回null
     */
    override fun getRemoteAddress(): String? {
        return when {
            _tcpSocket != null -> {
                val remoteAddr = _tcpSocket?.remoteSocketAddress
                remoteAddr?.toString()
            }
            _localSocket != null -> {
                _localSocket?.remoteSocketAddress?.name
            }
            else -> null
        }
    }

    /**
     * 释放资源
     */
    override suspend fun release() {
        disconnect()
        _messageListener = null
    }

    // ==================== 私有方法 ====================

    /**
     * 连接Unix Domain Socket
     *
     * 使用Android LocalSocket连接Unix域套接字。
     *
     * @param config Socket配置
     */
    private fun connectUnixSocket(config: SocketConfig) {
        val unixType = config.socketType as SocketType.Unix
        val path = unixType.path

        // 检查Socket文件是否存在（对于服务端创建的Socket）
        val socketFile = File(path)
        if (!socketFile.exists()) {
            throw CommunicationError.ConnectionError("Unix socket file not found: $path")
        }

        try {
            // 使用Android LocalSocket
            val localSocket = LocalSocket()
            val address = LocalSocketAddress(
                path.substringAfterLast('/'),
                LocalSocketAddress.Namespace.FILESYSTEM
            )
            localSocket.connect(address)

            _localSocket = localSocket
            _inputStream = localSocket.inputStream
            _outputStream = localSocket.outputStream
        } catch (e: Exception) {
            throw CommunicationError.ConnectionError("Failed to connect Unix socket: ${e.message}")
        }
    }

    /**
     * 连接TCP Socket
     *
     * 使用Java Socket连接TCP套接字。
     *
     * @param config Socket配置
     */
    private fun connectTcpSocket(config: SocketConfig) {
        val tcpType = config.socketType as SocketType.Tcp
        val host = tcpType.host
        val port = tcpType.port

        try {
            val socket = Socket()
            socket.connect(
                InetSocketAddress(host, port),
                config.connectionTimeoutMs.toInt()
            )

            _tcpSocket = socket
            _inputStream = socket.getInputStream()
            _outputStream = socket.getOutputStream()
        } catch (e: Exception) {
            throw CommunicationError.ConnectionError("Failed to connect TCP socket: ${e.message}")
        }
    }

    /**
     * 应用Socket选项
     *
     * @param config Socket配置
     */
    private fun applySocketOptions(config: SocketConfig) {
        _tcpSocket?.apply {
            keepAlive = config.keepAlive
            tcpNoDelay = config.tcpNoDelay
            sendBufferSize = config.sendBufferSize
            receiveBufferSize = config.receiveBufferSize
            soTimeout = config.readTimeoutMs.toInt()
            reuseAddress = true
        }

        _socketOptions.value = SocketOptions(
            keepAlive = config.keepAlive,
            tcpNoDelay = config.tcpNoDelay,
            sendBufferSize = config.sendBufferSize,
            receiveBufferSize = config.receiveBufferSize,
            soTimeout = config.readTimeoutMs.toInt(),
            soLinger = config.soLinger,
            trafficClass = config.trafficClass
        )
    }

    /**
     * 启动接收循环
     *
     * 在后台协程中持续接收数据并通过Flow和监听器分发。
     */
    private fun startReceiveLoop() {
        _receiveJob?.cancel()
        _receiveJob = scope.launch {
            val buffer = ByteBuffer.allocate(_config?.bufferSize ?: ChannelConfig.DEFAULT_BUFFER_SIZE)
            val byteArray = ByteArray(buffer.capacity())

            while (isActive && isConnected()) {
                try {
                    val bytesRead = _inputStream?.read(byteArray) ?: -1
                    if (bytesRead > 0) {
                        val data = byteArray.copyOf(bytesRead)

                        // 发送到Flow
                        _receiveData.emit(data)

                        // 更新统计
                        val now = System.currentTimeMillis()
                        _channelState.value = _channelState.value.copy(
                            bytesReceived = _channelState.value.bytesReceived + bytesRead,
                            messagesReceived = _channelState.value.messagesReceived + 1,
                            lastReceivedAt = now
                        )
                        updateConnectionInfo {
                            it.copy(
                                bytesReceived = it.bytesReceived + bytesRead,
                                lastActivityAt = now
                            )
                        }

                        // 通知监听器
                        _messageListener?.onMessage(data)
                    } else if (bytesRead == -1) {
                        // 连接已关闭
                        handleError(CommunicationError.ConnectionError("Connection closed by remote"))
                        break
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        handleError(CommunicationError.ChannelError("Receive error: ${e.message}"))
                        if (_config?.enableReconnect == true) {
                            startReconnectLoop()
                        }
                        break
                    }
                }
            }
        }
    }

    /**
     * 停止接收循环
     */
    private fun stopReceiveLoop() {
        _receiveJob?.cancel()
        _receiveJob = null
    }

    /**
     * 启动重连循环
     */
    private fun startReconnectLoop() {
        val config = _config ?: return
        if (!config.enableReconnect) return

        _state.value = CommunicationState.RECONNECTING
        _channelState.value = _channelState.value.copy(isConnecting = true)
        updateConnectionInfo { it.copy(state = CommunicationState.RECONNECTING) }
        _messageListener?.onStateChanged(CommunicationState.RECONNECTING)

        _reconnectJob?.cancel()
        _reconnectJob = scope.launch {
            var attempts = 0
            while (isActive && attempts < config.maxReconnectAttempts) {
                delay(config.reconnectDelayMs * (attempts + 1))
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

    /**
     * 停止重连循环
     */
    private fun stopReconnectLoop() {
        _reconnectJob?.cancel()
        _reconnectJob = null
    }

    /**
     * 处理错误
     *
     * @param error 通信错误
     */
    private fun handleError(error: CommunicationError) {
        _state.value = CommunicationState.ERROR
        _channelState.value = _channelState.value.copy(
            isConnected = false,
            isConnecting = false,
            errorCount = _channelState.value.errorCount + 1,
            lastError = error.message
        )
        updateConnectionInfo {
            it.copy(
                state = CommunicationState.ERROR,
                errorMessage = error.message
            )
        }
        _messageListener?.onError(error)
        _messageListener?.onStateChanged(CommunicationState.ERROR)
    }

    /**
     * 更新连接信息
     *
     * @param update 更新函数
     */
    private inline fun updateConnectionInfo(update: (ConnectionInfo) -> ConnectionInfo) {
        _connectionInfo.value = update(_connectionInfo.value)
    }
}

/**
 * Socket通道工厂实现
 *
 * 用于创建不同类型的Socket通道实例。
 *
 * @constructor 创建工厂实例
 * @author DFA Team
 * @since 1.0.0
 */
class SocketChannelFactoryImpl @Inject constructor() : SocketChannelFactory {

    /**
     * 创建Unix Domain Socket通道
     *
     * @param config Socket配置
     * @return Socket通道实例
     */
    override fun createUnixChannel(config: SocketConfig): SocketChannel {
        return SocketChannelImpl()
    }

    /**
     * 创建TCP Socket通道
     *
     * @param config Socket配置
     * @return Socket通道实例
     */
    override fun createTcpChannel(config: SocketConfig): SocketChannel {
        return SocketChannelImpl()
    }

    /**
     * 创建Socket通道
     *
     * @param type Socket类型
     * @param config Socket配置
     * @return Socket通道实例
     */
    override fun createChannel(type: SocketType, config: SocketConfig): SocketChannel {
        return SocketChannelImpl()
    }

    /**
     * 检查是否支持指定Socket类型
     *
     * @param type Socket类型
     * @return 是否支持
     */
    override fun isSupported(type: SocketType): Boolean {
        return when (type) {
            is SocketType.Unix -> true // Android API 21+支持LocalSocket
            is SocketType.Tcp -> true
        }
    }
}