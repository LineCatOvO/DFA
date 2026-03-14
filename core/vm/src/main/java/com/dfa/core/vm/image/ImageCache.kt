package com.dfa.core.vm.image

/**
 * 镜像缓存接口
 *
 * 提供镜像缓存管理功能
 */
interface ImageCache {

    /**
     * 获取缓存的镜像信息
     *
     * @param imageId 镜像ID
     * @return 缓存的镜像信息，如果不存在则返回null
     */
    suspend fun getCachedImage(imageId: String): ImageInfo?

    /**
     * 获取所有缓存的镜像列表
     *
     * @return 缓存的镜像列表
     */
    suspend fun getAllCachedImages(): List<ImageInfo>

    /**
     * 保存镜像到缓存
     *
     * @param imageInfo 镜像信息
     * @return 是否保存成功
     */
    suspend fun saveToCache(imageInfo: ImageInfo): Boolean

    /**
     * 从缓存中删除镜像
     *
     * @param imageId 镜像ID
     * @return 是否删除成功
     */
    suspend fun removeFromCache(imageId: String): Boolean

    /**
     * 检查镜像是否已缓存
     *
     * @param imageId 镜像ID
     * @return 是否已缓存
     */
    suspend fun isCached(imageId: String): Boolean

    /**
     * 获取缓存大小
     *
     * @return 缓存总大小（字节）
     */
    suspend fun getCacheSize(): Long

    /**
     * 清理缓存
     *
     * @param keepRecent 保留最近N个镜像
     * @return 清理的字节数
     */
    suspend fun clearCache(keepRecent: Int = 0): Long

    /**
     * 获取缓存目录路径
     *
     * @return 缓存目录路径
     */
    fun getCacheDirectory(): String

    /**
     * 获取镜像本地路径
     *
     * @param imageId 镜像ID
     * @return 本地路径
     */
    fun getLocalPath(imageId: String): String

    /**
     * 更新镜像访问时间
     *
     * @param imageId 镜像ID
     */
    suspend fun updateAccessTime(imageId: String)

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计
     */
    suspend fun getCacheStats(): CacheStats
}

/**
 * 缓存统计信息
 *
 * @property totalImages 镜像总数
 * @property totalSize 总大小（字节）
 * @property oldestAccessTime 最早访问时间
 * @property newestAccessTime 最新访问时间
 * @property averageSize 平均大小（字节）
 */
data class CacheStats(
    val totalImages: Int,
    val totalSize: Long,
    val oldestAccessTime: Long? = null,
    val newestAccessTime: Long? = null,
    val averageSize: Long = if (totalImages > 0) totalSize / totalImages else 0
)