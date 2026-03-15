package com.dfa.core.docker.provider

import kotlinx.coroutines.flow.StateFlow

/**
 * Docker Provider管理器接口
 *
 * 负责管理多个DockerProvider实例，提供Provider的注册、查询、切换和创建功能。
 * 支持Provider的热切换和状态监控。
 *
 * 设计模式：
 * - Manager Pattern: 管理多个Provider实例
 * - Observer Pattern: 通过StateFlow提供状态观察
 *
 * 核心职责：
 * - Provider注册与注销
 * - Provider查询与选择
 * - Provider切换与激活
 * - Provider创建与销毁
 *
 * @since 1.0.0
 */
interface DockerProviderManager {

    // ==================== 状态属性 ====================

    /**
     * 当前活动的Provider
     *
     * 返回当前正在使用的Provider实例，如果没有活动的Provider则返回null。
     *
     * @return 活动的Provider实例，或null
     */
    val activeProvider: DockerProvider?

    /**
     * 活动Provider的StateFlow
     *
     * 用于观察活动Provider的变化。
     *
     * @return 活动Provider的StateFlow
     */
    val activeProviderFlow: StateFlow<DockerProvider?>

    /**
     * 所有已注册的Provider列表
     *
     * @return Provider列表
     */
    val providers: List<DockerProvider>

    /**
     * Provider数量的StateFlow
     *
     * @return Provider数量的StateFlow
     */
    val providerCountFlow: StateFlow<Int>

    // ==================== Provider注册方法 ====================

    /**
     * 注册Provider
     *
     * 将Provider实例注册到管理器中。注册后Provider可以被查询和激活。
     *
     * @param provider 要注册的Provider实例
     * @return 注册结果
     * @throws ProviderConfigException 如果Provider ID已存在
     */
    suspend fun registerProvider(provider: DockerProvider): Result<Unit>

    /**
     * 注销Provider
     *
     * 从管理器中移除Provider。如果Provider是活动的，会先切换到其他可用Provider。
     *
     * @param providerId 要注销的Provider ID
     * @param destroy 是否销毁Provider实例
     * @return 注销结果
     */
    suspend fun unregisterProvider(providerId: String, destroy: Boolean = false): Result<Unit>

    // ==================== Provider查询方法 ====================

    /**
     * 获取Provider
     *
     * 根据Provider ID获取Provider实例。
     *
     * @param providerId Provider ID
     * @return Provider实例，如果不存在则返回null
     */
    fun getProvider(providerId: String): DockerProvider?

    /**
     * 获取活动的Provider
     *
     * 返回当前活动的Provider实例。
     *
     * @return 活动的Provider实例，如果没有则返回null
     */
    fun getActiveProvider(): DockerProvider?

    /**
     * 获取所有Provider列表
     *
     * @return 所有已注册的Provider列表
     */
    fun listProviders(): List<DockerProvider>

    /**
     * 获取指定类型的Provider列表
     *
     * @param type Provider类型
     * @return 指定类型的Provider列表
     */
    fun listProvidersByType(type: DockerProviderType): List<DockerProvider>

    /**
     * 获取可用的Provider列表
     *
     * 返回所有处于可用状态的Provider列表。
     *
     * @return 可用的Provider列表
     */
    suspend fun listAvailableProviders(): List<DockerProvider>

    /**
     * 检查Provider是否存在
     *
     * @param providerId Provider ID
     * @return 如果存在返回true
     */
    fun hasProvider(providerId: String): Boolean

    /**
     * 获取Provider信息
     *
     * @param providerId Provider ID
     * @return Provider信息，如果不存在则返回null
     */
    suspend fun getProviderInfo(providerId: String): DockerProviderInfo?

    // ==================== Provider切换方法 ====================

    /**
     * 设置活动Provider
     *
     * 将指定的Provider设置为活动Provider。
     * 如果当前有活动的Provider，会先将其停止（可选）。
     *
     * @param providerId 要激活的Provider ID
     * @param stopCurrent 是否停止当前活动的Provider
     * @return 切换结果
     */
    suspend fun setActiveProvider(providerId: String, stopCurrent: Boolean = false): Result<Unit>

    /**
     * 切换到最佳可用Provider
     *
     * 自动选择并激活最佳可用的Provider。
     * 选择依据：优先级 > 状态 > 类型
     *
     * @return 切换结果
     */
    suspend fun switchToBestAvailable(): Result<DockerProvider>

    // ==================== Provider创建方法 ====================

    /**
     * 创建Provider
     *
     * 使用工厂创建并注册Provider实例。
     *
     * @param config Provider配置
     * @param autoActivate 是否自动激活
     * @return 创建结果，包含Provider实例
     */
    suspend fun createProvider(
        config: DockerProviderConfig,
        autoActivate: Boolean = false
    ): Result<DockerProvider>

