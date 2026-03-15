package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationChannel
import com.dfa.core.vm.communication.CommunicationState
import com.dfa.core.vm.communication.ConnectionInfo

/**
 * Vsock通道接口
 *
 * 专门用于Vsock套接字通信的接口扩展
 */
interface VsockChannel : CommunicationChannel {

    /**
     * Vsock端口
     */
    val vsockPort: Int

    /**
     * 宿主CID（Context ID）
     */
    val hostCid: Int

    /**
     * 客户端CID
     */
    val clientCid: Int

    /**
     * 获取Vsock地址
     *
     * @return Vsock地址字符串
     */
    fun getVsockAddress(): String

    /**
     * 设置超时
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 设置结果
     */
    suspend fun setTimeout(timeoutMs: Long): Result<Unit>

    /**
     * 获取套接字选项
     *
     * @return 套接字选项
     */
    suspend fun getSocketOptions(): VsockSocketOptions

    /**
     * 设置套接字选项
     *
     * @param options 套接字选项
     * @return 设置结果
     */
    suspend fun setSocketOptions(options: VsockSocketOptions): Result<Unit>
}

/**
 * Vsock套接字选项
 */
data class VsockSocketOptions(
    val sendBufferSize: Int = DEFAULT_SEND_BUFFER_SIZE,
    val receiveBufferSize: Int = DEFAULT_RECEIVE_BUFFER_SIZE,
    val keepAlive: Boolean = true,
    val tcpNoDelay: Boolean = true,
    val soTimeout: Int = DEFAULT_SO_TIMEOUT
) {
    companion object {
        const val DEFAULT_SEND_BUFFER_SIZE = 262144 // 256KB
        const val DEFAULT_RECEIVE_BUFFER_SIZE = 262144 // 256KB
        const val DEFAULT_SO_TIMEOUT = 30000 // 30 seconds
    }
}

/**
 * Vsock通道配置
 */
class VsockChannelConfig(
    override val type: ChannelType = ChannelType.VSOCK,
    override val port: Int,
    override val path: String? = null,
    override val bufferSize: Int = ChannelConfig.DEFAULT_BUFFER_SIZE,
    override val timeoutMs: Long = ChannelConfig.DEFAULT_TIMEOUT_MS,
    override val enableReconnect: Boolean = true,
    override val maxReconnectAttempts: Int = ChannelConfig.MAX_RECONNECT_ATTEMPTS,
    override val reconnectDelayMs: Long = ChannelConfig.RECONNECT_DELAY_MS,
    val hostCid: Int = VMADDR_CID_HOST,
    val clientCid: Int = VMADDR_CID_ANY,
    val socketOptions: VsockSocketOptions = VsockSocketOptions()
) : ChannelConfig(
    type = type,
    port = port,
    path = path,
    bufferSize = bufferSize,
    timeoutMs = timeoutMs,
    enableReconnect = enableReconnect,
    maxReconnectAttempts = maxReconnectAttempts,
    reconnectDelayMs = reconnectDelayMs
) {
    companion object {
        // Vsock CID常量
        const val VMADDR_CID_ANY = -1
        const val VMADDR_CID_HYPERVISOR = 0
        const val VMADDR_CID_LOCAL = 1
        const val VMADDR_CID_HOST = 2
    }
}