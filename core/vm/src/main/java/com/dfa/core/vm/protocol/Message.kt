package com.dfa.core.vm.protocol

import kotlinx.serialization.Serializable

/**
 * 消息接口
 *
 * 所有消息类型的基接口
 */
interface Message<T> {
    /**
     * 消息头
     */
    val header: MessageHeader

    /**
     * 消息体
     */
    val body: T

    /**
     * 验证消息有效性
     *
     * @return 是否有效
     */
    fun validate(): Boolean

    /**
     * 获取消息大小
     *
     * @return 消息大小（字节）
     */
    fun getMessageSize(): Int {
        return MessageHeader.HEADER_SIZE + header.payloadSize
    }
}

/**
 * 通知消息体
 */
@Serializable
data class NotificationBody(
    val eventType: String,
    val eventData: Map<String, String> = emptyMap(),
    val severity: NotificationSeverity = NotificationSeverity.INFO
)

/**
 * 通知严重级别
 */
enum class NotificationSeverity {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * 通知消息
 */
@Serializable
data class Notification(
    override val header: MessageHeader,
    override val body: NotificationBody
) : Message<NotificationBody> {

    constructor(
        messageId: String = generateMessageId(),
        eventType: String,
        sourceId: String,
        targetId: String,
        eventData: Map<String, String> = emptyMap(),
        severity: NotificationSeverity = NotificationSeverity.INFO
    ) : this(
        header = MessageHeader(
            messageId = messageId,
            type = MessageType.NOTIFICATION,
            sourceId = sourceId,
            targetId = targetId
        ),
        body = NotificationBody(
            eventType = eventType,
            eventData = eventData,
            severity = severity
        )
    )

    override fun validate(): Boolean {
        return header.validate() && body.eventType.isNotBlank()
    }

    companion object {
        fun generateMessageId(): String = "notif-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
}

/**
 * 心跳消息体
 */
@Serializable
data class HeartbeatBody(
    val sequence: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val status: HeartbeatStatus = HeartbeatStatus.ALIVE
)

/**
 * 心跳状态
 */
enum class HeartbeatStatus {
    ALIVE,
    BUSY,
    IDLE,
    SHUTTING_DOWN
}

/**
 * 心跳消息
 */
@Serializable
data class Heartbeat(
    override val header: MessageHeader,
    override val body: HeartbeatBody
) : Message<HeartbeatBody> {

    constructor(
        messageId: String = generateMessageId(),
        sourceId: String,
        targetId: String,
        sequence: Long,
        status: HeartbeatStatus = HeartbeatStatus.ALIVE
    ) : this(
        header = MessageHeader(
            messageId = messageId,
            type = MessageType.HEARTBEAT,
            sourceId = sourceId,
            targetId = targetId
        ),
        body = HeartbeatBody(
            sequence = sequence,
            status = status
        )
    )

    override fun validate(): Boolean {
        return header.validate() && body.sequence >= 0
    }

    companion object {
        fun generateMessageId(): String = "hb-${System.currentTimeMillis()}"
    }
}

/**
 * 流控制消息体
 */
@Serializable
data class FlowControlBody(
    val action: FlowControlAction,
    val windowSize: Int = 0,
    val sequenceNumber: Long = 0
)

/**
 * 流控制动作
 */
enum class FlowControlAction {
    PAUSE,
    RESUME,
    ACK,
    NACK,
    WINDOW_UPDATE
}

/**
 * 流控制消息
 */
@Serializable
data class FlowControlMessage(
    override val header: MessageHeader,
    override val body: FlowControlBody
) : Message<FlowControlBody> {

    constructor(
        messageId: String = generateMessageId(),
        sourceId: String,
        targetId: String,
        action: FlowControlAction,
        windowSize: Int = 0,
        sequenceNumber: Long = 0
    ) : this(
        header = MessageHeader(
            messageId = messageId,
            type = MessageType.FLOW_CONTROL,
            sourceId = sourceId,
            targetId = targetId
        ),
        body = FlowControlBody(
            action = action,
            windowSize = windowSize,
            sequenceNumber = sequenceNumber
        )
    )

    override fun validate(): Boolean {
        return header.validate()
    }

    companion object {
        fun generateMessageId(): String = "fc-${System.currentTimeMillis()}"
    }
}

/**
 * 消息包装器（用于序列化）
 */
@Serializable
sealed class MessageWrapper {
    @Serializable
    data class RequestWrapper(val request: Request) : MessageWrapper()

    @Serializable
    data class ResponseWrapper(val response: Response<String>) : MessageWrapper()

    @Serializable
    data class NotificationWrapper(val notification: Notification) : MessageWrapper()

    @Serializable
    data class HeartbeatWrapper(val heartbeat: Heartbeat) : MessageWrapper()

    @Serializable
    data class FlowControlWrapper(val flowControl: FlowControlMessage) : MessageWrapper()
}