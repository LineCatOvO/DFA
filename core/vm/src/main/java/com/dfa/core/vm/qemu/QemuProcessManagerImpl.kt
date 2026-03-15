package com.dfa.core.vm.qemu

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * QEMU进程管理器实现
 *
 * 提供QEMU进程的完整生命周期管理功能
 *
 * @property config 管理器配置
 */
class QemuProcessManagerImpl(
    private val config: QemuProcessManagerConfig = QemuProcessManagerConfig.DEFAULT
) : QemuProcessManager {

    /** 进程存储映射 */
    private val processes = ConcurrentHashMap<String, QemuProcess>()

    /** 进程句柄存储映射 */
    private val handles = ConcurrentHashMap<String, QemuProcessHandle>()

    /** 状态变更事件流 */
    private val stateEvents = MutableSharedFlow<ProcessStateEvent>(replay = 100)

    /** 管理器是否已关闭 */
    private val isShutdown = AtomicBoolean(false)

    /** 协程作用域 */
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** 监控任务 */
    private var monitoringJob: Job? = null

    init {
        startMonitoring()
    }

    /**
     * 启动后台监控任务
     */
    private fun startMonitoring() {
        monitoringJob = scope.launch {
            while (isActive && !isShutdown.get()) {
                try {
                    checkAllProcesses()
                    delay(config.healthCheckIntervalMs)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    // 记录错误但继续监控
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 检查所有进程状态
     */
    private fun checkAllProcesses() {
        handles.forEach { (processId, handle) ->
            try {
                if (!handle.isAlive) {
                    val process = processes[processId] ?: return@forEach
                    if (process.state.isActive) {
                        handleProcessExit(processId, handle.exitCode)
                    }
                }
            } catch (e: Exception) {
                // 忽略单个进程检查错误
            }
        }
    }

    /**
     * 处理进程退出
     */
    private fun handleProcessExit(processId: String, exitCode: Int?) {
        val process = processes[processId] ?: return
        val oldState = process.state

        val newState = if (exitCode == 0) {
            QemuProcessState.STOPPED
        } else {
            QemuProcessState.CRASHED
        }

        val error = if (exitCode != null && exitCode != 0) {
            "Process exited with code $exitCode"
        } else null

        val updatedProcess = process.markStopped(exitCode, error)
        processes[processId] = updatedProcess
        handles.remove(processId)

        emitStateEvent(processId, oldState, newState, error)
    }

    /**
     * 发送状态变更事件
     */
    private fun emitStateEvent(
        processId: String,
        oldState: QemuProcessState,
        newState: QemuProcessState,
        reason: String? = null
    ) {
        val event = ProcessStateEvent(
            processId = processId,
            oldState = oldState,
            newState = newState,
            reason = reason
        )
        scope.launch {
            stateEvents.emit(event)
        }
    }

    override suspend fun startProcess(config: QemuConfig): Result<QemuProcess> {
        if (isShutdown.get()) {
            return Result.failure(IllegalStateException("Process manager is shutdown"))
        }

        // 验证配置
        if (!config.validate()) {
            return Result.failure(IllegalArgumentException("Invalid QEMU configuration"))
        }

        // 检查进程数量限制
        if (processes.size >= this.config.maxProcesses) {
            return Result.failure(IllegalStateException("Maximum process limit reached: ${this.config.maxProcesses}"))
        }

        // 检查是否已存在相同VM ID的进程
        if (getProcessByVmId(config.id) != null) {
            return Result.failure(IllegalStateException("Process already exists for VM: ${config.id}"))
        }

        val processId = generateProcessId()
        val process = QemuProcess.create(processId, config)

        return withContext(Dispatchers.IO) {
            try {
                // 更新状态为启动中
                processes[processId] = process.withState(QemuProcessState.STARTING)

                // 构建命令行
                val commandLine = QemuCommandLine(config)
                val validationResult = commandLine.validate()
                if (!validationResult.isValid) {
                    processes.remove(processId)
                    return@withContext Result.failure(
                        IllegalArgumentException("Configuration validation failed: ${validationResult.getErrorSummary()}")
                    )
                }

                val args = commandLine.build()
                val executable = config.getQemuExecutable()

                // 创建进程构建器
                val processBuilder = ProcessBuilder(mutableListOf(executable).apply { addAll(args) })
                config.workingDirectory?.let { processBuilder.directory(java.io.File(it)) }
                processBuilder.redirectErrorStream(false)

                // 启动进程
                val javaProcess = processBuilder.start()
                val handle = QemuProcessHandle.from(javaProcess, process)

                // 更新进程信息
                val runningProcess = process.withPid(javaProcess.pid())
                processes[processId] = runningProcess
                handles[processId] = handle

                // 启动输出流读取任务
                startOutputReaders(processId, handle)

                // 发送状态变更事件
                emitStateEvent(processId, QemuProcessState.STARTING, QemuProcessState.RUNNING)

                Result.success(runningProcess)
            } catch (e: Exception) {
                processes.remove(processId)
                handles.remove(processId)
                Result.failure(e)
            }
        }
    }

    /**
     * 启动输出流读取任务
     */
    private fun startOutputReaders(processId: String, handle: QemuProcessHandle) {
        // 读取标准输出
        scope.launch(Dispatchers.IO) {
            try {
                handle.stdout?.let { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            // 可以在这里处理输出日志
                            // 目前仅消费输出流，防止缓冲区满
                        }
                    }
                }
            } catch (e: Exception) {
                // 忽略读取错误
            }
        }

        // 读取标准错误
        scope.launch(Dispatchers.IO) {
            try {
                handle.stderr?.let { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            // 可以在这里处理错误日志
                            // 目前仅消费输出流，防止缓冲区满
                        }
                    }
                }
            } catch (e: Exception) {
                // 忽略读取错误
            }
        }
    }

    override suspend fun stopProcess(processId: String, force: Boolean): Result<Unit> {
        val process = processes[processId]
            ?: return Result.failure(IllegalArgumentException("Process not found: $processId"))

        val handle = handles[processId]
            ?: return Result.failure(IllegalStateException("Process handle not found: $processId"))

        if (!process.state.canStop) {
            return Result.failure(IllegalStateException("Process cannot be stopped in state: ${process.state}"))
        }

        val oldState = process.state
        processes[processId] = process.withState(QemuProcessState.STOPPING)
        emitStateEvent(processId, oldState, QemuProcessState.STOPPING)

        return withContext(Dispatchers.IO) {
            try {
                if (force) {
                    handle.destroyForcibly()
                } else {
                    handle.destroy()
                }

                // 等待进程结束
                val exited = handle.waitFor(config.gracefulShutdownTimeoutMs)
                if (!exited && !force) {
                    // 超时后强制停止
                    handle.destroyForcibly()
                    handle.waitFor(5000)
                }

                val exitCode = handle.exitCode
                handleProcessExit(processId, exitCode)

                Result.success(Unit)
            } catch (e: Exception) {
                val errorProcess = process.markError(e.message ?: "Stop failed")
                processes[processId] = errorProcess
                emitStateEvent(processId, QemuProcessState.STOPPING, QemuProcessState.CRASHED, e.message)
                Result.failure(e)
            }
        }
    }

    override suspend fun pauseProcess(processId: String): Result<Unit> {
        val process = processes[processId]
            ?: return Result.failure(IllegalArgumentException("Process not found: $processId"))

        if (!process.state.canPause) {
            return Result.failure(IllegalStateException("Process cannot be paused in state: ${process.state}"))
        }

        // QEMU暂停通常通过monitor接口发送"stop"命令
        // 这里简化处理，实际实现需要通过QMP或monitor socket
        val oldState = process.state
        processes[processId] = process.withState(QemuProcessState.PAUSED)
        emitStateEvent(processId, oldState, QemuProcessState.PAUSED)

        return Result.success(Unit)
    }

    override suspend fun resumeProcess(processId: String): Result<Unit> {
        val process = processes[processId]
            ?: return Result.failure(IllegalArgumentException("Process not found: $processId"))

        if (!process.state.canResume) {
            return Result.failure(IllegalStateException("Process cannot be resumed in state: ${process.state}"))
        }

        // QEMU恢复通常通过monitor接口发送"cont"命令
        // 这里简化处理，实际实现需要通过QMP或monitor socket
        val oldState = process.state
        processes[processId] = process.withState(QemuProcessState.RUNNING)
        emitStateEvent(processId, oldState, QemuProcessState.RUNNING)

        return Result.success(Unit)
    }

    override fun getProcess(processId: String): QemuProcess? {
        return processes[processId]
    }

    override fun getProcessByVmId(vmId: String): QemuProcess? {
        return processes.values.find { it.vmId == vmId }
    }

    override fun listProcesses(): List<QemuProcess> {
        return processes.values.toList()
    }

    override fun listRunningProcesses(): List<QemuProcess> {
        return processes.values.filter { it.isRunning }.toList()
    }

    override fun monitorProcess(processId: String): Flow<ProcessStateEvent> {
        return stateEvents
            .filter { it.processId == processId }
            .onStart {
                // 发送当前状态
                getProcess(processId)?.let { process ->
                    emit(ProcessStateEvent(
                        processId = processId,
                        oldState = QemuProcessState.UNKNOWN,
                        newState = process.state,
                        reason = "Initial state"
                    ))
                }
            }
    }

    override fun monitorAllProcesses(): Flow<ProcessStateEvent> {
        return stateEvents
    }

    override fun isProcessHealthy(processId: String): Boolean {
        val process = processes[processId] ?: return false
        val handle = handles[processId] ?: return false

        return process.isHealthy && handle.isAlive
    }

    override suspend fun getProcessStats(processId: String): QemuProcessStats? {
        val process = processes[processId] ?: return null
        val handle = handles[processId] ?: return process.stats

        // 更新统计信息
        return try {
            val pid = handle.pid
            val stats = collectProcessStats(pid, process)
            processes[processId] = process.withStats(stats)
            stats
        } catch (e: Exception) {
            process.stats
        }
    }

    /**
     * 收集进程统计信息
     */
    private fun collectProcessStats(pid: Long, process: QemuProcess): QemuProcessStats {
        val uptimeSeconds = (Instant.now().toEpochMilli() - process.startTime.toEpochMilli()) / 1000

        // 尝试从/proc读取进程信息（Linux）
        return try {
            val procDir = java.io.File("/proc/$pid")
            if (procDir.exists()) {
                val statFile = java.io.File("/proc/$pid/stat")
                if (statFile.exists()) {
                    val statContent = statFile.readText()
                    val parts = statContent.split(" ")

                    // 解析/proc/[pid]/stat格式
                    // 参考: man proc
                    val utime = parts.getOrElse(13) { "0" }.toLongOrNull() ?: 0
                    val stime = parts.getOrElse(14) { "0" }.toLongOrNull() ?: 0
                    val vsize = parts.getOrElse(22) { "0" }.toLongOrNull() ?: 0
                    val rss = parts.getOrElse(23) { "0" }.toLongOrNull() ?: 0

                    // 转换为MB
                    val memoryUsedMb = (rss * 4096) / (1024 * 1024) // RSS in pages to MB
                    val memoryTotalMb = process.config.memoryMb.toLong()

                    // CPU使用率计算需要两次采样，这里简化处理
                    QemuProcessStats(
                        cpuUsagePercent = 0.0,
                        memoryUsedMb = memoryUsedMb,
                        memoryTotalMb = memoryTotalMb,
                        uptimeSeconds = uptimeSeconds,
                        timestamp = Instant.now()
                    )
                } else {
                    QemuProcessStats(uptimeSeconds = uptimeSeconds, timestamp = Instant.now())
                }
            } else {
                QemuProcessStats(uptimeSeconds = uptimeSeconds, timestamp = Instant.now())
            }
        } catch (e: Exception) {
            QemuProcessStats(uptimeSeconds = uptimeSeconds, timestamp = Instant.now())
        }
    }

    override suspend fun waitForProcessStart(processId: String, timeoutMillis: Long): Boolean {
        val process = processes[processId] ?: return false

        if (process.state == QemuProcessState.RUNNING) {
            return true
        }

        return withTimeoutOrNull(timeoutMillis) {
            monitorProcess(processId)
                .first { it.newState == QemuProcessState.RUNNING || !it.newState.isActive }
            process.state == QemuProcessState.RUNNING
        } ?: false
    }

    override suspend fun waitForProcessStop(processId: String, timeoutMillis: Long): Boolean {
        val process = processes[processId] ?: return true

        if (process.isStopped) {
            return true
        }

        return withTimeoutOrNull(timeoutMillis) {
            monitorProcess(processId)
                .first { it.newState.isStopTransition }
            true
        } ?: false
    }

    override fun getProcessHandle(processId: String): QemuProcessHandle? {
        return handles[processId]
    }

    override suspend fun shutdown() {
        if (isShutdown.getAndSet(true)) {
            return
        }

        // 停止所有进程
        processes.keys.toList().forEach { processId ->
            try {
                stopProcess(processId, force = true)
            } catch (e: Exception) {
                // 忽略停止错误
            }
        }

        // 取消监控任务
        monitoringJob?.cancel()

        // 取消所有协程
        scope.cancel()

        // 清空存储
        processes.clear()
        handles.clear()
    }

    override fun getProcessCount(): Int {
        return processes.size
    }

    override fun getRunningProcessCount(): Int {
        return processes.values.count { it.isRunning }
    }

    companion object {
        /**
         * 生成唯一的进程ID
         */
        private fun generateProcessId(): String {
            return "qemu-${UUID.randomUUID().toString().substring(0, 8)}"
        }
    }
}