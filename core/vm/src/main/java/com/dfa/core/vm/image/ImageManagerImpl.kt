package com.dfa.core.vm.image

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 镜像管理器实现
 *
 * 协调下载器、缓存和验证器，提供统一的镜像管理功能
 */
@Singleton
class ImageManagerImpl @Inject constructor(
    private val downloader: ImageDownloader,
    private val cache: ImageCache,
    private val validator: ImageValidator
) : ImageManager {

    private val mutex = Mutex()

    private val _currentImageState = MutableStateFlow(ImageState.NOT_DOWNLOADED)
    override val currentImageState: StateFlow<ImageState> = _currentImageState.asStateFlow()

    private val _currentImageInfo = MutableStateFlow<ImageInfo?>(null)
    override val currentImageInfo: StateFlow<ImageInfo?> = _currentImageInfo.asStateFlow()

    private val _downloadProgress = MutableStateFlow<ImageDownloadProgress?>(null)
    override val downloadProgress: StateFlow<ImageDownloadProgress?> = _downloadProgress.asStateFlow()

    private var isInitialized = false
    private var currentDownloadImageId: String? = null

    override suspend fun initialize(): Result<Unit> {
        return mutex.withLock {
            try {
                // 检查默认镜像是否已缓存
                val cachedImage = cache.getCachedImage(ImageConstants.DEFAULT_IMAGE_ID)
                if (cachedImage != null && cachedImage.isReady) {
                    _currentImageInfo.value = cachedImage
                    _currentImageState.value = ImageState.READY
                } else {
                    _currentImageState.value = ImageState.NOT_DOWNLOADED
                }

                isInitialized = true
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(ImageError.UnknownError("Initialization failed: ${e.message}", e))
            }
        }
    }

    override suspend fun downloadDefaultImage(): Flow<ImageDownloadProgress> {
        val defaultImage = ImageInfo(
            id = ImageConstants.DEFAULT_IMAGE_ID,
            name = ImageConstants.DEFAULT_IMAGE_NAME,
            url = ImageConstants.DEFAULT_IMAGE_URL,
            localPath = cache.getLocalPath(ImageConstants.DEFAULT_IMAGE_ID),
            checksumType = ImageConstants.DEFAULT_CHECKSUM_TYPE
        )
        return downloadImage(defaultImage)
    }

    override suspend fun downloadImage(imageInfo: ImageInfo): Flow<ImageDownloadProgress> {
        return mutex.withLock {
            // 检查是否已缓存
            if (cache.isCached(imageInfo.id)) {
                val cachedInfo = cache.getCachedImage(imageInfo.id)
                if (cachedInfo != null && cachedInfo.isReady) {
                    _currentImageInfo.value = cachedInfo
                    _currentImageState.value = ImageState.READY
                    return@withLock kotlinx.coroutines.flow.flow {
                        emit(
                            ImageDownloadProgress(
                                imageId = imageInfo.id,
                                downloadedBytes = cachedInfo.size,
                                totalBytes = cachedInfo.size,
                                state = ImageState.READY
                            )
                        )
                    }
                }
            }

            // 准备下载
            val localPath = cache.getLocalPath(imageInfo.id)
            val updatedInfo = imageInfo.copy(localPath = localPath)
            _currentImageInfo.value = updatedInfo
            _currentImageState.value = ImageState.DOWNLOADING
            currentDownloadImageId = imageInfo.id

            val request = ImageDownloadRequest(
                url = imageInfo.url,
                targetPath = localPath,
                checksum = imageInfo.checksum,
                checksumType = imageInfo.checksumType ?: ImageConstants.DEFAULT_CHECKSUM_TYPE
            )

            downloader.download(request)
        }.also { flow ->
            flow.collect { progress ->
                _downloadProgress.value = progress
                _currentImageState.value = progress.state

                if (progress.state == ImageState.DOWNLOADED) {
                    // 下载完成，进行验证
                    validateAndCache(imageInfo.id, progress.downloadedBytes)
                }
            }
        }
    }

    private suspend fun validateAndCache(imageId: String, downloadedSize: Long) {
        val imageInfo = _currentImageInfo.value ?: return

        // 验证镜像
        _currentImageState.value = ImageState.VERIFYING
        val validationResult = validator.validate(imageInfo)

        if (validationResult.isValid) {
            // 保存到缓存
            val finalInfo = imageInfo.copy(
                size = downloadedSize,
                state = ImageState.READY,
                updatedAt = System.currentTimeMillis()
            )
            cache.saveToCache(finalInfo)

            _currentImageInfo.value = finalInfo
            _currentImageState.value = ImageState.READY
        } else {
            _currentImageState.value = ImageState.ERROR
            _currentImageInfo.value = imageInfo.copy(
                errorMessage = validationResult.errorMessage
            )
        }
    }

    override suspend fun cancelDownload() {
        mutex.withLock {
            currentDownloadImageId?.let { imageId ->
                downloader.cancelDownload(imageId)
            }
            currentDownloadImageId = null
            _currentImageState.value = ImageState.NOT_DOWNLOADED
            _downloadProgress.value = null
        }
    }

    override suspend fun getImageInfo(imageId: String): ImageInfo? {
        return cache.getCachedImage(imageId)
    }

    override suspend fun getCachedImages(): List<ImageInfo> {
        return cache.getAllCachedImages()
    }

    override suspend fun isImageCached(imageId: String): Boolean {
        return cache.isCached(imageId)
    }

    override suspend fun validateImage(imageId: String): ImageValidationResult {
        val imageInfo = cache.getCachedImage(imageId)
            ?: return ImageValidationResult(
                imageId = imageId,
                isValid = false,
                errorMessage = "Image not found: $imageId"
            )

        return validator.validate(imageInfo)
    }

    override suspend fun deleteImage(imageId: String): Boolean {
        return mutex.withLock {
            val result = cache.removeFromCache(imageId)

            // 如果删除的是当前镜像，重置状态
            if (result && _currentImageInfo.value?.id == imageId) {
                _currentImageInfo.value = null
                _currentImageState.value = ImageState.NOT_DOWNLOADED
            }

            result
        }
    }

    override suspend fun getImageLocalPath(imageId: String): String? {
        val imageInfo = cache.getCachedImage(imageId)
        return imageInfo?.localPath
    }

    override suspend fun prepareImage(imageId: String): Result<String> {
        return mutex.withLock {
            try {
                // 检查缓存
                val cachedImage = cache.getCachedImage(imageId)
                if (cachedImage != null && cachedImage.isReady) {
                    cache.updateAccessTime(imageId)
                    return@withLock Result.success(cachedImage.localPath!!)
                }

                // 查找预定义镜像
                val predefinedImage = ImageConstants.PREDEFINED_IMAGES.find { it.id == imageId }
                if (predefinedImage != null) {
                    // 下载镜像
                    downloadImage(predefinedImage).collect { progress ->
                        _downloadProgress.value = progress
                    }

                    val finalInfo = _currentImageInfo.value
                    if (finalInfo != null && finalInfo.isReady) {
                        return@withLock Result.success(finalInfo.localPath!!)
                    } else {
                        return@withLock Result.failure(
                            ImageError.UnknownError("Failed to prepare image: $imageId")
                        )
                    }
                } else {
                    return@withLock Result.failure(
                        ImageError.ConfigurationError("Unknown image: $imageId")
                    )
                }
            } catch (e: Exception) {
                Result.failure(ImageError.UnknownError("Failed to prepare image: ${e.message}", e))
            }
        }
    }

    override suspend fun getCacheStats(): CacheStats {
        return cache.getCacheStats()
    }

    override suspend fun clearCache(keepRecent: Int): Long {
        return mutex.withLock {
            val clearedSize = cache.clearCache(keepRecent)

            // 重置当前镜像状态
            _currentImageInfo.value = null
            _currentImageState.value = ImageState.NOT_DOWNLOADED

            clearedSize
        }
    }

    override fun getPredefinedImages(): List<ImageInfo> {
        return ImageConstants.PREDEFINED_IMAGES
    }

    override suspend fun release() {
        mutex.withLock {
            currentDownloadImageId?.let { imageId ->
                downloader.cancelDownload(imageId)
            }
            currentDownloadImageId = null
            _currentImageInfo.value = null
            _currentImageState.value = ImageState.NOT_DOWNLOADED
            _downloadProgress.value = null
            isInitialized = false
        }
    }
}