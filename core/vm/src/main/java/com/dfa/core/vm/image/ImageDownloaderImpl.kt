package com.dfa.core.vm.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 镜像下载器实现
 *
 * 使用OkHttp进行HTTP下载，支持断点续传
 */
@Singleton
class ImageDownloaderImpl @Inject constructor(
    private val config: ImageDownloaderConfig
) : ImageDownloader {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val downloadProgress = ConcurrentHashMap<String, ImageDownloadProgress>()
    private val mutex = Mutex()

    override suspend fun download(request: ImageDownloadRequest): Flow<ImageDownloadProgress> = flow {
        val imageId = extractImageId(request.url)

        // 检查目标文件是否存在
        val targetFile = File(request.targetPath)
        if (targetFile.exists() && !request.overwrite) {
            emit(
                ImageDownloadProgress(
                    imageId = imageId,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    state = ImageState.DOWNLOADED
                )
            )
            return@flow
        }

        // 确保目标目录存在
        targetFile.parentFile?.mkdirs()

        // 检查URL可访问性并获取文件大小
        val (isAccessible, remoteSize) = checkUrl(request.url)
        if (!isAccessible) {
            emit(
                ImageDownloadProgress(
                    imageId = imageId,
                    downloadedBytes = 0,
                    totalBytes = 0,
                    state = ImageState.ERROR
                )
            )
            throw ImageError.NetworkError("Cannot access URL: ${request.url}")
        }

        // 获取已下载的大小（断点续传）
        val existingSize = if (targetFile.exists()) targetFile.length() else 0L
        val totalSize = remoteSize ?: existingSize

        // 更新初始进度
        val initialProgress = ImageDownloadProgress(
            imageId = imageId,
            downloadedBytes = existingSize,
            totalBytes = totalSize,
            state = ImageState.DOWNLOADING
        )
        downloadProgress[imageId] = initialProgress
        emit(initialProgress)

        // 执行下载
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < config.maxRetryCount) {
            try {
                downloadInternal(request, imageId, existingSize, totalSize).collect { progress ->
                    downloadProgress[imageId] = progress
                    emit(progress)
                }

                // 下载完成
                val finalProgress = ImageDownloadProgress(
                    imageId = imageId,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    state = ImageState.DOWNLOADED
                )
                downloadProgress[imageId] = finalProgress
                emit(finalProgress)
                return@flow
            } catch (e: Exception) {
                lastException = e
                retryCount++
                if (retryCount < config.maxRetryCount) {
                    delay(config.retryDelayMs)
                }
            }
        }

        // 所有重试都失败
        val errorProgress = ImageDownloadProgress(
            imageId = imageId,
            downloadedBytes = targetFile.length(),
            totalBytes = totalSize,
            state = ImageState.ERROR
        )
        downloadProgress[imageId] = errorProgress
        emit(errorProgress)
        throw lastException ?: ImageError.NetworkError("Download failed after $retryCount retries")
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadInternal(
        request: ImageDownloadRequest,
        imageId: String,
        startBytes: Long,
        totalSize: Long
    ): Flow<ImageDownloadProgress> = flow {
        val targetFile = File(request.targetPath)

        val requestBuilder = Request.Builder()
            .url(request.url)
            .get()

        // 断点续传
        if (startBytes > 0) {
            requestBuilder.header("Range", "bytes=$startBytes-")
        }

        val httpRequest = requestBuilder.build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    throw ImageError.NetworkError("HTTP error: ${response.code}")
                }

                val body = response.body ?: throw ImageError.NetworkError("Empty response body")

                val outputStream = if (startBytes > 0) {
                    FileOutputStream(targetFile, true)
                } else {
                    FileOutputStream(targetFile)
                }

                var downloadedBytes = startBytes
                var lastUpdateTime = System.currentTimeMillis()
                var lastBytes = startBytes

                outputStream.use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(config.bufferSize)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            // 控制进度更新频率
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime >= config.progressUpdateIntervalMs) {
                                val speed = ((downloadedBytes - lastBytes) * 1000) /
                                    (currentTime - lastUpdateTime)

                                emit(
                                    ImageDownloadProgress(
                                        imageId = imageId,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalSize,
                                        speed = speed,
                                        state = ImageState.DOWNLOADING
                                    )
                                )

                                lastUpdateTime = currentTime
                                lastBytes = downloadedBytes
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            throw ImageError.NetworkError("Download failed: ${e.message}", e)
        }
    }

    override suspend fun cancelDownload(imageId: String) {
        mutex.withLock {
            downloadJobs[imageId]?.cancel()
            downloadJobs.remove(imageId)
            downloadProgress.remove(imageId)
        }
    }

    override suspend fun pauseDownload(imageId: String) {
        mutex.withLock {
            downloadJobs[imageId]?.cancel()
            downloadJobs.remove(imageId)
        }
    }

    override suspend fun resumeDownload(imageId: String): Flow<ImageDownloadProgress> {
        val progress = downloadProgress[imageId]
            ?: throw ImageError.ConfigurationError("No paused download for image: $imageId")

        // 这里需要从持久化存储中恢复下载信息
        // 简化实现：直接返回当前进度
        return flow {
            emit(progress)
        }
    }

    override suspend fun checkUrl(url: String): Pair<Boolean, Long?> {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val contentLength = response.header("Content-Length")?.toLongOrNull()
                    Pair(true, contentLength)
                } else {
                    Pair(false, null)
                }
            }
        } catch (e: Exception) {
            Pair(false, null)
        }
    }

    override fun getDownloadProgress(imageId: String): ImageDownloadProgress? {
        return downloadProgress[imageId]
    }

    override fun isDownloading(imageId: String): Boolean {
        return downloadJobs.containsKey(imageId) && downloadJobs[imageId]?.isActive == true
    }

    private fun extractImageId(url: String): String {
        return url.substringAfterLast("/").substringBefore(".")
    }
}