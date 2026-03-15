package com.dfa.core.docker.provider

import com.dfa.core.docker.DockerClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 本地Docker Provider实现类
 *
 * 直接连接本地Docker守护进程，用于开发测试环境。
 * 支持Unix Socket和TCP两种连接方式。
 *
 * 连接方式：
 * - Unix Socket: unix:///var/run/docker.sock (Linux/macOS默认)
 * - TCP: tcp://localhost:2375 (需要Docker守护进程开启TCP监听)
 * - TLS: tcp://localhost:2376 (需要配置TLS证书)
 *
 * 设计模式：
 * - Strategy Pattern: 实现DockerProvider接口
 * - State Pattern: 使用状态机管理生命周期
 *
 * @property config 本地Docker Provider配置
 * @property dockerClientFactory Docker客户端工厂函数
 * @property scope 协程作用域
 * @since 1.0.0
 */
class LocalDockerProviderImpl(
    private val config: LocalDockerProviderConfig,
    private val dockerClientFactory: (String, DockerTlsConfig?) -> DockerClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : DockerProvider {

    // ==================== 属性 ====================

    override val providerType: DockerProviderType
        get() = DockerProviderType.LOCAL

    override val providerId: String
        get() = config.providerId

    // ==================== 状态管理 ====================

    private val _state = MutableStateFlow<DockerProviderState>(DockerProviderState.CREATED)
    override fun getState(): DockerProviderState = _state.value

    /**
     * 状态流，用于观察状态变化
     */
    val stateFlow: StateFlow<DockerProviderState> = _state.asStateFlow()

    // ==================== 内部状态 ====================

    private var dockerClient: DockerClient? = null
    private var monitorJob: Job? = null
    private var _providerInfo: DockerProviderInfo? = null
    private var _isAvailable: Boolean = false

    // ==================== 回调管理 ====================

    private val callbacks = CopyOnWriteArrayList<DockerProviderCallback>()

    // ==================== 支持的特性 ====================

    private val supportedFeatures: Set<DockerProviderFeature> = setOf(
        DockerProviderFeature.DOCKER_API,
        DockerProviderFeature.DOCKER_COMPOSE,
        DockerProviderFeature.DOCKER_SWARM,
        DockerProviderFeature.RESOURCE_LIMITS,
        DockerProviderFeature.NETWORK_ISOLATION,
        DockerProviderFeature.PERSISTENT_STORAGE,
        DockerProviderFeature.HEALTH_CHECK,
        DockerProviderFeature.LOG_COLLECTION,
        DockerProviderFeature.METRICS,
        DockerProviderFeature.TLS_SUPPORT
    )

    // ==================== 元数据存储 ====================

    private val metadata = ConcurrentHashMap<String, Any>()

    // ==================== 生命周期方法 ====================

    /**
     * 初始化Provider
     *
     * 执行以下操作：
     * 1. 检查Docker是否已安装
     * 2. 检查Docker守护进程是否运行
     * 3. 验证连接配置
     * 4. 创建Docker客户端实例
     *
     * @return 初始化结果
     */
    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Default) {
        if (_state.value != DockerProviderState.CREATED) {
            return@withContext Result.failure(
                ProviderStateException(
                    providerId = providerId,
                    currentState = _state.value,
                    expectedStates = listOf(DockerProviderState.CREATED)
                )
            )
        }

        updateState(DockerProviderState.INITIALIZING)

        try {
            // 检查Docker是否已安装
            if (!isDockerInstalled()) {
                throw ProviderInitializationException(
                    providerId = providerId,
                    reason = "Docker is not installed on this system"
                )
            }

            // 检查Docker守护进程是否运行
            if (!isDockerDaemonRunning()) {
                throw ProviderInitializationException(
                    providerId = providerId,
                    reason = "Docker daemon is not running. Please start Docker."
                )
            }

            // 验证连接配置
            val validationResult = validateConnectionConfig()
            if (validationResult.isFailure) {
                throw ProviderInitializationException(
                    providerId = providerId,
                    reason = validationResult.exceptionOrNull()?.message ?: "Invalid connection configuration"
                )
            }

            // 创建Docker客户端
            dockerClient = dockerClientFactory(config.dockerHost, config.tlsConfig)

            // 更新状态
            updateState(DockerProviderState.INITIALIZED)

            // 初始化Provider信息
            _providerInfo = DockerProviderInfo(
                providerId = providerId,
                providerType = providerType,
                state = _state.value,
                metadata = mapOf(
                    "dockerHost" to config.dockerHost,
                    "useTls" to config.useTls,
                    "socketPath" to config.socketPath
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            updateState(DockerProviderState.ERROR)
            notifyError(e)
            Result.failure(
                ProviderInitializationException(
                    providerId = providerId,
                    reason = e.message ?: "Unknown error during initialization",
                    cause = e
                )
            )
        }
    }

    /**
     * 启动Provider
     *
     * 执行以下操作：
     * 1. 连接Docker守护进程
     * 2. 验证连接
     * 3. 获取Docker信息
     * 4. 启动健康监控
     *
     * @return 启动结果
     */
    override suspend fun start(): Result<Unit> = withContext(Dispatchers.Default) {
        val currentState = _state.value
        if (!currentState.canStart()) {
            return@withContext Result.failure(
                ProviderStateException(
                    providerId = providerId,
                    currentState = currentState,
                    expectedStates = listOf(DockerProviderState.INITIALIZED, DockerProviderState.STOPPED)
                )
            )
        }

        updateState(DockerProviderState.STARTING)

        try {
            val client = dockerClient ?: throw ProviderStartException(
                providerId = providerId,
                reason = "Docker client is null, initialize first"
            )

            // 连接Docker
            val connectResult = client.connect()
            if (connectResult.isFailure) {
                throw ProviderStartException(
                    providerId = providerId,
                    reason = "Failed to connect to Docker: ${connectResult.exceptionOrNull()?.message}",
                    cause = connectResult.exceptionOrNull()
                )
            }

            // 验证Docker连接
            val pingResult = client.ping()
            if (pingResult.isFailure) {
                throw ProviderStartException(
                    providerId = providerId,
                    reason = "Docker daemon is not responding: ${pingResult.exceptionOrNull()?.message}"
                )
            }

            // 更新状态
            updateState(DockerProviderState.RUNNING)
            _isAvailable = true

            // 启动健康监控
            startHealthMonitor()

            // 更新Provider信息
            updateProviderInfo()

            // 通知可用性变化
            notifyAvailabilityChanged(true)

            Result.success(Unit)
        } catch (e: Exception) {
            updateState(DockerProviderState.ERROR)
            notifyError(e)
            Result.failure(
                ProviderStartException(
                    providerId = providerId,
                    reason = e.message ?: "Unknown error during start",
                    cause = e
                )
            )
        }
    }

    /**
     * 停止Provider
     *
     * 执行以下操作：
     * 1. 停止健康监控
     * 2. 断开Docker连接
     *
     * @param force 是否强制停止（本地Docker忽略此参数）
     * @return 停止结果
     */
    override suspend fun stop(force: Boolean): Result<Unit> = withContext(Dispatchers.Default) {
        val currentState = _state.value
        if (!currentState.canStop()) {
            return@withContext Result.failure(
                ProviderStateException(
                    providerId = providerId,
                    currentState = currentState,
                    expectedStates = listOf(DockerProviderState.RUNNING)
                )
            )
        }

        updateState(DockerProviderState.STOPPING)

        try {
            // 停止健康监控
            stopHealthMonitor()

            // 断开Docker连接
            dockerClient?.disconnect()

            // 更新状态
            updateState(DockerProviderState.STOPPED)
            _isAvailable = false

            // 通知可用性变化
            notifyAvailabilityChanged(false)

            Result.success(Unit)
        } catch (e: Exception) {
            updateState(DockerProviderState.ERROR)
            notifyError(e)
            Result.failure(
                ProviderStopException(
                    providerId = providerId,
                    reason = e.message ?: "Unknown error during stop",
                    cause = e
                )
            )
        }
    }

    /**
     * 销毁Provider
     *
     * 执行以下操作：
     * 1. 停止Provider（如果正在运行）
     * 2. 清理资源
     *
     * @return 销毁结果
     */
    override suspend fun destroy(): Result<Unit> = withContext(Dispatchers.Default) {
        val currentState = _state.value
        if (!currentState.canDestroy()) {
            return@withContext Result.failure(
                ProviderStateException(
                    providerId = providerId,
                    currentState = currentState,
                    expectedStates = listOf(
                        DockerProviderState.CREATED,
                        DockerProviderState.INITIALIZED,
                        DockerProviderState.STOPPED,
                        DockerProviderState.ERROR
                    )
                )
            )
        }

        updateState(DockerProviderState.DESTROYING)

        try {
            // 如果正在运行，先停止
            if (currentState == DockerProviderState.RUNNING) {
                stop(force = true).getOrThrow()
            }

            // 清理资源
            dockerClient = null
            _providerInfo = null
            _isAvailable = false
            metadata.clear()

            // 更新状态
            updateState(DockerProviderState.DESTROYED)

            Result.success(Unit)
        } catch (e: Exception) {
            updateState(DockerProviderState.ERROR)
            notifyError(e)
            Result.failure(
                ProviderDestroyException(
                    providerId = providerId,
                    reason = e.message ?: "Unknown error during destroy",
                    cause = e
                )
            )
        }
    }

    // ==================== 状态查询方法 ====================

    /**
     * 获取Provider信息
     *
     * @return Provider详细信息
     */
    override suspend fun getInfo(): Result<DockerProviderInfo> = withContext(Dispatchers.Default) {
        try {
            val currentInfo = _providerInfo ?: DockerProviderInfo(
                providerId = providerId,
                providerType = providerType,
                state = _state.value
            )

            // 如果正在运行，获取Docker信息
            if (_state.value == DockerProviderState.RUNNING && dockerClient != null) {
                val dockerInfo = dockerClient?.info()?.getOrNull()
                val dockerVersion = dockerClient?.version()?.getOrNull()

                val updatedInfo = currentInfo.copy(
                    state = _state.value,
                    version = dockerVersion?.version,
                    apiVersion = dockerVersion?.apiVersion,
                    operatingSystem = dockerInfo?.operatingSystem,
                    architecture = dockerInfo?.architecture,
                    cpus = dockerInfo?.cpus,
                    memoryTotal = dockerInfo?.memory,
                    containersTotal = dockerInfo?.containers,
                    containersRunning = dockerInfo?.containersRunning,
                    containersStopped = dockerInfo?.containersStopped,
                    containersPaused = dockerInfo?.containersPaused,
                    imagesTotal = dockerInfo?.images,
                    dockerRootDir = dockerInfo?.dockerRootDir,
                    storageDriver = dockerInfo?.driver
                )

                _providerInfo = updatedInfo
                Result.success(updatedInfo)
            } else {
                val updatedInfo = currentInfo.copy(state = _state.value)
                _providerInfo = updatedInfo
                Result.success(updatedInfo)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查Provider是否可用
     *
     * @return 如果Provider可用返回true
     */
    override suspend fun isAvailable(): Boolean {
        return when (_state.value) {
            DockerProviderState.RUNNING -> {
                _isAvailable && dockerClient?.ping()?.isSuccess == true
            }
            else -> false
        }
    }

    // ==================== Docker客户端获取 ====================

    /**
     * 获取Docker客户端
     *
     * @return Docker客户端实例
     * @throws ProviderStateException 如果Provider不在RUNNING状态
     */
    override fun getDockerClient(): DockerClient {
        if (_state.value != DockerProviderState.RUNNING) {
            throw ProviderStateException(
                providerId = providerId,
                currentState = _state.value,
                expectedStates = listOf(DockerProviderState.RUNNING)
            )
        }
        return dockerClient ?: throw ProviderUnavailableException(
            providerId = providerId,
            reason = "Docker client is not available"
        )
    }

    // ==================== 回调管理 ====================

    override fun registerCallback(callback: DockerProviderCallback) {
        callbacks.add(callback)
    }

    override fun unregisterCallback(callback: DockerProviderCallback) {
        callbacks.remove(callback)
    }

    // ==================== 特性支持 ====================

    override fun supportsFeature(feature: DockerProviderFeature): Boolean {
        return feature in supportedFeatures
    }

    override fun getSupportedFeatures(): Set<DockerProviderFeature> {
        return supportedFeatures.toSet()
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查Docker是否已安装
     */
    private fun isDockerInstalled(): Boolean {
        return try {
            // 检查docker命令是否存在
            val process = ProcessBuilder("docker", "--version")
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查Docker守护进程是否运行
     */
    private fun isDockerDaemonRunning(): Boolean {
        return try {
            // 首先检查Unix Socket
            if (checkUnixSocket()) {
                return true
            }

            // 然后检查TCP连接
            if (config.host != null) {
                return checkTcpConnection(config.host!!)
            }

            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查Unix Socket是否可用
     */
    private fun checkUnixSocket(): Boolean {
        return try {
            val socketFile = File(config.socketPath)
            socketFile.exists() && socketFile.canRead() && socketFile.canWrite()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查TCP连接是否可用
     */
    private fun checkTcpConnection(host: String): Boolean {
        return try {
            // 解析主机和端口
            val (hostname, port) = parseHostAndPort(host)
            
            withTimeout(5000) {
                Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(hostname, port), 5000)
                    true
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 解析主机和端口
     */
    private fun parseHostAndPort(host: String): Pair<String, Int> {
        // 移除协议前缀
        val cleanHost = host.removePrefix("tcp://").removePrefix("http://").removePrefix("https://")
        
        // 解析主机和端口
        val parts = cleanHost.split(":")
        return when {
            parts.size == 2 -> parts[0] to (parts[1].toIntOrNull() ?: 2375)
            parts.size == 1 -> parts[0] to 2375
            else -> "localhost" to 2375
        }
    }

    /**
     * 验证连接配置
     */
    private fun validateConnectionConfig(): Result<Unit> {
        return try {
            // 检查Unix Socket
            if (config.host == null) {
                val socketFile = File(config.socketPath)
                if (!socketFile.exists()) {
                    return Result.failure(
                        ProviderConfigException(
                            providerId = providerId,
                            configField = "socketPath",
                            reason = "Docker socket not found at ${config.socketPath}"
                        )
                    )
                }
            }

            // 检查TLS配置
            if (config.useTls && config.tlsConfig != null) {
                val tlsConfig = config.tlsConfig!!
                val certFile = File(tlsConfig.fullCertPath)
                val keyFile = File(tlsConfig.fullKeyPath)
                val caFile = File(tlsConfig.fullCaPath)

                if (!certFile.exists()) {
                    return Result.failure(
                        ProviderConfigException(
                            providerId = providerId,
                            configField = "tlsConfig.certFile",
                            reason = "TLS certificate not found at ${tlsConfig.fullCertPath}"
                        )
                    )
                }

                if (!keyFile.exists()) {
                    return Result.failure(
                        ProviderConfigException(
                            providerId = providerId,
                            configField = "tlsConfig.keyFile",
                            reason = "TLS key not found at ${tlsConfig.fullKeyPath}"
                        )
                    )
                }

                if (tlsConfig.verify && !caFile.exists()) {
                    return Result.failure(
                        ProviderConfigException(
                            providerId = providerId,
                            configField = "tlsConfig.caFile",
                            reason = "TLS CA certificate not found at ${tlsConfig.fullCaPath}"
                        )
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 启动健康监控
     */
    private fun startHealthMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive && _state.value == DockerProviderState.RUNNING) {
                try {
                    val pingResult = dockerClient?.ping()
                    val newAvailable = pingResult?.isSuccess == true
                    
                    if (newAvailable != _isAvailable) {
                        _isAvailable = newAvailable
                        notifyAvailabilityChanged(newAvailable)
                    }
                    
                    if (!newAvailable) {
                        notifyError(
                            ProviderUnavailableException(
                                providerId = providerId,
                                reason = "Docker daemon is not responding"
                            )
                        )
                    }
                } catch (e: Exception) {
                    _isAvailable = false
                    notifyAvailabilityChanged(false)
                    notifyError(e)
                }

                delay(30000) // 每30秒检查一次
            }
        }
    }

    /**
     * 停止健康监控
     */
    private fun stopHealthMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * 更新状态
     */
    private fun updateState(newState: DockerProviderState) {
        val oldState = _state.value
        if (oldState != newState) {
            _state.value = newState
            notifyStateChanged(oldState, newState)
        }
    }

    /**
     * 更新Provider信息
     */
    private suspend fun updateProviderInfo() {
        try {
            getInfo()
        } catch (e: Exception) {
            // 忽略更新错误
        }
    }

    // ==================== 通知方法 ====================

    /**
     * 通知状态变化
     */
    private fun notifyStateChanged(oldState: DockerProviderState, newState: DockerProviderState) {
        callbacks.forEach { callback ->
            try {
                callback.onStateChanged(providerId, oldState, newState)
            } catch (e: Exception) {
                // 忽略回调错误
            }
        }
    }

    /**
     * 通知错误
     */
    private fun notifyError(error: Throwable) {
        callbacks.forEach { callback ->
            try {
                callback.onError(providerId, error)
            } catch (e: Exception) {
                // 忽略回调错误
            }
        }
    }

    /**
     * 通知可用性变化
     */
    private fun notifyAvailabilityChanged(available: Boolean) {
        callbacks.forEach { callback ->
            try {
                callback.onAvailabilityChanged(providerId, available)
            } catch (e: Exception) {
                // 忽略回调错误
            }
        }
    }

    // ==================== 扩展方法 ====================

    /**
     * 获取Docker主机地址
     */
    fun getDockerHost(): String = config.dockerHost

    /**
     * 检查是否使用TLS
     */
    fun isTlsEnabled(): Boolean = config.useTls

    /**
     * 获取元数据
     */
    fun getMetadata(key: String): Any? = metadata[key]

    /**
     * 设置元数据
     */
    fun setMetadata(key: String, value: Any) {
        metadata[key] = value
    }

    /**
     * 获取Docker版本信息
     */
    suspend fun getDockerVersion(): Result<String> {
        return try {
            val version = dockerClient?.version()?.getOrNull()?.version
            if (version != null) {
                Result.success(version)
            } else {
                Result.failure(ProviderUnavailableException(
                    providerId = providerId,
                    reason = "Unable to get Docker version"
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /**
         * 默认Unix Socket路径
         */
        const val DEFAULT_SOCKET_PATH = "/var/run/docker.sock"

        /**
         * 默认TCP端口
         */
        const val DEFAULT_TCP_PORT = 2375

        /**
         * 默认TLS端口
         */
        const val DEFAULT_TLS_PORT = 2376

        /**
         * 创建默认配置的本地Docker Provider
         *
         * @param providerId Provider标识符
         * @param dockerClientFactory Docker客户端工厂
         * @param scope 协程作用域
         * @return LocalDockerProviderImpl实例
         */
        fun createDefault(
            providerId: String = "local-docker",
            dockerClientFactory: (String, DockerTlsConfig?) -> DockerClient,
            scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
        ): LocalDockerProviderImpl {
            val config = LocalDockerProviderConfig(
                providerId = providerId,
                socketPath = DEFAULT_SOCKET_PATH
            )
            return LocalDockerProviderImpl(config, dockerClientFactory, scope)
        }

        /**
         * 创建使用TCP连接的本地Docker Provider
         *
         * @param providerId Provider标识符
         * @param host Docker主机地址
         * @param tlsConfig TLS配置（可选）
         * @param dockerClientFactory Docker客户端工厂
         * @param scope 协程作用域
         * @return LocalDockerProviderImpl实例
         */
        fun createWithTcp(
            providerId: String = "local-docker-tcp",
            host: String,
            tlsConfig: DockerTlsConfig? = null,
            dockerClientFactory: (String, DockerTlsConfig?) -> DockerClient,
            scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
        ): LocalDockerProviderImpl {
            val config = LocalDockerProviderConfig(
                providerId = providerId,
                host = host,
                tlsConfig = tlsConfig
            )
            return LocalDockerProviderImpl(config, dockerClientFactory, scope)
        }
    }
}