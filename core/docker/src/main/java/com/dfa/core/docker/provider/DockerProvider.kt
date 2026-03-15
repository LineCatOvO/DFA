package com.dfa.core.docker.provider

import com.dfa.core.docker.DockerClient

/**
 * Docker Provider接口
 *
 * 定义Docker运行环境提供者的核心契约。
 * 支持多种Docker运行环境：本地Docker、QEMU虚拟机、AVF虚拟机等。
 *
 * 设计模式：Strategy Pattern
 *
 * 生命周期：
 * 1. CREATED -> initialize() -> INITIALIZED
 * 2. INITIALIZED -> start() -> RUNNING
 * 3. RUNNING -> stop() -> STOPPED
 * 4. STOPPED/INITIALIZED/ERROR -> destroy() -> DESTROYED
 *
 * @since 1.0.0
 */
interface DockerProvider {

    /**
     * 获取Provider类型
     *
     * @return Docker Provider类型
     */
    val providerType: DockerProviderType

    /**
     * 获取Provider ID
     *
     * @return Provider唯一标识符
     */
    val providerId: String

    /**
     * 初始化Provider
     *
     * 执行Provider初始化操作，包括资源分配、环境准备等。
     * 初始化完成后Provider进入INITIALIZED状态。
     *
     * @return 初始化结果
     */
    suspend fun initialize(): Result<Unit>

    /**
     * 启动Provider
     *
     * 启动Provider，使其进入可服务状态。
     * 启动成功后Provider进入RUNNING状态。
     *
     * @return 启动结果
     */
    suspend fun start(): Result<Unit>

    /**
     * 停止Provider
     *
     * 停止Provider服务，释放部分资源。
     * 停止后Provider进入STOPPED状态，可以重新启动。
     *
     * @param force 是否强制停止
     * @return 停止结果
     */
    suspend fun stop(force: Boolean = false): Result<Unit>

    /**
     * 销毁Provider
     *
     * 完全销毁Provider，释放所有资源。
     * 销毁后Provider进入DESTROYED状态，无法再使用。
     *
     * @return 销毁结果
     */
    suspend fun destroy(): Result<Unit>

    /**
     * 获取Provider状态
     *
     * @return 当前Provider状态
     */
    fun getState(): DockerProviderState

    /**
     * 获取Provider信息
     *
     * @return Provider详细信息
     */
    suspend fun getInfo(): Result<DockerProviderInfo>

    /**
     * 检查Provider是否可用
     *
     * @return 如果Provider可用返回true
     */
    suspend fun isAvailable(): Boolean

    /**
     * 获取Docker客户端
     *
     * 获取用于与Docker守护进程交互的客户端实例。
     * Provider必须处于RUNNING状态才能获取客户端。
     *
     * @return Docker客户端实例
     * @throws ProviderStateException 如果Provider不在RUNNING状态
     */
    fun getDockerClient(): DockerClient

    /**
     * 注册状态回调
     *
     * @param callback 状态回调接口
     */
    fun registerCallback(callback: DockerProviderCallback)

    /**
     * 注销状态回调
     *
     * @param callback 状态回调接口
     */
    fun unregisterCallback(callback: DockerProviderCallback)

    /**
     * 检查是否支持指定特性
     *
     * @param feature 特性
     * @return 如果支持返回true
     */
    fun supportsFeature(feature: DockerProviderFeature): Boolean

    /**
     * 获取支持的特性列表
     *
     * @return 支持的特性集合
     */
    fun getSupportedFeatures(): Set<DockerProviderFeature>
}

/**
 * Docker Provider状态回调接口
 *
 * 用于接收Provider状态变化通知。
 *
 * @since 1.0.0
 */
interface DockerProviderCallback {
    /**
     * 状态变化回调
     *
     * @param providerId Provider标识符
     * @param oldState 旧状态
     * @param newState 新状态
     */
    fun onStateChanged(providerId: String, oldState: DockerProviderState, newState: DockerProviderState)

    /**
     * 错误回调
     *
     * @param providerId Provider标识符
     * @param error 错误信息
     */
    fun onError(providerId: String, error: Throwable)

    /**
     * 可用性变化回调
     *
     * @param providerId Provider标识符
     * @param available 是否可用
     */
    fun onAvailabilityChanged(providerId: String, available: Boolean)
}

/**
 * Docker Provider特性枚举
 *
 * 定义Provider可能支持的特性。
 *
 * @since 1.0.0
 */
enum class DockerProviderFeature {
    /**
     * 支持Docker API
     */
    DOCKER_API,

    /**
     * 支持Docker Compose
     */
    DOCKER_COMPOSE,

    /**
     * 支持Docker Swarm
     */
    DOCKER_SWARM,

    /**
     * 支持Kubernetes
     */
    KUBERNETES,

    /**
     * 支持GPU
     */
    GPU_SUPPORT,

    /**
     * 支持实时迁移
     */
    LIVE_MIGRATION,

    /**
     * 支持快照
     */
    SNAPSHOTS,

    /**
     * 支持资源限制
     */
    RESOURCE_LIMITS,

    /**
     * 支持网络隔离
     */
    NETWORK_ISOLATION,

    /**
     * 支持持久化存储
     */
    PERSISTENT_STORAGE,

    /**
     * 支持健康检查
     */
    HEALTH_CHECK,

    /**
     * 支持日志收集
     */
    LOG_COLLECTION,

    /**
     * 支持监控指标
     */
    METRICS,

    /**
     * 支持TLS
     */
    TLS_SUPPORT
}

// ==================== 扩展函数 ====================

/**
 * DockerProvider扩展函数：初始化并启动
 *
 * @return 启动结果
 */
suspend fun DockerProvider.initializeAndStart(): Result<Unit> {
    return initialize().getOrElse {
        return Result.failure(it)
    }.let {
        start()
    }
}

/**
 * DockerProvider扩展函数：安全停止并销毁
 *
 * @param force 是否强制停止
 * @return 销毁结果
 */
suspend fun DockerProvider.stopAndDestroy(force: Boolean = false): Result<Unit> {
    val state = getState()
    return when {
        state == DockerProviderState.RUNNING -> {
            stop(force).getOrElse {
                if (!force) {
                    stop(force = true)
                } else {
                    return Result.failure(it)
                }
            }
            destroy()
        }
        state in listOf(DockerProviderState.INITIALIZED, DockerProviderState.STOPPED, DockerProviderState.ERROR) -> {
            destroy()
        }
        else -> Result.success(Unit)
    }
}

/**
 * DockerProvider扩展函数：等待Provider就绪
 *
 * @param timeoutMs 超时时间（毫秒）
 * @param checkIntervalMs 检查间隔（毫秒）
 * @return 是否成功就绪
 */
suspend fun DockerProvider.waitForReady(
    timeoutMs: Long = 60000L,
    checkIntervalMs: Long = 1000L
): Result<Boolean> {
    val startTime = System.currentTimeMillis()
    
    while (System.currentTimeMillis() - startTime < timeoutMs) {
        if (getState() == DockerProviderState.RUNNING && isAvailable()) {
            return Result.success(true)
        }
        kotlinx.coroutines.delay(checkIntervalMs)
    }
    
    return Result.failure(
        ProviderTimeoutException(
            providerId = providerId,
            operation = "waitForReady",
            timeoutMs = timeoutMs
        )
    )
}