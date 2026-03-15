package com.dfa.core.docker.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Docker Provider管理器实现类
 *
 * 管理多个DockerProvider实例，提供Provider的注册、查询、切换和创建功能。
 * 使用协程进行异步操作，使用StateFlow管理状态。
 *
 * 线程安全：
 * - 使用ConcurrentHashMap存储Provider实例
 * - 使用CopyOnWriteArrayList存储回调
 * - 使用StateFlow进行状态发布
 *
 * @property scope 协程作用域
 * @property factoryRegistry Provider工厂注册表
 * @since 1.0.0
 */
class DockerProviderManagerImpl(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val factoryRegistry: DockerProviderFactoryRegistry = DockerProviderFactoryRegistry
) : DockerProviderManager {

    // ==================== 内部状态 ====================

    /**
     * Provider存储映射
     * Key: Provider ID
     * Value: Provider实例
     */
    private val providerMap = ConcurrentHashMap<String, DockerProvider>()

    /**
     * Provider状态监听Job映射
     */
    private val providerMonitorJobs = ConcurrentHashMap<String, Job>()

    /**
     * 管理器回调列表
     */
    private val callbacks = CopyOnWriteArrayList<DockerProviderManagerCallback>()

    // ==================== StateFlow状态管理 ====================

    /**
     * 活动Provider的MutableStateFlow
     */
    private val _activeProviderFlow = MutableStateFlow<DockerProvider?>(null)

    override val activeProviderFlow: StateFlow<DockerProvider?>
        get() = _activeProviderFlow.asStateFlow()

    override val activeProvider: DockerProvider?
        get() = _activeProviderFlow.value

    /**
     * Provider数量的MutableStateFlow
     */
    private val _providerCountFlow = MutableStateFlow(0)

    override val providerCountFlow: StateFlow<Int>
        get() = _providerCountFlow.asStateFlow()

    /**
     * 管理器状态的MutableStateFlow
     */
    private val _managerStateFlow = MutableStateFlow(DockerProviderManagerState())

    override val providers: List<DockerProvider>
        get() = providerMap.values.toList()

    // ==================== Provider注册方法 ====================

    override suspend fun registerProvider(provider: DockerProvider): Result<Unit> = withContext(Dispatchers.Default) {
        val providerId = provider.providerId

        // 检查是否已存在
        if (providerMap.containsKey(providerId)) {
            return@withContext Result.failure(
                ProviderConfigException(
                    providerId = providerId,
                    configField = "providerId",
                    reason = "Provider with ID '$providerId' already exists"
                )
            )
        }

        try {
            // 注册Provider
            providerMap[providerId] = provider

            // 启动状态监听
            startProviderMonitoring(provider)

            // 更新计数
            updateProviderCount()

            // 更新管理器状态
            updateManagerState()

            // 通知回调
            notifyProviderRegistered(provider)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                ProviderInitializationException(
                    providerId = providerId,
                    reason = "Failed to register provider: ${e.message}",
                    cause = e
                )
            )
        }
    }

    override suspend fun unregisterProvider(providerId: String, destroy: Boolean): Result<Unit> = withContext(Dispatchers.Default) {
        val provider = providerMap[providerId]

        if (provider == null) {
            return@withContext Result.failure(
                ProviderNotFoundException(providerId)
            )
        }

        try {
            // 如果是活动Provider，先切换
            if (activeProvider?.providerId == providerId) {
                val switchResult = switchToOtherProvider(providerId)
                if (switchResult.isFailure && destroy) {
                    // 如果切换失败且需要销毁，清除活动Provider
                    _activeProviderFlow.value = null
                }
            }

            // 停止状态监听
            stopProviderMonitoring(providerId)

            // 从映射中移除
            providerMap.remove(providerId)

            // 如果需要销毁
            if (destroy) {
                provider.destroy()
            }

            // 更新计数
            updateProviderCount()

            // 更新管理器状态
            updateManagerState()

            // 通知回调
            notifyProviderUnregistered(providerId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                ProviderDestroyException(
                    providerId = providerId,
                    reason = "Failed to unregister provider: ${e.message}",
                    cause = e
                )
            )
        }
    }

    // ==================== Provider查询方法 ====================

    override fun getProvider(providerId: String): DockerProvider? {
        return providerMap[providerId]
    }

    override fun getActiveProvider(): DockerProvider? {
        return activeProvider
    }

    override fun listProviders(): List<DockerProvider> {
        return providerMap.values.toList()
    }

    override fun listProvidersByType(type: DockerProviderType): List<DockerProvider> {
        return providerMap.values.filter { it.providerType == type }
    }

    override suspend fun listAvailableProviders(): List<DockerProvider> = withContext(Dispatchers.Default) {
        providerMap.values.filter { provider ->
            provider.isAvailable() && provider.getState() == DockerProviderState.RUNNING
        }
    }

    override fun hasProvider(providerId: String): Boolean {
        return providerMap.containsKey(providerId)
    }

    override suspend fun getProviderInfo(providerId: String): DockerProviderInfo? = withContext(Dispatchers.Default) {
        val provider = providerMap[providerId] ?: return@withContext null
        provider.getInfo().getOrNull()
    }

    // ==================== Provider切换方法 ====================

    override suspend fun setActiveProvider(providerId: String, stopCurrent: Boolean): Result<Unit> = withContext(Dispatchers.Default) {
        val newProvider = providerMap[providerId]

        if (newProvider == null) {
            return@withContext Result.failure(
                ProviderNotFoundException(providerId)
            )
        }

        val currentProvider = activeProvider

        try {
            // 如果需要停止当前Provider
            if (stopCurrent && currentProvider != null && currentProvider.providerId != providerId) {
                currentProvider.stop().getOrElse { error ->
                    // 记录错误但继续切换
                    notifyProviderError(currentProvider.providerId, error)
                }
            }

            // 检查新Provider是否可用
            if (newProvider.getState() != DockerProviderState.RUNNING) {
                // 尝试启动新Provider
                if (newProvider.getState().canStart()) {
                    newProvider.start().getOrElse { error ->
                        return@withContext Result.failure(
                            ProviderStartException(
                                providerId = providerId,
                                reason = "Failed to start provider: ${error.message}",
                                cause = error
                            )
                        )
                    }
                }
            }

            // 更新活动Provider
            val oldProvider = _activeProviderFlow.value
            _activeProviderFlow.value = newProvider

            // 更新管理器状态
            updateManagerState()

            // 通知回调
            notifyActiveProviderChanged(oldProvider, newProvider)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                ProviderStateException(
                    providerId = providerId,
                    currentState = newProvider.getState(),
                    expectedStates = listOf(DockerProviderState.RUNNING)
                )
            )
        }
    }

    override suspend fun switchToBestAvailable(): Result<DockerProvider> = withContext(Dispatchers.Default) {
        val availableProviders = listAvailableProviders()

        if (availableProviders.isEmpty()) {
            // 尝试启动一个Provider
            val stoppedProvider = providerMap.values.firstOrNull { 
                it.getState().canStart() 
            }

            if (stoppedProvider != null) {
                stoppedProvider.start().getOrElse { error ->
                    return@withContext Result.failure(
                        ProviderUnavailableException(
                            providerId = stoppedProvider.providerId,
                            reason = "No available providers and failed to start one: ${error.message}",
                            cause = error
                        )
                    )
                }

                val oldProvider = _activeProviderFlow.value
                _activeProviderFlow.value = stoppedProvider
                updateManagerState()
                notifyActiveProviderChanged(oldProvider, stoppedProvider)

                return@withContext Result.success(stoppedProvider)
            }

            return@withContext Result.failure(
                ProviderUnavailableException(
                    providerId = "manager",
                    reason = "No available providers found"
                )
            )
        }

        // 选择最佳Provider（优先级：运行中 > 已初始化）
        val bestProvider = availableProviders.sortedByDescending { provider ->
            when (provider.getState()) {
                DockerProviderState.RUNNING -> 2
                DockerProviderState.INITIALIZED -> 1
                else -> 0
            }
        }.first()

        val oldProvider = _activeProviderFlow.value
        _activeProviderFlow.value = bestProvider
        updateManagerState()
        notifyActiveProviderChanged(oldProvider, bestProvider)

        Result.success(bestProvider)
    }

    // ==================== Provider创建方法 ====================

    override suspend fun createProvider(
        config: DockerProviderConfig,
        autoActivate: Boolean
    ): Result<DockerProvider> = withContext(Dispatchers.Default) {
        val providerType = config.getProviderType()
        val factory = factoryRegistry.getFactory(providerType)

        if (factory == null) {
            return@withContext Result.failure(
                ProviderConfigException(
                    providerId = config.providerId,
                    configField = "providerType",
                    reason = "No factory registered for type: $providerType"
                )
            )
        }

        // 验证配置
        val validationErrors = factory.validateConfig(config)
        if (validationErrors.isNotEmpty()) {
            return@withContext Result.failure(
                ProviderConfigException(
                    providerId = config.providerId,
                    configField = "config",
                    reason = validationErrors.joinToString("; ")
                )
            )
        }

        // 创建Provider
        val createResult = factory.create(config)

        if (createResult.isFailure) {
            return@withContext Result.failure(
                ProviderInitializationException(
                    providerId = config.providerId,
                    reason = "Failed to create provider: ${createResult.exceptionOrNull()?.message}",
                    cause = createResult.exceptionOrNull()
                )
            )
        }

        val provider = createResult.getOrThrow()

        // 注册Provider
        val registerResult = registerProvider(provider)

        if (registerResult.isFailure) {
            return@withContext Result.failure(
                ProviderInitializationException(
                    providerId = config.providerId,
                    reason = "Failed to register provider: ${registerResult.exceptionOrNull()?.message}",
                    cause = registerResult.exceptionOrNull()
                )
            )
        }

        // 自动激活
        if (autoActivate) {
            setActiveProvider(provider.providerId)
        }

        Result.success(provider)
    }

    override suspend fun createAndInitializeProvider(
        config: DockerProviderConfig,
        autoActivate: Boolean
    ): Result<DockerProvider> = withContext(Dispatchers.Default) {
        val createResult = createProvider(config, autoActivate = false)

        if (createResult.isFailure) {
            return@withContext createResult
        }

        val provider = createResult.getOrThrow()

        // 初始化Provider
        val initResult = provider.initialize()

        if (initResult.isFailure) {
            // 注销失败的Provider
            unregisterProvider(provider.providerId, destroy = true)
            return@withContext Result.failure(
                ProviderInitializationException(
                    providerId = config.providerId,
                    reason = "Failed to initialize provider: ${initResult.exceptionOrNull()?.message}",
                    cause = initResult.exceptionOrNull()
                )
            )
        }

        // 自动激活
        if (autoActivate) {
            setActiveProvider(provider.providerId)
        }

        Result.success(provider)
    }

    override suspend fun createAndStartProvider(
        config: DockerProviderConfig,
        autoActivate: Boolean
    ): Result<DockerProvider> = withContext(Dispatchers.Default) {
        val createResult = createAndInitializeProvider(config, autoActivate = false)

        if (createResult.isFailure) {
            return@withContext createResult
        }

        val provider = createResult.getOrThrow()

        // 启动Provider
        val startResult = provider.start()

        if (startResult.isFailure) {
            // 注销失败的Provider
            unregisterProvider(provider.providerId, destroy = true)
            return@withContext Result.failure(
                ProviderStartException(
                    providerId = config.providerId,
                    reason = "Failed to start provider: ${startResult.exceptionOrNull()?.message}",
                    cause = startResult.exceptionOrNull()
                )
            )
        }

        // 自动激活
        if (autoActivate) {
            setActiveProvider(provider.providerId)
        }

        Result.success(provider)
    }

    // ==================== 批量操作方法 ====================

    override suspend fun initializeAll(): Map<String, Result<Unit>> = withContext(Dispatchers.Default) {
        providerMap.values.associate { provider ->
            val state = provider.getState()
            val result = if (state == DockerProviderState.CREATED) {
                provider.initialize()
            } else {
                Result.success(Unit)
            }
            provider.providerId to result
        }
    }

    override suspend fun startAll(): Map<String, Result<Unit>> = withContext(Dispatchers.Default) {
        providerMap.values.associate { provider ->
            val state = provider.getState()
            val result = if (state.canStart()) {
                provider.start()
            } else {
                Result.success(Unit)
            }
            provider.providerId to result
        }
    }

    override suspend fun stopAll(force: Boolean): Map<String, Result<Unit>> = withContext(Dispatchers.Default) {
        providerMap.values.associate { provider ->
            val state = provider.getState()
            val result = if (state.canStop()) {
                provider.stop(force)
            } else {
                Result.success(Unit)
            }
            provider.providerId to result
        }
    }

    override suspend fun destroyAll(): Map<String, Result<Unit>> = withContext(Dispatchers.Default) {
        val results = mutableMapOf<String, Result<Unit>>()

        // 清除活动Provider
        _activeProviderFlow.value = null

        // 停止所有监听
        providerMonitorJobs.keys.toList().forEach { providerId ->
            stopProviderMonitoring(providerId)
        }

        // 销毁所有Provider
        providerMap.values.forEach { provider ->
            val result = try {
                if (provider.getState().canDestroy()) {
                    provider.destroy()
                } else {
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
            results[provider.providerId] = result
        }

        // 清空映射
        providerMap.clear()

        // 更新状态
        updateProviderCount()
        updateManagerState()

        results
    }

    // ==================== 状态监控方法 ====================

    override fun getManagerState(): DockerProviderManagerState {
        return _managerStateFlow.value
    }

    override fun getManagerStateFlow(): StateFlow<DockerProviderManagerState> {
        return _managerStateFlow.asStateFlow()
    }

    override fun hasActiveProvider(): Boolean {
        return activeProvider != null
    }

    override suspend fun isActiveProviderAvailable(): Boolean = withContext(Dispatchers.Default) {
        val provider = activeProvider ?: return@withContext false
        provider.isAvailable() && provider.getState() == DockerProviderState.RUNNING
    }

    // ==================== 回调管理 ====================

    /**
     * 注册管理器回调
     *
     * @param callback 回调实例
     */
    fun registerCallback(callback: DockerProviderManagerCallback) {
        callbacks.add(callback)
    }

    /**
     * 注销管理器回调
     *
     * @param callback 回调实例
     */
    fun unregisterCallback(callback: DockerProviderManagerCallback) {
        callbacks.remove(callback)
    }

    // ==================== 私有方法 ====================

    /**
     * 启动Provider状态监听
     */
    private fun startProviderMonitoring(provider: DockerProvider) {
        val providerId = provider.providerId

        // 如果已有监听任务，先停止
        stopProviderMonitoring(providerId)

        // 创建状态监听回调
        val callback = object : DockerProviderCallback {
            override fun onStateChanged(pid: String, oldState: DockerProviderState, newState: DockerProviderState) {
                updateManagerState()
                notifyProviderStateChanged(pid, newState)
            }

            override fun onError(pid: String, error: Throwable) {
                notifyProviderError(pid, error)
            }

            override fun onAvailabilityChanged(pid: String, available: Boolean) {
                updateManagerState()
            }
        }

        provider.registerCallback(callback)
    }

    /**
     * 停止Provider状态监听
     */
    private fun stopProviderMonitoring(providerId: String) {
        providerMonitorJobs.remove(providerId)?.cancel()
    }

    /**
     * 切换到其他Provider
     */
    private suspend fun switchToOtherProvider(excludeProviderId: String): Result<Unit> {
        val otherProviders = providerMap.values.filter { 
            it.providerId != excludeProviderId && it.getState() == DockerProviderState.RUNNING 
        }

        if (otherProviders.isEmpty()) {
            _activeProviderFlow.value = null
            return Result.success(Unit)
        }

        val newProvider = otherProviders.first()
        val oldProvider = _activeProviderFlow.value
        _activeProviderFlow.value = newProvider
        updateManagerState()
        notifyActiveProviderChanged(oldProvider, newProvider)

        return Result.success(Unit)
    }

    /**
     * 更新Provider计数
     */
    private fun updateProviderCount() {
        _providerCountFlow.value = providerMap.size
    }

    /**
     * 更新管理器状态
     */
    private fun updateManagerState() {
        val providers = providerMap.values.toList()
        val active = activeProvider

        _managerStateFlow.update { currentState ->
            currentState.copy(
                totalProviders = providers.size,
                activeProviderId = active?.providerId,
                activeProviderState = active?.getState(),
                availableCount = providers.count { it.getState() == DockerProviderState.RUNNING },
                runningCount = providers.count { it.getState() == DockerProviderState.RUNNING },
                errorCount = providers.count { it.getState() == DockerProviderState.ERROR }
            )
        }
    }

    // ==================== 回调通知方法 ====================

    private fun notifyProviderRegistered(provider: DockerProvider) {
        callbacks.forEach { callback ->
            try {
                callback.onProviderRegistered(provider)
            } catch (e: Exception) {
                // 忽略回调异常
            }
        }
    }

    private fun notifyProviderUnregistered(providerId: String) {
        callbacks.forEach { callback ->
            try {
                callback.onProviderUnregistered(providerId)
            } catch (e: Exception) {
                // 忽略回调异常
            }
        }
    }

    private fun notifyActiveProviderChanged(oldProvider: DockerProvider?, newProvider: DockerProvider?) {
        callbacks.forEach { callback ->
            try {
                callback.onActiveProviderChanged(oldProvider, newProvider)
            } catch (e: Exception) {
                // 忽略回调异常
            }
        }
    }

    private fun notifyProviderStateChanged(providerId: String, state: DockerProviderState) {
        callbacks.forEach { callback ->
            try {
                callback.onProviderStateChanged(providerId, state)
            } catch (e: Exception) {
                // 忽略回调异常
            }
        }
    }

    private fun notifyProviderError(providerId: String, error: Throwable) {
        callbacks.forEach { callback ->
            try {
                callback.onProviderError(providerId, error)
            } catch (e: Exception) {
                // 忽略回调异常
            }
        }
    }
}

// ==================== 扩展函数 ====================

/**
 * DockerProviderManager扩展函数：获取或创建Provider
 *
 * 如果Provider已存在则返回，否则创建新的Provider。
 *
 * @param config Provider配置
 * @param autoActivate 是否自动激活
 * @return Provider实例
 */
suspend fun DockerProviderManager.getOrCreateProvider(
    config: DockerProviderConfig,
    autoActivate: Boolean = false
): Result<DockerProvider> {
    val existingProvider = getProvider(config.providerId)
    if (existingProvider != null) {
        if (autoActivate) {
            setActiveProvider(config.providerId)
        }
        return Result.success(existingProvider)
    }

    return createProvider(config, autoActivate)
}

/**
 * DockerProviderManager扩展函数：确保有活动Provider
 *
 * 如果没有活动Provider，则自动选择最佳可用的Provider。
 *
 * @return 活动Provider
 */
suspend fun DockerProviderManager.ensureActiveProvider(): Result<DockerProvider> {
    val current = getActiveProvider()
    if (current != null && current.getState() == DockerProviderState.RUNNING) {
        return Result.success(current)
    }

    return switchToBestAvailable()
}

/**
 * DockerProviderManager扩展函数：获取活动Provider的Docker客户端
 *
 * @return Docker客户端，如果没有活动Provider则返回null
 */
fun DockerProviderManager.getActiveDockerClient(): com.dfa.core.docker.DockerClient? {
    val provider = getActiveProvider() ?: return null
    return try {
        if (provider.getState() == DockerProviderState.RUNNING) {
            provider.getDockerClient()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}