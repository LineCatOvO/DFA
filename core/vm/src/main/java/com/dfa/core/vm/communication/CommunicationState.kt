package com.dfa.core.vm.communication

/**
 * 通信状态枚举
 */
enum class CommunicationState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

/**
 * 通信错误 sealed class
 */
sealed class CommunicationError : Throwable() {
    data class ConnectionError(override val message: String) : CommunicationError()
    data class ChannelError(override val message: String) : CommunicationError()
    data class ProtocolError(override val message: String) : CommunicationError()
    data class TimeoutError(override val message: String) : CommunicationError()
    data class PermissionError(override val message: String) : CommunicationError()
    data class ConfigurationError(override val message: String) : CommunicationError()
    data class UnknownError(override val message: String, override val cause: Throwable? = null) : CommunicationError()
}

/**
 * 通道类型枚举
 */
enum class ChannelType {
    VIRTIO_SERIAL,
    VSOCK,
    SHARED_MEMORY
}

/**
 * 通道配置
 */
open class ChannelConfig(
    open val type: ChannelType,
    open val port: Int = 0,
    open val path: String? = null,
    open val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    open val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    open val enableReconnect: Boolean = true,
    open val maxReconnectAttempts: Int = MAX_RECONNECT_ATTEMPTS,
    open val reconnectDelayMs: Long = RECONNECT_DELAY_MS
) {
    companion object {
        const val DEFAULT_BUFFER_SIZE = 65536 // 64KB
        const val DEFAULT_TIMEOUT_MS = 30000L // 30 seconds
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val RECONNECT_DELAY_MS = 1000L
    }

    fun validate(): Boolean {
        return when (type) {
            ChannelType.VIRTIO_SERIAL -> !path.isNullOrBlank()
            ChannelType.VSOCK -> port > 0
            ChannelType.SHARED_MEMORY -> !path.isNullOrBlank()
        }
    }
}

/**
 * 连接信息
 */
data class ConnectionInfo(
    val channelId: String,
    val type: ChannelType,
    val state: CommunicationState,
    val connectedAt: Long? = null,
    val lastActivityAt: Long = System.currentTimeMillis(),
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val errorMessage: String? = null
) {
    val isConnected: Boolean
        get() = state == CommunicationState.CONNECTED

    val connectionDuration: Long
        get() = if (connectedAt != null) System.currentTimeMillis() - connectedAt else 0
}