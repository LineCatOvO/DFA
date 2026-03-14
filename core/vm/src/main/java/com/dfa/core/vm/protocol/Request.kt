package com.dfa.core.vm.protocol

import kotlinx.serialization.Serializable

/**
 * 请求方法枚举
 */
enum class RequestMethod {
    // 虚拟机管理
    VM_START,
    VM_STOP,
    VM_PAUSE,
    VM_RESUME,
    VM_RESET,
    VM_STATUS,
    VM_CONFIG,

    // 文件操作
    FILE_READ,
    FILE_WRITE,
    FILE_DELETE,
    FILE_LIST,
    FILE_EXISTS,

    // 进程管理
    PROCESS_START,
    PROCESS_STOP,
    PROCESS_STATUS,
    PROCESS_LIST,

    // 网络操作
    NETWORK_CONFIG,
    NETWORK_STATUS,
    NETWORK_CONNECT,

    // 系统操作
    SYSTEM_INFO,
    SYSTEM_STATS,
    SYSTEM_SHUTDOWN,

    // 自定义操作
    CUSTOM
}

/**
 * 请求参数
 */
@Serializable
data class RequestParams(
    val params: Map<String, String> = emptyMap(),
    val binaryData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RequestParams) return false
        return params == other.params && binaryData?.contentEquals(other.binaryData) == true
    }

    override fun hashCode(): Int {
        var result = params.hashCode()
        result = 31 * result + (binaryData?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 请求体
 */
@Serializable
data class RequestBody(
    val method: RequestMethod,
    val params: RequestParams = RequestParams(),
    val timeout: Long = DEFAULT_REQUEST_TIMEOUT,
    val retryCount: Int = 0,
    val maxRetries: Int = DEFAULT_MAX_RETRIES
) {
    companion object {
        const val DEFAULT_REQUEST_TIMEOUT = 30000L // 30 seconds
        const val DEFAULT_MAX_RETRIES = 3
    }

    fun canRetry(): Boolean = retryCount < maxRetries

    fun withRetry(): RequestBody = copy(retryCount = retryCount + 1)
}

/**
 * 请求消息
 */
@Serializable
data class Request(
    override val header: MessageHeader,
    override val body: RequestBody
) : Message<RequestBody> {

    constructor(
        messageId: String = generateMessageId(),
        method: RequestMethod,
        sourceId: String,
        targetId: String,
        params: RequestParams = RequestParams(),
        priority: MessagePriority = MessagePriority.NORMAL,
        correlationId: String? = null
    ) : this(
        header = MessageHeader(
            messageId = messageId,
            type = MessageType.REQUEST,
            priority = priority,
            sourceId = sourceId,
            targetId = targetId,
            correlationId = correlationId
        ),
        body = RequestBody(
            method = method,
            params = params
        )
    )

    override fun validate(): Boolean {
        return header.validate() && body.method != RequestMethod.CUSTOM || body.params.params.isNotEmpty()
    }

    companion object {
        fun generateMessageId(): String = "req-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
}