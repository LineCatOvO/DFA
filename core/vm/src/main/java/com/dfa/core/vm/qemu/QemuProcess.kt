package com.dfa.core.vm.qemu

import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/**
 * QEMU进程状态枚举
 *
 * 定义QEMU进程可能的状态
 */
enum class QemuProcessState {
    /** 进程正在启动中 */
    STARTING,
    /** 进程正在运行 */
    RUNNING,
    /** 进程正在暂停 */
    PAUSING,
    /** 进程已暂停 */
    PAUSED,
    /** 进程正在恢复 */
    RESUMING,
    /** 进程正在停止 */
    STOPPING,
    /** 进程已停止 */
    STOPPED,
    /** 进程异常退出 */
    CRASHED,
    /** 进程状态未知 */
    UNKNOWN;

    /**
     * 检查进程是否处于活跃状态
     */
    val isActive: Boolean
        get() = this in listOf(STARTING, RUNNING, PAUSING, PAUSED, RESUMING)

    /**
     * 检查进程是否可以停止
     */
    val canStop: Boolean
        get() = this in listOf(RUNNING, PAUSED, UNKNOWN)

    /**
     * 检查进程是否可以暂停
     */
    val canPause: Boolean
        get() = this == RUNNING

    /**
     * 检查进程是否可以恢复
     */
    val canResume: Boolean
        get() = this == PAUSED

    /**
     * 检查进程是否处于停止过渡状态
     */
    val isStopTransition: Boolean
        get() = this in listOf(STOPPING, STOPPED, CRASHED)
}

/**
 * QEMU进程资源使用统计
 *
 * 记录进程的资源使用情况
 *
 * @property cpuUsagePercent CPU使用率（百分比）
 * @property memoryUsedMb 已使用内存（MB）
 * @property memoryTotalMb 总内存（MB）
 * @property diskReadBytes 磁盘读取字节数
 * @property diskWriteBytes 磁盘写入字节数
 * @property networkRxBytes 网络接收字节数
 * @property networkTxBytes 网络发送字节数
 * @property uptimeSeconds 运行时间（秒）
 * @property timestamp 统计时间戳
 */
