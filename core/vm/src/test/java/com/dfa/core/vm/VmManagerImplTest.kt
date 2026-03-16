package com.dfa.core.vm

import com.dfa.core.vm.qemu.QemuVmAdapter
import com.dfa.core.vm.repository.VmRepository
import com.dfa.core.vm.statemachine.VmStateMachine
import com.dfa.core.vm.termux.TermuxBridge
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * VmManagerImpl 单元测试
 * 
 * 使用QemuVmAdapter和TermuxBridge进行测试
 */
class VmManagerImplTest {

    private lateinit var stateMachine: VmStateMachine
    private lateinit var qemuAdapter: QemuVmAdapter
    private lateinit var repository: VmRepository
    private lateinit var termuxBridge: TermuxBridge
    private lateinit var vmManager: VmManagerImpl

    @Before
    fun setup() {
        stateMachine = VmStateMachine()
        repository = mockk(relaxed = true)
        qemuAdapter = mockk(relaxed = true)
        termuxBridge = mockk(relaxed = true)
        
        // 配置默认mock行为
        coEvery { termuxBridge.isTermuxAvailable() } returns true
        coEvery { qemuAdapter.isQemuAvailable() } returns true
        coEvery { qemuAdapter.isConfigSupported(any()) } returns true
        
        vmManager = VmManagerImpl(stateMachine, qemuAdapter, repository, termuxBridge)
    }

    @Test
    fun `initial state should be CREATED`() = runTest {
        assertEquals(VmState.CREATED, vmManager.getCurrentState())
    }

    @Test
    fun `isInitialized should be false initially`() = runTest {
        assertFalse(vmManager.isInitialized.first())
    }

    @Test
    fun `initialize should succeed with valid config`() = runTest {
        val config = createTestConfig()
        val handle = createTestHandle()
        
        coEvery { qemuAdapter.createVm(config) } returns Result.success(handle)

        val result = vmManager.initialize(config)

        assertTrue(result.isSuccess)
        assertTrue(vmManager.isInitialized.first())
        assertEquals(VmState.CREATED, vmManager.getCurrentState())
    }

    @Test
    fun `initialize should fail when Termux is not available`() = runTest {
        val config = createTestConfig()
        coEvery { termuxBridge.isTermuxAvailable() } returns false

        val result = vmManager.initialize(config)

        assertTrue(result.isFailure)
        assertFalse(vmManager.isInitialized.first())
    }

    @Test
    fun `initialize should fail when QEMU is not available`() = runTest {
        val config = createTestConfig()
        coEvery { qemuAdapter.isQemuAvailable() } returns false

        val result = vmManager.initialize(config)

        assertTrue(result.isFailure)
        assertFalse(vmManager.isInitialized.first())
    }

    @Test
    fun `initialize should fail with invalid config`() = runTest {
        val config = createTestConfig(memory = 0)
        coEvery { qemuAdapter.isConfigSupported(config) } returns false

        val result = vmManager.initialize(config)

        assertTrue(result.isFailure)
        assertFalse(vmManager.isInitialized.first())
    }

    @Test
    fun `start should succeed after initialization`() = runTest {
        val config = createTestConfig()
        val handle = createTestHandle()
        val vmInfo = createTestVmInfo(config, VmState.RUNNING)
        
        coEvery { qemuAdapter.createVm(config) } returns Result.success(handle)
        coEvery { qemuAdapter.startVm(handle) } returns Result.success(vmInfo)

        vmManager.initialize(config)
        val result = vmManager.start()

        assertTrue(result.isSuccess)
        assertEquals(VmState.RUNNING, vmManager.getCurrentState())
    }

    @Test
    fun `start should fail without initialization`() = runTest {
        val result = vmManager.start()

        assertTrue(result.isFailure)
    }

    @Test
    fun `stop should succeed after start`() = runTest {
        val config = createTestConfig()
        val handle = createTestHandle()
        val vmInfoRunning = createTestVmInfo(config, VmState.RUNNING)
        val vmInfoStopped = createTestVmInfo(config, VmState.STOPPED)
        
        coEvery { qemuAdapter.createVm(config) } returns Result.success(handle)
        coEvery { qemuAdapter.startVm(handle) } returns Result.success(vmInfoRunning)
        coEvery { qemuAdapter.stopVm(handle, any()) } returns Result.success(Unit)
        coEvery { qemuAdapter.getVmStatus(handle) } returns Result.success(vmInfoStopped)

        vmManager.initialize(config)
        vmManager.start()
        val result = vmManager.stop()

        assertTrue(result.isSuccess)
        assertEquals(VmState.STOPPED, vmManager.getCurrentState())
    }

