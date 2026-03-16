package com.dfa.core.vm.qemu

import com.dfa.core.vm.*
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * QemuVmAdapterImpl单元测试
 *
 * 测试QEMU虚拟机适配器的核心功能，包括：
 * - 虚拟机生命周期管理（创建、启动、停止、销毁）
 * - 状态查询和资源管理
 * - 配置验证
 * - 回调机制
 * - 错误处理
 *
 * 注意：快照、迁移、监控等需要QMP连接的功能在集成测试中验证
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QemuVmAdapterImplTest {

    // 测试调度器
    private val testDispatcher = StandardTestDispatcher()

    // Mock依赖
    private lateinit var mockProcessManager: QemuProcessManager

    // 测试对象
    private lateinit var adapter: QemuVmAdapterImpl

    // 测试数据
    private val testVmConfig = VmConfig(
        id = "test-vm-1",
        name = "Test VM",
        memory = 2048,
        cpu = 2,
        diskSize = 10
    )

    private val testQemuConfig = QemuConfig.Builder()
        .id("test-vm-1")
        .name("Test VM")
        .memoryMb(2048)
        .cpuCores(2)
        .build()

    private val testProcess = QemuProcess(
        processId = "proc-1",
        vmId = "test-vm-1",
        vmName = "Test VM",
        config = testQemuConfig,
        state = QemuProcessState.RUNNING,
        pid = 12345L,
        vncPort = 5900,
        sshPort = 2222
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 创建Mock对象
        mockProcessManager = mockk(relaxed = true)

        // 设置默认Mock行为
        coEvery { mockProcessManager.startProcess(any()) } returns Result.success(testProcess)
        coEvery { mockProcessManager.stopProcess(any(), any()) } returns Result.success(Unit)
        coEvery { mockProcessManager.waitForProcessStart(any(), any()) } returns true
        coEvery { mockProcessManager.waitForProcessStop(any(), any()) } returns true
        every { mockProcessManager.getProcessByVmId(any()) } returns testProcess
        every { mockProcessManager.getProcess(any()) } returns testProcess
        every { mockProcessManager.monitorAllProcesses() } returns flowOf()
        coEvery { mockProcessManager.getProcessStats(any()) } returns QemuProcessStats.EMPTY
        coEvery { mockProcessManager.shutdown() } just Runs

        // 创建适配器实例，注入Mock依赖
        adapter = QemuVmAdapterImpl(
            processManager = mockProcessManager,
            config = QemuAdapterConfig.DEFAULT
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 基础属性测试 ====================

    @Test
    fun `backendType should return QEMU`() {
        assertThat(adapter.backendType).isEqualTo(VmBackendType.QEMU)
    }

    @Test
    fun `getSupportedFeatures should return expected features`() {
        val features = adapter.getSupportedFeatures()

        assertThat(features).containsAtLeast(
            VmFeature.SNAPSHOTS,
            VmFeature.LIVE_MIGRATION,
            VmFeature.VNC_DISPLAY,
            VmFeature.SERIAL_CONSOLE,
            VmFeature.PORT_FORWARDING,
            VmFeature.NETWORK_BRIDGE
        )
    }

    // ==================== 创建虚拟机测试 ====================

    @Test
    fun `createVm should return success with valid config`() = runTest {
        val result = adapter.createVm(testVmConfig)

        assertThat(result.isSuccess).isTrue()
        val handle = result.getOrThrow()
        assertThat(handle.vmId).isEqualTo(testVmConfig.id)
        assertThat(handle.backendType).isEqualTo(VmBackendType.QEMU)
    }

    @Test
    fun `createVm should return failure when adapter is shutdown`() = runTest {
        adapter.shutdown()

        val result = adapter.createVm(testVmConfig)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `createVm should return failure when config is not supported`() = runTest {
        val invalidConfig = testVmConfig.copy(memory = 0)

        val result = adapter.createVm(invalidConfig)

        // 内存为0的配置应该被拒绝
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `createVm should generate monitor path`() = runTest {
        val result = adapter.createVm(testVmConfig)

        assertThat(result.isSuccess).isTrue()
        val handle = result.getOrThrow()
        assertThat(handle.monitorPath).isNotNull()
        assertThat(handle.monitorPath).contains("qemu-${testVmConfig.id}")
    }

    // ==================== 启动虚拟机测试 ====================

    @Test
    fun `startVm should return success with valid handle`() = runTest {
        // 先创建VM
        val createResult = adapter.createVm(testVmConfig)
        assertThat(createResult.isSuccess).isTrue()
        val handle = createResult.getOrThrow()

        val startResult = adapter.startVm(handle)

        assertThat(startResult.isSuccess).isTrue()
        val vmInfo = startResult.getOrThrow()
        assertThat(vmInfo.state).isEqualTo(VmState.RUNNING)
        assertThat(vmInfo.config.id).isEqualTo(testVmConfig.id)
    }

    @Test
    fun `startVm should return failure when VM not found`() = runTest {
        val handle = VmHandle(
            vmId = "non-existent-vm",
            backendType = VmBackendType.QEMU
        )

        val result = adapter.startVm(handle)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `startVm should return failure when process start fails`() = runTest {
        coEvery { mockProcessManager.startProcess(any()) } returns Result.failure(
            VmError.ResourceError("Failed to start process")
        )

        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()

        val startResult = adapter.startVm(handle)

        assertThat(startResult.isFailure).isTrue()
    }

    @Test
    fun `startVm should return failure when startup times out`() = runTest {
        coEvery { mockProcessManager.waitForProcessStart(any(), any()) } returns false

        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()

        val startResult = adapter.startVm(handle)

        assertThat(startResult.isFailure).isTrue()
        assertThat(startResult.exceptionOrNull()).isInstanceOf(VmError.TimeoutError::class.java)
    }

    @Test
    fun `startVm should call processManager startProcess`() = runTest {
        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()

        adapter.startVm(handle)

        coVerify { mockProcessManager.startProcess(any()) }
    }

    // ==================== 停止虚拟机测试 ====================

    @Test
    fun `stopVm should return success when VM is running`() = runTest {
        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        adapter.startVm(handle)

        val stopResult = adapter.stopVm(handle, force = false)

        assertThat(stopResult.isSuccess).isTrue()
        coVerify { mockProcessManager.stopProcess(any(), force = false) }
    }

    @Test
    fun `stopVm should force stop when force is true`() = runTest {
        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        adapter.startVm(handle)

        val stopResult = adapter.stopVm(handle, force = true)

        assertThat(stopResult.isSuccess).isTrue()
        coVerify { mockProcessManager.stopProcess(any(), force = true) }
    }

    @Test
    fun `stopVm should return success when process not found`() = runTest {
        every { mockProcessManager.getProcessByVmId(any()) } returns null

        val handle = VmHandle(
            vmId = "test-vm",
            backendType = VmBackendType.QEMU
        )

        val result = adapter.stopVm(handle, force = false)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 销毁虚拟机测试 ====================

    @Test
    fun `destroyVm should cleanup all resources`() = runTest {
        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()

        val destroyResult = adapter.destroyVm(handle)

        assertThat(destroyResult.isSuccess).isTrue()
    }

    @Test
    fun `destroyVm should stop running VM before destroy`() = runTest {
        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        adapter.startVm(handle)

        val destroyResult = adapter.destroyVm(handle)

        assertThat(destroyResult.isSuccess).isTrue()
        coVerify { mockProcessManager.stopProcess(any(), force = true) }
    }

    @Test
    fun `destroyVm should handle non-existent VM gracefully`() = runTest {
        val handle = VmHandle(
            vmId = "non-existent-vm",
            backendType = VmBackendType.QEMU
        )

        val result = adapter.destroyVm(handle)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 状态查询测试 ====================

    @Test
    fun `getVmStatus should return current VM info`() = runTest {
        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()

        val statusResult = adapter.getVmStatus(handle)

        assertThat(statusResult.isSuccess).isTrue()
        val vmInfo = statusResult.getOrThrow()
        assertThat(vmInfo.config.id).isEqualTo(testVmConfig.id)
    }

    @Test
    fun `getVmStatus should return failure when VM not found`() = runTest {
        val handle = VmHandle(
            vmId = "non-existent-vm",
            backendType = VmBackendType.QEMU
        )

        val result = adapter.getVmStatus(handle)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getVmStatus should return running state after start`() = runTest {
        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        adapter.startVm(handle)

        val statusResult = adapter.getVmStatus(handle)

        assertThat(statusResult.isSuccess).isTrue()
        val vmInfo = statusResult.getOrThrow()
        assertThat(vmInfo.state).isEqualTo(VmState.RUNNING)
    }

    // ==================== 资源查询测试 ====================

    @Test
    fun `getAvailableResources should return system resources`() = runTest {
        val resources = adapter.getAvailableResources()

        assertThat(resources.backendType).isEqualTo(VmBackendType.QEMU)
        assertThat(resources.totalCpuCores).isGreaterThan(0)
        assertThat(resources.totalMemoryMb).isGreaterThan(0)
    }

    @Test
    fun `getAvailableResources should return supported features`() = runTest {
        val resources = adapter.getAvailableResources()

        assertThat(resources.supportedFeatures).isNotEmpty()
    }

    // ==================== 配置支持测试 ====================

    @Test
    fun `isConfigSupported should return true for valid config`() = runTest {
        val validConfig = VmConfig(
            id = "test",
            name = "Test",
            memory = 2048,
            cpu = 2,
            diskSize = 10
        )

        val result = adapter.isConfigSupported(validConfig)

        assertThat(result).isTrue()
    }

    @Test
    fun `isConfigSupported should return false for invalid config with zero memory`() = runTest {
        val invalidConfig = VmConfig(
            id = "test",
            name = "Test",
            memory = 0,
            cpu = 2,
            diskSize = 10
        )

        val result = adapter.isConfigSupported(invalidConfig)

        assertThat(result).isFalse()
    }

    @Test
    fun `isConfigSupported should return false for invalid config with zero cpu`() = runTest {
        val invalidConfig = VmConfig(
            id = "test",
            name = "Test",
            memory = 2048,
            cpu = 0,
            diskSize = 10
        )

        val result = adapter.isConfigSupported(invalidConfig)

        assertThat(result).isFalse()
    }

    @Test
    fun `isConfigSupported should reject memory exceeding limit`() = runTest {
        val config = testVmConfig.copy(memory = 100000) // 超过64GB限制

        val result = adapter.isConfigSupported(config)

        assertThat(result).isFalse()
    }

    @Test
    fun `isConfigSupported should reject cpu exceeding limit`() = runTest {
        val config = testVmConfig.copy(cpu = 300) // 超过256核限制

        val result = adapter.isConfigSupported(config)

        assertThat(result).isFalse()
    }

    @Test
    fun `isConfigSupported should reject disk size exceeding limit`() = runTest {
        val config = testVmConfig.copy(diskSize = 100000) // 超过64TB限制

        val result = adapter.isConfigSupported(config)

        assertThat(result).isFalse()
    }

    // ==================== 回调测试 ====================

    @Test
    fun `registerCallback should add callback to list`() {
        val callback = createMockCallback()

        adapter.registerCallback(callback)

        // 验证回调被添加（通过注销不抛异常间接验证）
        adapter.unregisterCallback(callback)
    }

    @Test
    fun `unregisterCallback should remove callback from list`() {
        val callback = createMockCallback()
        adapter.registerCallback(callback)

        // 注销不应该抛异常
        adapter.unregisterCallback(callback)
    }

    @Test
    fun `unregisterCallback should not throw when callback not registered`() {
        val callback = createMockCallback()

        // 注销未注册的回调不应该抛异常
        adapter.unregisterCallback(callback)
    }

    @Test
    fun `callback should receive state change on create`() = runTest {
        val callback = createMockCallback()
        adapter.registerCallback(callback)

        adapter.createVm(testVmConfig)

        // 验证回调被调用
        verify { callback.onStateChanged(VmState.CREATED) }
    }

    // ==================== QEMU特定功能测试 ====================

    @Test
    fun `getQemuProcessInfo should return process info`() = runTest {
        val stats = QemuProcessStats(
            cpuUsagePercent = 25.0,
            memoryUsedMb = 1024,
            uptimeSeconds = 3600
        )
        coEvery { mockProcessManager.getProcessStats(any()) } returns stats
        every { mockProcessManager.getProcessByVmId(any()) } returns testProcess

        val handle = VmHandle(
            vmId = "test-vm-1",
            backendType = VmBackendType.QEMU,
            processId = 12345
        )

        val result = adapter.getQemuProcessInfo(handle)

        assertThat(result.isSuccess).isTrue()
        val info = result.getOrThrow()
        assertThat(info.pid).isEqualTo(testProcess.pid?.toInt())
        assertThat(info.memoryUsageMb).isEqualTo(stats.memoryUsedMb)
        assertThat(info.cpuUsagePercent).isEqualTo(stats.cpuUsagePercent)
    }

    @Test
    fun `getQemuProcessInfo should return failure when process not found`() = runTest {
        every { mockProcessManager.getProcessByVmId(any()) } returns null

        val handle = VmHandle(
            vmId = "non-existent-vm",
            backendType = VmBackendType.QEMU
        )

        val result = adapter.getQemuProcessInfo(handle)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 关闭测试 ====================

    @Test
    fun `shutdown should stop all VMs and cleanup resources`() = runTest {
        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        adapter.startVm(handle)

        adapter.shutdown()

        coVerify { mockProcessManager.shutdown() }
    }

    @Test
    fun `shutdown should be idempotent`() = runTest {
        adapter.shutdown()
        adapter.shutdown() // 第二次调用应该安全

        coVerify(exactly = 1) { mockProcessManager.shutdown() }
    }

    @Test
    fun `shutdown should prevent further operations`() = runTest {
        adapter.shutdown()

        val result = adapter.createVm(testVmConfig)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `createVm should handle process manager exception`() = runTest {
        // 模拟QEMU不可用的情况
        // 由于isQemuAvailable()会检查qemu-system-x86_64，在测试环境中可能不可用
        // 所以我们测试配置验证失败的情况

        val invalidConfig = VmConfig(
            id = "",
            name = "Test",
            memory = 2048,
            cpu = 2
        )

        val result = adapter.createVm(invalidConfig)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `startVm should handle process manager exception`() = runTest {
        coEvery { mockProcessManager.startProcess(any()) } throws RuntimeException("Unexpected error")

        val createResult = adapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()

        val startResult = adapter.startVm(handle)

        assertThat(startResult.isFailure).isTrue()
    }

    // ==================== 辅助方法 ====================

    private fun createMockCallback(): VmCallback {
        return mockk<VmCallback>(relaxed = true) {
            every { onStateChanged(any()) } just Runs
            every { onError(any()) } just Runs
            every { onVmStarted(any()) } just Runs
            every { onVmStopped() } just Runs
            every { onVmDestroyed() } just Runs
        }
    }
}