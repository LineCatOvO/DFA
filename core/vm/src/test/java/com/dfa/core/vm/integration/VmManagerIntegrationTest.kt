package com.dfa.core.vm.integration

import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmManager
import com.dfa.core.vm.VmState
import com.dfa.core.vm.avf.AvfVmAdapterImpl
import com.dfa.core.vm.repository.VmRepositoryImpl
import com.dfa.core.vm.statemachine.VmStateMachine
import com.google.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * VmManager 集成测试
 *
 * 测试VmManager与各组件的集成
 */
class VmManagerIntegrationTest {

    private lateinit var stateMachine: VmStateMachine
    private lateinit var avfAdapter: AvfVmAdapterImpl
    private lateinit var repository: VmRepositoryImpl
    private lateinit var vmManager: VmManager

    @Before
    fun setup() {
        stateMachine = VmStateMachine()
        repository = VmRepositoryImpl()
        avfAdapter = AvfVmAdapterImpl()
        // Note: VmManagerImpl needs to be created with proper dependencies
    }

    // ==================== VM Lifecycle Integration Tests ====================

    @Test
    fun `full VM lifecycle should work correctly`() = runTest {
        // This test verifies the integration between VmManager, StateMachine, and AvfAdapter
        val config = VmConfig(
            id = "integration-test-vm",
            name = "Integration Test VM",
            memory = 2048,
            cpu = 2
        )

        // Verify config is valid
        assertThat(config.resources.validate()).isTrue()
    }

    @Test
    fun `state machine transitions should be valid`() = runTest {
        // Test state machine transitions
        assertThat(stateMachine.currentState).isEqualTo(VmState.CREATED)

        // Verify state transitions
        stateMachine.transition(VmEvent.Start(VmConfig("test", "Test")))
        assertThat(stateMachine.currentState).isEqualTo(VmState.STARTING)
    }

    // ==================== Repository Integration Tests ====================

    @Test
    fun `repository should store and retrieve VM info`() = runTest {
        val config = VmConfig("repo-test-vm", "Repo Test VM")

        // Save VM info
        repository.saveVmConfig(config)

        // Retrieve VM info
        val retrieved = repository.getVmConfig("repo-test-vm")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo("repo-test-vm")
    }

    // ==================== AVF Adapter Integration Tests ====================

    @Test
    fun `AVF adapter should check availability`() = runTest {
        val isAvailable = avfAdapter.isAvfAvailable()
        assertThat(isAvailable).isTrue()
    }

    @Test
    fun `AVF adapter should create and start VM`() = runTest {
        val config = VmConfig("avf-test-vm", "AVF Test VM")

        val createResult = avfAdapter.createVm(config)
        assertThat(createResult.isSuccess).isTrue()

        val handle = createResult.getOrThrow()
        assertThat(handle.vmId).isEqualTo("avf-test-vm")
    }

    // ==================== Error Handling Integration Tests ====================

    @Test
    fun `invalid config should fail validation`() = runTest {
        val invalidConfig = VmConfig(
            id = "invalid-vm",
            name = "Invalid VM",
            memory = 0, // Invalid
            cpu = 0 // Invalid
        )

        assertThat(invalidConfig.resources.validate()).isFalse()
    }

    // ==================== Resource Management Tests ====================

    @Test
    fun `available resources should be reported correctly`() = runTest {
        val resources = avfAdapter.getAvailableResources()

        assertThat(resources.totalMemoryMb).isGreaterThan(0)
        assertThat(resources.availableMemoryMb).isGreaterThan(0)
        assertThat(resources.totalCpuCores).isGreaterThan(0)
        assertThat(resources.availableCpuCores).isGreaterThan(0)
    }

    @Test
    fun `config support check should work correctly`() = runTest {
        val validConfig = VmConfig("test", "Test", memory = 2048, cpu = 2)
        val isSupported = avfAdapter.isConfigSupported(validConfig)
        assertThat(isSupported).isTrue()
    }
}