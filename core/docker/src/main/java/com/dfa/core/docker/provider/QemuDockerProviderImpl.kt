package com.dfa.core.docker.provider

import com.dfa.core.docker.DockerClient
import com.dfa.core.vm.VmHandle
import com.dfa.core.vm.channel.SshAuthMethod
import com.dfa.core.vm.channel.SshChannelConfig
import com.dfa.core.vm.qemu.QemuAccelerator
import com.dfa.core.vm.qemu.QemuConfig
import com.dfa.core.vm.qemu.QemuDiskConfig
import com.dfa.core.vm.qemu.QemuDiskFormat
import com.dfa.core.vm.qemu.QemuDisplayConfig
import com.dfa.core.vm.qemu.QemuDisplayType
import com.dfa.core.vm.qemu.QemuNetworkConfig
import com.dfa.core.vm.qemu.QemuNetworkMode
import com.dfa.core.vm.qemu.QemuPortForward
import com.dfa.core.vm.qemu.QemuSerialConfig
import com.dfa.core.vm.qemu.QemuSerialMode
import com.dfa.core.vm.qemu.QemuTargetArch
import com.dfa.core.vm.qemu.QemuVmAdapter
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * QEMU Docker Provider实现类
 *
 * 通过QEMU虚拟机提供Docker运行环境。
 * 支持完整的生命周期管理，包括虚拟机的创建、启动、停止和销毁。
 * 通过SSH通道连接虚拟机中的Docker守护进程。
 *
 * 设计模式：
 * - Strategy Pattern: 实现DockerProvider接口
 * - State Pattern: 使用状态机管理生命周期
 *
 * @property config QEMU Docker Provider配置
 * @property qemuVmAdapter QEMU虚拟机适配器
 * @property dockerClientFactory Docker客户端工厂函数
 * @property scope 协程作用域
 * @since 1.0.0
 */
