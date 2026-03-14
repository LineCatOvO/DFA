package com.dfa.core.vm.protocol

import com.dfa.core.vm.communication.CommunicationError

/**
 * 消息编解码器接口
 *
 * 负责消息的序列化和反序列化
 */
interface MessageCodec {

    /**
     * 编码请求消息
     *
     * @param request 请求消息
     * @return 编码后的字节数组
     */
    suspend fun encodeRequest(request: Request): Result<ByteArray>

    /**
     * 解码请求消息
     *
     * @param data 字节数组
     * @return 请求消息
     */
    suspend fun decodeRequest(data: ByteArray): Result<Request>

    /**
     * 编码响应消息
     *
     * @param response 响应消息
     * @return 编码后的字节数组
     */
    suspend fun <T> encodeResponse(response: Response<T>): Result<ByteArray>

    /**
     * 解码响应消息
     *
     * @param data 字节数组
     * @return 响应消息
     */
    suspend fun <T> decodeResponse(data: ByteArray, clazz: Class<T>): Result<Response<T>>

    /**
     * 编码通知消息
     *
     * @param notification 通知消息
     * @return 编码后的字节数组
     */
    suspend fun encodeNotification(notification: Notification): Result<ByteArray>

    /**
     * 解码通知消息
     *
     * @param data 字节数组
     * @return 通知消息
     */
    suspend fun decodeNotification(data: ByteArray): Result<Notification>

    /**
     * 编码心跳消息
     *
     * @param heartbeat 心跳消息
     * @return 编码后的字节数组
     */
    suspend fun encodeHeartbeat(heartbeat: Heartbeat): Result<ByteArray>

    /**
     * 解码心跳消息
     *
     * @param data 字节数组
     * @return 心跳消息
     */
    suspend fun decodeHeartbeat(data: ByteArray): Result<Heartbeat>

    /**
     * 编码流控制消息
     *
     * @param flowControl 流控制消息
     * @return 编码后的字节数组
     */
    suspend fun encodeFlowControl(flowControl: FlowControlMessage): Result<ByteArray>

    /**
     * 解码流控制消息
     *
     * @param data 字节数组
     * @return 流控制消息
     */
    suspend fun decodeFlowControl(data: ByteArray): Result<FlowControlMessage>

    /**
     * 根据类型解码消息
     *
     * @param data 字节数组
     * @return 消息包装器
     */
    suspend fun decode(data: ByteArray): Result<MessageWrapper>

    /**
     * 计算校验和
     *
     * @param data 字节数组
     * @return 校验和字符串
     */
    fun calculateChecksum(data: ByteArray): String

    /**
     * 验证校验和
     *
     * @param data 字节数组
     * @param checksum 校验和
     * @return 是否验证通过
     */
    fun verifyChecksum(data: ByteArray, checksum: String): Boolean
}

/**
 * 编解码器配置
 */
data class CodecConfig(
    val enableCompression: Boolean = false,
    val compressionThreshold: Int = 1024, // bytes
    val enableEncryption: Boolean = false,
    val encryptionKey: String? = null,
    val maxMessageSize: Int = MessageHeader.MAX_MESSAGE_SIZE,
    val validateChecksum: Boolean = true
)

/**
 * 编解码器异常
 */
sealed class CodecError : Throwable() {
    data class EncodingError(override val message: String) : CodecError()
    data class DecodingError(override val message: String) : CodecError()
    data class ValidationError(override val message: String) : CodecError()
    data class SizeExceededError(override val message: String) : CodecError()
    data class ChecksumError(override val message: String) : CodecError()
}