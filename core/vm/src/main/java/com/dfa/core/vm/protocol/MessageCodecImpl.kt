package com.dfa.core.vm.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.inject.Inject

/**
 * 消息编解码器实现
 *
 * 使用Kotlin Serialization进行消息编解码
 */
@OptIn(ExperimentalSerializationApi::class)
class MessageCodecImpl @Inject constructor(
    private val config: CodecConfig = CodecConfig()
) : MessageCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
        allowSpecialFloatingPointValues = true
        allowStructuredMapKeys = true
    }

    override suspend fun encodeRequest(request: Request): Result<ByteArray> {
        return try {
            validateMessage(request)
            val jsonString = json.encodeToString(request)
            val data = jsonString.toByteArray(Charsets.UTF_8)
            Result.success(processOutgoingData(data, request.header))
        } catch (e: SerializationException) {
            Result.failure(CodecError.EncodingError("Failed to encode request: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.EncodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun decodeRequest(data: ByteArray): Result<Request> {
        return try {
            val processedData = processIncomingData(data)
            val request = json.decodeFromString<Request>(processedData.toString(Charsets.UTF_8))
            validateMessage(request)
            Result.success(request)
        } catch (e: SerializationException) {
            Result.failure(CodecError.DecodingError("Failed to decode request: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.DecodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun <T> encodeResponse(response: Response<T>): Result<ByteArray> {
        return try {
            validateMessage(response)
            // 将响应转换为字符串格式进行序列化
            val stringResponse = when (response.body.data) {
                is String -> response as Response<String>
                is ByteArray -> {
                    // 对于二进制数据，使用Base64编码
                    val base64Data = java.util.Base64.getEncoder().encodeToString(response.body.data as ByteArray)
                    Response(
                        header = response.header,
                        body = ResponseBody(
                            code = response.body.code,
                            message = response.body.message,
                            data = base64Data,
                            error = response.body.error,
                            metadata = response.body.metadata
                        )
                    )
                }
                else -> {
                    // 对于其他类型，转换为JSON字符串
                    val jsonData = response.body.data.toString()
                    Response(
                        header = response.header,
                        body = ResponseBody(
                            code = response.body.code,
                            message = response.body.message,
                            data = jsonData,
                            error = response.body.error,
                            metadata = response.body.metadata
                        )
                    )
                }
            }
            val jsonString = json.encodeToString(stringResponse)
            val data = jsonString.toByteArray(Charsets.UTF_8)
            Result.success(processOutgoingData(data, response.header))
        } catch (e: SerializationException) {
            Result.failure(CodecError.EncodingError("Failed to encode response: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.EncodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun <T> decodeResponse(data: ByteArray, clazz: Class<T>): Result<Response<T>> {
        return try {
            val processedData = processIncomingData(data)
            @Suppress("UNCHECKED_CAST")
            val response = json.decodeFromString<Response<String>>(processedData.toString(Charsets.UTF_8)) as Response<T>
            validateMessage(response)
            Result.success(response)
        } catch (e: SerializationException) {
            Result.failure(CodecError.DecodingError("Failed to decode response: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.DecodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun encodeNotification(notification: Notification): Result<ByteArray> {
        return try {
            validateMessage(notification)
            val jsonString = json.encodeToString(notification)
            val data = jsonString.toByteArray(Charsets.UTF_8)
            Result.success(processOutgoingData(data, notification.header))
        } catch (e: SerializationException) {
            Result.failure(CodecError.EncodingError("Failed to encode notification: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.EncodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun decodeNotification(data: ByteArray): Result<Notification> {
        return try {
            val processedData = processIncomingData(data)
            val notification = json.decodeFromString<Notification>(processedData.toString(Charsets.UTF_8))
            validateMessage(notification)
            Result.success(notification)
        } catch (e: SerializationException) {
            Result.failure(CodecError.DecodingError("Failed to decode notification: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.DecodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun encodeHeartbeat(heartbeat: Heartbeat): Result<ByteArray> {
        return try {
            validateMessage(heartbeat)
            val jsonString = json.encodeToString(heartbeat)
            val data = jsonString.toByteArray(Charsets.UTF_8)
            Result.success(processOutgoingData(data, heartbeat.header))
        } catch (e: SerializationException) {
            Result.failure(CodecError.EncodingError("Failed to encode heartbeat: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.EncodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun decodeHeartbeat(data: ByteArray): Result<Heartbeat> {
        return try {
            val processedData = processIncomingData(data)
            val heartbeat = json.decodeFromString<Heartbeat>(processedData.toString(Charsets.UTF_8))
            validateMessage(heartbeat)
            Result.success(heartbeat)
        } catch (e: SerializationException) {
            Result.failure(CodecError.DecodingError("Failed to decode heartbeat: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.DecodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun encodeFlowControl(flowControl: FlowControlMessage): Result<ByteArray> {
        return try {
            validateMessage(flowControl)
            val jsonString = json.encodeToString(flowControl)
            val data = jsonString.toByteArray(Charsets.UTF_8)
            Result.success(processOutgoingData(data, flowControl.header))
        } catch (e: SerializationException) {
            Result.failure(CodecError.EncodingError("Failed to encode flow control: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.EncodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun decodeFlowControl(data: ByteArray): Result<FlowControlMessage> {
        return try {
            val processedData = processIncomingData(data)
            val flowControl = json.decodeFromString<FlowControlMessage>(processedData.toString(Charsets.UTF_8))
            validateMessage(flowControl)
            Result.success(flowControl)
        } catch (e: SerializationException) {
            Result.failure(CodecError.DecodingError("Failed to decode flow control: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.DecodingError("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun decode(data: ByteArray): Result<MessageWrapper> {
        return try {
            val processedData = processIncomingData(data)
            val jsonString = processedData.toString(Charsets.UTF_8)

            // 尝试解析为不同类型的消息
            return try {
                val request = json.decodeFromString<Request>(jsonString)
                validateMessage(request)
                Result.success(MessageWrapper.RequestWrapper(request))
            } catch (e: Exception) {
                try {
                    val response = json.decodeFromString<Response<String>>(jsonString)
                    validateMessage(response)
                    Result.success(MessageWrapper.ResponseWrapper(response))
                } catch (e: Exception) {
                    try {
                        val notification = json.decodeFromString<Notification>(jsonString)
                        validateMessage(notification)
                        Result.success(MessageWrapper.NotificationWrapper(notification))
                    } catch (e: Exception) {
                        try {
                            val heartbeat = json.decodeFromString<Heartbeat>(jsonString)
                            validateMessage(heartbeat)
                            Result.success(MessageWrapper.HeartbeatWrapper(heartbeat))
                        } catch (e: Exception) {
                            val flowControl = json.decodeFromString<FlowControlMessage>(jsonString)
                            validateMessage(flowControl)
                            Result.success(MessageWrapper.FlowControlWrapper(flowControl))
                        }
                    }
                }
            }
        } catch (e: SerializationException) {
            Result.failure(CodecError.DecodingError("Failed to decode message: ${e.message}"))
        } catch (e: CodecError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CodecError.DecodingError("Unexpected error: ${e.message}"))
        }
    }

    override fun calculateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    override fun verifyChecksum(data: ByteArray, checksum: String): Boolean {
        return calculateChecksum(data) == checksum
    }

    private fun validateMessage(message: Message<*>) {
        if (!message.validate()) {
            throw CodecError.ValidationError("Message validation failed")
        }

        val size = message.getMessageSize()
        if (size > config.maxMessageSize) {
            throw CodecError.SizeExceededError("Message size $size exceeds maximum ${config.maxMessageSize}")
        }
    }

    private fun processOutgoingData(data: ByteArray, header: MessageHeader): ByteArray {
        var processedData = data

        // 压缩
        if (config.enableCompression && data.size > config.compressionThreshold) {
            processedData = compress(processedData)
        }

        // 加密
        if (config.enableEncryption && config.encryptionKey != null) {
            processedData = encrypt(processedData, config.encryptionKey)
        }

        return processedData
    }

    private fun processIncomingData(data: ByteArray): ByteArray {
        var processedData = data

        // 解密
        if (config.enableEncryption && config.encryptionKey != null) {
            processedData = decrypt(processedData, config.encryptionKey)
        }

        // 解压
        if (config.enableCompression) {
            processedData = decompress(processedData)
        }

        return processedData
    }

    private fun compress(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()

        val outputBuffer = ByteArray(data.size * 2)
        val compressedSize = deflater.deflate(outputBuffer)
        deflater.end()

        return outputBuffer.copyOf(compressedSize)
    }

    private fun decompress(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)

        val outputBuffer = ByteArray(data.size * 10)
        val decompressedSize = inflater.inflate(outputBuffer)
        inflater.end()

        return outputBuffer.copyOf(decompressedSize)
    }

    private fun encrypt(data: ByteArray, key: String): ByteArray {
        // 简单的XOR加密（生产环境应使用更安全的加密方式）
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val encrypted = ByteArray(data.size)
        for (i in data.indices) {
            encrypted[i] = (data[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return encrypted
    }

    private fun decrypt(data: ByteArray, key: String): ByteArray {
        // XOR解密与加密相同
        return encrypt(data, key)
    }
}