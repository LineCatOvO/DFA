package com.dfa.core.vm.image

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 镜像管理器接口
 *
 * 提供镜像下载、缓存、验证的统一管理接口
 */
interface ImageManager {

    /**
     * 当前管理的镜像状态流
     */
    val currentImageState: StateFlow<ImageState>

    /**
     * 当前镜像信息流
     */
    val currentImageInfo: StateFlow<ImageInfo?>

    /**
     * 下载进度流
     */
    val downloadProgress: StateFlow<ImageDownloadProgress?>

    /**
     * 初始化镜像管理器
     *
     * @return 初始化结果
     */
    suspend fun initialize(): Result<Unit>

    /**
     * 下载默认镜像
     *
     * @return 下载进度流
     */
    suspend fun downloadDefaultImage(): Flow<ImageDownloadProgress>

    /**
     * 下载指定镜像
     *
     * @param imageInfo 镜像信息
     * @return 下载进度流
     */
    suspend fun downloadImage(imageInfo: ImageInfo): Flow<ImageDownloadProgress>

    /**
     * 取消当前下载
     */
    suspend fun cancelDownload()

    /**
     * 获取镜像信息
     *
     * @param imageId 镜像ID
     * @return 镜像信息
     */
    suspend fun getImageInfo(imageId: String): ImageInfo?

    /**
     * 获取所有缓存的镜像
     *
     * @return 镜像列表
     */
    suspend fun getCachedImages(): List<ImageInfo>

    /**
     * 检查镜像是否已缓存
     *
     * @param imageId 镜像ID
     * @return 是否已缓存
     */
    suspend fun isImageCached(imageId: String): Boolean

    /**
     * 验证镜像
     *
     * @param imageId 镜像ID
     * @return 验证结果
     */
    suspend fun validateImage(imageId: String): ImageValidationResult

    /**
     * 删除镜像
     *
     * @param imageId 镜像ID
     * @return 是否删除成功
     */
    suspend fun deleteImage(imageId: String): Boolean

    /**
     * 获取镜像本地路径
     *
     * @param imageId 镜像ID
     * @return 本地路径，如果不存在则返回null
     */
    suspend fun getImageLocalPath(imageId: String): String?

    /**
     * 准备镜像（下载或从缓存获取）
     *
     * @param imageId 镜像ID
     * @return 镜像本地路径
     */
    suspend fun prepareImage(imageId: String): Result<String>

    /**
     * 获取缓存统计
     *
     * @return 缓存统计
     */
    suspend fun getCacheStats(): CacheStats

    /**
     * 清理缓存
     *
     * @param keepRecent 保留最近N个镜像
     * @return 清理的字节数
     */
    suspend fun clearCache(keepRecent: Int = 0): Long

    /**
     * 获取预定义镜像列表
     *
     * @return 预定义镜像列表
     */
    fun getPredefinedImages(): List<ImageInfo>

    /**
     * 释放资源
     */
    suspend fun release()
}

/**
 * 镜像管理器工厂接口
 */
interface ImageManagerFactory {
    /**
     * 创建镜像管理器实例
     *
     * @return 镜像管理器实例
     */
    fun create(): ImageManager
}