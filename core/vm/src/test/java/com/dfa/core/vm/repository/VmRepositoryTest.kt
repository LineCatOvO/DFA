package com.dfa.core.vm.repository

import com.dfa.core.vm.AvfVmHandle
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmInfo
import com.dfa.core.vm.VmState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * VmRepository 单元测试
 */
class VmRepositoryTest {
    
    private lateinit var repository: VmRepositoryImpl
    
    @Before
    fun setup() {
        repository = VmRepositoryImpl()
    }
    
    @Test
    fun `saveVmInfo and getVmInfo should work correctly`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val vmInfo = VmInfo(config, VmState.CREATED)
        
        repository.saveVmInfo(vmInfo)
        val retrieved = repository.getVmInfo("test-vm")
        
        assertNotNull(retrieved)
        assertEquals(vmInfo, retrieved)
    }
    
    @Test
    fun `getVmInfo should return null for non-existent vm`() = runTest {
        val result = repository.getVmInfo("non-existent")
        assertNull(result)
    }
    
    @Test
    fun `getAllVmInfo should return all saved vms`() = runTest {
        val config1 = VmConfig("vm1", "VM 1")
        val config2 = VmConfig("vm2", "VM 2")
        
        repository.saveVmInfo(VmInfo(config1, VmState.CREATED))
        repository.saveVmInfo(VmInfo(config2, VmState.RUNNING))
        
        val allVmInfo = repository.getAllVmInfo()
        
        assertEquals(2, allVmInfo.size)
        assertTrue(allVmInfo.any { it.config.id == "vm1" })
        assertTrue(allVmInfo.any { it.config.id == "vm2" })
    }
    
    @Test
    fun `deleteVmInfo should remove vm info`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        repository.saveVmInfo(VmInfo(config, VmState.CREATED))
        
        repository.deleteVmInfo("test-vm")
        
        assertNull(repository.getVmInfo("test-vm"))
    }
    
    @Test
    fun `updateVmState should update state correctly`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        repository.saveVmInfo(VmInfo(config, VmState.CREATED))
        
        repository.updateVmState("test-vm", VmState.RUNNING)
        
        val updated = repository.getVmInfo("test-vm")
        assertNotNull(updated)
        assertEquals(VmState.RUNNING, updated?.state)
    }
    
    @Test
    fun `saveVmHandle and getVmHandle should work correctly`() = runTest {
        val handle = AvfVmHandle(vmId = "test-vm", processId = 1234)
        
        repository.saveVmHandle("test-vm", handle)
        val retrieved = repository.getVmHandle("test-vm")
        
        assertNotNull(retrieved)
        assertEquals(handle, retrieved)
    }
    
    @Test
    fun `deleteVmHandle should remove handle`() = runTest {
        val handle = AvfVmHandle(vmId = "test-vm")
        repository.saveVmHandle("test-vm", handle)
        
        repository.deleteVmHandle("test-vm")
        
        assertNull(repository.getVmHandle("test-vm"))
    }
    
    @Test
    fun `saveVmConfig and getVmConfig should work correctly`() = runTest {
        val config = VmConfig("test-vm", "Test VM", memory = 4096)
        
        repository.saveVmConfig(config)
        val retrieved = repository.getVmConfig("test-vm")
        
        assertNotNull(retrieved)
        assertEquals(config, retrieved)
    }
    
    @Test
    fun `observeVmState should emit state changes`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        repository.saveVmInfo(VmInfo(config, VmState.CREATED))
        
        val stateFlow = repository.observeVmState("test-vm")
        assertEquals(VmState.CREATED, stateFlow.first())
        
        repository.updateVmState("test-vm", VmState.RUNNING)
        assertEquals(VmState.RUNNING, stateFlow.first())
    }
    
    @Test
    fun `observeVmInfo should emit info changes`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val vmInfo = VmInfo(config, VmState.CREATED)
        
        repository.saveVmInfo(vmInfo)
        
        val infoFlow = repository.observeVmInfo("test-vm")
        val emitted = infoFlow.first()
        
        assertNotNull(emitted)
        assertEquals(vmInfo, emitted)
    }
    
    @Test
    fun `clearAll should remove all data`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = AvfVmHandle(vmId = "test-vm")
        
        repository.saveVmInfo(VmInfo(config, VmState.CREATED))
        repository.saveVmHandle("test-vm", handle)
        repository.saveVmConfig(config)
        
        repository.clearAll()
        
        assertTrue(repository.getAllVmInfo().isEmpty())
        assertNull(repository.getVmHandle("test-vm"))
        assertNull(repository.getVmConfig("test-vm"))
    }
}