data class QemuProcessStats(
    val cpuUsagePercent: Double = 0.0,
    val memoryUsedMb: Long = 0,
    val memoryTotalMb: Long = 0,
    val diskReadBytes: Long = 0,
    val diskWriteBytes: Long = 0,
    val networkRxBytes: Long = 0,
    val networkTxBytes: Long = 0,
    val uptimeSeconds: Long = 0,
    val timestamp: Instant = Instant.now()
) {
    /**
     * 获取内存使用率
     */
    val memoryUsagePercent: Double
        get() = if (memoryTotalMb > 0) (memoryUsedMb.toDouble() / memoryTotalMb) * 100 else 0.0

    /**
     * 获取格式化的运行时间
     */
    val formattedUptime: String
        get() {
            val hours = uptimeSeconds / 3600
            val minutes = (uptimeSeconds % 3600) / 60
            val seconds = uptimeSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

    companion object {
        /**
         * 空统计
         */
        val EMPTY = QemuProcessStats()
    }
}

/**
 * QEMU进程信息
 *
 * 封装QEMU进程的所有相关信息
 *
 * @property processId 进程唯一标识符
 * @property vmId 虚拟机ID
 * @property vmName 虚拟机名称
 * @property config QEMU配置
 * @property state 进程状态
 * @property pid 系统进程ID
 * @property startTime 启动时间
 * @property endTime 结束时间（如果已停止）
 * @property exitCode 退出码（如果已停止）
 * @property errorMessage 错误信息（如果有）
 * @property stats 资源使用统计
 * @property monitorPath 监控套接字路径
 * @property vncPort VNC端口
 * @property sshPort SSH端口
 */
data class QemuProcess(
    val processId: String,
    val vmId: String,
    val vmName: String,
    val config: QemuConfig,
    val state: QemuProcessState = QemuProcessState.STARTING,
    val pid: Long? = null,
    val startTime: Instant = Instant.now(),
    val endTime: Instant? = null,
    val exitCode: Int? = null,
    val errorMessage: String? = null,
    val stats: QemuProcessStats = QemuProcessStats.EMPTY,
    val monitorPath: String? = null,
    val vncPort: Int? = null,
    val sshPort: Int? = null
) {
    /**
     * 进程是否正在运行
     */
    val isRunning: Boolean
        get() = state == QemuProcessState.RUNNING

    /**
     * 进程是否已停止
     */
    val isStopped: Boolean
        get() = state in listOf(QemuProcessState.STOPPED, QemuProcessState.CRASHED)

    /**
     * 进程是否健康
     */
    val isHealthy: Boolean
        get() = state.isActive && errorMessage == null

    /**
     * 获取运行时长（毫秒）
     */
    val uptimeMillis: Long
        get() {
            val end = endTime ?: Instant.now()
            return end.toEpochMilli() - startTime.toEpochMilli()
        }

    /**
     * 获取运行时长（秒）
     */
    val uptimeSeconds: Long
        get() = uptimeMillis / 1000

    /**
     * 更新状态
     */
    fun withState(newState: QemuProcessState): QemuProcess =
        copy(state = newState)

    /**
     * 更新PID
     */
    fun withPid(newPid: Long): QemuProcess =
        copy(pid = newPid, state = QemuProcessState.RUNNING)

    /**
     * 更新统计信息
     */
    fun withStats(newStats: QemuProcessStats): QemuProcess =
        copy(stats = newStats)

    /**
     * 标记为已停止
     */
    fun markStopped(code: Int? = null, error: String? = null): QemuProcess =
        copy(
            state = if (error != null) QemuProcessState.CRASHED else QemuProcessState.STOPPED,
            endTime = Instant.now(),
            exitCode = code,
            errorMessage = error
        )

    /**
     * 标记为错误
     */
    fun markError(error: String): QemuProcess =
        copy(
            state = QemuProcessState.CRASHED,
            errorMessage = error,
            endTime = Instant.now()
        )

    /**
     * 获取进程摘要信息
     */
    val summary: String
        get() = buildString {
            append("QemuProcess[$processId]")
            append(" vm=$vmName")
            append(" state=$state")
            pid?.let { append(" pid=$it") }
            append(" uptime=${stats.formattedUptime}")
        }

    companion object {
        /**
         * 创建新的进程实例
         */
        fun create(
            processId: String,
            config: QemuConfig
        ): QemuProcess = QemuProcess(
            processId = processId,
            vmId = config.id,
            vmName = config.name,
            config = config,
            state = QemuProcessState.STARTING,
            startTime = Instant.now()
        )
    }
}

/**
 * QEMU进程句柄
 *
 * 用于操作正在运行的QEMU进程
 *
 * @property process Java进程对象
 * @property processInfo 进程信息
 * @property stdin 标准输入流
 * @property stdout 标准输出流
 * @property stderr 标准错误流
 */
data class QemuProcessHandle(
    val process: Process,
    val processInfo: QemuProcess,
    val stdin: OutputStream? = null,
    val stdout: InputStream? = null,
    val stderr: InputStream? = null
) {
    /**
     * 进程是否存活
     */
    val isAlive: Boolean
        get() = process.isAlive

    /**
     * 获取进程PID
     */
    val pid: Long
        get() = process.pid()

    /**
     * 获取退出码
     */
    val exitCode: Int?
        get() = if (process.isAlive) null else process.exitValue()

    /**
     * 销毁进程
     */
    fun destroy() {
        process.destroy()
    }

    /**
     * 强制销毁进程
     */
    fun destroyForcibly() {
        process.destroyForcibly()
    }

    /**
     * 等待进程结束
     *
     * @param timeoutMillis 超时时间（毫秒）
     * @return 进程是否在超时前结束
     */
    fun waitFor(timeoutMillis: Long = 0): Boolean {
        return if (timeoutMillis > 0) {
            process.waitFor(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
        } else {
            process.waitFor()
            true
        }
    }

    companion object {
        /**
         * 从Process创建句柄
         */
        fun from(process: Process, processInfo: QemuProcess): QemuProcessHandle {
            return QemuProcessHandle(
                process = process,
                processInfo = processInfo,
                stdin = process.outputStream,
                stdout = process.inputStream,
                stderr = process.errorStream
            )
        }
    }
}

/**
 * 进程状态变更事件
 *
 * @property processId 进程ID
 * @property oldState 旧状态
 * @property newState 新状态
 * @property timestamp 时间戳
 * @property reason 变更原因
 */
data class ProcessStateEvent(
    val processId: String,
    val oldState: QemuProcessState,
    val newState: QemuProcessState,
    val timestamp: Instant = Instant.now(),
    val reason: String? = null
) {
    /**
     * 是否为错误状态变更
     */
    val isErrorTransition: Boolean
        get() = newState == QemuProcessState.CRASHED

    /**
     * 是否为停止状态变更
     */
    val isStopTransition: Boolean
        get() = newState in listOf(QemuProcessState.STOPPED, QemuProcessState.CRASHED)
}

/**
 * 进程启动结果
 *
 * @property success 是否成功
 * @property process 进程信息（成功时）
 * @property handle 进程句柄（成功时）
 * @property error 错误信息（失败时）
 */
data class ProcessStartResult(
    val success: Boolean,
    val process: QemuProcess? = null,
    val handle: QemuProcessHandle? = null,
    val error: String? = null
) {
    companion object {
        /**
         * 创建成功结果
         */
        fun success(process: QemuProcess, handle: QemuProcessHandle): ProcessStartResult =
            ProcessStartResult(
                success = true,
                process = process,
                handle = handle
            )

        /**
         * 创建失败结果
         */
        fun failure(error: String): ProcessStartResult =
            ProcessStartResult(
                success = false,
                error = error
            )
    }
}

/**
 * 进程停止结果
 *
 * @property success 是否成功
 * @property processId 进程ID
 * @property exitCode 退出码
 * @property error 错误信息
 */
data class ProcessStopResult(
    val success: Boolean,
    val processId: String,
    val exitCode: Int? = null,
    val error: String? = null
) {
    companion object {
        /**
         * 创建成功结果
         */
        fun success(processId: String, exitCode: Int? = null): ProcessStopResult =
            ProcessStopResult(
                success = true,
                processId = processId,
                exitCode = exitCode
            )

        /**
         * 创建失败结果
         */
        fun failure(processId: String, error: String): ProcessStopResult =
            ProcessStopResult(
                success = false,
                processId = processId,
                error = error
            )
    }
}