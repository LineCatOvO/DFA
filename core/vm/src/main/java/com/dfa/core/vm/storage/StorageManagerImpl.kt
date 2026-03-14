package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.CleanupOptions
import com.dfa.core.vm.storage.models.CleanupResult
import com.dfa.core.vm.storage.models.EncryptionConfig
import com.dfa.core.vm.storage.models.StorageConfig
import com.dfa.core.vm.storage.models.StorageInfo
import com.dfa.core.vm.storage.models.StorageState
import com.dfa.core.vm.storage.models.StorageStatistics
import com.dfa.core.vm.storage.models.StorageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 存储管理器实现
 *
 * 协调磁盘镜像、加密、配额和持久化管理，提供统一的存储管理接口
 */
@Singleton
class StorageManagerImpl @Inject constructor(
    private val diskImageManager: DiskImageManager,
    private val encryptionManager: EncryptionManager,
    private val quotaManager: QuotaManager,
    private val persistenceManager: PersistenceManager,
    private val safStorageProvider: SafStorageProvider
) : StorageManager, StorageConfigProvider {

    private val mutex = Mutex()
    private var config: StorageConfig? = null
    private var isReady = false

    private val _storageState = MutableStateFlow(StorageState())

    override suspend fun initialize(config: StorageConfig): Result<Unit> = mutex.withLock {
        return try {
            if (!config.validate()) {
                return Result.failure(
                    StorageException.PersistenceException("Invalid storage configuration")
                )
            }

            // 创建存储目录
            val storageDir = File(config.storagePath)
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            // 初始化加密管理器（如果启用）
            if (config.enableEncryption && config.encryptionKeyAlias != null) {
                val encryptionConfig = EncryptionConfig(
                    keyAlias = config.encryptionKeyAlias
                )
                val encryptionResult = encryptionManager.initialize(encryptionConfig)
                if (encryptionResult.isFailure) {
                    return Result.failure(
                        StorageException.EncryptionException(
                            "Failed to initialize encryption: ${encryptionResult.exceptionOrNull()?.message}"
                        )
                    )
                }
            }

            // 初始化持久化管理器
            val persistenceResult = persistenceManager.initialize(config.storagePath)
            if (persistenceResult.isFailure) {
                return Result.failure(
                    StorageException.PersistenceException(
                        "Failed to initialize persistence: ${persistenceResult.exceptionOrNull()?.message}"
                    )
                )
            }

            // 设置配额
            quotaManager.setQuota(
                com.dfa.core.vm.storage.QuotaType.TOTAL_STORAGE,
                config.maxStorageBytes
            )

            this.config = config
            this.isReady = true

            updateStorageState()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to initialize storage manager: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun getStorageInfo(): Result<StorageInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentConfig = config ?: return@withContext Result.failure(
                StorageException.PersistenceException("Storage manager not initialized")
            )

            val storageDir = File(currentConfig.storagePath)
            val totalSpace = storageDir.totalSpace
            val freeSpace = storageDir.freeSpace
            val usedSpace = totalSpace - freeSpace

            val storageInfo = StorageInfo(
                path = currentConfig.storagePath,
                type = if (currentConfig.enableEncryption) StorageType.ENCRYPTED else StorageType.INTERNAL,
                totalBytes = totalSpace,
                usedBytes = usedSpace,
                availableBytes = freeSpace,
                isEncrypted = currentConfig.enableEncryption
            )

            Result.success(storageInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to get storage info: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun getStorageStatistics(): Result<StorageStatistics> = withContext(Dispatchers.IO) {
        return@withContext try {
            val imageSize = diskImageManager.getTotalImageSize()
            val snapshots = persistenceManager.listSnapshots("").getOrDefault(emptyList())
            val encryptedSize = if (encryptionManager.isInitialized()) {
                persistenceManager.getStorageUsage()
            } else 0L

            val statistics = StorageStatistics(
                totalImages = 0, // 从镜像管理器获取
                totalImageBytes = imageSize,
                totalSnapshots = snapshots.size,
                totalSnapshotBytes = snapshots.sumOf { it.sizeBytes },
                encryptedDataBytes = encryptedSize
            )

            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to get storage statistics: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun checkStorageSpace(requiredBytes: Long): Boolean {
        val storageInfo = getStorageInfo().getOrNull() ?: return false
        return storageInfo.hasEnoughSpace(requiredBytes)
    }

    override suspend fun cleanupStorage(options: CleanupOptions): Result<CleanupResult> = mutex.withLock {
        return try {
            val result = CleanupResult()
            var imagesCleaned = 0
            var snapshotsCleaned = 0
            var tempFilesCleaned = 0
            var bytesReclaimed = 0L
            val errors = mutableListOf<String>()

            // 清理未使用的镜像
            if (options.cleanUnusedImages) {
                val cleanupResult = diskImageManager.cleanupUnusedImages()
                if (cleanupResult.isSuccess) {
                    imagesCleaned = cleanupResult.getOrThrow()
                }
            }

            // 清理旧快照
            if (options.cleanOldSnapshots) {
                // 实现快照清理逻辑
                // 保留最近N个快照
            }

            // 清理临时文件
            if (options.cleanTempFiles) {
                val currentConfig = config
                if (currentConfig != null) {
                    val tempDir = File(currentConfig.storagePath, "temp")
                    if (tempDir.exists()) {
                        tempDir.walkTopDown().forEach { file ->
                            if (file.isFile && file.extension == "tmp") {
                                bytesReclaimed += file.length()
                                if (!options.dryRun) {
                                    file.delete()
                                }
                                tempFilesCleaned++
                            }
                        }
                    }
                }
            }

            Result.success(
                CleanupResult(
                    imagesCleaned = imagesCleaned,
                    snapshotsCleaned = snapshotsCleaned,
                    tempFilesCleaned = tempFilesCleaned,
                    bytesReclaimed = bytesReclaimed,
                    errors = errors
                )
            )
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to cleanup storage: ${e.message}",
                    e
                )
            )
        }
    }

    override fun getDiskImageManager(): DiskImageManager = diskImageManager

    override fun getEncryptionManager(): EncryptionManager = encryptionManager

    override fun getQuotaManager(): QuotaManager = quotaManager

    override fun getPersistenceManager(): PersistenceManager = persistenceManager

    override fun getSafStorageProvider(): SafStorageProvider = safStorageProvider

    override suspend fun setStorageQuota(maxStorageBytes: Long): Result<Unit> {
        return quotaManager.setQuota(
            com.dfa.core.vm.storage.QuotaType.TOTAL_STORAGE,
            maxStorageBytes
        )
    }

    override suspend fun setEncryptionEnabled(enable: Boolean): Result<Unit> = mutex.withLock {
        return try {
            val currentConfig = config ?: return Result.failure(
                StorageException.PersistenceException("Storage manager not initialized")
            )

            if (enable && !encryptionManager.isInitialized()) {
                val keyAlias = currentConfig.encryptionKeyAlias ?: "dfa_storage_key"
                val encryptionConfig = EncryptionConfig(keyAlias = keyAlias)
                encryptionManager.initialize(encryptionConfig)

                config = currentConfig.copy(
                    enableEncryption = true,
                    encryptionKeyAlias = keyAlias
                )
            } else if (!enable && encryptionManager.isInitialized()) {
                encryptionManager.release()

                config = currentConfig.copy(enableEncryption = false)
            }

            updateStorageState()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.EncryptionException(
                    "Failed to set encryption: ${e.message}",
                    e
                )
            )
        }
    }

    override fun observeStorageState(): Flow<StorageState> = _storageState.asStateFlow()

    override suspend fun checkStorageHealth(): Result<StorageHealthStatus> = withContext(Dispatchers.IO) {
        return@withContext try {
            val storageInfo = getStorageInfo().getOrNull()
            val statistics = getStorageStatistics().getOrNull()

            val issues = mutableListOf<StorageIssue>()

            // 检查存储空间
            if (storageInfo != null) {
                if (storageInfo.usagePercent >= 95) {
                    issues.add(
                        StorageIssue(
                            type = IssueType.CRITICAL_SPACE,
                            severity = IssueSeverity.CRITICAL,
                            message = "Storage is critically low: ${storageInfo.usagePercent}% used",
                            suggestedAction = "Clean up unused images and snapshots"
                        )
                    )
                } else if (storageInfo.usagePercent >= 80) {
                    issues.add(
                        StorageIssue(
                            type = IssueType.LOW_SPACE,
                            severity = IssueSeverity.WARNING,
                            message = "Storage is running low: ${storageInfo.usagePercent}% used",
                            suggestedAction = "Consider cleaning up unused data"
                        )
                    )
                }
            }

            // 检查配额
            val quotaStatus = quotaManager.getAllQuotaStatus()
            for (status in quotaStatus) {
                if (status.isOverLimit) {
                    issues.add(
                        StorageIssue(
                            type = IssueType.QUOTA_EXCEEDED,
                            severity = IssueSeverity.ERROR,
                            message = "Quota exceeded for ${status.type}"
                        )
                    )
                }
            }

            // 验证数据完整性
            val integrityResult = persistenceManager.verifyIntegrity()
            if (integrityResult.isFailure || integrityResult.getOrNull() == false) {
                issues.add(
                    StorageIssue(
                        type = IssueType.CORRUPTED_IMAGE,
                        severity = IssueSeverity.ERROR,
                        message = "Data integrity check failed"
                    )
                )
            }

            val healthStatus = StorageHealthStatus(
                isHealthy = issues.none { it.severity == IssueSeverity.ERROR || it.severity == IssueSeverity.CRITICAL },
                totalSpace = storageInfo?.totalBytes ?: 0,
                usedSpace = storageInfo?.usedBytes ?: 0,
                availableSpace = storageInfo?.availableBytes ?: 0,
                imageCount = statistics?.totalImages ?: 0,
                snapshotCount = statistics?.totalSnapshots ?: 0,
                encryptedDataSize = statistics?.encryptedDataBytes ?: 0,
                issues = issues
            )

            Result.success(healthStatus)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to check storage health: ${e.message}",
                    e
                )
            )
        }
    }

    override fun getConfig(): StorageConfig? = config

    override fun isInitialized(): Boolean = isReady

    override suspend fun migrateStorage(targetPath: String): Result<Unit> = mutex.withLock {
        return try {
            val currentConfig = config ?: return Result.failure(
                StorageException.PersistenceException("Storage manager not initialized")
            )

            // 创建目标目录
            val targetDir = File(targetPath)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // 备份当前数据
            val backupResult = backupStorage("$targetPath/.migration_backup")
            if (backupResult.isFailure) {
                return Result.failure(
                    StorageException.PersistenceException(
                        "Migration backup failed: ${backupResult.exceptionOrNull()?.message}"
                    )
                )
            }

            // 恢复到新位置
            val restoreResult = persistenceManager.importData("$targetPath/.migration_backup")
            if (restoreResult.isFailure) {
                return Result.failure(
                    StorageException.PersistenceException(
                        "Migration restore failed: ${restoreResult.exceptionOrNull()?.message}"
                    )
                )
            }

            // 更新配置
            config = currentConfig.copy(storagePath = targetPath)

            // 清理备份
            File("$targetPath/.migration_backup").delete()

            updateStorageState()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to migrate storage: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun backupStorage(backupPath: String): Result<Unit> {
        return persistenceManager.exportData(backupPath)
    }

    override suspend fun restoreStorage(backupPath: String): Result<Unit> = mutex.withLock {
        return try {
            val restoreResult = persistenceManager.importData(backupPath)
            if (restoreResult.isFailure) {
                return restoreResult
            }

            updateStorageState()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to restore storage: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun release() = mutex.withLock {
        encryptionManager.release()
        persistenceManager.release()
        safStorageProvider.release()

        config = null
        isReady = false

        _storageState.value = StorageState()
    }

    // StorageConfigProvider 实现

    override fun getDefaultImagePath(): String {
        return config?.storagePath?.let { "$it/images" } ?: "/data/images"
    }

    override fun getStoragePath(): String {
        return config?.storagePath ?: "/data"
    }

    override fun getMaxStorageBytes(): Long {
        return config?.maxStorageBytes ?: (10L * 1024 * 1024 * 1024)
    }

    // 私有方法

    private suspend fun updateStorageState() {
        val storageInfo = getStorageInfo().getOrNull()

        _storageState.value = StorageState(
            isInitialized = isReady,
            storageInfo = storageInfo,
            isEncrypted = encryptionManager.isInitialized(),
            usagePercent = storageInfo?.usagePercent ?: 0,
            needsCleanup = storageInfo?.needsCleanup() ?: false
        )
    }
}