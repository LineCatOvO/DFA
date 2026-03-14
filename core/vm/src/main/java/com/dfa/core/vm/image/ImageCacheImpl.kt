package com.dfa.core.vm.image

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 镜像缓存实现
 *
 * 使用文件系统存储镜像，内存缓存元数据
 */
@Singleton
class ImageCacheImpl @Inject constructor(
    private val context: Context
) : ImageCache {

    private val cacheDir: File by lazy {
        File(context.filesDir, ImageConstants.IMAGE_SUBDIRECTORY).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val metadataCache = ConcurrentHashMap<String, ImageInfo>()

    init {
        // 初始化时加载已有镜像的元数据
        loadExistingMetadata()
    }

    private fun loadExistingMetadata() {
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && isImageFile(file.name)) {
                val imageId = file.nameWithoutExtension
                metadataCache[imageId] = ImageInfo(
                    id = imageId,
                    name = file.name,
                    url = "",
                    localPath = file.absolutePath,
                    size = file.length(),
                    state = ImageState.CACHED,
                    updatedAt = file.lastModified()
                )
            }
        }
    }

    private fun isImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast(".", "")
        return extension.lowercase() in ImageConstants.SUPPORTED_FORMATS
    }

    override suspend fun getCachedImage(imageId: String): ImageInfo? {
        return withContext(Dispatchers.IO) {
            val cached = metadataCache[imageId]
            if (cached != null) {
                // 验证文件是否存在
                val file = File(cached.localPath ?: return@withContext null)
                if (file.exists()) {
                    cached
                } else {
                    // 文件不存在，清理缓存
                    metadataCache.remove(imageId)
                    null
                }
            } else {
                null
            }
        }
    }

    override suspend fun getAllCachedImages(): List<ImageInfo> {
        return withContext(Dispatchers.IO) {
            // 刷新缓存，移除不存在的文件
            val validImages = mutableListOf<ImageInfo>()
            val toRemove = mutableListOf<String>()

            metadataCache.forEach { (id, info) ->
                val file = info.localPath?.let { File(it) }
                if (file?.exists() == true) {
                    validImages.add(info)
                } else {
                    toRemove.add(id)
                }
            }

            toRemove.forEach { metadataCache.remove(it) }
            validImages
        }
    }

    override suspend fun saveToCache(imageInfo: ImageInfo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val localPath = imageInfo.localPath
                if (localPath == null) {
                    return@withContext false
                }

                val file = File(localPath)
                if (!file.exists()) {
                    return@withContext false
                }

                // 更新元数据
                val updatedInfo = imageInfo.copy(
                    state = ImageState.CACHED,
                    updatedAt = System.currentTimeMillis()
                )
                metadataCache[imageInfo.id] = updatedInfo

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun removeFromCache(imageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imageInfo = metadataCache[imageId]
                if (imageInfo != null) {
                    // 删除文件
                    imageInfo.localPath?.let { path ->
                        File(path).delete()
                    }
                    // 移除元数据
                    metadataCache.remove(imageId)
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun isCached(imageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val info = metadataCache[imageId]
            if (info != null) {
                val file = info.localPath?.let { File(it) }
                file?.exists() == true
            } else {
                false
            }
        }
    }

    override suspend fun getCacheSize(): Long {
        return withContext(Dispatchers.IO) {
            var totalSize = 0L
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                }
            }
            totalSize
        }
    }

    override suspend fun clearCache(keepRecent: Int): Long {
        return withContext(Dispatchers.IO) {
            var clearedSize = 0L

            if (keepRecent <= 0) {
                // 清理所有缓存
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        clearedSize += file.length()
                        file.delete()
                    }
                }
                metadataCache.clear()
            } else {
                // 按访问时间排序，保留最近的N个
                val sortedImages = metadataCache.values
                    .sortedByDescending { it.updatedAt }

                val toKeep = sortedImages.take(keepRecent).map { it.id }.toSet()

                metadataCache.forEach { (id, info) ->
                    if (id !in toKeep) {
                        info.localPath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                clearedSize += file.length()
                                file.delete()
                            }
                        }
                        metadataCache.remove(id)
                    }
                }
            }

            clearedSize
        }
    }

    override fun getCacheDirectory(): String {
        return cacheDir.absolutePath
    }

    override fun getLocalPath(imageId: String): String {
        // 尝试找到已存在的文件
        val existingInfo = metadataCache[imageId]
        if (existingInfo?.localPath != null) {
            return existingInfo.localPath!!
        }

        // 生成新路径
        return File(cacheDir, "$imageId.qcow2").absolutePath
    }

    override suspend fun updateAccessTime(imageId: String) {
        withContext(Dispatchers.IO) {
            metadataCache[imageId]?.let { info ->
                metadataCache[imageId] = info.copy(
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    override suspend fun getCacheStats(): CacheStats {
        return withContext(Dispatchers.IO) {
            val images = getAllCachedImages()
            val totalSize = images.sumOf { it.size }

            val oldestTime = images.minOfOrNull { it.updatedAt }
            val newestTime = images.maxOfOrNull { it.updatedAt }

            CacheStats(
                totalImages = images.size,
                totalSize = totalSize,
                oldestAccessTime = oldestTime,
                newestAccessTime = newestTime
            )
        }
    }
}