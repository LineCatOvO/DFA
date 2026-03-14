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
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject

/**
 * VirtIO串口通道实现
 *
 * 通过VirtIO串口设备实现虚拟机与宿主之间的通信
 */
class VirtIOChannelImpl @Inject constructor() : VirtIOChannel {

    override val channelId: String = UUID.randomUUID().toString()
    override val channelType: ChannelType = ChannelType.VIRTIO_SERIAL
    override val devicePath: String
        get() = _devicePath

    private var _devicePath: String = ""
    private var _config: VirtIOChannelConfig? = null
    private var _serialFile: RandomAccessFile? = null
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

    private val _supportsInterrupt = MutableStateFlow(false)

    private val scope = CoroutineScope(Dispatchers.IO)

    override val state: StateFlow<CommunicationState> = _state.asStateFlow()
    override val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo.asStateFlow()
    override val receiveData: Flow<ByteArray> = _receiveData.asSharedFlow()
    override val supportsInterrupt: Boolean
        get() = _supportsInterrupt.value

    override suspend fun connect(config: ChannelConfig): Result<ConnectionInfo> {
        if (config.type != ChannelType.VIRTIO_SERIAL) {
            return Result.failure(
                CommunicationError.ConfigurationError("Invalid channel type: ${config.type}")
            )
        }

        val virtioConfig = config as? VirtIOChannelConfig
            ?: VirtIOChannelConfig(
                path = config.path ?: return Result.failure(
                    CommunicationError.ConfigurationError("Device path is required for VirtIO serial")
                )
            )

        return try {
            _state.value = CommunicationState.CONNECTING
            updateConnectionInfo { it.copy(state = CommunicationState.CONNECTING) }

            _devicePath = virtioConfig.path
            _config = virtioConfig

            // 检查设备文件是否存在
            val deviceFile = File(_devicePath)
            if (!deviceFile.exists()) {
                throw CommunicationError.ConnectionError("Device file not found: $_devicePath")
            }

            // 打开设备文件
            _serialFile = RandomAccessFile(deviceFile, "rw")
            _inputStream = FileInputStream(deviceFile)
            _outputStream = FileOutputStream(deviceFile)

            // 检查是否支持中断
            _supportsInterrupt.value = checkInterruptSupport(deviceFile)

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
            _serialFile?.close()

            _outputStream = null
            _inputStream = null
            _serialFile = null

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
        return _state.value == CommunicationState.CONNECTED && _serialFile != null
    }

    override fun getConnectionInfo(): ConnectionInfo = _connectionInfo.value

    override suspend fun getSerialStatus(): SerialPortStatus {
        return SerialPortStatus(
            isOpen = isConnected(),
            baudRate = _config?.baudRate ?: SerialPortStatus.DEFAULT_BAUD_RATE,
            dataBits = _config?.dataBits ?: 8,
            stopBits = _config?.stopBits ?: 1,
            parity = _config?.parity ?: Parity.NONE,
            flowControl = _config?.flowControl ?: FlowControl.NONE
        )
    }

    override suspend fun setBaudRate(baudRate: Int): Result<Unit> {
        // VirtIO串口通常不支持动态设置波特率
        // 波特率由虚拟机配置决定
        return Result.success(Unit)
    }

    override suspend fun flush(): Result<Unit> {
        return try {
            _outputStream?.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(CommunicationError.ChannelError("Failed to flush: ${e.message}"))
        }
    }

    override suspend fun release() {
        disconnect()
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

    private fun checkInterruptSupport(deviceFile: File): Boolean {
        // 检查是否支持中断（通过检查设备属性）
        return try {
            val interruptFile = File(deviceFile.parentFile, "${deviceFile.name}_interrupt")
            interruptFile.exists()
        } catch (e: Exception) {
            false
        }
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