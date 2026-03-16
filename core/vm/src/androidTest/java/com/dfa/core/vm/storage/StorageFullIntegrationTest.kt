package com.dfa.core.vm.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.dfa.core.vm.storage.models.CleanupOptions
import com.dfa.core.vm.storage.models.CreateDiskImageRequest
import com.dfa.core.vm.storage.models.DiskImageFormat
import com.dfa.core.vm.storage.models.EncryptionConfig
import com.dfa.core.vm.storage.models.EncryptionAlgorithm
import com.dfa.core.vm.storage.models.EncryptionRequest
import com.dfa.core.vm.storage.models.DecryptionRequest
import com.dfa.core.vm.storage.models.KeyGenerationRequest
import com.dfa.core.vm.storage.models.StorageConfig
import com.dfa.core.vm.storage.models.StorageType
import com.dfa.core.vm.storage.models.StorageState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 存储系统完整集成测试
 *
 * 测试存储管理器、磁盘镜像、加密、配额和持久化功能的完整集成。
 * 需要在真实的Android设备上运行，验证与文件系统的集成。
 *
 * 测试覆盖范围：
 * - 存储管理器初始化和配置
 * - 磁盘镜像操作
 * - 加密存储功能
 * - 配额管理
 * - 持久化存储
 * - 存储健康检查
 *
 * 运行条件：
 * - 设备有足够的存储空间
 * - 文件系统权限正常
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 33)
class StorageFullIntegrationTest {

    // 存储管理器实例
    private lateinit var storageManager: StorageManager

    // 测试用的存储配置
    private lateinit var testStorageConfig: StorageConfig

    // 测试用的临时目录
    private val testStoragePath = "/data/local/tmp/dfa-storage-test-${System.currentTimeMillis()}"

    @Before
    fun setup() = runTest {
        // 初始化存储配置
        testStorageConfig = StorageConfig(
            storagePath = testStoragePath,
            maxStorageBytes = 1024 * 1024 * 1024, // 1GB
            enableEncryption = false
        )

        // 检查存储是否可用
        val isStorageAvailable = checkStorageAvailability()
        Assume.assumeTrue("Storage is not available on this device", isStorageAvailable)
    }

    @After
    fun tearDown() = runTest {
        // 清理测试资源
        cleanupTestResources()
        if (::storageManager.isInitialized) {
            storageManager.release()
        }
    }

    // ==================== 辅助方法 ====================

    private fun checkStorageAvailability(): Boolean {
        return try {
            val testDir = java.io.File(testStoragePath)
            testDir.mkdirs() || testDir.exists()
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun cleanupTestResources() {
        try {
            val testDir = java.io.File(testStoragePath)
            if (testDir.exists()) {
                testDir.deleteRecursively()
            }
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }

    // ==================== 存储管理器初始化测试 ====================

    @Test
    fun `initialize should set up storage manager correctly`() = runTest {
        // When: 初始化存储管理器
        val result = storageManager.initialize(testStorageConfig)

        // Then: 应该成功初始化
        assertThat(result.isSuccess).isTrue()
        assertThat(storageManager.isInitialized()).isTrue()
    }

    @Test
    fun `initialize should create storage directories`() = runTest {
        // When: 初始化存储管理器
        storageManager.initialize(testStorageConfig)

        // Then: 存储目录应该存在
        val storageDir = java.io.File(testStoragePath)
        assertThat(storageDir.exists()).isTrue()
        assertThat(storageDir.isDirectory).isTrue()
    }

    @Test
    fun `getConfig should return initialized config`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 获取配置
        val config = storageManager.getConfig()

        // Then: 应该返回正确的配置
        assertThat(config).isNotNull()
        assertThat(config?.storagePath).isEqualTo(testStoragePath)
    }

    @Test
    fun `getStorageInfo should return valid storage information`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 获取存储信息
        val result = storageManager.getStorageInfo()

        // Then: 应该返回有效的信息
        assertThat(result.isSuccess).isTrue()
        val info = result.getOrThrow()
        assertThat(info.totalBytes).isGreaterThan(0)
        assertThat(info.availableBytes).isGreaterThan(0)
    }

    @Test
    fun `getStorageStatistics should return detailed statistics`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 获取存储统计
        val result = storageManager.getStorageStatistics()

        // Then: 应该返回有效的统计信息
        assertThat(result.isSuccess).isTrue()
        val stats = result.getOrThrow()
        assertThat(stats.totalImages).isAtLeast(0)
        assertThat(stats.totalSnapshots).isAtLeast(0)
    }

