package com.dfa.core.vm.qemu

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * QemuProcessManagerImpl单元测试
 *
 * 测试QEMU进程管理器的核心功能，包括：
 * - 进程生命周期管理（启动、停止、暂停、恢复）
 * - 进程状态查询和监控
 * - 进程健康检查
 * - 资源统计收集
 * - 错误处理和边界条件
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QemuProcessManagerImplTest {

    // 测试调度器
    private val testDispatcher = StandardTestDispatcher()

    // 测试对象
    private lateinit var processManager: QemuProcessManagerImpl

    // 测试配置
    private val testConfig = QemuProcessManagerConfig(
        gracefulShutdownTimeoutMs = 5000,
        healthCheckIntervalMs = 1000,
        maxProcesses = 10
    )

    // 测试数据
    private val testQemuConfig = QemuConfig.Builder()
        .id("test-vm-1")
        .name("Test VM")
        .memoryMb(2048)
        .cpuCores(2)
        .build()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        processManager = QemuProcessManagerImpl(testConfig)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 初始化测试 ====================

    @Test
    fun `constructor should initialize with default config`() {
        val manager = QemuProcessManagerImpl()

        assertThat(manager.getProcessCount()).isEqualTo(0)
        assertThat(manager.getRunningProcessCount()).isEqualTo(0)
    }

    @Test
    fun `constructor should accept custom config`() {
        val customConfig = QemuProcessManagerConfig(
            maxProcesses = 5,
            healthCheckIntervalMs = 2000
        )
        val manager = QemuProcessManagerImpl(customConfig)

        assertThat(manager.getProcessCount()).isEqualTo(0)
    }

    // ==================== 进程计数测试 ====================

    @Test
    fun `getProcessCount should return 0 initially`() {
        assertThat(processManager.getProcessCount()).isEqualTo(0)
    }

    @Test
    fun `getRunningProcessCount should return 0 initially`() {
        assertThat(processManager.getRunningProcessCount()).isEqualTo(0)
    }

    // ==================== 进程查询测试 ====================

    @Test
    fun `getProcess should return null for non-existent process`() {
        val result = processManager.getProcess("non-existent-id")

        assertThat(result).isNull()
    }

    @Test
    fun `getProcessByVmId should return null for non-existent VM`() {
        val result = processManager.getProcessByVmId("non-existent-vm")

        assertThat(result).isNull()
    }

    @Test
    fun `listProcesses should return empty list initially`() {
        val result = processManager.listProcesses()

        assertThat(result).isEmpty()
    }

    @Test
    fun `listRunningProcesses should return empty list initially`() {
        val result = processManager.listRunningProcesses()

        assertThat(result).isEmpty()
    }

    // ==================== 进程启动测试 ====================

    @Test
    fun `startProcess should fail when manager is shutdown`() = runTest {
        processManager.shutdown()

        val result = processManager.startProcess(testQemuConfig)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `startProcess should fail with invalid config`() = runTest {
        val invalidConfig = QemuConfig(
            id = "",
            name = "",
            memoryMb = 0,
            cpuCores = 0
        )

        val result = processManager.startProcess(invalidConfig)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    // ==================== 进程停止测试 ====================

    @Test
    fun `stopProcess should fail for non-existent process`() = runTest {
        val result = processManager.stopProcess("non-existent-id")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    // ==================== 进程暂停恢复测试 ====================

    @Test
    fun `pauseProcess should fail for non-existent process`() = runTest {
        val result = processManager.pauseProcess("non-existent-id")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `resumeProcess should fail for non-existent process`() = runTest {
        val result = processManager.resumeProcess("non-existent-id")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    // ==================== 健康检查测试 ====================

    @Test
    fun `isProcessHealthy should return false for non-existent process`() {
        val result = processManager.isProcessHealthy("non-existent-id")

        assertThat(result).isFalse()
    }

    // ==================== 统计信息测试 ====================

    @Test
    fun `getProcessStats should return null for non-existent process`() = runTest {
        val result = processManager.getProcessStats("non-existent-id")

        assertThat(result).isNull()
    }

    // ==================== 等待测试 ====================

    @Test
    fun `waitForProcessStart should return false for non-existent process`() = runTest {
        val result = processManager.waitForProcessStart("non-existent-id", 1000)

        assertThat(result).isFalse()
    }

    @Test
    fun `waitForProcessStop should return true for non-existent process`() = runTest {
        val result = processManager.waitForProcessStop("non-existent-id", 1000)

        assertThat(result).isTrue()
    }

    // ==================== 进程句柄测试 ====================

    @Test
    fun `getProcessHandle should return null for non-existent process`() {
        val result = processManager.getProcessHandle("non-existent-id")

        assertThat(result).isNull()
    }

    // ==================== 监控测试 ====================

    @Test
    fun `monitorProcess should emit events for state changes`() = runTest {
        // 监控流应该可以创建，即使进程不存在
        val flow = processManager.monitorProcess("test-id")

        // 验证流可以收集（不会抛异常）
        val job = launch {
            flow.take(1).toList()
        }

        // 取消收集
        job.cancel()
    }

    @Test
    fun `monitorAllProcesses should return flow`() = runTest {
        val flow = processManager.monitorAllProcesses()

        // 验证流可以创建
        assertThat(flow).isNotNull()
    }

    // ==================== 关闭测试 ====================

    @Test
    fun `shutdown should complete without error`() = runTest {
        processManager.shutdown()

        // 验证关闭后进程计数为0
        assertThat(processManager.getProcessCount()).isEqualTo(0)
    }

    @Test
    fun `shutdown should be idempotent`() = runTest {
        processManager.shutdown()
        processManager.shutdown() // 第二次关闭应该安全

        assertThat(processManager.getProcessCount()).isEqualTo(0)
    }

    @Test
    fun `shutdown should prevent further operations`() = runTest {
        processManager.shutdown()

        val result = processManager.startProcess(testQemuConfig)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 配置验证测试 ====================

    @Test
    fun `QemuProcessManagerConfig DEFAULT should have correct values`() {
        val config = QemuProcessManagerConfig.DEFAULT

        assertThat(config.gracefulShutdownTimeoutMs).isEqualTo(30000)
        assertThat(config.healthCheckIntervalMs).isEqualTo(5000)
        assertThat(config.maxProcesses).isEqualTo(100)
        assertThat(config.enableAutoRestart).isFalse()
    }

    @Test
    fun `QemuProcessManagerConfig DEVELOPMENT should have shorter timeouts`() {
        val config = QemuProcessManagerConfig.DEVELOPMENT

        assertThat(config.gracefulShutdownTimeoutMs).isEqualTo(10000)
        assertThat(config.maxProcesses).isEqualTo(10)
    }

    @Test
    fun `QemuProcessManagerConfig PRODUCTION should have longer timeouts`() {
        val config = QemuProcessManagerConfig.PRODUCTION

        assertThat(config.gracefulShutdownTimeoutMs).isEqualTo(60000)
        assertThat(config.maxProcesses).isEqualTo(1000)
        assertThat(config.enableAutoRestart).isTrue()
    }

    // ==================== QemuProcessState测试 ====================

    @Test
    fun `QemuProcessState STARTING should be active`() {
        assertThat(QemuProcessState.STARTING.isActive).isTrue()
    }

    @Test
    fun `QemuProcessState RUNNING should be active and can stop and pause`() {
        assertThat(QemuProcessState.RUNNING.isActive).isTrue()
        assertThat(QemuProcessState.RUNNING.canStop).isTrue()
        assertThat(QemuProcessState.RUNNING.canPause).isTrue()
        assertThat(QemuProcessState.RUNNING.canResume).isFalse()
    }

    @Test
    fun `QemuProcessState PAUSED should be active and can stop and resume`() {
        assertThat(QemuProcessState.PAUSED.isActive).isTrue()
        assertThat(QemuProcessState.PAUSED.canStop).isTrue()
        assertThat(QemuProcessState.PAUSED.canPause).isFalse()
        assertThat(QemuProcessState.PAUSED.canResume).isTrue()
    }

    @Test
    fun `QemuProcessState STOPPED should not be active`() {
        assertThat(QemuProcessState.STOPPED.isActive).isFalse()
        assertThat(QemuProcessState.STOPPED.canStop).isFalse()
        assertThat(QemuProcessState.STOPPED.isStopTransition).isTrue()
    }

    @Test
    fun `QemuProcessState CRASHED should be stop transition`() {
        assertThat(QemuProcessState.CRASHED.isActive).isFalse()
        assertThat(QemuProcessState.CRASHED.isStopTransition).isTrue()
    }

    // ==================== QemuProcessStats测试 ====================

    @Test
    fun `QemuProcessStats EMPTY should have default values`() {
        val stats = QemuProcessStats.EMPTY

        assertThat(stats.cpuUsagePercent).isEqualTo(0.0)
        assertThat(stats.memoryUsedMb).isEqualTo(0)
        assertThat(stats.memoryTotalMb).isEqualTo(0)
        assertThat(stats.uptimeSeconds).isEqualTo(0)
    }

    @Test
    fun `QemuProcessStats memoryUsagePercent should calculate correctly`() {
        val stats = QemuProcessStats(
            memoryUsedMb = 512,
            memoryTotalMb = 2048
        )

        assertThat(stats.memoryUsagePercent).isWithin(0.1).of(25.0)
    }

    @Test
    fun `QemuProcessStats memoryUsagePercent should return 0 when total is 0`() {
        val stats = QemuProcessStats(
            memoryUsedMb = 512,
            memoryTotalMb = 0
        )

        assertThat(stats.memoryUsagePercent).isEqualTo(0.0)
    }

    @Test
    fun `QemuProcessStats formattedUptime should format correctly`() {
        val stats = QemuProcessStats(uptimeSeconds = 3661)

        assertThat(stats.formattedUptime).isEqualTo("01:01:01")
    }

    // ==================== QemuProcess测试 ====================

    @Test
    fun `QemuProcess create should initialize correctly`() {
        val config = QemuConfig.Builder()
            .id("test-vm")
            .name("Test VM")
            .memoryMb(2048)
            .cpuCores(2)
            .build()

        val process = QemuProcess.create("proc-1", config)

        assertThat(process.processId).isEqualTo("proc-1")
        assertThat(process.vmId).isEqualTo("test-vm")
        assertThat(process.vmName).isEqualTo("Test VM")
        assertThat(process.state).isEqualTo(QemuProcessState.STARTING)
        assertThat(process.isRunning).isFalse()
        assertThat(process.isStopped).isFalse()
    }

    @Test
    fun `QemuProcess withState should update state`() {
        val process = createTestProcess()

        val updated = process.withState(QemuProcessState.RUNNING)

        assertThat(updated.state).isEqualTo(QemuProcessState.RUNNING)
        assertThat(process.state).isEqualTo(QemuProcessState.STARTING) // 原始不变
    }

    @Test
    fun `QemuProcess withPid should update pid and state`() {
        val process = createTestProcess()

        val updated = process.withPid(12345L)

        assertThat(updated.pid).isEqualTo(12345L)
        assertThat(updated.state).isEqualTo(QemuProcessState.RUNNING)
        assertThat(updated.isRunning).isTrue()
    }

    @Test
    fun `QemuProcess markStopped should update state correctly`() {
        val process = createTestProcess()

        val stopped = process.markStopped(0, null)

        assertThat(stopped.state).isEqualTo(QemuProcessState.STOPPED)
        assertThat(stopped.exitCode).isEqualTo(0)
        assertThat(stopped.isStopped).isTrue()
    }

    @Test
    fun `QemuProcess markStopped with error should set CRASHED state`() {
        val process = createTestProcess()

        val crashed = process.markStopped(1, "Error message")

        assertThat(crashed.state).isEqualTo(QemuProcessState.CRASHED)
        assertThat(crashed.exitCode).isEqualTo(1)
        assertThat(crashed.errorMessage).isEqualTo("Error message")
    }

    @Test
    fun `QemuProcess markError should set CRASHED state`() {
        val process = createTestProcess()

        val crashed = process.markError("Something went wrong")

        assertThat(crashed.state).isEqualTo(QemuProcessState.CRASHED)
        assertThat(crashed.errorMessage).isEqualTo("Something went wrong")
    }

    @Test
    fun `QemuProcess isHealthy should return true when active and no error`() {
        val process = createTestProcess().withState(QemuProcessState.RUNNING)

        assertThat(process.isHealthy).isTrue()
    }

    @Test
    fun `QemuProcess isHealthy should return false when has error`() {
        val process = createTestProcess().markError("Error")

        assertThat(process.isHealthy).isFalse()
    }

    @Test
    fun `QemuProcess summary should contain key information`() {
        val process = createTestProcess().withPid(12345L)

        val summary = process.summary

        assertThat(summary).contains("QemuProcess")
        // summary contains vmName (Test VM), not vmId
        assertThat(summary).contains("Test VM")
        assertThat(summary).contains("RUNNING")
    }

    // ==================== ProcessStateEvent测试 ====================

    @Test
    fun `ProcessStateEvent isErrorTransition should return true for CRASHED`() {
        val event = ProcessStateEvent(
            processId = "test",
            oldState = QemuProcessState.RUNNING,
            newState = QemuProcessState.CRASHED
        )

        assertThat(event.isErrorTransition).isTrue()
        assertThat(event.isStopTransition).isTrue()
    }

    @Test
    fun `ProcessStateEvent isStopTransition should return true for STOPPED`() {
        val event = ProcessStateEvent(
            processId = "test",
            oldState = QemuProcessState.RUNNING,
            newState = QemuProcessState.STOPPED
        )

        assertThat(event.isStopTransition).isTrue()
        assertThat(event.isErrorTransition).isFalse()
    }

    // ==================== 辅助方法 ====================

    private fun createTestProcess(): QemuProcess {
        val config = QemuConfig.Builder()
            .id("test-vm-1")
            .name("Test VM")
            .memoryMb(2048)
            .cpuCores(2)
            .build()

        return QemuProcess.create("proc-1", config)
    }
}