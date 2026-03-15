package com.dfa.core.vm.image

/**
 * 镜像状态枚举
 */
enum class ImageState {
    /** 未下载 */
    NOT_DOWNLOADED,
    /** 下载中 */
    DOWNLOADING,
    /** 已下载，待验证 */
    DOWNLOADED,
    /** 验证中 */
    VERIFYING,
    /** 验证通过，可用 */
    READY,
    /** 错误状态 */
    ERROR,
    /** 缓存中 */
    CACHED
}

/**
 * 镜像信息
 *
 * @property id 镜像唯一标识
 * @property name 镜像名称
 * @property url 下载URL
 * @property localPath 本地存储路径
 * @property size 文件大小（字节）
 * @property checksum 校验和（可选）
 * @property checksumType 校验和类型（如sha256）
 * @property state 当前状态
 * @property downloadedBytes 已下载字节数
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 * @property errorMessage 错误信息
 */
data class ImageInfo(
    val id: String,
    val name: String,
    val url: String,
    val localPath: String? = null,
    val size: Long = 0,
    val checksum: String? = null,
    val checksumType: String? = null,
    val state: ImageState = ImageState.NOT_DOWNLOADED,
    val downloadedBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    /**
     * 下载进度百分比（0-100）
     */
    val progress: Int
        get() = if (size > 0) ((downloadedBytes * 100) / size).toInt().coerceIn(0, 100) else 0

    /**
     * 是否已完成下载
     */
    val isDownloaded: Boolean
        get() = state == ImageState.DOWNLOADED || state == ImageState.READY || state == ImageState.CACHED

    /**
     * 是否可用
     */
    val isReady: Boolean
        get() = state == ImageState.READY || state == ImageState.CACHED

    /**
     * 是否正在下载
     */
    val isDownloading: Boolean
        get() = state == ImageState.DOWNLOADING
}

/**
 * 镜像下载进度
 *
 * @property imageId 镜像ID
 * @property downloadedBytes 已下载字节数
 * @property totalBytes 总字节数
 * @property speed 当前下载速度（字节/秒）
 * @property state 当前状态
 */
data class ImageDownloadProgress(
    val imageId: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Long = 0,
    val state: ImageState = ImageState.DOWNLOADING
) {
    /**
     * 下载进度百分比（0-100）
     */
    val progress: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    /**
     * 剩余时间估算（秒），如果无法估算则为null
     */
    val estimatedTimeRemaining: Long?
        get() = if (speed > 0 && totalBytes > downloadedBytes) {
            (totalBytes - downloadedBytes) / speed
        } else null
}

/**
 * 镜像错误 sealed class
 */
sealed class ImageError : Throwable() {
    /** 网络错误 */
    data class NetworkError(override val message: String, override val cause: Throwable? = null) : ImageError()

    /** 存储错误 */
    data class StorageError(override val message: String, override val cause: Throwable? = null) : ImageError()

    /** 校验错误 */
    data class ChecksumError(override val message: String, val expected: String, val actual: String) : ImageError()

    /** 配置错误 */
    data class ConfigurationError(override val message: String) : ImageError()

    /** 权限错误 */
    data class PermissionError(override val message: String) : ImageError()

    /** 超时错误 */
    data class TimeoutError(override val message: String) : ImageError()

    /** 未知错误 */
    data class UnknownError(override val message: String, override val cause: Throwable? = null) : ImageError()
}

/**
 * 镜像下载请求
 *
 * @property url 下载URL
 * @property targetPath 目标存储路径
 * @property checksum 预期校验和（可选）
 * @property checksumType 校验和类型
 * @property overwrite 是否覆盖已存在的文件
 */
data class ImageDownloadRequest(
    val url: String,
    val targetPath: String,
    val checksum: String? = null,
    val checksumType: String = "sha256",
    val overwrite: Boolean = false
)

/**
 * 镜像验证结果
 *
 * @property imageId 镜像ID
 * @property isValid 是否有效
 * @property checksumMatch 校验和是否匹配
 * @property fileSize 文件大小
 * @property errorMessage 错误信息
 */
data class ImageValidationResult(
    val imageId: String,
    val isValid: Boolean,
    val checksumMatch: Boolean = true,
    val fileSize: Long = 0,
    val errorMessage: String? = null
)