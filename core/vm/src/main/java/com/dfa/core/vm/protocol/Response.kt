package com.dfa.core.vm.protocol

import kotlinx.serialization.Serializable

/**
 * 响应状态码
 */
enum class ResponseCode {
    // 成功状态
    SUCCESS,
    PARTIAL_SUCCESS,

    // 客户端错误
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    TIMEOUT,
    INVALID_PARAMS,

    // 服务端错误
    INTERNAL_ERROR,
    NOT_IMPLEMENTED,
    SERVICE_UNAVAILABLE,
    RESOURCE_EXHAUSTED,

    // 传输错误
    NETWORK_ERROR,
    CONNECTION_ERROR,
    ENCODING_ERROR
}

/**
 * 响应元数据
 */
@Serializable
data class ResponseMetadata(
    val processingTimeMs: Long = 0,
    val serverVersion: String? = null,
    val additionalInfo: Map<String, String> = emptyMap()
)

/**
 * 响应体
 */
@Serializable
data class ResponseBody<T>(
    val code: ResponseCode,
    val message: String? = null,
    val data: T? = null,
    val error: ErrorDetail? = null,
    val metadata: ResponseMetadata = ResponseMetadata()
) {
    val isSuccess: Boolean
        get() = code == ResponseCode.SUCCESS || code == ResponseCode.PARTIAL_SUCCESS

    val isError: Boolean
        get() = !isSuccess

    companion object {
        fun <T> success(data: T? = null, message: String? = null): ResponseBody<T> {
            return ResponseBody(
                code = ResponseCode.SUCCESS,
                message = message,
                data = data
            )
        }

        fun <T> error(
            code: ResponseCode,
            message: String,
            errorDetail: ErrorDetail? = null
        ): ResponseBody<T> {
            return ResponseBody(
                code = code,
                message = message,
                error = errorDetail
            )
        }
    }
}

/**
 * 错误详情
 */
@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val stackTrace: String? = null,
    val suggestions: List<String> = emptyList()
)

/**
 * 响应消息
 */
@Serializable
data class Response<T>(
    override val header: MessageHeader,
    override val body: ResponseBody<T>
) : Message<ResponseBody<T>> {

    constructor(
        messageId: String = generateMessageId(),
        correlationId: String,
        code: ResponseCode,
        sourceId: String,
        targetId: String,
        data: T? = null,
        message: String? = null,
        error: ErrorDetail? = null
    ) : this(
        header = MessageHeader(
            messageId = messageId,
            type = MessageType.RESPONSE,
            sourceId = sourceId,
            targetId = targetId,
            correlationId = correlationId
        ),
        body = ResponseBody(
            code = code,
            message = message,
            data = data,
            error = error
        )
    )

    override fun validate(): Boolean {
        return header.validate() && header.correlationId != null
    }

    companion object {
        fun generateMessageId(): String = "res-${System.currentTimeMillis()}-${(1000..9999).random()}"

        fun <T> success(
            correlationId: String,
            sourceId: String,
            targetId: String,
            data: T? = null
        ): Response<T> {
            return Response(
                correlationId = correlationId,
                code = ResponseCode.SUCCESS,
                sourceId = sourceId,
                targetId = targetId,
                data = data
            )
        }

        fun <T> error(
            correlationId: String,
            sourceId: String,
            targetId: String,
            code: ResponseCode,
            message: String,
            errorDetail: ErrorDetail? = null
        ): Response<T> {
            return Response(
                correlationId = correlationId,
                code = code,
                sourceId = sourceId,
                targetId = targetId,
                message = message,
                error = errorDetail
            )
        }
    }
}