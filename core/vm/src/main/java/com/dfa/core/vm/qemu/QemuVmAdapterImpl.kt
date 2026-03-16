package com.dfa.core.vm.qemu

import com.dfa.core.vm.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * QEMU虚拟机适配器实现
 *
 * 提供与QEMU虚拟机交互的具体实现，支持完整的虚拟机生命周期管理。
 * 包括创建、启动、停止、暂停、恢复、销毁等操作，以及QEMU特定功能如快照、迁移等。
 *
 * ## 功能特性
 * - 完整的虚拟机生命周期管理
 * - 快照创建、恢复、删除
 * - 虚拟机迁移支持
 * - QMP监控协议支持
 * - 资源管理和监控
 * - 回调通知机制
 *
 * @property processManager QEMU进程管理器
 * @property config 适配器配置
 */
class QemuVmAdapterImpl(
    private val processManager: QemuProcessManager = QemuProcessManagerFactory.create(),
    private val config: QemuAdapterConfig = QemuAdapterConfig.DEFAULT
) : QemuVmAdapter {

    companion object {
        private const val TAG = "QemuVmAdapterImpl"
        private const val DEFAULT_MONITOR_SOCKET_DIR = "/tmp/qemu-monitor"
        private const val DEFAULT_STARTUP_TIMEOUT_MS = 60000L
        private const val DEFAULT_STOP_TIMEOUT_MS = 30000L
    }

    // 回调列表
    private val callbacks = CopyOnWriteArrayList<VmCallback>()

    // 虚拟机句柄映射
    private val vmHandles = ConcurrentHashMap<String, VmHandle>()

    // QEMU配置映射
    private val qemuConfigs = ConcurrentHashMap<String, QemuConfig>()

    // QEMU监控器映射
    private val qemuMonitors = ConcurrentHashMap<String, QemuMonitor>()

    // 虚拟机状态流
    private val vmStates = MutableStateFlow<Map<String, VmState>>(emptyMap())

    // 是否已关闭
    private val isShutdown = AtomicBoolean(false)

    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 监控目录
    private val monitorSocketDir: File = File(config.monitorSocketDir ?: DEFAULT_MONITOR_SOCKET_DIR)

    init {
        // 确保监控目录存在
        if (!monitorSocketDir.exists()) {
            monitorSocketDir.mkdirs()
        }

        // 启动进程状态监控
        startProcessMonitoring()
    }

    /**
     * 启动进程状态监控
     */
    private fun startProcessMonitoring() {
        scope.launch {
            processManager.monitorAllProcesses().collect { event ->
                handleProcessEvent(event)
            }
        }
    }

    /**
     * 处理进程状态变更事件
     */
    private fun handleProcessEvent(event: ProcessStateEvent) {
        val vmId = extractVmIdFromProcessId(event.processId) ?: return
        val oldState = mapProcessStateToVmState(event.oldState)
        val newState = mapProcessStateToVmState(event.newState)

        // 更新状态
        updateVmState(vmId, newState)

        // 通知回调
        notifyStateChanged(vmId, oldState, newState)

        // 处理错误状态
        if (event.newState == QemuProcessState.CRASHED) {
            notifyError(vmId, VmError.UnknownError(event.reason ?: "Process crashed"))
        }

        // 清理资源
        if (!event.newState.isActive) {
            cleanupVmResources(vmId)
        }
    }

    /**
     * 从进程ID提取虚拟机ID
     */
    private fun extractVmIdFromProcessId(processId: String): String? {
        return vmHandles.entries.find { it.value.processId?.toString() == processId }?.key
            ?: qemuConfigs.entries.find { processManager.getProcessByVmId(it.key)?.processId == processId }?.key
    }

    /**
     * 映射进程状态到虚拟机状态
     */
    private fun mapProcessStateToVmState(processState: QemuProcessState): VmState {
        return when (processState) {
            QemuProcessState.STARTING -> VmState.STARTING
            QemuProcessState.RUNNING -> VmState.RUNNING
            QemuProcessState.PAUSING -> VmState.RUNNING
            QemuProcessState.PAUSED -> VmState.PAUSED
            QemuProcessState.RESUMING -> VmState.RESUMING
            QemuProcessState.STOPPING -> VmState.STOPPING
            QemuProcessState.STOPPED -> VmState.STOPPED
            QemuProcessState.CRASHED -> VmState.ERROR
            QemuProcessState.UNKNOWN -> VmState.CREATED
        }
    }

    /**
     * 更新虚拟机状态
     */
    private fun updateVmState(vmId: String, state: VmState) {
        vmStates.update { states ->
            states + (vmId to state)
        }
    }

    /**
     * 清理虚拟机资源
     */
    private fun cleanupVmResources(vmId: String) {
        qemuMonitors.remove(vmId)?.let { monitor ->
            scope.launch {
                try {
                    monitor.disconnect()
                } catch (e: Exception) {
                    // 忽略断开连接错误
                }
            }
        }
    }

    // ==================== VmAdapter 基础实现 ====================

    override val backendType: VmBackendType
        get() = VmBackendType.QEMU

    override suspend fun isAvailable(): Boolean {
        return isQemuAvailable()
    }

    override suspend fun createVm(config: VmConfig): Result<VmHandle> {
        if (isShutdown.get()) {
            return Result.failure(IllegalStateException("Adapter is shutdown"))
        }

        return withContext(Dispatchers.IO) {
            try {
                // 检查QEMU可用性
                if (!isQemuAvailable()) {
                    return@withContext Result.failure(
                        VmError.ResourceError("QEMU is not available on this system")
                    )
                }

                // 验证配置
                if (!isConfigSupported(config)) {
                    return@withContext Result.failure(
                        VmError.ConfigurationError("Configuration not supported: $config")
                    )
                }

                // 转换为QEMU配置
                val qemuConfig = convertToQemuConfig(config)

                // 生成监控套接字路径
                val monitorPath = generateMonitorSocketPath(config.id)
                val qemuConfigWithMonitor = qemuConfig.copy(monitorPath = monitorPath)

                // 存储配置
                qemuConfigs[config.id] = qemuConfigWithMonitor

                // 创建句柄
                val handle = VmHandle(
                    vmId = config.id,
                    backendType = VmBackendType.QEMU,
                    monitorPath = monitorPath,
                    createdAt = System.currentTimeMillis()
                )

                vmHandles[config.id] = handle
                updateVmState(config.id, VmState.CREATED)

                notifyStateChanged(config.id, VmState.CREATED, VmState.CREATED)

                Result.success(handle)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to create VM: ${e.message}", e))
            }
        }
    }

    override suspend fun startVm(handle: VmHandle): Result<VmInfo> {
        if (isShutdown.get()) {
            return Result.failure(IllegalStateException("Adapter is shutdown"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val qemuConfig = qemuConfigs[vmId]
                    ?: return@withContext Result.failure(
                        VmError.ConfigurationError("VM not found: $vmId")
                    )

                // 更新状态
                updateVmState(vmId, VmState.STARTING)
                notifyStateChanged(vmId, VmState.CREATED, VmState.STARTING)

                // 启动进程
                val startResult = processManager.startProcess(qemuConfig)
                if (startResult.isFailure) {
                    updateVmState(vmId, VmState.ERROR)
                    notifyStateChanged(vmId, VmState.STARTING, VmState.ERROR)
                    return@withContext Result.failure(
                        startResult.exceptionOrNull() as? VmError
                            ?: VmError.ResourceError("Failed to start QEMU process")
                    )
                }

                val process = startResult.getOrThrow()

                // 等待进程启动
                val started = processManager.waitForProcessStart(
                    process.processId,
                    config.startupTimeoutMs ?: DEFAULT_STARTUP_TIMEOUT_MS
                )

                if (!started) {
                    processManager.stopProcess(process.processId, force = true)
                    updateVmState(vmId, VmState.ERROR)
                    notifyStateChanged(vmId, VmState.STARTING, VmState.ERROR)
                    return@withContext Result.failure(
                        VmError.TimeoutError("VM startup timed out")
                    )
                }

                // 更新句柄
                val updatedHandle = handle.copy(
                    processId = process.pid?.toInt(),
                    monitorPath = qemuConfig.monitorPath,
                    vncPort = process.vncPort,
                    sshPort = process.sshPort
                )
                vmHandles[vmId] = updatedHandle

                // 更新状态
                updateVmState(vmId, VmState.RUNNING)
                notifyStateChanged(vmId, VmState.STARTING, VmState.RUNNING)

                // 创建VM信息
                val vmInfo = VmInfo(
                    config = convertToVmConfig(qemuConfig),
                    state = VmState.RUNNING,
                    uptime = 0,
                    handle = convertToAvfHandle(updatedHandle)
                )

                Result.success(vmInfo)
            } catch (e: Exception) {
                updateVmState(handle.vmId, VmState.ERROR)
                notifyStateChanged(handle.vmId, VmState.STARTING, VmState.ERROR)
                Result.failure(VmError.UnknownError("Failed to start VM: ${e.message}", e))
            }
        }
    }

    override suspend fun stopVm(handle: VmHandle, force: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val process = processManager.getProcessByVmId(vmId)

                if (process == null) {
                    // 进程不存在，视为已停止
                    updateVmState(vmId, VmState.STOPPED)
                    return@withContext Result.success(Unit)
                }

                // 更新状态
                updateVmState(vmId, VmState.STOPPING)
                notifyStateChanged(vmId, VmState.RUNNING, VmState.STOPPING)

                // 停止进程
                val stopResult = processManager.stopProcess(
                    process.processId,
                    force = force
                )

                if (stopResult.isFailure) {
                    updateVmState(vmId, VmState.ERROR)
                    notifyStateChanged(vmId, VmState.STOPPING, VmState.ERROR)
                    return@withContext stopResult
                }

                // 等待停止完成
                processManager.waitForProcessStop(
                    process.processId,
                    config.stopTimeoutMs ?: DEFAULT_STOP_TIMEOUT_MS
                )

                // 更新状态
                updateVmState(vmId, VmState.STOPPED)
                notifyStateChanged(vmId, VmState.STOPPING, VmState.STOPPED)

                Result.success(Unit)
            } catch (e: Exception) {
                updateVmState(handle.vmId, VmState.ERROR)
                Result.failure(VmError.UnknownError("Failed to stop VM: ${e.message}", e))
            }
        }
    }

    override suspend fun pauseVm(handle: VmHandle): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val currentState = vmStates.value[vmId]

                if (currentState != VmState.RUNNING) {
                    return@withContext Result.failure(
                        VmError.ResourceError("Cannot pause VM in state: $currentState")
                    )
                }

                // 通过QMP发送停止命令
                val monitor = getOrCreateMonitor(handle)
                val result = monitor.stop()

                if (result.isFailure) {
                    return@withContext result
                }

                // 更新状态
                updateVmState(vmId, VmState.PAUSED)
                notifyStateChanged(vmId, VmState.RUNNING, VmState.PAUSED)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to pause VM: ${e.message}", e))
            }
        }
    }

    override suspend fun resumeVm(handle: VmHandle): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val currentState = vmStates.value[vmId]

                if (currentState != VmState.PAUSED) {
                    return@withContext Result.failure(
                        VmError.ResourceError("Cannot resume VM in state: $currentState")
                    )
                }

                // 通过QMP发送继续命令
                val monitor = getOrCreateMonitor(handle)
                val result = monitor.cont()

                if (result.isFailure) {
                    return@withContext result
                }

                // 更新状态
                updateVmState(vmId, VmState.RUNNING)
                notifyStateChanged(vmId, VmState.PAUSED, VmState.RUNNING)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to resume VM: ${e.message}", e))
            }
        }
    }

    override suspend fun getVmStatus(handle: VmHandle): Result<VmInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val qemuConfig = qemuConfigs[vmId]
                    ?: return@withContext Result.failure(
                        VmError.ConfigurationError("VM not found: $vmId")
                    )

                val process = processManager.getProcessByVmId(vmId)
                val state = vmStates.value[vmId] ?: VmState.CREATED

                // 获取进程统计信息
                val stats = process?.let { processManager.getProcessStats(it.processId) }

                val vmInfo = VmInfo(
                    config = convertToVmConfig(qemuConfig),
                    state = state,
                    uptime = stats?.uptimeSeconds ?: 0,
                    handle = convertToAvfHandle(handle)
                )

                Result.success(vmInfo)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to get VM status: ${e.message}", e))
            }
        }
    }

    override suspend fun destroyVm(handle: VmHandle): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val currentState = vmStates.value[vmId]

                // 如果正在运行，先停止
                if (currentState == VmState.RUNNING || currentState == VmState.PAUSED) {
                    stopVm(handle, force = true)
                }

                // 清理资源
                qemuConfigs.remove(vmId)
                vmHandles.remove(vmId)
                qemuMonitors.remove(vmId)?.disconnect()

                // 更新状态
                vmStates.update { it - vmId }

                notifyVmDestroyed(vmId)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to destroy VM: ${e.message}", e))
            }
        }
    }

    override fun registerCallback(callback: VmCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    override fun unregisterCallback(callback: VmCallback) {
        callbacks.remove(callback)
    }

    override suspend fun isConfigSupported(config: VmConfig): Boolean {
        return withContext(Dispatchers.Default) {
            // 检查基本配置限制
            config.memory > 0 &&
            config.memory <= 65536 && // 最大64GB
            config.cpu > 0 &&
            config.cpu <= 256 && // 最大256核
            config.diskSize > 0 &&
            config.diskSize <= 65536 // 最大64TB
        }
    }

    override suspend fun getAvailableResources(): VmResources {
        return withContext(Dispatchers.IO) {
            // 获取系统资源
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / (1024 * 1024) // MB
            val availableProcessors = runtime.availableProcessors()

            // 获取磁盘空间
            val root = File("/")
            val totalDisk = if (root.exists()) {
                root.totalSpace / (1024 * 1024 * 1024) // GB
            } else {
                256L
            }
            val freeDisk = if (root.exists()) {
                root.freeSpace / (1024 * 1024 * 1024) // GB
            } else {
                200L
            }

            VmResources(
                memoryMb = (maxMemory * 0.75).toInt(),
                cpuCores = (availableProcessors * 0.75).toInt().coerceAtLeast(1),
                diskSizeGb = freeDisk.toInt(),
                networkBandwidthMbps = 1000,
                gpuEnabled = false,
                gpuMemoryMb = 0
            )
        }
    }

    override fun getSupportedFeatures(): Set<VmFeature> {
        return setOf(
            VmFeature.SNAPSHOTS,
            VmFeature.LIVE_MIGRATION,
            VmFeature.VNC_DISPLAY,
            VmFeature.SERIAL_CONSOLE,
            VmFeature.PORT_FORWARDING,
            VmFeature.NETWORK_BRIDGE
        )
    }

    // ==================== QEMU 特定方法实现 ====================

    override suspend fun isQemuAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder("qemu-system-x86_64", "--version")
                    .redirectErrorStream(true)
                    .start()
                process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
                process.exitValue() == 0
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun getQemuVersion(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder("qemu-system-x86_64", "--version")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)

                // 解析版本号
                val versionRegex = Regex("QEMU emulator version ([\\d.]+)")
                val match = versionRegex.find(output)
                val version = match?.groupValues?.get(1) ?: "unknown"

                Result.success(version)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSupportedArchitectures(): List<QemuTargetArch> {
        return withContext(Dispatchers.IO) {
            val supportedArchs = mutableListOf<QemuTargetArch>()

            for (arch in QemuTargetArch.values()) {
                try {
                    val process = ProcessBuilder(arch.systemEmulator, "--version")
                        .redirectErrorStream(true)
                        .start()
                    process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                    if (process.exitValue() == 0) {
                        supportedArchs.add(arch)
                    }
                } catch (e: Exception) {
                    // 架构不支持
                }
            }

            supportedArchs
        }
    }

    override suspend fun createSnapshot(handle: VmHandle, name: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val monitor = getOrCreateMonitor(handle)
                monitor.saveSnapshot(name)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to create snapshot: ${e.message}", e))
            }
        }
    }

    override suspend fun restoreSnapshot(handle: VmHandle, name: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val monitor = getOrCreateMonitor(handle)
                monitor.loadSnapshot(name)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to restore snapshot: ${e.message}", e))
            }
        }
    }

    override suspend fun deleteSnapshot(handle: VmHandle, name: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val monitor = getOrCreateMonitor(handle)
                monitor.deleteSnapshot(name)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to delete snapshot: ${e.message}", e))
            }
        }
    }

    override suspend fun listSnapshots(handle: VmHandle): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val monitor = getOrCreateMonitor(handle)
                val result = monitor.listSnapshots()
                result.map { snapshots -> snapshots.map { it.name } }
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to list snapshots: ${e.message}", e))
            }
        }
    }

    override suspend fun migrateVm(
        handle: VmHandle,
        targetUri: String,
        liveMigration: Boolean
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val currentState = vmStates.value[vmId]

                if (currentState != VmState.RUNNING) {
                    return@withContext Result.failure(
                        VmError.ResourceError("Cannot migrate VM in state: $currentState")
                    )
                }

                // 更新状态
                updateVmState(vmId, VmState.MIGRATING)
                notifyStateChanged(vmId, VmState.RUNNING, VmState.MIGRATING)

                val monitor = getOrCreateMonitor(handle)
                val options = QemuMigrateOptions(live = liveMigration)
                val result = monitor.migrateStart(targetUri, options)

                if (result.isFailure) {
                    updateVmState(vmId, VmState.RUNNING)
                    notifyStateChanged(vmId, VmState.MIGRATING, VmState.RUNNING)
                    return@withContext result
                }

                // 等待迁移完成
                var migrateStatus = monitor.queryMigrateStatus()
                while (migrateStatus.isSuccess && migrateStatus.getOrThrow().isInProgress) {
                    delay(1000)
                    migrateStatus = monitor.queryMigrateStatus()
                }

                if (migrateStatus.isSuccess && migrateStatus.getOrThrow().isCompleted) {
                    updateVmState(vmId, VmState.STOPPED)
                    notifyStateChanged(vmId, VmState.MIGRATING, VmState.STOPPED)
                    Result.success(Unit)
                } else {
                    updateVmState(vmId, VmState.ERROR)
                    notifyStateChanged(vmId, VmState.MIGRATING, VmState.ERROR)
                    Result.failure(VmError.UnknownError("Migration failed"))
                }
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to migrate VM: ${e.message}", e))
            }
        }
    }

    override suspend fun getQemuMonitor(handle: VmHandle): Result<QemuMonitor> {
        return try {
            val monitor = getOrCreateMonitor(handle)
            Result.success(monitor)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQemuProcessInfo(handle: VmHandle): Result<QemuProcessInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val process = processManager.getProcessByVmId(vmId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Process not found for VM: $vmId")
                    )

                val stats = processManager.getProcessStats(process.processId)
                    ?: QemuProcessStats.EMPTY

                val processInfo = QemuProcessInfo(
                    pid = process.pid?.toInt() ?: 0,
                    memoryUsageMb = stats.memoryUsedMb,
                    cpuUsagePercent = stats.cpuUsagePercent,
                    uptimeSeconds = stats.uptimeSeconds,
                    threads = 1, // 简化处理
                    fileDescriptors = 0,
                    networkBytesReceived = stats.networkRxBytes,
                    networkBytesSent = stats.networkTxBytes,
                    diskReadBytes = stats.diskReadBytes,
                    diskWriteBytes = stats.diskWriteBytes
                )

                Result.success(processInfo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun setCpuCount(handle: VmHandle, cpuCount: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val monitor = getOrCreateMonitor(handle)
                // CPU热插拔需要通过QMP命令实现
                monitor.executeCommand("device_add", mapOf(
                    "driver" to "host-x86_64-cpu",
                    "id" to "cpu-$cpuCount",
                    "socket-id" to cpuCount
                ))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to set CPU count: ${e.message}", e))
            }
        }
    }

    override suspend fun setMemorySize(handle: VmHandle, memoryMb: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val monitor = getOrCreateMonitor(handle)
                // 内存热插拔需要通过QMP命令实现
                monitor.executeCommand("object-add", mapOf(
                    "qom-type" to "memory-backend-ram",
                    "id" to "mem-$memoryMb",
                    "size" to (memoryMb * 1024 * 1024L)
                ))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to set memory size: ${e.message}", e))
            }
        }
    }

    override suspend fun takeScreenshot(handle: VmHandle, format: String): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val monitor = getOrCreateMonitor(handle)
                monitor.screendump(format)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to take screenshot: ${e.message}", e))
            }
        }
    }

    override suspend fun sendKeys(handle: VmHandle, keys: List<Int>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val monitor = getOrCreateMonitor(handle)
                val keyEvents = keys.map { QemuKeyEvent(it, true) }
                monitor.sendKeyEvent(keyEvents)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to send keys: ${e.message}", e))
            }
        }
    }

    override suspend fun sendMouseEvent(
        handle: VmHandle,
        x: Int,
        y: Int,
        buttons: Int
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val monitor = getOrCreateMonitor(handle)
                monitor.sendMouseMoveEvent(x, y)

                // 处理按钮状态
                if (buttons and 0x01 != 0) {
                    monitor.sendMouseButtonEvent(QemuMouseButton.LEFT, true)
                }
                if (buttons and 0x02 != 0) {
                    monitor.sendMouseButtonEvent(QemuMouseButton.RIGHT, true)
                }
                if (buttons and 0x04 != 0) {
                    monitor.sendMouseButtonEvent(QemuMouseButton.MIDDLE, true)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Failed to send mouse event: ${e.message}", e))
            }
        }
    }

    override suspend fun isKvmAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            val kvmDevice = File("/dev/kvm")
            kvmDevice.exists() && kvmDevice.canRead() && kvmDevice.canWrite()
        }
    }

    override suspend fun getSupportedAccelerators(): List<QemuAccelerator> {
        return withContext(Dispatchers.IO) {
            val accelerators = mutableListOf<QemuAccelerator>()

            // TCG始终可用
            accelerators.add(QemuAccelerator.TCG)

            // 检查KVM
            if (isKvmAvailable()) {
                accelerators.add(QemuAccelerator.KVM)
            }

            // 检查HVF (macOS)
            val osName = System.getProperty("os.name", "").lowercase()
            if (osName.contains("mac")) {
                accelerators.add(QemuAccelerator.HVF)
            }

            accelerators
        }
    }

    override suspend fun createDiskImage(
        path: String,
        format: QemuDiskFormat,
        sizeGb: Int
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val sizeBytes = sizeGb.toLong() * 1024 * 1024 * 1024
                val process = ProcessBuilder(
                    "qemu-img", "create",
                    "-f", format.toQemuArg(),
                    path,
                    sizeBytes.toString()
                ).redirectErrorStream(true).start()

                process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)

                if (process.exitValue() == 0) {
                    Result.success(Unit)
                } else {
                    val error = process.inputStream.bufferedReader().readText()
                    Result.failure(IOException("Failed to create disk image: $error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun convertDiskImage(
        sourcePath: String,
        targetPath: String,
        targetFormat: QemuDiskFormat
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(
                    "qemu-img", "convert",
                    "-O", targetFormat.toQemuArg(),
                    sourcePath,
                    targetPath
                ).redirectErrorStream(true).start()

                process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS)

                if (process.exitValue() == 0) {
                    Result.success(Unit)
                } else {
                    val error = process.inputStream.bufferedReader().readText()
                    Result.failure(IOException("Failed to convert disk image: $error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getDiskImageInfo(path: String): Result<QemuDiskImageInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(
                    "qemu-img", "info",
                    "--output=json",
                    path
                ).redirectErrorStream(true).start()

                process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)

                if (process.exitValue() != 0) {
                    val error = process.inputStream.bufferedReader().readText()
                    return@withContext Result.failure(IOException("Failed to get disk info: $error"))
                }

                val output = process.inputStream.bufferedReader().readText()
                // 解析JSON输出（简化处理）
                val info = parseDiskImageInfo(output, path)
                Result.success(info)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取或创建QEMU监控器
     */
    private suspend fun getOrCreateMonitor(handle: VmHandle): QemuMonitor {
        val vmId = handle.vmId

        return qemuMonitors.getOrPut(vmId) {
            val monitorPath = handle.monitorPath
                ?: throw IllegalStateException("Monitor path not set for VM: $vmId")

            val monitor = QemuMonitorImpl(monitorPath)
            monitor.connect().getOrThrow()
            monitor
        }
    }

    /**
     * 生成监控套接字路径
     */
    private fun generateMonitorSocketPath(vmId: String): String {
        return File(monitorSocketDir, "qemu-$vmId.sock").absolutePath
    }

    /**
     * 转换VmConfig到QemuConfig
     */
    private suspend fun convertToQemuConfig(config: VmConfig): QemuConfig {
        return QemuConfig.Builder()
            .id(config.id)
            .name(config.name)
            .memoryMb(config.memory)
            .cpuCores(config.cpu)
            .bootImage(config.bootImage)
            .kernelImage(config.kernelImage)
            .initrdImage(config.initrdImage)
            .kernelArgs(config.kernelArgs)
            .enableGpu(config.enableGpu)
            .enableKvm(isKvmAvailable())
            .build()
    }

    /**
     * 转换QemuConfig到VmConfig
     */
    private fun convertToVmConfig(qemuConfig: QemuConfig): VmConfig {
        return VmConfig(
            id = qemuConfig.id,
            name = qemuConfig.name,
            memory = qemuConfig.memoryMb,
            cpu = qemuConfig.cpuCores,
            diskSize = qemuConfig.disks.firstOrNull()?.sizeGb ?: 10,
            bootImage = qemuConfig.bootImage,
            kernelImage = qemuConfig.kernelImage,
            initrdImage = qemuConfig.initrdImage,
            kernelArgs = qemuConfig.kernelArgs,
            enableGpu = qemuConfig.enableGpu
        )
    }

    /**
     * 转换VmHandle（保持原样，用于类型兼容）
     */
    private fun convertToAvfHandle(handle: VmHandle): VmHandle {
        return handle
    }

    /**
     * 解析磁盘镜像信息
     */
    private fun parseDiskImageInfo(jsonOutput: String, path: String): QemuDiskImageInfo {
        // 简化JSON解析
        val formatRegex = Regex("\"format\":\\s*\"(\\w+)\"")
        val virtualSizeRegex = Regex("\"virtual-size\":\\s*(\\d+)")
        val actualSizeRegex = Regex("\"actual-size\":\\s*(\\d+)")

        val format = formatRegex.find(jsonOutput)?.groupValues?.get(1) ?: "raw"
        val virtualSize = virtualSizeRegex.find(jsonOutput)?.groupValues?.get(1)?.toLongOrNull() ?: 0
        val actualSize = actualSizeRegex.find(jsonOutput)?.groupValues?.get(1)?.toLongOrNull() ?: 0

        return QemuDiskImageInfo(
            path = path,
            format = QemuDiskFormat.values().find { it.formatName == format } ?: QemuDiskFormat.RAW,
            virtualSizeGb = virtualSize.toDouble() / (1024 * 1024 * 1024),
            actualSizeGb = actualSize.toDouble() / (1024 * 1024 * 1024)
        )
    }

    /**
     * 通知状态变化
     */
    private fun notifyStateChanged(vmId: String, oldState: VmState, newState: VmState) {
        callbacks.forEach { callback ->
            try {
                callback.onStateChanged(newState)
            } catch (e: Exception) {
                // 忽略回调错误
            }
        }
    }

    /**
     * 通知错误
     */
    private fun notifyError(vmId: String, error: VmError) {
        callbacks.forEach { callback ->
            try {
                callback.onError(error)
            } catch (e: Exception) {
                // 忽略回调错误
            }
        }
    }

    /**
     * 通知虚拟机销毁
     */
    private fun notifyVmDestroyed(vmId: String) {
        callbacks.forEach { callback ->
            try {
                callback.onVmDestroyed()
            } catch (e: Exception) {
                // 忽略回调错误
            }
        }
    }

    /**
     * 关闭适配器
     */
    suspend fun shutdown() {
        if (isShutdown.getAndSet(true)) {
            return
        }

        // 停止所有虚拟机
        vmHandles.keys.toList().forEach { vmId ->
            try {
                vmHandles[vmId]?.let { handle ->
                    stopVm(handle, force = true)
                    destroyVm(handle)
                }
            } catch (e: Exception) {
                // 忽略关闭错误
            }
        }

        // 关闭进程管理器
        processManager.shutdown()

        // 取消协程
        scope.cancel()

        // 清空存储
        callbacks.clear()
        vmHandles.clear()
        qemuConfigs.clear()
        qemuMonitors.clear()
    }
}

/**
 * QEMU适配器配置
 *
 * @property monitorSocketDir 监控套接字目录
 * @property startupTimeoutMs 启动超时时间（毫秒）
 * @property stopTimeoutMs 停止超时时间（毫秒）
 * @property enableKvm 是否启用KVM
 * @property defaultAccelerator 默认加速器
 */
data class QemuAdapterConfig(
    val monitorSocketDir: String? = null,
    val startupTimeoutMs: Long? = null,
    val stopTimeoutMs: Long? = null,
    val enableKvm: Boolean = true,
    val defaultAccelerator: QemuAccelerator = QemuAccelerator.TCG
) {
    companion object {
        /**
         * 默认配置
         */
        val DEFAULT = QemuAdapterConfig()

        /**
         * 开发环境配置
         */
        val DEVELOPMENT = QemuAdapterConfig(
            startupTimeoutMs = 30000,
            stopTimeoutMs = 10000
        )

        /**
         * 生产环境配置
         */
        val PRODUCTION = QemuAdapterConfig(
            startupTimeoutMs = 120000,
            stopTimeoutMs = 60000,
            enableKvm = true
        )
    }
}

/**
 * QEMU适配器工厂
 */
object QemuVmAdapterFactoryImpl : QemuVmAdapterFactory {

    override fun create(): QemuVmAdapter {
        return QemuVmAdapterImpl()
    }

    override suspend fun isQemuBackendAvailable(): Boolean {
        val adapter = create()
        return adapter.isQemuAvailable()
    }

    override suspend fun getQemuVersion(): String? {
        val adapter = create()
        return adapter.getQemuVersion().getOrNull()
    }
}

/**
 * VmFeature枚举定义
 */
enum class VmFeature {
    SNAPSHOTS,
    LIVE_MIGRATION,
    CPU_HOTPLUG,
    MEMORY_HOTPLUG,
    DEVICE_HOTPLUG,
    VNC_DISPLAY,
    SPICE_DISPLAY,
    SERIAL_CONSOLE,
    USB_PASSTHROUGH,
    PCI_PASSTHROUGH,
    GPU_PASSTHROUGH,
    NETWORK_BRIDGE,
    PORT_FORWARDING,
    SHARED_FOLDERS,
    CLIPBOARD_SHARING,
    DRAG_AND_DROP
}

/**
 * IOException别名用于兼容
 */
private typealias IOException = java.io.IOException