package com.dfa.core.vm.image

import kotlinx.coroutines.flow.Flow

/**
 * 镜像下载器接口
 *
 * 提供镜像下载功能的核心接口
 */
interface ImageDownloader {

    /**
     * 下载镜像
     *
     * @param request 下载请求
     * @return 下载进度流，完成后返回最终结果
     */
    suspend fun download(request: ImageDownloadRequest): Flow<ImageDownloadProgress>

    /**
     * 取消下载
     *
     * @param imageId 镜像ID
     */
    suspend fun cancelDownload(imageId: String)

    /**
     * 暂停下载
     *
     * @param imageId 镜像ID
     */
    suspend fun pauseDownload(imageId: String)

    /**
     * 恢复下载
     *
     * @param imageId 镜像ID
     * @return 恢复后的下载进度流
     */
    suspend fun resumeDownload(imageId: String): Flow<ImageDownloadProgress>

    /**
     * 检查URL是否可访问
     *
     * @param url 镜像URL
     * @return 是否可访问，以及文件大小（如果可获取）
     */
    suspend fun checkUrl(url: String): Pair<Boolean, Long?>

    /**
     * 获取当前下载状态
     *
     * @param imageId 镜像ID
     * @return 当前下载进度，如果没有正在下载则返回null
     */
    fun getDownloadProgress(imageId: String): ImageDownloadProgress?

    /**
     * 是否正在下载
     *
     * @param imageId 镜像ID
     * @return 是否正在下载
     */
    fun isDownloading(imageId: String): Boolean
}

/**
 * 镜像下载器工厂接口
 */
interface ImageDownloaderFactory {
    /**
     * 创建镜像下载器实例
     *
     * @param config 下载配置
     * @return 镜像下载器实例
     */
    fun create(config: ImageDownloaderConfig): ImageDownloader
}

/**
 * 镜像下载器配置
 *
 * @property connectTimeoutMs 连接超时时间（毫秒）
 * @property readTimeoutMs 读取超时时间（毫秒）
 * @property bufferSize 缓冲区大小
 * @property maxRetryCount 最大重试次数
 * @property retryDelayMs 重试间隔（毫秒）
 * @property progressUpdateIntervalMs 进度更新间隔（毫秒）
 */
data class ImageDownloaderConfig(
    val connectTimeoutMs: Long = ImageConstants.CONNECT_TIMEOUT_MS,
    val readTimeoutMs: Long = ImageConstants.READ_TIMEOUT_MS,
    val bufferSize: Int = ImageConstants.DOWNLOAD_BUFFER_SIZE,
    val maxRetryCount: Int = ImageConstants.MAX_RETRY_COUNT,
    val retryDelayMs: Long = ImageConstants.RETRY_DELAY_MS,
    val progressUpdateIntervalMs: Long = ImageConstants.PROGRESS_UPDATE_INTERVAL_MS
)