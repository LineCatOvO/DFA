package com.dfa.core.vm.qemu

import kotlinx.coroutines.flow.Flow

/**
 * QEMU进程管理器接口
 *
 * 定义QEMU进程的生命周期管理操作，包括启动、停止、监控和健康检查
 *
 * ## 功能概述
 * - 进程启动：根据配置启动QEMU虚拟机进程
 * - 进程停止：支持优雅停止和强制停止
 * - 进程查询：查询进程信息和状态
 * - 进程监控：实时监控进程状态变化
 * - 健康检查：检查进程运行状态
 *
 * ## 使用示例
 * ```kotlin
 * val manager: QemuProcessManager = QemuProcessManagerImpl()
 *
 * // 启动进程
 * val startResult = manager.startProcess(config)
 * if (startResult.isOk) {
 *     val process = startResult.value
 *     println("Started VM: ${process.vmName}")
 * }
 *
 * // 监控进程
 * manager.monitorProcess(process.processId).collect { state ->
 *     println("Process state: $state")
 * }
 *
 * // 停止进程
 * manager.stopProcess(process.processId, force = false)
 * ```
 */
interface QemuProcessManager {

    /**
     * 启动QEMU进程
     *
     * 根据提供的配置启动一个新的QEMU虚拟机进程
     *
     * @param config QEMU虚拟机配置
     * @return 启动结果，包含进程信息或错误
     *
     * ## 启动流程
     * 1. 验证配置有效性
     * 2. 检查资源可用性（内存、磁盘等）
     * 3. 构建QEMU命令行
     * 4. 启动进程
     * 5. 等待进程初始化完成
     * 6. 返回进程信息
     *
     * ## 错误情况
     * - 配置无效
     * - 资源不足
     * - QEMU可执行文件不存在
     * - 进程启动失败
     */
    suspend fun startProcess(config: QemuConfig): Result<QemuProcess>

    /**
     * 停止QEMU进程
     *
     * 停止指定的QEMU虚拟机进程
     *
     * @param processId 进程唯一标识符
     * @param force 是否强制停止（立即终止）
     * @return 停止结果
     *
     * ## 停止模式
     * - **优雅停止**（force=false）：发送停止信号，等待进程正常退出
     * - **强制停止**（force=true）：立即终止进程
     *
     * ## 超时处理
     * 优雅停止有超时限制，超时后自动转为强制停止
     */
    suspend fun stopProcess(processId: String, force: Boolean = false): Result<Unit>

    /**
     * 暂停QEMU进程
     *
     * 暂停正在运行的QEMU虚拟机进程
     *
     * @param processId 进程唯一标识符
     * @return 暂停结果
     */
    suspend fun pauseProcess(processId: String): Result<Unit>

    /**
     * 恢复QEMU进程
     *
     * 恢复已暂停的QEMU虚拟机进程
     *
     * @param processId 进程唯一标识符
     * @return 恢复结果
     */
    suspend fun resumeProcess(processId: String): Result<Unit>

    /**
     * 获取进程信息
     *
     * 查询指定进程的详细信息
     *
     * @param processId 进程唯一标识符
     * @return 进程信息，如果进程不存在则返回null
     */
    fun getProcess(processId: String): QemuProcess?

    /**
     * 根据虚拟机ID获取进程
     *
     * @param vmId 虚拟机ID
     * @return 进程信息，如果不存在则返回null
     */
    fun getProcessByVmId(vmId: String): QemuProcess?

    /**
     * 获取所有进程列表
     *
     * @return 所有已知进程的列表
     */
    fun listProcesses(): List<QemuProcess>

    /**
     * 获取运行中的进程列表
     *
     * @return 所有运行中进程的列表
     */
    fun listRunningProcesses(): List<QemuProcess>

    /**
     * 监控进程状态
     *
     * 获取进程状态变化的实时流
     *
     * @param processId 进程唯一标识符
     * @return 状态变更事件流
     *
     * ## 使用示例
     * ```kotlin
     * manager.monitorProcess(processId).collect { event ->
     *     when (event.newState) {
     *         QemuProcessState.RUNNING -> println("Process is running")
     *         QemuProcessState.STOPPED -> println("Process stopped")
     *         QemuProcessState.CRASHED -> println("Process crashed: ${event.reason}")
     *         else -> {}
     *     }
     * }
     * ```
     */
    fun monitorProcess(processId: String): Flow<ProcessStateEvent>

