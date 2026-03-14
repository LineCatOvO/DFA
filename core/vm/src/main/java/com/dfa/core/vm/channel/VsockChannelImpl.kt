package com.dfa.core.vm.channel

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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject

/**
 * Vsock套接字通道实现
 *
 * 通过Vsock套接字实现虚拟机与宿主之间的通信
 * 注意：Android上的Vsock需要通过特定的设备文件或API访问
 */
class VsockChannelImpl @Inject constructor() : VsockChannel {

    override val channelId: String = UUID.randomUUID().toString()
    override val channelType: ChannelType = ChannelType.VSOCK
    override val vsockPort: Int
        get() = _config?.port ?: 0
    override val hostCid: Int
        get() = _config?.hostCid ?: VsockChannelConfig.VMADDR_CID_HOST
    override val clientCid: Int
        get() = _config?.clientCid ?: VsockChannelConfig.VMADDR_CID_ANY

    private var _config: VsockChannelConfig? = null
    private var _socket: Socket? = null
    private var _inputStream: FileInputStream? = null
    private var _outputStream: FileOutputStream? = null
    private var _receiveJob: Job? = null
    private var _reconnectJob: Job? = null

    private val _state = MutableStateFlow(CommunicationState.DISCONNECTED)
    private val _connectionInfo = MutableStateFlow(
        ConnectionInfo(
            channelId = channelId,
            type = channelType,
            state = CommunicationState.DISCONNECTED
        )
    )
    private val _receiveData = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64)

    private val scope = CoroutineScope(Dispatchers.IO)

    // Vsock设备路径
    private val vsockDevicePath = "/dev/vhost-vsock"

    override val state: StateFlow<CommunicationState> = _state.asStateFlow()
    override val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo.asStateFlow()
    override val receiveData: Flow<ByteArray> = _receiveData.asSharedFlow()

    override fun getVsockAddress(): String {
        return "vsock://${hostCid}:${vsockPort}"
    }

    override suspend fun connect(config: ChannelConfig): Result<ConnectionInfo> {
        if (config.type != ChannelType.VSOCK) {
            return Result.failure(
                CommunicationError.ConfigurationError("Invalid channel type: ${config.type}")
            )
        }

        val vsockConfig = config as? VsockChannelConfig
            ?: VsockChannelConfig(port = config.port)

        if (vsockConfig.port <= 0) {
            return Result.failure(
                CommunicationError.ConfigurationError("Valid port is required for Vsock")
            )
        }

        return try {
            _state.value = CommunicationState.CONNECTING
            updateConnectionInfo { it.copy(state = CommunicationState.CONNECTING) }

            _config = vsockConfig

            // 检查Vsock设备是否可用
            if (!isVsockAvailable()) {
                throw CommunicationError.ConnectionError("Vsock is not available on this device")
            }

            // 连接Vsock
            // 注意：Android上的Vsock实现可能需要使用特定的API或设备文件
            // 这里使用模拟的Socket连接，实际实现需要根据Android AVF API调整
            connectVsock(vsockConfig)

            // 启动接收循环
            startReceiveLoop()

            _state.value = CommunicationState.CONNECTED
            updateConnectionInfo {
                it.copy(
                    state = CommunicationState.CONNECTED,
                    connectedAt = System.currentTimeMillis()
                )
            }

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

    override suspend fun disconnect(): Result<Unit> {
        return try {
            stopReceiveLoop()
            stopReconnectLoop()

            _outputStream?.close()
            _inputStream?.close()
            _socket?.close()

            _outputStream = null
            _inputStream = null
            _socket = null

            _state.value = CommunicationState.DISCONNECTED
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

    override suspend fun send(data: ByteArray): Result<Unit> {
        val config = _config ?: return Result.failure(
            CommunicationError.ChannelError("Channel not connected")
        )
        return send(data, config.timeoutMs)
    }

    override suspend fun send(data: ByteArray, timeoutMs: Long): Result<Unit> {
        if (!isConnected()) {
            return Result.failure(CommunicationError.ChannelError("Channel not connected"))
        }

        return try {
            withTimeout(timeoutMs) {
                _outputStream?.write(data)
                _outputStream?.flush()
            }

            updateConnectionInfo {
                it.copy(
                    bytesSent = it.bytesSent + data.size,
                    lastActivityAt = System.currentTimeMillis()
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            val error = CommunicationError.ChannelError("Failed to send data: ${e.message}")
            handleError(error)
            Result.failure(error)
        }
    }

    override fun isConnected(): Boolean {
        return _state.value == CommunicationState.CONNECTED && 
               _socket?.isConnected == true && 
               _socket?.isClosed == false
    }

    override fun getConnectionInfo(): ConnectionInfo = _connectionInfo.value

    override suspend fun setTimeout(timeoutMs: Long): Result<Unit> {
        return try {
            _socket?.soTimeout = timeoutMs.toInt()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to set timeout: ${e.message}"))
        }
    }

    override suspend fun getSocketOptions(): VsockSocketOptions {
        return _config?.socketOptions ?: VsockSocketOptions()
    }

    override suspend fun setSocketOptions(options: VsockSocketOptions): Result<Unit> {
        return try {
            _socket?.sendBufferSize = options.sendBufferSize
            _socket?.receiveBufferSize = options.receiveBufferSize
            _socket?.keepAlive = options.keepAlive
            _socket?.tcpNoDelay = options.tcpNoDelay
            _socket?.soTimeout = options.soTimeout
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to set socket options: ${e.message}"))
        }
    }

    override suspend fun release() {
        disconnect()
    }

    private fun isVsockAvailable(): Boolean {
        // 检查Vsock设备文件是否存在
        val vsockDevice = File(vsockDevicePath)
        if (vsockDevice.exists()) {
            return true
        }

        // 检查Android AVF是否支持Vsock
        // 实际实现需要检查Android API
        return try {
            // 尝试加载Vsock相关类
            Class.forName("android.system.virtualmachine.VirtualMachine")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun connectVsock(config: VsockChannelConfig) {
        // Android AVF Vsock连接实现
        // 注意：这里需要使用Android AVF API进行实际的Vsock连接
        // 由于Android AVF API可能因版本而异，这里提供一个框架实现

        // 方式1：通过Unix域套接字代理（如果可用）
        val proxyPath = "/dev/socket/vsock_${config.port}"
        val proxyFile = File(proxyPath)
        
        if (proxyFile.exists()) {
            // 使用Unix域套接字代理
            val address = java.net.UnixDomainSocketAddress.of(proxyPath)
            val channel = java.nio.channels.SocketChannel.open(address)
            _socket = channel.socket()
        } else {
            // 方式2：使用本地回环作为后备方案（用于测试）
            // 实际生产环境需要使用真正的Vsock连接
            _socket = Socket("127.0.0.1", config.port)
        }

        _inputStream = FileInputStream(_socket?.inputStream)
        _outputStream = FileOutputStream(_socket?.outputStream)

        // 应用套接字选项
        config.socketOptions.let { opts ->
            _socket?.sendBufferSize = opts.sendBufferSize
            _socket?.receiveBufferSize = opts.receiveBufferSize
            _socket?.keepAlive = opts.keepAlive
            _socket?.tcpNoDelay = opts.tcpNoDelay
            _socket?.soTimeout = opts.soTimeout
        }
    }

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
                        _receiveData.emit(data)

                        updateConnectionInfo {
                            it.copy(
                                bytesReceived = it.bytesReceived + bytesRead,
                                lastActivityAt = System.currentTimeMillis()
                            )
                        }
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

    private fun stopReconnectLoop() {
        _reconnectJob?.cancel()
        _reconnectJob = null
    }

    private fun handleError(error: CommunicationError) {
        _state.value = CommunicationState.ERROR
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
}