package com.dfa.core.docker.provider

/**
 * Docker Provider信息数据类
 *
 * 包含Provider的详细信息和状态数据。
 *
 * @property providerId Provider唯一标识符
 * @property providerType Provider类型
 * @property state 当前状态
 * @property version Docker版本信息
 * @property apiVersion Docker API版本
 * @property operatingSystem 操作系统信息
 * @property architecture 系统架构
 * @property cpus CPU核心数
 * @property memoryTotal 总内存（字节）
 * @property memoryUsed 已用内存（字节）
 * @property containersTotal 容器总数
 * @property containersRunning 运行中容器数
 * @property containersStopped 已停止容器数
 * @property containersPaused 暂停容器数
 * @property imagesTotal 镜像总数
 * @property dockerRootDir Docker根目录
 * @property storageDriver 存储驱动
 * @property createdAt 创建时间戳
 * @property lastUpdated 最后更新时间戳
 * @property metadata 扩展元数据
 * @since 1.0.0
 */
data class DockerProviderInfo(
    val providerId: String,
    val providerType: DockerProviderType,
    val state: DockerProviderState,
    val version: String? = null,
    val apiVersion: String? = null,
    val operatingSystem: String? = null,
    val architecture: String? = null,
    val cpus: Int? = null,
    val memoryTotal: Long? = null,
    val memoryUsed: Long? = null,
    val containersTotal: Int? = null,
    val containersRunning: Int? = null,
    val containersStopped: Int? = null,
    val containersPaused: Int? = null,
    val imagesTotal: Int? = null,
    val dockerRootDir: String? = null,
    val storageDriver: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 更新最后修改时间
     */
    fun touch(): DockerProviderInfo = copy(lastUpdated = System.currentTimeMillis())

    /**
     * 添加元数据
     */
    fun withMetadata(key: String, value: Any): DockerProviderInfo =
        copy(metadata = metadata + (key to value), lastUpdated = System.currentTimeMillis())

    /**
     * 检查信息是否有效
     */
    val isValid: Boolean
        get() = providerId.isNotEmpty() && providerType != DockerProviderType.UNKNOWN

    /**
     * 获取内存使用率
     *
     * @return 内存使用率（0-100），如果无法计算则返回null
     */
    val memoryUsagePercent: Double?
        get() {
            if (memoryTotal == null || memoryUsed == null || memoryTotal == 0L) return null
            return (memoryUsed.toDouble() / memoryTotal.toDouble()) * 100
        }

    /**
     * 获取运行中容器占比
     *
     * @return 运行中容器占比（0-100），如果无法计算则返回null
     */
    val runningContainerPercent: Double?
        get() {
            if (containersTotal == null || containersRunning == null || containersTotal == 0) return null
            return (containersRunning.toDouble() / containersTotal.toDouble()) * 100
        }

    /**
     * 获取信息摘要
     */
    val summary: String
        get() = buildString {
            append("Provider[$providerId]")
            append(" type=$providerType")
            append(" state=$state")
            version?.let { append(" version=$it") }
            cpus?.let { append(" cpus=$it") }
            memoryTotal?.let { append(" memory=${it / 1024 / 1024}MB") }
        }
}