    /**
     * 监控所有进程状态
     *
     * 获取所有进程状态变化的实时流
     *
     * @return 状态变更事件流
     */
    fun monitorAllProcesses(): Flow<ProcessStateEvent>

    /**
     * 检查进程是否健康
     *
     * 检查指定进程是否正常运行
     *
     * @param processId 进程唯一标识符
     * @return 进程是否健康
     *
     * ## 健康检查内容
     * - 进程是否存在
     * - 进程状态是否正常
     * - 进程是否响应
     */
    fun isProcessHealthy(processId: String): Boolean

    /**
     * 获取进程资源使用统计
     *
     * @param processId 进程唯一标识符
     * @return 资源使用统计，如果进程不存在则返回null
     */
    suspend fun getProcessStats(processId: String): QemuProcessStats?

    /**
     * 等待进程启动完成
     *
     * 等待进程完成初始化并进入运行状态
     *
     * @param processId 进程唯一标识符
     * @param timeoutMillis 超时时间（毫秒）
     * @return 是否成功启动
     */
    suspend fun waitForProcessStart(processId: String, timeoutMillis: Long = 30000): Boolean

    /**
     * 等待进程停止完成
     *
     * 等待进程完全停止
     *
     * @param processId 进程唯一标识符
     * @param timeoutMillis 超时时间（毫秒）
     * @return 是否成功停止
     */
    suspend fun waitForProcessStop(processId: String, timeoutMillis: Long = 30000): Boolean

    /**
     * 获取进程句柄
     *
     * 获取进程的底层操作句柄
     *
     * @param processId 进程唯一标识符
     * @return 进程句柄，如果进程不存在则返回null
     */
    fun getProcessHandle(processId: String): QemuProcessHandle?

    /**
     * 关闭进程管理器
     *
     * 停止所有管理的进程并释放资源
     */
    suspend fun shutdown()

    /**
     * 获取进程数量
     *
     * @return 当前管理的进程总数
     */
    fun getProcessCount(): Int

    /**
     * 获取运行中进程数量
     *
     * @return 当前运行中的进程数量
     */
    fun getRunningProcessCount(): Int
}

/**
 * QEMU进程管理器工厂
 *
 * 用于创建QemuProcessManager实例
 */
object QemuProcessManagerFactory {

    /**
     * 创建默认的进程管理器实例
     *
     * @return QemuProcessManager实例
     */
    fun create(): QemuProcessManager {
        return QemuProcessManagerImpl()
    }

    /**
     * 创建带有自定义配置的进程管理器实例
     *
     * @param config 管理器配置
     * @return QemuProcessManager实例
     */
    fun create(config: QemuProcessManagerConfig): QemuProcessManager {
        return QemuProcessManagerImpl(config)
    }
}

/**
 * 进程管理器配置
 *
 * @property gracefulShutdownTimeoutMs 优雅停止超时时间（毫秒）
 * @property healthCheckIntervalMs 健康检查间隔（毫秒）
 * @property statsUpdateIntervalMs 统计更新间隔（毫秒）
 * @property maxProcesses 最大进程数量
 * @property enableAutoRestart 是否启用自动重启
 * @property workingDirectory 工作目录
 */
data class QemuProcessManagerConfig(
    val gracefulShutdownTimeoutMs: Long = 30000,
    val healthCheckIntervalMs: Long = 5000,
    val statsUpdateIntervalMs: Long = 10000,
    val maxProcesses: Int = 100,
    val enableAutoRestart: Boolean = false,
    val workingDirectory: String? = null
) {
    companion object {
        /**
         * 默认配置
         */
        val DEFAULT = QemuProcessManagerConfig()

        /**
         * 开发环境配置
         */
        val DEVELOPMENT = QemuProcessManagerConfig(
            gracefulShutdownTimeoutMs = 10000,
            healthCheckIntervalMs = 2000,
            statsUpdateIntervalMs = 5000,
            maxProcesses = 10
        )

        /**
         * 生产环境配置
         */
        val PRODUCTION = QemuProcessManagerConfig(
            gracefulShutdownTimeoutMs = 60000,
            healthCheckIntervalMs = 10000,
            statsUpdateIntervalMs = 30000,
            maxProcesses = 1000,
            enableAutoRestart = true
        )
    }
}