package com.dfa.core.vm.image

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预定义镜像提供者接口
 *
 * 提供预定义镜像的查询、搜索和过滤功能
 */
interface PredefinedImageProvider {

    /**
     * 获取所有预定义镜像
     *
     * @return 所有预定义镜像列表
     */
    fun getAllImages(): List<PredefinedImageSource>

    /**
     * 根据架构获取镜像列表
     *
     * @param arch 目标架构
     * @return 匹配的镜像列表
     */
    fun getImagesByArchitecture(arch: ImageArchitecture): List<PredefinedImageSource>

    /**
     * 根据操作系统类型获取镜像列表
     *
     * @param osType 操作系统类型
     * @return 匹配的镜像列表
     */
    fun getImagesByOsType(osType: OsType): List<PredefinedImageSource>

    /**
     * 根据ID获取镜像
     *
     * @param id 镜像ID
     * @return 镜像源，如果不存在则返回null
     */
    fun getImageById(id: String): PredefinedImageSource?

    /**
     * 搜索镜像
     *
     * @param query 搜索关键词
     * @return 匹配的镜像列表
     */
    fun searchImages(query: String): List<PredefinedImageSource>

    /**
     * 获取推荐镜像列表
     *
     * @return 推荐镜像列表
     */
    fun getRecommendedImages(): List<PredefinedImageSource>

    /**
     * 获取最小镜像列表
     *
     * @return 最小镜像列表
     */
    fun getMinimalImages(): List<PredefinedImageSource>

    /**
     * 获取默认镜像
     *
     * @return 默认镜像
     */
    fun getDefaultImage(): PredefinedImageSource

    /**
     * 根据标签获取镜像列表
     *
     * @param tag 标签名称
     * @return 匹配的镜像列表
     */
    fun getImagesByTag(tag: String): List<PredefinedImageSource>

    /**
     * 获取所有可用的架构列表
     *
     * @return 架构列表
     */
    fun getAvailableArchitectures(): List<ImageArchitecture>

    /**
     * 获取所有可用的操作系统类型列表
     *
     * @return 操作系统类型列表
     */
    fun getAvailableOsTypes(): List<OsType>
}

/**
 * 预定义镜像提供者实现
 *
 * 基于PredefinedImageRegistry提供镜像查询功能
 */
@Singleton
class PredefinedImageProviderImpl @Inject constructor() : PredefinedImageProvider {

    override fun getAllImages(): List<PredefinedImageSource> {
        return PredefinedImageRegistry.ALL_IMAGES
    }

    override fun getImagesByArchitecture(arch: ImageArchitecture): List<PredefinedImageSource> {
        return PredefinedImageRegistry.BY_ARCHITECTURE[arch] ?: emptyList()
    }

    override fun getImagesByOsType(osType: OsType): List<PredefinedImageSource> {
        return PredefinedImageRegistry.BY_OS_TYPE[osType] ?: emptyList()
    }

    override fun getImageById(id: String): PredefinedImageSource? {
        return PredefinedImageRegistry.getById(id)
    }

    override fun searchImages(query: String): List<PredefinedImageSource> {
        return PredefinedImageRegistry.search(query)
    }

    override fun getRecommendedImages(): List<PredefinedImageSource> {
        return PredefinedImageRegistry.RECOMMENDED_IMAGES
    }

    override fun getMinimalImages(): List<PredefinedImageSource> {
        return PredefinedImageRegistry.MINIMAL_IMAGES
    }

    override fun getDefaultImage(): PredefinedImageSource {
        return PredefinedImageRegistry.DEFAULT_IMAGE
    }

    override fun getImagesByTag(tag: String): List<PredefinedImageSource> {
        return PredefinedImageRegistry.ALL_IMAGES.filter { image ->
            tag.lowercase() in image.tags.map { it.lowercase() }
        }
    }

    override fun getAvailableArchitectures(): List<ImageArchitecture> {
        return PredefinedImageRegistry.ALL_IMAGES.map { it.architecture }.distinct()
    }

    override fun getAvailableOsTypes(): List<OsType> {
        return PredefinedImageRegistry.ALL_IMAGES.map { it.osType }.distinct()
    }
}