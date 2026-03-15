package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationChannel
import com.dfa.core.vm.communication.CommunicationState
import com.dfa.core.vm.communication.ConnectionInfo

/**
 * VirtIO串口通道接口
 *
 * 专门用于VirtIO串口通信的接口扩展
 */
interface VirtIOChannel : CommunicationChannel {

    /**
     * 串口设备路径
     */
    val devicePath: String

    /**
     * 是否支持中断
     */
    val supportsInterrupt: Boolean

    /**
     * 获取串口状态
     *
     * @return 串口状态
     */
    suspend fun getSerialStatus(): SerialPortStatus

    /**
     * 设置波特率（如果支持）
     *
     * @param baudRate 波特率
     * @return 设置结果
     */
    suspend fun setBaudRate(baudRate: Int): Result<Unit>

    /**
     * 刷新缓冲区
     *
     * @return 刷新结果
     */
    suspend fun flush(): Result<Unit>
}

/**
 * 串口状态
 */
data class SerialPortStatus(
    val isOpen: Boolean,
    val baudRate: Int = DEFAULT_BAUD_RATE,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Parity = Parity.NONE,
    val flowControl: FlowControl = FlowControl.NONE,
    val receiveBufferCount: Int = 0,
    val transmitBufferCount: Int = 0
) {
    companion object {
        const val DEFAULT_BAUD_RATE = 115200
    }
}

/**
 * 校验位
 */
enum class Parity {
    NONE,
    ODD,
    EVEN
}

/**
 * 流控制
 */
enum class FlowControl {
    NONE,
    HARDWARE,
    SOFTWARE
}

/**
 * VirtIO通道配置
 */
class VirtIOChannelConfig(
    override val type: ChannelType = ChannelType.VIRTIO_SERIAL,
    override val port: Int = 0,
    override val path: String,
    override val bufferSize: Int = ChannelConfig.DEFAULT_BUFFER_SIZE,
    override val timeoutMs: Long = ChannelConfig.DEFAULT_TIMEOUT_MS,
    override val enableReconnect: Boolean = true,
    override val maxReconnectAttempts: Int = ChannelConfig.MAX_RECONNECT_ATTEMPTS,
    override val reconnectDelayMs: Long = ChannelConfig.RECONNECT_DELAY_MS,
    val baudRate: Int = SerialPortStatus.DEFAULT_BAUD_RATE,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Parity = Parity.NONE,
    val flowControl: FlowControl = FlowControl.NONE
) : ChannelConfig(
    type = type,
    port = port,
    path = path,
    bufferSize = bufferSize,
    timeoutMs = timeoutMs,
    enableReconnect = enableReconnect,
    maxReconnectAttempts = maxReconnectAttempts,
    reconnectDelayMs = reconnectDelayMs
)