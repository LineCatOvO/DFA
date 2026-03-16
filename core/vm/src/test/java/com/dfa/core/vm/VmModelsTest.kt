package com.dfa.core.vm

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * VmModels 单元测试
 */
class VmModelsTest {
    
    @Test
    fun `VmState enum should contain all expected states`() {
        val expectedStates = listOf(
            VmState.CREATED,
            VmState.STARTING,
            VmState.RUNNING,
            VmState.STOPPING,
            VmState.STOPPED,
            VmState.ERROR,
            VmState.PAUSED,
            VmState.RESUMING,
            VmState.MIGRATING
        )
        
        assertEquals(expectedStates.size, VmState.entries.size)
        expectedStates.forEach { state ->
            assertTrue("VmState should contain $state", VmState.entries.contains(state))
        }
    }
    
    @Test
    fun `VmResourceConfig validate should return true for valid resources`() {
        val resources = VmResourceConfig(
            memoryMb = 2048,
            cpuCores = 2,
            diskSizeGb = 10,
            networkBandwidthMbps = 100
        )
        
        assertTrue(resources.validate())
    }
    
    @Test
    fun `VmResourceConfig validate should return false for invalid memory`() {
        val resources = VmResourceConfig(
            memoryMb = 0,
            cpuCores = 2,
            diskSizeGb = 10,
            networkBandwidthMbps = 100
        )
        
        assertFalse(resources.validate())
    }
    
    @Test
    fun `VmResourceConfig validate should return false for invalid cpu`() {
        val resources = VmResourceConfig(
            memoryMb = 2048,
            cpuCores = 0,
            diskSizeGb = 10,
            networkBandwidthMbps = 100
        )
        
        assertFalse(resources.validate())
    }
    
    @Test
    fun `VmResourceConfig validate should return false for gpu enabled but no gpu memory`() {
        val resources = VmResourceConfig(
            memoryMb = 2048,
            cpuCores = 2,
            diskSizeGb = 10,
            networkBandwidthMbps = 100,
            gpuEnabled = true,
            gpuMemoryMb = 0
        )
        
        assertFalse(resources.validate())
    }
    
    @Test
    fun `VmResourceConfig validate should return true for gpu enabled with gpu memory`() {
        val resources = VmResourceConfig(
            memoryMb = 2048,
            cpuCores = 2,
            diskSizeGb = 10,
            networkBandwidthMbps = 100,
            gpuEnabled = true,
            gpuMemoryMb = 512
        )
        
        assertTrue(resources.validate())
    }
    
    @Test
    fun `VmConfig should have default values`() {
        val config = VmConfig(
            id = "test-vm",
            name = "Test VM"
        )
        
        assertEquals("test-vm", config.id)
        assertEquals("Test VM", config.name)
        assertEquals(2048, config.memory)
        assertEquals(2, config.cpu)
        assertEquals(10, config.diskSize)
        assertFalse(config.enableGpu)
    }
    
    @Test
    fun `VmInfo isRunning should return correct value`() {
        val runningInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.RUNNING
        )
        assertTrue(runningInfo.isRunning)
        
        val stoppedInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.STOPPED
        )
        assertFalse(stoppedInfo.isRunning)
    }
    
    @Test
    fun `VmInfo isStopped should return correct value`() {
        val stoppedInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.STOPPED
        )
        assertTrue(stoppedInfo.isStopped)
        
        val errorInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.ERROR
        )
        assertTrue(errorInfo.isStopped)
        
        val runningInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.RUNNING
        )
        assertFalse(runningInfo.isStopped)
    }
    
    @Test
    fun `VmInfo canStart should return correct value`() {
        val createdInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.CREATED
        )
        assertTrue(createdInfo.canStart)
        
        val stoppedInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.STOPPED
        )
        assertTrue(stoppedInfo.canStart)
        
        val runningInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.RUNNING
        )
        assertFalse(runningInfo.canStart)
    }
    
    @Test
    fun `VmInfo canStop should return correct value`() {
        val runningInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.RUNNING
        )
        assertTrue(runningInfo.canStop)
        
        val pausedInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.PAUSED
        )
        assertTrue(pausedInfo.canStop)
        
        val stoppedInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.STOPPED
        )
        assertFalse(stoppedInfo.canStop)
    }
    
    @Test
    fun `VmInfo canPause should return correct value`() {
        val runningInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.RUNNING
        )
        assertTrue(runningInfo.canPause)
        
        val stoppedInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.STOPPED
        )
        assertFalse(stoppedInfo.canPause)
    }
    
    @Test
    fun `VmInfo canResume should return correct value`() {
        val pausedInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.PAUSED
        )
        assertTrue(pausedInfo.canResume)
        
        val runningInfo = VmInfo(
            config = VmConfig("test", "Test"),
            state = VmState.RUNNING
        )
        assertFalse(runningInfo.canResume)
    }
    
    @Test
    fun `AvfVmHandle should have default timestamps`() {
        val before = System.currentTimeMillis()
        val handle = AvfVmHandle(vmId = "test-vm")
        val after = System.currentTimeMillis()
        
        assertEquals("test-vm", handle.vmId)
        assertTrue(handle.createdAt >= before && handle.createdAt <= after)
        assertTrue(handle.lastUpdated >= before && handle.lastUpdated <= after)
    }
    
    @Test
    fun `VmEvent Start should contain config`() {
        val config = VmConfig("test", "Test")
        val event = VmEvent.Start(config)
        
        assertEquals(config, event.config)
    }
    
    @Test
    fun `VmError should have message`() {
        val error = VmError.ConfigurationError("Invalid config")
        assertEquals("Invalid config", error.message)
    }
}