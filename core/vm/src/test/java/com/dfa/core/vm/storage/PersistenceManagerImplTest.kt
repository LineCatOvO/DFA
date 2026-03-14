package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.VmStateData
import com.google.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * PersistenceManagerImpl 单元测试
 */
class PersistenceManagerImplTest {

    private lateinit var encryptionManager: EncryptionManager
    private lateinit var manager: PersistenceManagerImpl

    @Before
    fun setup() {
        encryptionManager = mockk(relaxed = true)
        every { encryptionManager.isInitialized() } returns false
        manager = PersistenceManagerImpl(encryptionManager)
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `isInitialized should be false initially`() {
        assertThat(manager.isInitialized()).isFalse()
    }

    // ==================== Initialize Tests ====================

    @Test
    fun `initialize should succeed with valid path`() = runTest {
        val result = manager.initialize("/tmp/test-persistence")

        assertThat(result.isSuccess).isTrue()
        assertThat(manager.isInitialized()).isTrue()
    }

    // ==================== Save Vm State Tests ====================

    @Test
    fun `saveVmState should fail when not initialized`() = runTest {
        val stateData = VmStateData(
            vmId = "test-vm",
            configJson = "{}",
            timestamp = System.currentTimeMillis()
        )

        val result = manager.saveVmState("test-vm", stateData)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `saveVmState should succeed when initialized`() = runTest {
        manager.initialize("/tmp/test-persistence")
        val stateData = VmStateData(
            vmId = "test-vm",
            configJson = "{}",
            timestamp = System.currentTimeMillis()
        )

        val result = manager.saveVmState("test-vm", stateData)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Load Vm State Tests ====================

    @Test
    fun `loadVmState should fail when not initialized`() = runTest {
        val result = manager.loadVmState("test-vm")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `loadVmState should return null for non-existent state`() = runTest {
        manager.initialize("/tmp/test-persistence")

        val result = manager.loadVmState("non-existent")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    // ==================== Delete Vm State Tests ====================

    @Test
    fun `deleteVmState should fail when not initialized`() = runTest {
        val result = manager.deleteVmState("test-vm")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `deleteVmState should succeed when initialized`() = runTest {
        manager.initialize("/tmp/test-persistence")

        val result = manager.deleteVmState("test-vm")

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Has Vm State Tests ====================

    @Test
    fun `hasVmState should return false when not initialized`() = runTest {
        val result = manager.hasVmState("test-vm")

        assertThat(result).isFalse()
    }

    @Test
    fun `hasVmState should return false for non-existent state`() = runTest {
        manager.initialize("/tmp/test-persistence")

        val result = manager.hasVmState("non-existent")

        assertThat(result).isFalse()
    }

    // ==================== Create Snapshot Tests ====================

    @Test
    fun `createSnapshot should fail when not initialized`() = runTest {
        val result = manager.createSnapshot("test-vm", "snapshot-1", "Test snapshot")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Restore Snapshot Tests ====================

    @Test
    fun `restoreSnapshot should fail when not initialized`() = runTest {
        val result = manager.restoreSnapshot("snapshot-1")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Delete Snapshot Tests ====================

    @Test
    fun `deleteSnapshot should fail when not initialized`() = runTest {
        val result = manager.deleteSnapshot("snapshot-1")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== List Snapshots Tests ====================

    @Test
    fun `listSnapshots should return empty list when not initialized`() = runTest {
        val result = manager.listSnapshots("test-vm")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    // ==================== Get Snapshot Metadata Tests ====================

    @Test
    fun `getSnapshotMetadata should return null for non-existent snapshot`() = runTest {
        manager.initialize("/tmp/test-persistence")

        val result = manager.getSnapshotMetadata("non-existent")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    // ==================== Save Data Tests ====================

    @Test
    fun `saveData should fail when not initialized`() = runTest {
        val result = manager.saveData("key", byteArrayOf(1, 2, 3))

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `saveData should succeed when initialized`() = runTest {
        manager.initialize("/tmp/test-persistence")

        val result = manager.saveData("key", byteArrayOf(1, 2, 3))

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Load Data Tests ====================

    @Test
    fun `loadData should fail when not initialized`() = runTest {
        val result = manager.loadData("key")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `loadData should return null for non-existent key`() = runTest {
        manager.initialize("/tmp/test-persistence")

        val result = manager.loadData("non-existent")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    // ==================== Delete Data Tests ====================

    @Test
    fun `deleteData should fail when not initialized`() = runTest {
        val result = manager.deleteData("key")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Has Data Tests ====================

    @Test
    fun `hasData should return false when not initialized`() = runTest {
        val result = manager.hasData("key")

        assertThat(result).isFalse()
    }

    // ==================== Clear All Tests ====================

    @Test
    fun `clearAll should fail when not initialized`() = runTest {
        val result = manager.clearAll()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Get Storage Usage Tests ====================

    @Test
    fun `getStorageUsage should return 0 when not initialized`() = runTest {
        val result = manager.getStorageUsage()

        assertThat(result).isEqualTo(0L)
    }

    // ==================== Get Persistence State Tests ====================

    @Test
    fun `getPersistenceState should return flow`() {
        val flow = manager.getPersistenceState()
        assertThat(flow).isNotNull()
    }

    // ==================== Export Data Tests ====================

    @Test
    fun `exportData should fail when not initialized`() = runTest {
        val result = manager.exportData("/backup/path")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Import Data Tests ====================

    @Test
    fun `importData should fail when not initialized`() = runTest {
        val result = manager.importData("/backup/path")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Compact Tests ====================

    @Test
    fun `compact should fail when not initialized`() = runTest {
        val result = manager.compact()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Verify Integrity Tests ====================

    @Test
    fun `verifyIntegrity should fail when not initialized`() = runTest {
        val result = manager.verifyIntegrity()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Release Tests ====================

    @Test
    fun `release should reset state`() = runTest {
        manager.initialize("/tmp/test-persistence")
        manager.release()

        assertThat(manager.isInitialized()).isFalse()
    }
}