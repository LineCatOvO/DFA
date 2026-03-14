package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.CleanupOptions
import com.dfa.core.vm.storage.models.StorageConfig
import com.dfa.core.vm.storage.models.StorageType
import com.google.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * StorageManagerImpl 单元测试
 */
class StorageManagerImplTest {

    private lateinit var diskImageManager: DiskImageManager
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var quotaManager: QuotaManager
    private lateinit var persistenceManager: PersistenceManager
    private lateinit var safStorageProvider: SafStorageProvider
    private lateinit var manager: StorageManagerImpl

    @Before
    fun setup() {
        diskImageManager = mockk(relaxed = true)
        encryptionManager = mockk(relaxed = true)
        quotaManager = mockk(relaxed = true)
        persistenceManager = mockk(relaxed = true)
        safStorageProvider = mockk(relaxed = true)

        manager = StorageManagerImpl(
            diskImageManager,
            encryptionManager,
            quotaManager,
            persistenceManager,
            safStorageProvider
        )
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `isInitialized should be false initially`() {
        assertThat(manager.isInitialized()).isFalse()
    }

    @Test
    fun `getConfig should return null initially`() {
        assertThat(manager.getConfig()).isNull()
    }

    // ==================== Initialize Tests ====================

    @Test
    fun `initialize should fail with invalid config`() = runTest {
        val config = StorageConfig(
            storagePath = "",
            maxStorageBytes = 0
        )

        val result = manager.initialize(config)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `initialize should succeed with valid config`() = runTest {
        val config = StorageConfig(
            storagePath = "/tmp/test-storage",
            maxStorageBytes = 1024 * 1024 * 1024
        )
        coEvery { persistenceManager.initialize(any()) } returns Result.success(Unit)

        val result = manager.initialize(config)

        assertThat(result.isSuccess).isTrue()
        assertThat(manager.isInitialized()).isTrue()
    }

    // ==================== Get Storage Info Tests ====================

    @Test
    fun `getStorageInfo should fail when not initialized`() = runTest {
        val result = manager.getStorageInfo()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Check Storage Space Tests ====================

    @Test
    fun `checkStorageSpace should return false when not initialized`() = runTest {
        val result = manager.checkStorageSpace(1024)

        assertThat(result).isFalse()
    }

    // ==================== Cleanup Storage Tests ====================

    @Test
    fun `cleanupStorage should succeed with valid options`() = runTest {
        val config = StorageConfig(
            storagePath = "/tmp/test-storage",
            maxStorageBytes = 1024 * 1024 * 1024
        )
        coEvery { persistenceManager.initialize(any()) } returns Result.success(Unit)
        coEvery { diskImageManager.cleanupUnusedImages() } returns Result.success(0)
        manager.initialize(config)

        val options = CleanupOptions(
            cleanUnusedImages = true,
            cleanOldSnapshots = false,
            cleanTempFiles = false
        )

        val result = manager.cleanupStorage(options)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Get Managers Tests ====================

    @Test
    fun `getDiskImageManager should return correct instance`() {
        assertThat(manager.getDiskImageManager()).isEqualTo(diskImageManager)
    }

    @Test
    fun `getEncryptionManager should return correct instance`() {
        assertThat(manager.getEncryptionManager()).isEqualTo(encryptionManager)
    }

    @Test
    fun `getQuotaManager should return correct instance`() {
        assertThat(manager.getQuotaManager()).isEqualTo(quotaManager)
    }

    @Test
    fun `getPersistenceManager should return correct instance`() {
        assertThat(manager.getPersistenceManager()).isEqualTo(persistenceManager)
    }

    @Test
    fun `getSafStorageProvider should return correct instance`() {
        assertThat(manager.getSafStorageProvider()).isEqualTo(safStorageProvider)
    }

    // ==================== Set Storage Quota Tests ====================

    @Test
    fun `setStorageQuota should succeed`() = runTest {
        coEvery { quotaManager.setQuota(any(), any()) } returns Result.success(Unit)

        val result = manager.setStorageQuota(1024 * 1024 * 1024)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Set Encryption Enabled Tests ====================

    @Test
    fun `setEncryptionEnabled should fail when not initialized`() = runTest {
        val result = manager.setEncryptionEnabled(true)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Observe Storage State Tests ====================

    @Test
    fun `observeStorageState should return flow`() {
        val flow = manager.observeStorageState()
        assertThat(flow).isNotNull()
    }

    // ==================== Check Storage Health Tests ====================

    @Test
    fun `checkStorageHealth should fail when not initialized`() = runTest {
        val result = manager.checkStorageHealth()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Migrate Storage Tests ====================

    @Test
    fun `migrateStorage should fail when not initialized`() = runTest {
        val result = manager.migrateStorage("/new/path")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Backup Storage Tests ====================

    @Test
    fun `backupStorage should delegate to persistenceManager`() = runTest {
        coEvery { persistenceManager.exportData(any()) } returns Result.success(Unit)

        val result = manager.backupStorage("/backup/path")

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Restore Storage Tests ====================

    @Test
    fun `restoreStorage should fail when not initialized`() = runTest {
        val result = manager.restoreStorage("/backup/path")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Release Tests ====================

    @Test
    fun `release should reset state`() = runTest {
        manager.release()

        assertThat(manager.isInitialized()).isFalse()
        assertThat(manager.getConfig()).isNull()
    }

    // ==================== StorageConfigProvider Tests ====================

    @Test
    fun `getDefaultImagePath should return valid path`() = runTest {
        val config = StorageConfig(
            storagePath = "/tmp/test-storage",
            maxStorageBytes = 1024 * 1024 * 1024
        )
        coEvery { persistenceManager.initialize(any()) } returns Result.success(Unit)
        manager.initialize(config)

        val path = manager.getDefaultImagePath()

        assertThat(path).contains("images")
    }

    @Test
    fun `getStoragePath should return valid path when initialized`() = runTest {
        val config = StorageConfig(
            storagePath = "/tmp/test-storage",
            maxStorageBytes = 1024 * 1024 * 1024
        )
        coEvery { persistenceManager.initialize(any()) } returns Result.success(Unit)
        manager.initialize(config)

        val path = manager.getStoragePath()

        assertThat(path).isEqualTo("/tmp/test-storage")
    }

    @Test
    fun `getMaxStorageBytes should return valid value when initialized`() = runTest {
        val config = StorageConfig(
            storagePath = "/tmp/test-storage",
            maxStorageBytes = 1024 * 1024 * 1024
        )
        coEvery { persistenceManager.initialize(any()) } returns Result.success(Unit)
        manager.initialize(config)

        val maxBytes = manager.getMaxStorageBytes()

        assertThat(maxBytes).isEqualTo(1024 * 1024 * 1024)
    }
}