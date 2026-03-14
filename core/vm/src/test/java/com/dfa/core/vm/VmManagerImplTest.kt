package com.dfa.core.vm

import com.dfa.core.vm.avf.AvfVmAdapter
import com.dfa.core.vm.repository.VmRepository
import com.dfa.core.vm.statemachine.VmStateMachine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * VmManagerImpl 单元测试
 */
class VmManagerImplTest {
    
    private lateinit var stateMachine: VmStateMachine
    private lateinit var avfAdapter: AvfVmAdapter
    private lateinit var repository: VmRepository
    private lateinit var vmManager: VmManagerImpl
    
    @Before
    fun setup() {
        stateMachine = VmStateMachine()
        repository = VmRepositoryImpl()
        avfAdapter = AvfVmAdapterImpl()
        vmManager = VmManagerImpl(stateMachine, avfAdapter, repository)
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
        val config = VmConfig("test-vm", "Test VM")
        
        val result = vmManager.initialize(config)
        
        assertTrue(result.isSuccess)
        assertTrue(vmManager.isInitialized.first())
        assertEquals(VmState.CREATED, vmManager.getCurrentState())
    }
    
    @Test
    fun `initialize should fail with invalid config`() = runTest {
        val config = VmConfig("test-vm", "Test VM", memory = 0)
        
        val result = vmManager.initialize(config)
        
        assertTrue(result.isFailure)
        assertFalse(vmManager.isInitialized.first())
    }
    
    @Test
    fun `start should succeed after initialization`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
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
        val config = VmConfig("test-vm", "Test VM")
        vmManager.initialize(config)
        vmManager.start()
        
        val result = vmManager.stop()
        
        assertTrue(result.isSuccess)
        assertEquals(VmState.STOPPED, vmManager.getCurrentState())
    }
    
    @Test
    fun `pause should succeed when running`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        vmManager.initialize(config)
        vmManager.start()
        
        val result = vmManager.pause()
        
        assertTrue(result.isSuccess)
        assertEquals(VmState.PAUSED, vmManager.getCurrentState())
    }
    
    @Test
    fun `resume should succeed when paused`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        vmManager.initialize(config)
        vmManager.start()
        vmManager.pause()
        
        val result = vmManager.resume()
        
        assertTrue(result.isSuccess)
        assertEquals(VmState.RUNNING, vmManager.getCurrentState())
    }
    
    @Test
    fun `canPerformOperation should return correct values`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        
        // Before initialization
        assertFalse(vmManager.canPerformOperation(VmManager.VmOperation.START))
        
        // After initialization (CREATED state)
        vmManager.initialize(config)
        assertTrue(vmManager.canPerformOperation(VmManager.VmOperation.START))
        assertFalse(vmManager.canPerformOperation(VmManager.VmOperation.STOP))
        assertFalse(vmManager.canPerformOperation(VmManager.VmOperation.PAUSE))
        
        // After start (RUNNING state)
        vmManager.start()
        assertFalse(vmManager.canPerformOperation(VmManager.VmOperation.START))
        assertTrue(vmManager.canPerformOperation(VmManager.VmOperation.STOP))
        assertTrue(vmManager.canPerformOperation(VmManager.VmOperation.PAUSE))
        
        // After pause (PAUSED state)
        vmManager.pause()
        assertTrue(vmManager.canPerformOperation(VmManager.VmOperation.RESUME))
        assertTrue(vmManager.canPerformOperation(VmManager.VmOperation.STOP))
    }
    
    @Test
    fun `release should clear all state`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        vmManager.initialize(config)
        vmManager.start()
        
        vmManager.release()
        
        assertEquals(VmState.CREATED, vmManager.getCurrentState())
        assertNull(vmManager.getCurrentInfo())
        assertFalse(vmManager.isInitialized.first())
    }
    
    @Test
    fun `reset should reinitialize vm`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        vmManager.initialize(config)
        vmManager.start()
        
        val result = vmManager.reset()
        
        assertTrue(result.isSuccess)
        assertEquals(VmState.CREATED, vmManager.getCurrentState())
        assertTrue(vmManager.isInitialized.first())
    }
    
    @Test
    fun `vmInfo should be updated after operations`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        vmManager.initialize(config)
        
        var vmInfo = vmManager.getCurrentInfo()
        assertNotNull(vmInfo)
        assertEquals(VmState.CREATED, vmInfo?.state)
        
        vmManager.start()
        vmInfo = vmManager.getCurrentInfo()
        assertNotNull(vmInfo)
        assertEquals(VmState.RUNNING, vmInfo?.state)
        assertNotNull(vmInfo?.ipAddress)
    }
}