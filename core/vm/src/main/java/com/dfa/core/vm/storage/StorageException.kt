package com.dfa.core.vm.storage

/**
 * 存储异常基类
 */
sealed class StorageException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)

    /**
     * 磁盘空间不足异常
     */
    data class InsufficientSpaceException(
        override val message: String,
        val requiredBytes: Long,
        val availableBytes: Long
    ) : StorageException(message)

    /**
     * 磁盘镜像异常
     */
    data class DiskImageException(
        override val message: String,
        val imagePath: String? = null,
        override val cause: Throwable? = null
    ) : StorageException(message, cause)

    /**
     * 加密异常
     */
    data class EncryptionException(
        override val message: String,
        override val cause: Throwable? = null
    ) : StorageException(message, cause)

    /**
     * 解密异常
     */
    data class DecryptionException(
        override val message: String,
        override val cause: Throwable? = null
    ) : StorageException(message, cause)

    /**
     * 密钥管理异常
     */
    data class KeyManagementException(
        override val message: String,
        override val cause: Throwable? = null
    ) : StorageException(message, cause)

    /**
     * 配额超限异常
     */
    data class QuotaExceededException(
        override val message: String,
        val quotaLimit: Long,
        val currentUsage: Long,
        val requestedBytes: Long
    ) : StorageException(message)

    /**
     * 存储访问异常
     */
    data class StorageAccessException(
        override val message: String,
        val path: String? = null,
        override val cause: Throwable? = null
    ) : StorageException(message, cause)

    /**
     * 镜像格式异常
     */
    data class ImageFormatException(
        override val message: String,
        val format: String? = null,
        override val cause: Throwable? = null
    ) : StorageException(message, cause)

    /**
     * 持久化异常
     */
    data class PersistenceException(
        override val message: String,
        override val cause: Throwable? = null
    ) : StorageException(message, cause)

    /**
     * SAF存储异常
     */
    data class SafStorageException(
        override val message: String,
        val uri: String? = null,
        override val cause: Throwable? = null
    ) : StorageException(message, cause)
}

/**
 * 存储错误 sealed class
 */
sealed class StorageError : Throwable() {
    data class InsufficientSpace(
        override val message: String,
        val required: Long,
        val available: Long
    ) : StorageError()

    data class DiskImageError(
        override val message: String,
        val path: String? = null
    ) : StorageError()

    data class EncryptionError(override val message: String) : StorageError()

    data class DecryptionError(override val message: String) : StorageError()

    data class KeyError(override val message: String) : StorageError()

    data class QuotaError(
        override val message: String,
        val limit: Long,
        val usage: Long
    ) : StorageError()

    data class AccessError(
        override val message: String,
        val path: String? = null
    ) : StorageError()

    data class FormatError(
        override val message: String,
        val format: String? = null
    ) : StorageError()

    data class PersistenceError(override val message: String) : StorageError()

    data class SafError(
        override val message: String,
        val uri: String? = null
    ) : StorageError()

    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : StorageError()
}

/**
 * 存储异常处理工具
 */
object StorageExceptionHandler {
    /**
     * 将StorageError转换为StorageException
     */
    fun fromStorageError(error: StorageError, path: String? = null): StorageException {
        return when (error) {
            is StorageError.InsufficientSpace -> StorageException.InsufficientSpaceException(
                message = error.message,
                requiredBytes = error.required,
                availableBytes = error.available
            )
            is StorageError.DiskImageError -> StorageException.DiskImageException(
                message = error.message,
                imagePath = error.path
            )
            is StorageError.EncryptionError -> StorageException.EncryptionException(
                message = error.message
            )
            is StorageError.DecryptionError -> StorageException.DecryptionException(
                message = error.message
            )
            is StorageError.KeyError -> StorageException.KeyManagementException(
                message = error.message
            )
            is StorageError.QuotaError -> StorageException.QuotaExceededException(
                message = error.message,
                quotaLimit = error.limit,
                currentUsage = error.usage,
                requestedBytes = 0
            )
            is StorageError.AccessError -> StorageException.StorageAccessException(
                message = error.message,
                path = error.path
            )
            is StorageError.FormatError -> StorageException.ImageFormatException(
                message = error.message,
                format = error.format
            )
            is StorageError.PersistenceError -> StorageException.PersistenceException(
                message = error.message
            )
            is StorageError.SafError -> StorageException.SafStorageException(
                message = error.message,
                uri = error.uri
            )
            is StorageError.UnknownError -> StorageException.PersistenceException(
                message = error.message,
                cause = error.cause
            )
        }
    }

    /**
     * 判断异常是否可重试
     */
    fun isRetryable(exception: StorageException): Boolean {
        return when (exception) {
            is StorageException.StorageAccessException -> true
            is StorageException.SafStorageException -> true
            is StorageException.PersistenceException -> true
            is StorageException.InsufficientSpaceException -> false
            is StorageException.DiskImageException -> false
            is StorageException.EncryptionException -> false
            is StorageException.DecryptionException -> false
            is StorageException.KeyManagementException -> false
            is StorageException.QuotaExceededException -> false
            is StorageException.ImageFormatException -> false
        }
    }
}