    /**
     * 创建并初始化Provider
     *
     * 创建Provider实例并执行初始化操作。
     *
     * @param config Provider配置
     * @param autoActivate 是否自动激活
     * @return 创建结果，包含已初始化的Provider实例
     */
    suspend fun createAndInitializeProvider(
        config: DockerProviderConfig,
        autoActivate: Boolean = false
    ): Result<DockerProvider>

    /**
     * 创建并启动Provider
     *
     * 创建Provider实例，执行初始化并启动。
     *
     * @param config Provider配置
     * @param autoActivate 是否自动激活
     * @return 创建结果，包含已启动的Provider实例
     */
    suspend fun createAndStartProvider(
        config: DockerProviderConfig,
        autoActivate: Boolean = false
    ): Result<DockerProvider>

    // ==================== 批量操作方法 ====================

    /**
     * 初始化所有Provider
     *
     * 初始化所有已注册但未初始化的Provider。
     *
     * @return 初始化结果映射（Provider ID -> 结果）
     */
    suspend fun initializeAll(): Map<String, Result<Unit>>

    /**
     * 启动所有Provider
     *
     * 启动所有已初始化但未运行的Provider。
     *
     * @return 启动结果映射（Provider ID -> 结果）
     */
    suspend fun startAll(): Map<String, Result<Unit>>

    /**
     * 停止所有Provider
     *
     * 停止所有正在运行的Provider。
     *
     * @param force 是否强制停止
     * @return 停止结果映射（Provider ID -> 结果）
     */
    suspend fun stopAll(force: Boolean = false): Map<String, Result<Unit>>

    /**
     * 销毁所有Provider
     *
     * 销毁所有Provider并清空注册表。
     *
     * @return 销毁结果映射（Provider ID -> 结果）
     */
    suspend fun destroyAll(): Map<String, Result<Unit>>

    // ==================== 状态监控方法 ====================

    /**
     * 获取管理器状态
     *
     * @return 管理器状态
     */
    fun getManagerState(): DockerProviderManagerState

    /**
     * 获取管理器状态流
     *
     * @return 管理器状态的StateFlow
     */
    fun getManagerStateFlow(): StateFlow<DockerProviderManagerState>

    /**
     * 检查是否有活动的Provider
     *
     * @return 如果有活动的Provider返回true
     */
    fun hasActiveProvider(): Boolean

    /**
     * 检查活动Provider是否可用
     *
     * @return 如果活动Provider可用返回true
     */
    suspend fun isActiveProviderAvailable(): Boolean
}

/**
 * Docker Provider管理器状态
 *
 * @property totalProviders Provider总数
 * @property activeProviderId 活动Provider ID
 * @property activeProviderState 活动Provider状态
 * @property availableCount 可用Provider数量
 * @property runningCount 运行中Provider数量
 * @property errorCount 错误状态Provider数量
 * @since 1.0.0
 */
data class DockerProviderManagerState(
    val totalProviders: Int = 0,
    val activeProviderId: String? = null,
    val activeProviderState: DockerProviderState? = null,
    val availableCount: Int = 0,
    val runningCount: Int = 0,
    val errorCount: Int = 0
) {
    /**
     * 检查是否有Provider
     */
    val hasProviders: Boolean
        get() = totalProviders > 0

    /**
     * 检查是否有活动Provider
     */
    val hasActiveProvider: Boolean
        get() = activeProviderId != null

    /**
     * 检查活动Provider是否正在运行
     */
    val isActiveProviderRunning: Boolean
        get() = activeProviderState == DockerProviderState.RUNNING

    /**
     * 获取状态摘要
     */
    val summary: String
        get() = buildString {
            append("DockerProviderManager[")
            append("total=$totalProviders, ")
            append("active=$activeProviderId(${activeProviderState?.name ?: "none"}), ")
            append("available=$availableCount, ")
            append("running=$runningCount, ")
            append("errors=$errorCount")
            append("]")
        }
}

/**
 * Docker Provider管理器回调接口
 *
 * 用于接收管理器状态变化通知。
 *
 * @since 1.0.0
 */
interface DockerProviderManagerCallback {
    /**
     * Provider注册回调
     *
     * @param provider 注册的Provider
     */
    fun onProviderRegistered(provider: DockerProvider)

    /**
     * Provider注销回调
     *
     * @param providerId 注销的Provider ID
     */
    fun onProviderUnregistered(providerId: String)

    /**
     * 活动Provider变化回调
     *
     * @param oldProvider 旧的活动Provider
     * @param newProvider 新的活动Provider
     */
    fun onActiveProviderChanged(oldProvider: DockerProvider?, newProvider: DockerProvider?)

    /**
     * Provider状态变化回调
     *
     * @param providerId Provider ID
     * @param state 新状态
     */
    fun onProviderStateChanged(providerId: String, state: DockerProviderState)

    /**
     * 错误回调
     *
     * @param providerId Provider ID
     * @param error 错误信息
     */
    fun onProviderError(providerId: String, error: Throwable)
}