    // ==================== 存储空间检查测试 ====================

    @Test
    fun `checkStorageSpace should return true for available space`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 检查是否有足够空间
        val hasSpace = storageManager.checkStorageSpace(1024 * 1024) // 1MB

        // Then: 应该返回true
        assertThat(hasSpace).isTrue()
    }

    @Test
    fun `checkStorageSpace should return false for excessive space`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 检查是否有足够空间（请求超过限制）
        val hasSpace = storageManager.checkStorageSpace(Long.MAX_VALUE)

        // Then: 应该返回false
        assertThat(hasSpace).isFalse()
    }

    // ==================== 磁盘镜像管理测试 ====================

    @Test
    fun `getDiskImageManager should return valid manager`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 获取磁盘镜像管理器
        val diskImageManager = storageManager.getDiskImageManager()

        // Then: 应该返回有效的管理器
        assertThat(diskImageManager).isNotNull()
    }

    @Test
    fun `create disk image should succeed`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)
        val diskImageManager = storageManager.getDiskImageManager()
        val request = CreateDiskImageRequest(
            name = "test-image-${System.currentTimeMillis()}",
            sizeBytes = 1024 * 1024 * 100, // 100MB
            format = DiskImageFormat.QCOW2
        )

        // When: 创建磁盘镜像
        val result = diskImageManager.createImage(request)

        // Then: 应该成功创建
        assertThat(result.success).isTrue()
    }

    @Test
    fun `list disk images should return created images`() = runTest {
        // Given: 已初始化的存储管理器和已创建的镜像
        storageManager.initialize(testStorageConfig)
        val diskImageManager = storageManager.getDiskImageManager()
        val request = CreateDiskImageRequest(
            name = "test-list-${System.currentTimeMillis()}",
            sizeBytes = 1024 * 1024 * 50,
            format = DiskImageFormat.QCOW2
        )
        diskImageManager.createImage(request)

        // When: 列出磁盘镜像
        val result = diskImageManager.listImages()

        // Then: 应该返回镜像列表
        assertThat(result.isSuccess).isTrue()
        val images = result.getOrThrow()
        assertThat(images).isNotNull()
    }

    // ==================== 加密存储测试 ====================

    @Test
    fun `getEncryptionManager should return valid manager`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 获取加密管理器
        val encryptionManager = storageManager.getEncryptionManager()

        // Then: 应该返回有效的管理器
        assertThat(encryptionManager).isNotNull()
    }

    @Test
    fun `setEncryptionEnabled should enable encryption`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 启用加密
        val result = storageManager.setEncryptionEnabled(true)

        // Then: 应该成功启用
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `encrypt data should produce encrypted output`() = runTest {
        // Given: 已初始化的加密管理器
        storageManager.initialize(testStorageConfig)
        val encryptionManager = storageManager.getEncryptionManager()
        encryptionManager.initialize(
            EncryptionConfig(
                algorithm = EncryptionAlgorithm.AES_256_GCM,
                keyAlias = "test-key"
            )
        )

        // When: 加密数据
        val testData = "Test data for encryption".toByteArray()
        val result = encryptionManager.encrypt(EncryptionRequest(data = testData))

        // Then: 应该成功加密
        assertThat(result.success).isTrue()
        assertThat(result.encryptedData).isNotNull()
    }

    @Test
    fun `decrypt data should restore original data`() = runTest {
        // Given: 已初始化的加密管理器和已加密的数据
        storageManager.initialize(testStorageConfig)
        val encryptionManager = storageManager.getEncryptionManager()
        encryptionManager.initialize(
            EncryptionConfig(
                algorithm = EncryptionAlgorithm.AES_256_GCM,
                keyAlias = "test-key"
            )
        )
        val originalData = "Test data for encryption".toByteArray()
        val encryptedResult = encryptionManager.encrypt(EncryptionRequest(data = originalData))
        val encryptedData = encryptedResult.encryptedData!!

        // When: 解密数据
        val result = encryptionManager.decrypt(DecryptionRequest(encryptedData = encryptedData))

        // Then: 应该恢复原始数据
        assertThat(result.success).isTrue()
        assertThat(result.data).isEqualTo(originalData)
    }

    @Test
    fun `generate key should create valid key`() = runTest {
        // Given: 已初始化的加密管理器
        storageManager.initialize(testStorageConfig)
        val encryptionManager = storageManager.getEncryptionManager()
        encryptionManager.initialize(
            EncryptionConfig(
                algorithm = EncryptionAlgorithm.AES_256_GCM,
                keyAlias = "test-key"
            )
        )

        // When: 生成密钥
        val result = encryptionManager.generateKey(
            KeyGenerationRequest(
                alias = "test-generated-key",
                keySize = 256
            )
        )

        // Then: 应该成功生成
        assertThat(result.isSuccess).isTrue()
        val keyInfo = result.getOrThrow()
        assertThat(keyInfo.alias).isEqualTo("test-generated-key")
    }

    // ==================== 配额管理测试 ====================

    @Test
    fun `getQuotaManager should return valid manager`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 获取配额管理器
        val quotaManager = storageManager.getQuotaManager()

        // Then: 应该返回有效的管理器
        assertThat(quotaManager).isNotNull()
    }

    @Test
    fun `setStorageQuota should update quota limit`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 设置存储配额
        val newQuota = 512 * 1024 * 1024L // 512MB
        val result = storageManager.setStorageQuota(newQuota)

        // Then: 应该成功设置
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `setQuota should update quota for specific type`() = runTest {
        // Given: 已初始化的配额管理器
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()

        // When: 设置磁盘镜像配额
        val result = quotaManager.setQuota(
            quotaType = QuotaType.DISK_IMAGES,
            limitBytes = 200L * 1024 * 1024 // 200MB
        )

        // Then: 应该成功设置
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `getQuotaLimit should return set limit`() = runTest {
        // Given: 已设置配额的管理器
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()
        val limit = 200L * 1024 * 1024
        quotaManager.setQuota(QuotaType.DISK_IMAGES, limit)

        // When: 获取配额限制
        val result = quotaManager.getQuotaLimit(QuotaType.DISK_IMAGES)

        // Then: 应该返回正确的限制
        assertThat(result).isEqualTo(limit)
    }

    @Test
    fun `getCurrentUsage should return usage amount`() = runTest {
        // Given: 已初始化的配额管理器
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()

        // When: 获取当前使用量
        val usage = quotaManager.getCurrentUsage(QuotaType.TOTAL_STORAGE)

        // Then: 应该返回有效的使用量
        assertThat(usage).isAtLeast(0)
    }

    @Test
    fun `hasEnoughQuota should return true for sufficient quota`() = runTest {
        // Given: 已初始化的配额管理器
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()
        quotaManager.setQuota(QuotaType.DISK_IMAGES, 1024L * 1024 * 100) // 100MB

        // When: 检查是否有足够配额
        val hasEnough = quotaManager.hasEnoughQuota(QuotaType.DISK_IMAGES, 1024L * 1024) // 1MB

        // Then: 应该返回true
        assertThat(hasEnough).isTrue()
    }

    @Test
    fun `reserveQuota should create reservation`() = runTest {
        // Given: 已初始化的配额管理器
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()
        quotaManager.setQuota(QuotaType.DISK_IMAGES, 1024L * 1024 * 100)

        // When: 预留配额
        val result = quotaManager.reserveQuota(
            quotaType = QuotaType.DISK_IMAGES,
            bytes = 1024L * 1024 * 10, // 10MB
            reservationId = "test-reservation-1"
        )

        // Then: 应该成功预留
        assertThat(result.isSuccess).isTrue()
        val reservation = result.getOrThrow()
        assertThat(reservation.id).isEqualTo("test-reservation-1")
    }

    @Test
    fun `commitReservation should finalize reservation`() = runTest {
        // Given: 已预留的配额
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()
        quotaManager.setQuota(QuotaType.DISK_IMAGES, 1024L * 1024 * 100)
        quotaManager.reserveQuota(
            quotaType = QuotaType.DISK_IMAGES,
            bytes = 1024L * 1024 * 10,
            reservationId = "test-reservation-2"
        )

        // When: 提交预留
        val result = quotaManager.commitReservation("test-reservation-2")

        // Then: 应该成功提交
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `cancelReservation should release reserved quota`() = runTest {
        // Given: 已预留的配额
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()
        quotaManager.setQuota(QuotaType.DISK_IMAGES, 1024L * 1024 * 100)
        quotaManager.reserveQuota(
            quotaType = QuotaType.DISK_IMAGES,
            bytes = 1024L * 1024 * 10,
            reservationId = "test-reservation-3"
        )

        // When: 取消预留
        val result = quotaManager.cancelReservation("test-reservation-3")

        // Then: 应该成功取消
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `getUsagePercent should return percentage`() = runTest {
        // Given: 已设置配额的管理器
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()
        quotaManager.setQuota(QuotaType.DISK_IMAGES, 1024L * 1024 * 100)

        // When: 获取使用率
        val usagePercent = quotaManager.getUsagePercent(QuotaType.DISK_IMAGES)

        // Then: 应该返回有效的百分比
        assertThat(usagePercent).isAtLeast(0)
        assertThat(usagePercent).isAtMost(100)
    }

    @Test
    fun `isOverLimit should return false when under limit`() = runTest {
        // Given: 已设置配额的管理器
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()
        quotaManager.setQuota(QuotaType.DISK_IMAGES, 1024L * 1024 * 100)

        // When: 检查是否超限
        val isOverLimit = quotaManager.isOverLimit(QuotaType.DISK_IMAGES)

        // Then: 应该返回false
        assertThat(isOverLimit).isFalse()
    }

    @Test
    fun `getAllQuotaStatus should return all quota statuses`() = runTest {
        // Given: 已初始化的配额管理器
        storageManager.initialize(testStorageConfig)
        val quotaManager = storageManager.getQuotaManager()

        // When: 获取所有配额状态
        val statuses = quotaManager.getAllQuotaStatus()

        // Then: 应该返回非空列表
        assertThat(statuses).isNotEmpty()
    }

    // ==================== 持久化存储测试 ====================

    @Test
    fun `getPersistenceManager should return valid manager`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 获取持久化管理器
        val persistenceManager = storageManager.getPersistenceManager()

        // Then: 应该返回有效的管理器
        assertThat(persistenceManager).isNotNull()
    }

    @Test
    fun `save and load data should persist correctly`() = runTest {
        // Given: 已初始化的持久化管理器
        storageManager.initialize(testStorageConfig)
        val persistenceManager = storageManager.getPersistenceManager()
        persistenceManager.initialize(testStoragePath)
        val key = "test-key-${System.currentTimeMillis()}"
        val value = "test-value".toByteArray()

        // When: 保存数据
        val saveResult = persistenceManager.saveData(key, value)

        // Then: 应该成功保存
        assertThat(saveResult.isSuccess).isTrue()

        // When: 加载数据
        val loadResult = persistenceManager.loadData(key)

        // Then: 应该返回正确的数据
        assertThat(loadResult.isSuccess).isTrue()
        assertThat(loadResult.getOrNull()).isEqualTo(value)
    }

    @Test
    fun `delete data should remove persisted data`() = runTest {
        // Given: 已保存的数据
        storageManager.initialize(testStorageConfig)
        val persistenceManager = storageManager.getPersistenceManager()
        persistenceManager.initialize(testStoragePath)
        val key = "test-delete-key-${System.currentTimeMillis()}"
        persistenceManager.saveData(key, "test".toByteArray())

        // When: 删除数据
        val result = persistenceManager.deleteData(key)

        // Then: 应该成功删除
        assertThat(result.isSuccess).isTrue()

        // 验证数据已删除
        val loadResult = persistenceManager.loadData(key)
        assertThat(loadResult.getOrNull()).isNull()
    }

    // ==================== 存储状态监听测试 ====================

    @Test
    fun `observeStorageState should emit state changes`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 观察存储状态
        val state = storageManager.observeStorageState().first()

        // Then: 应该返回有效的状态
        assertThat(state.isInitialized).isTrue()
    }

    // ==================== 存储健康检查测试 ====================

    @Test
    fun `checkStorageHealth should return healthy status`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 检查存储健康状态
        val result = storageManager.checkStorageHealth()

        // Then: 应该返回健康状态
        assertThat(result.isSuccess).isTrue()
        val health = result.getOrThrow()
        assertThat(health.isHealthy).isTrue()
    }

    // ==================== 存储清理测试 ====================

    @Test
    fun `cleanupStorage should remove unnecessary files`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 清理存储
        val result = storageManager.cleanupStorage(CleanupOptions())

        // Then: 应该成功清理
        assertThat(result.isSuccess).isTrue()
        val cleanupResult = result.getOrThrow()
        assertThat(cleanupResult.totalItemsCleaned).isAtLeast(0)
    }

    // ==================== 存储迁移测试 ====================

    @Test
    fun `migrateStorage should move data to new location`() = runTest {
        // Given: 已初始化的存储管理器和新路径
        storageManager.initialize(testStorageConfig)
        val newPath = "$testStoragePath-migrated"

        // When: 迁移存储
        val result = storageManager.migrateStorage(newPath)

        // Then: 应该成功迁移
        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 存储备份恢复测试 ====================

    @Test
    fun `backupStorage should create backup`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)
        val backupPath = "$testStoragePath-backup"

        // When: 备份存储
        val result = storageManager.backupStorage(backupPath)

        // Then: 应该成功备份
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `restoreStorage should restore from backup`() = runTest {
        // Given: 已备份的存储
        storageManager.initialize(testStorageConfig)
        val backupPath = "$testStoragePath-backup"
        storageManager.backupStorage(backupPath)

        // When: 恢复存储
        val result = storageManager.restoreStorage(backupPath)

        // Then: 应该成功恢复
        assertThat(result.isSuccess).isTrue()
    }

    // ==================== SAF存储提供者测试 ====================

    @Test
    fun `getSafStorageProvider should return valid provider`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 获取SAF存储提供者
        val safProvider = storageManager.getSafStorageProvider()

        // Then: 应该返回有效的提供者
        assertThat(safProvider).isNotNull()
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `initialize should fail with invalid path`() = runTest {
        // Given: 无效的存储路径
        val invalidConfig = StorageConfig(
            storagePath = "/proc/invalid-path"
        )

        // When: 尝试初始化
        val result = storageManager.initialize(invalidConfig)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getStorageInfo should fail when not initialized`() = runTest {
        // Given: 未初始化的存储管理器

        // When: 尝试获取存储信息
        val result = storageManager.getStorageInfo()

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `release should clean up resources`() = runTest {
        // Given: 已初始化的存储管理器
        storageManager.initialize(testStorageConfig)

        // When: 释放资源
        storageManager.release()

        // Then: 应该不再初始化
        assertThat(storageManager.isInitialized()).isFalse()
    }
}