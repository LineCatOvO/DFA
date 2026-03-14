package com.dfa.core.vm.protocol

import kotlinx.serialization.Serializable

/**
 * 消息类型枚举
 */
enum class MessageType {
    // 请求类型
    REQUEST,
    // 响应类型
    RESPONSE,
    // 通知类型
    NOTIFICATION,
    // 错误类型
    ERROR,
    // 心跳类型
    HEARTBEAT,
    // 文件传输类型
    FILE_TRANSFER,
    // 流控制类型
    FLOW_CONTROL
}

/**
 * 消息优先级
 */
enum class MessagePriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * 消息标志
 */
@Serializable
data class MessageFlags(
    val compressed: Boolean = false,
    val encrypted: Boolean = false,
    val requiresAck: Boolean = false,
    val isFragmented: Boolean = false,
    val fragmentIndex: Int = 0,
    val totalFragments: Int = 1
)

/**
 * 消息头
 */
@Serializable
data class MessageHeader(
    val messageId: String,
    val type: MessageType,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceId: String,
    val targetId: String,
    val correlationId: String? = null,
    val ttl: Long = DEFAULT_TTL,
    val flags: MessageFlags = MessageFlags(),
    val payloadSize: Int = 0,
    val checksum: String? = null
) {
    companion object {
        const val DEFAULT_TTL = 60000L // 60 seconds
        const val MAX_MESSAGE_SIZE = 1024 * 1024 // 1MB
        const val HEADER_SIZE = 128 // bytes
    }

    fun isExpired(): Boolean {
        return System.currentTimeMillis() > timestamp + ttl
    }

    fun validate(): Boolean {
        return messageId.isNotBlank() &&
                sourceId.isNotBlank() &&
                ttl > 0 &&
                payloadSize >= 0 &&
                payloadSize <= MAX_MESSAGE_SIZE
    }
}