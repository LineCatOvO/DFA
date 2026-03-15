package com.dfa.core.vm.communication

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 文件传输会话接口
 */
interface FileTransferSession {
    /**
     * 会话ID
     */
    val sessionId: String

    /**
     * 文件名
     */
    val fileName: String

    /**
     * 文件大小（字节）
     */
    val fileSize: Long

    /**
     * 传输方向
     */
    val direction: TransferDirection

    /**
     * 传输状态流
     */
    val state: StateFlow<TransferState>

    /**
     * 传输进度流
     */
    val progress: StateFlow<TransferProgress>

    /**
     * 暂停传输
     */
    suspend fun pause(): Result<Unit>

    /**
     * 恢复传输
     */
    suspend fun resume(): Result<Unit>

    /**
     * 取消传输
     */
    suspend fun cancel(): Result<Unit>

    /**
     * 等待传输完成
     */
    suspend fun awaitCompletion(): Result<TransferResult>
}

/**
 * 传输方向
 */
enum class TransferDirection {
    UPLOAD,
    DOWNLOAD
}

/**
 * 传输状态
 */
enum class TransferState {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * 传输进度
 */
data class TransferProgress(
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 0,
    val currentSpeed: Long = 0, // bytes per second
    val averageSpeed: Long = 0,
    val estimatedTimeRemaining: Long = 0, // milliseconds
    val percentage: Float = 0f
) {
    val isComplete: Boolean
        get() = bytesTransferred >= totalBytes && totalBytes > 0

    companion object {
        fun calculate(
            bytesTransferred: Long,
            totalBytes: Long,
            elapsedTimeMs: Long
        ): TransferProgress {
            val percentage = if (totalBytes > 0) {
                (bytesTransferred.toFloat() / totalBytes) * 100
            } else {
                0f
            }

            val averageSpeed = if (elapsedTimeMs > 0) {
                (bytesTransferred * 1000) / elapsedTimeMs
            } else {
                0L
            }

            val estimatedTimeRemaining = if (averageSpeed > 0) {
                ((totalBytes - bytesTransferred) * 1000) / averageSpeed
            } else {
                0L
            }

            return TransferProgress(
                bytesTransferred = bytesTransferred,
                totalBytes = totalBytes,
                averageSpeed = averageSpeed,
                estimatedTimeRemaining = estimatedTimeRemaining,
                percentage = percentage
            )
        }
    }
}

/**
 * 传输结果
 */
sealed class TransferResult {
    data class Success(
        val sessionId: String,
        val fileName: String,
        val bytesTransferred: Long,
        val durationMs: Long
    ) : TransferResult()

    data class Failed(
        val sessionId: String,
        val error: TransferError
    ) : TransferResult()

    data class Cancelled(
        val sessionId: String,
        val bytesTransferred: Long
    ) : TransferResult()
}

/**
 * 传输错误
 */
sealed class TransferError : Throwable() {
    data class NetworkError(override val message: String) : TransferError()
    data class StorageError(override val message: String) : TransferError()
    data class TimeoutError(override val message: String) : TransferError()
    data class ChecksumError(override val message: String) : TransferError()
    data class PermissionError(override val message: String) : TransferError()
    data class SizeLimitError(override val message: String) : TransferError()
    data class UnknownError(override val message: String, override val cause: Throwable? = null) : TransferError()
}

/**
 * 文件传输配置
 */
data class FileTransferConfig(
    val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    val maxConcurrentChunks: Int = DEFAULT_MAX_CONCURRENT_CHUNKS,
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val enableChecksum: Boolean = true,
    val maxFileSize: Long = DEFAULT_MAX_FILE_SIZE
) {
    companion object {
        const val DEFAULT_CHUNK_SIZE = 64 * 1024 // 64KB
        const val DEFAULT_MAX_CONCURRENT_CHUNKS = 4
        const val DEFAULT_TIMEOUT_MS = 60000L // 60 seconds
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_MAX_FILE_SIZE = 1024L * 1024 * 1024 * 10 // 10GB
    }

    fun validate(): Boolean {
        return chunkSize > 0 &&
                maxConcurrentChunks > 0 &&
                timeoutMs > 0 &&
                maxRetries >= 0 &&
                maxFileSize > 0
    }
}