class QemuDockerProviderImpl(
    private val config: QemuDockerProviderConfig,
    private val qemuVmAdapter: QemuVmAdapter,
    private val dockerClientFactory: (SshChannelConfig) -> DockerClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : DockerProvider {

    // ==================== 属性 ====================

    override val providerType: DockerProviderType
        get() = DockerProviderType.QEMU

    override val providerId: String
        get() = config.providerId

    // ==================== 状态管理 ====================

    private val _state = MutableStateFlow<DockerProviderState>(DockerProviderState.CREATED)
    override fun getState(): DockerProviderState = _state.value
    val stateFlow: StateFlow<DockerProviderState> = _state.asStateFlow()

    // ==================== 内部状态 ====================

    private var vmHandle: VmHandle? = null
    private var dockerClient: DockerClient? = null
    private var monitorJob: Job? = null
    private var _providerInfo: DockerProviderInfo? = null

    // ==================== 回调管理 ====================

    private val callbacks = CopyOnWriteArrayList<DockerProviderCallback>()

    // ==================== 支持的特性 ====================

    private val supportedFeatures: Set<DockerProviderFeature> = setOf(
        DockerProviderFeature.DOCKER_API,
        DockerProviderFeature.DOCKER_COMPOSE,
        DockerProviderFeature.SNAPSHOTS,
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
     * 1. 检查QEMU是否可用
     * 2. 创建虚拟机配置
     * 3. 创建虚拟机实例
     * 4. 准备Docker环境
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
            // 检查QEMU是否可用
            if (!qemuVmAdapter.isQemuAvailable()) {
                throw ProviderInitializationException(
                    providerId = providerId,
                    reason = "QEMU is not available on this system"
                )
            }

            // 创建虚拟机配置
            val qemuConfig = createQemuConfig()

            // 创建虚拟机
            val createResult = qemuVmAdapter.createVm(qemuConfig)
            if (createResult.isFailure) {
                throw ProviderInitializationException(
                    providerId = providerId,
                    reason = "Failed to create VM: ${createResult.exceptionOrNull()?.message}",
                    cause = createResult.exceptionOrNull()
                )
            }

            vmHandle = createResult.getOrThrow()

            // 更新状态
            updateState(DockerProviderState.INITIALIZED)

            // 初始化Provider信息
            _providerInfo = DockerProviderInfo(
                providerId = providerId,
                providerType = providerType,
                state = _state.value,
                metadata = mapOf(
                    "vmId" to config.vmId,
                    "memoryMB" to config.memoryMB,
                    "cpus" to config.cpus
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
     * 1. 启动QEMU虚拟机
     * 2. 等待虚拟机就绪
     * 3. 建立SSH连接
     * 4. 连接Docker守护进程
     * 5. 启动健康监控
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
            val handle = vmHandle ?: throw ProviderStartException(
                providerId = providerId,
                reason = "VM handle is null, initialize first"
            )

            // 启动虚拟机
            val startResult = qemuVmAdapter.startVm(handle)
            if (startResult.isFailure) {
                throw ProviderStartException(
                    providerId = providerId,
                    reason = "Failed to start VM: ${startResult.exceptionOrNull()?.message}",
                    cause = startResult.exceptionOrNull()
                )
            }

            // 等待虚拟机就绪
            val readyResult = waitForVmReady(handle)
            if (readyResult.isFailure) {
                throw ProviderStartException(
                    providerId = providerId,
                    reason = "VM failed to become ready: ${readyResult.exceptionOrNull()?.message}",
                    cause = readyResult.exceptionOrNull()
                )
            }

            // 创建Docker客户端
            val sshConfig = createSshConfig()
            dockerClient = dockerClientFactory(sshConfig)

            // 连接Docker
            val connectResult = dockerClient?.connect()
                ?: throw ProviderStartException(
                    providerId = providerId,
                    reason = "Failed to create Docker client"
                )

            if (connectResult.isFailure) {
                throw ProviderStartException(
                    providerId = providerId,
                    reason = "Failed to connect to Docker: ${connectResult.exceptionOrNull()?.message}",
                    cause = connectResult.exceptionOrNull()
                )
            }

            // 验证Docker连接
            val pingResult = dockerClient?.ping()
            if (pingResult?.isFailure != false) {
                throw ProviderStartException(
                    providerId = providerId,
                    reason = "Docker daemon is not responding"
                )
            }

            // 更新状态
            updateState(DockerProviderState.RUNNING)

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
     * 3. 停止虚拟机
     *
     * @param force 是否强制停止
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
            dockerClient = null

            // 通知可用性变化
            notifyAvailabilityChanged(false)

            // 停止虚拟机
            val handle = vmHandle
            if (handle != null) {
                val stopResult = qemuVmAdapter.stopVm(handle, force)
                if (stopResult.isFailure && !force) {
                    // 尝试强制停止
                    qemuVmAdapter.stopVm(handle, force = true)
                }
            }

            // 更新状态
            updateState(DockerProviderState.STOPPED)

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
     * 2. 销毁虚拟机
     * 3. 清理资源
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

            // 销毁虚拟机
            val handle = vmHandle
            if (handle != null) {
                qemuVmAdapter.destroyVm(handle)
            }

            // 清理资源
            vmHandle = null
            dockerClient = null
            _providerInfo = null
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
                dockerClient?.ping()?.isSuccess == true
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
     * 创建QEMU虚拟机配置
     */
    private fun createQemuConfig(): QemuConfig {
        return QemuConfig.Builder()
            .id(config.vmId)
            .name("docker-${config.providerId}")
            .targetArch(QemuTargetArch.X86_64)
            .accelerator(determineAccelerator())
            .memoryMb(config.memoryMB)
            .cpuCores(config.cpus)
            .addDisk(
                QemuDiskConfig(
                    path = "${config.imageDir}/docker-${config.vmId}.qcow2",
                    format = QemuDiskFormat.QCOW2,
                    sizeGb = config.diskSizeGB
                )
            )
            .network(
                QemuNetworkConfig(
                    mode = QemuNetworkMode.USER,
                    portForwards = listOf(
                        // SSH端口转发
                        QemuPortForward(
                            protocol = "tcp",
                            hostPort = config.sshPort,
                            guestPort = 22
                        ),
                        // Docker API端口转发
                        QemuPortForward(
                            protocol = "tcp",
                            hostPort = 2375,
                            guestPort = 2375
                        )
                    )
                )
            )
            .display(
                QemuDisplayConfig(
                    type = QemuDisplayType.NONE
                )
            )
            .serial(
                QemuSerialConfig(
                    enabled = true,
                    mode = QemuSerialMode.File("${config.imageDir}/docker-${config.vmId}.log")
                )
            )
            .enableKvm(determineAccelerator() == QemuAccelerator.KVM)
            .build()
    }

    /**
     * 确定加速器类型
     */
    private fun determineAccelerator(): QemuAccelerator {
        return try {
            val isKvmAvailable = runCatching {
                // 检查KVM是否可用
                java.io.File("/dev/kvm").exists()
            }.getOrDefault(false)

            if (isKvmAvailable) QemuAccelerator.KVM else QemuAccelerator.TCG
        } catch (e: Exception) {
            QemuAccelerator.TCG
        }
    }

    /**
     * 创建SSH配置
     */
    private fun createSshConfig(): SshChannelConfig {
        val authMethod = config.sshKeyPath?.let { keyPath ->
            SshAuthMethod.PublicKeyFile(
                username = config.sshUser,
                privateKeyPath = keyPath
            )
        } ?: SshAuthMethod.Password(
            username = config.sshUser,
            password = "docker" // 默认密码，生产环境应从安全存储获取
        )

        return SshChannelConfig(
            host = config.sshHost,
            port = config.sshPort,
            authMethod = authMethod,
            timeoutConfig = com.dfa.core.vm.channel.SshTimeoutConfig(
                connectionTimeoutMs = config.connectionTimeout,
                readTimeoutMs = config.requestTimeout,
                writeTimeoutMs = config.requestTimeout
            )
        )
    }

    /**
     * 等待虚拟机就绪
     */
    private suspend fun waitForVmReady(handle: VmHandle): Result<Unit> {
        return withTimeout(config.connectionTimeout) {
            val startTime = System.currentTimeMillis()
            val checkInterval = 2000L

            while (System.currentTimeMillis() - startTime < config.connectionTimeout) {
                try {
                    val statusResult = qemuVmAdapter.getVmStatus(handle)
                    val vmInfo = statusResult.getOrNull()

                    if (vmInfo != null && vmInfo.state.name == "RUNNING") {
                        // 检查SSH是否可用
                        if (isSshReady()) {
                            return@withTimeout Result.success(Unit)
                        }
                    }
                } catch (e: Exception) {
                    // 忽略错误，继续等待
                }

                delay(checkInterval)
            }

            Result.failure(
                ProviderTimeoutException(
                    providerId = providerId,
                    operation = "waitForVmReady",
                    timeoutMs = config.connectionTimeout
                )
            )
        }
    }

    /**
     * 检查SSH是否就绪
     */
    private suspend fun isSshReady(): Boolean {
        return try {
            // 简单的SSH连接测试
            // 实际实现中应该尝试建立SSH连接
            delay(1000) // 模拟SSH连接测试
            true
        } catch (e: Exception) {
            false
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
                    if (pingResult?.isFailure == true) {
                        notifyAvailabilityChanged(false)
                    }
                } catch (e: Exception) {
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
     * 获取虚拟机句柄
     */
    fun getVmHandle(): VmHandle? = vmHandle

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
}