    @Test
    fun `pause should succeed when running`() = runTest {
        val config = createTestConfig()
        val handle = createTestHandle()
        val vmInfoRunning = createTestVmInfo(config, VmState.RUNNING)
        val vmInfoPaused = createTestVmInfo(config, VmState.PAUSED)
        
        coEvery { qemuAdapter.createVm(config) } returns Result.success(handle)
        coEvery { qemuAdapter.startVm(handle) } returns Result.success(vmInfoRunning)
        coEvery { qemuAdapter.pauseVm(handle) } returns Result.success(Unit)
        coEvery { qemuAdapter.getVmStatus(handle) } returns Result.success(vmInfoPaused)

        vmManager.initialize(config)
        vmManager.start()
        val result = vmManager.pause()

        assertTrue(result.isSuccess)
        assertEquals(VmState.PAUSED, vmManager.getCurrentState())
    }

    @Test
    fun `resume should succeed when paused`() = runTest {
        val config = createTestConfig()
        val handle = createTestHandle()
        val vmInfoRunning = createTestVmInfo(config, VmState.RUNNING)
        val vmInfoPaused = createTestVmInfo(config, VmState.PAUSED)
        
        coEvery { qemuAdapter.createVm(config) } returns Result.success(handle)
        coEvery { qemuAdapter.startVm(handle) } returns Result.success(vmInfoRunning)
        coEvery { qemuAdapter.pauseVm(handle) } returns Result.success(Unit)
        coEvery { qemuAdapter.resumeVm(handle) } returns Result.success(Unit)
        coEvery { qemuAdapter.getVmStatus(handle) } returns Result.success(vmInfoPaused)

        vmManager.initialize(config)
        vmManager.start()
        vmManager.pause()
        val result = vmManager.resume()

        assertTrue(result.isSuccess)
        assertEquals(VmState.RUNNING, vmManager.getCurrentState())
    }

    @Test
    fun `canPerformOperation should return correct values`() = runTest {
        val config = createTestConfig()
        val handle = createTestHandle()
        
        coEvery { qemuAdapter.createVm(config) } returns Result.success(handle)

        // Before initialization
        assertFalse(vmManager.canPerformOperation(VmManager.VmOperation.START))

        // After initialization (CREATED state)
        vmManager.initialize(config)
        assertTrue(vmManager.canPerformOperation(VmManager.VmOperation.START))
        assertFalse(vmManager.canPerformOperation(VmManager.VmOperation.STOP))
        assertFalse(vmManager.canPerformOperation(VmManager.VmOperation.PAUSE))
    }

    @Test
    fun `release should clear all state`() = runTest {
        val config = createTestConfig()
        val handle = createTestHandle()
        val vmInfoRunning = createTestVmInfo(config, VmState.RUNNING)
        
        coEvery { qemuAdapter.createVm(config) } returns Result.success(handle)
        coEvery { qemuAdapter.startVm(handle) } returns Result.success(vmInfoRunning)
        coEvery { qemuAdapter.destroyVm(handle) } returns Result.success(Unit)

        vmManager.initialize(config)
        vmManager.start()
        vmManager.release()

        assertEquals(VmState.CREATED, vmManager.getCurrentState())
        assertNull(vmManager.getCurrentInfo())
        assertFalse(vmManager.isInitialized.first())
    }

    @Test
    fun `reset should reinitialize vm`() = runTest {
        val config = createTestConfig()
        val handle = createTestHandle()
        val vmInfoRunning = createTestVmInfo(config, VmState.RUNNING)
        
        coEvery { qemuAdapter.createVm(config) } returns Result.success(handle)
        coEvery { qemuAdapter.startVm(handle) } returns Result.success(vmInfoRunning)
        coEvery { qemuAdapter.destroyVm(handle) } returns Result.success(Unit)

        vmManager.initialize(config)
        vmManager.start()
        val result = vmManager.reset()

        assertTrue(result.isSuccess)
        assertEquals(VmState.CREATED, vmManager.getCurrentState())
        assertTrue(vmManager.isInitialized.first())
    }

    // Helper functions

    private fun createTestConfig(
        id: String = "test-vm",
        name: String = "Test VM",
        memory: Int = 1024
    ): VmConfig {
        return VmConfig(
            id = id,
            name = name,
            memory = memory,
            cpu = 2,
            diskSize = 10
        )
    }

    private fun createTestHandle(): VmHandle {
        return VmHandle(
            vmId = "test-vm",
            backendType = VmBackendType.QEMU,
            processId = 12345,
            sshPort = 2222,
            vncPort = 5900
        )
    }

    private fun createTestVmInfo(config: VmConfig, state: VmState): VmInfo {
        return VmInfo(
            config = config,
            state = state,
            handle = createTestHandle(),
            ipAddress = if (state == VmState.RUNNING) "192.168.1.100" else null
        )
    }
}