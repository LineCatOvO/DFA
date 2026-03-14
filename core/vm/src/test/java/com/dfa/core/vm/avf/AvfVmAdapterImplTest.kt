package com.dfa.core.vm.avf

import com.dfa.core.vm.AvfVmHandle
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmState
import com.google.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * AvfVmAdapterImpl 单元测试
 */
class AvfVmAdapterImplTest {

    private lateinit var adapter: AvfVmAdapterImpl

    @Before
    fun setup() {
        adapter = AvfVmAdapterImpl()
    }

    // ==================== isAvfAvailable Tests ====================

    @Test
    fun `isAvfAvailable should return true`() = runTest {
        val result = adapter.isAvfAvailable()
        assertThat(result).isTrue()
    }

    // ==================== createVm Tests ====================

    @Test
    fun `createVm should succeed with valid config`() = runTest {
        val config = VmConfig(
            id = "test-vm",
            name = "Test VM",
            memory = 2048,
            cpu = 2
        )

        val result = adapter.createVm(config)

        assertThat(result.isSuccess).isTrue()
        val handle = result.getOrNull()
        assertThat(handle).isNotNull()
        assertThat(handle?.vmId).isEqualTo("test-vm")
        assertThat(handle?.processId).isGreaterThan(0)
    }

    @Test
    fun `createVm should fail with invalid config`() = runTest {
        val config = VmConfig(
            id = "test-vm",
            name = "Test VM",
            memory = 0 // Invalid memory
        )

        val result = adapter.createVm(config)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== startVm Tests ====================

    @Test
    fun `startVm should succeed after createVm`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()

        val result = adapter.startVm(handle)

        assertThat(result.isSuccess).isTrue()
        val vmInfo = result.getOrNull()
        assertThat(vmInfo).isNotNull()
        assertThat(vmInfo?.state).isEqualTo(VmState.RUNNING)
        assertThat(vmInfo?.ipAddress).isNotNull()
    }

    @Test
    fun `startVm should fail without createVm`() = runTest {
        val handle = AvfVmHandle(vmId = "non-existent")

        val result = adapter.startVm(handle)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== stopVm Tests ====================

    @Test
    fun `stopVm should succeed after startVm`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()
        adapter.startVm(handle)

        val result = adapter.stopVm(handle)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `stopVm with force should succeed`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()
        adapter.startVm(handle)

        val result = adapter.stopVm(handle, force = true)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== pauseVm Tests ====================

    @Test
    fun `pauseVm should succeed when running`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()
        adapter.startVm(handle)

        val result = adapter.pauseVm(handle)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `pauseVm should fail when not running`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()

        val result = adapter.pauseVm(handle)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== resumeVm Tests ====================

    @Test
    fun `resumeVm should succeed when paused`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()
        adapter.startVm(handle)
        adapter.pauseVm(handle)

        val result = adapter.resumeVm(handle)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `resumeVm should fail when not paused`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()
        adapter.startVm(handle)

        val result = adapter.resumeVm(handle)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== getVmStatus Tests ====================

    @Test
    fun `getVmStatus should return current status`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()

        val result = adapter.getVmStatus(handle)

        assertThat(result.isSuccess).isTrue()
        val vmInfo = result.getOrNull()
        assertThat(vmInfo?.state).isEqualTo(VmState.CREATED)
    }

    // ==================== destroyVm Tests ====================

    @Test
    fun `destroyVm should succeed`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()

        val result = adapter.destroyVm(handle)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `destroyVm should stop running vm first`() = runTest {
        val config = VmConfig("test-vm", "Test VM")
        val handle = adapter.createVm(config).getOrThrow()
        adapter.startVm(handle)

        val result = adapter.destroyVm(handle)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Callback Tests ====================

    @Test
    fun `registerCallback should add callback`() = runTest {
        var stateChanged = false
        val callback = object : AvfVmCallback {
            override fun onStateChanged(state: VmState) {
                stateChanged = true
            }
            override fun onError(error: com.dfa.core.vm.VmError) {}
        }

        adapter.registerCallback(callback)

        val config = VmConfig("test-vm", "Test VM")
        adapter.createVm(config)

        assertThat(stateChanged).isTrue()
    }

    @Test
    fun `unregisterCallback should remove callback`() = runTest {
        var callCount = 0
        val callback = object : AvfVmCallback {
            override fun onStateChanged(state: VmState) {
                callCount++
            }
            override fun onError(error: com.dfa.core.vm.VmError) {}
        }

        adapter.registerCallback(callback)
        adapter.unregisterCallback(callback)

        val config = VmConfig("test-vm", "Test VM")
        adapter.createVm(config)

        assertThat(callCount).isEqualTo(0)
    }

    @Test
    fun `registerCallback should not add duplicate callback`() = runTest {
        var callCount = 0
        val callback = object : AvfVmCallback {
            override fun onStateChanged(state: VmState) {
                callCount++
            }
            override fun onError(error: com.dfa.core.vm.VmError) {}
        }

        adapter.registerCallback(callback)
        adapter.registerCallback(callback) // Duplicate

        val config = VmConfig("test-vm", "Test VM")
        adapter.createVm(config)

        // Should only be called once
        assertThat(callCount).isEqualTo(1)
    }

    // ==================== isConfigSupported Tests ====================

    @Test
    fun `isConfigSupported should return true for valid config`() = runTest {
        val config = VmConfig(
            id = "test-vm",
            name = "Test VM",
            memory = 2048,
            cpu = 2
        )

        val result = adapter.isConfigSupported(config)

        assertThat(result).isTrue()
    }

    @Test
    fun `isConfigSupported should return false for excessive memory`() = runTest {
        val config = VmConfig(
            id = "test-vm",
            name = "Test VM",
            memory = 16384, // Exceeds 8192 limit
            cpu = 2
        )

        val result = adapter.isConfigSupported(config)

        assertThat(result).isFalse()
    }

    @Test
    fun `isConfigSupported should return false for excessive cpu`() = runTest {
        val config = VmConfig(
            id = "test-vm",
            name = "Test VM",
            memory = 2048,
            cpu = 16 // Exceeds 8 limit
        )

        val result = adapter.isConfigSupported(config)

        assertThat(result).isFalse()
    }

    // ==================== getAvailableResources Tests ====================

    @Test
    fun `getAvailableResources should return valid resources`() = runTest {
        val resources = adapter.getAvailableResources()

        assertThat(resources.totalMemoryMb).isGreaterThan(0)
        assertThat(resources.availableMemoryMb).isGreaterThan(0)
        assertThat(resources.totalCpuCores).isGreaterThan(0)
        assertThat(resources.availableCpuCores).isGreaterThan(0)
        assertThat(resources.totalDiskSpaceGb).isGreaterThan(0)
        assertThat(resources.availableDiskSpaceGb).isGreaterThan(0)
    }

    @Test
    fun `getAvailableResources available should not exceed total`() = runTest {
        val resources = adapter.getAvailableResources()

        assertThat(resources.availableMemoryMb).isAtMost(resources.totalMemoryMb)
        assertThat(resources.availableCpuCores).isAtMost(resources.totalCpuCores)
        assertThat(resources.availableDiskSpaceGb).isAtMost(resources.totalDiskSpaceGb)
    }
}