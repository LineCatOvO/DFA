package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.CleanupOptions
import com.dfa.core.vm.storage.models.CleanupResult
import com.dfa.core.vm.storage.models.StorageConfig
import com.dfa.core.vm.storage.models.StorageInfo
import com.dfa.core.vm.storage.models.StorageStatistics
import com.dfa.core.vm.storage.models.StorageType
import kotlinx.coroutines.flow.Flow

/**
 * 存储管理器接口
 *
 * 提供统一的存储管理功能，协调磁盘镜像、加密、配额和持久化管理
 */
interface StorageManager {

    /**
     * 初始化存储管理器
     *
     * @param config 存储配置
     * @return 初始化结果
     */
    suspend fun initialize(config: StorageConfig): Result<Unit>

    /**
     * 获取存储信息
     *
     * @return 存储信息
     */
    suspend fun getStorageInfo(): Result<StorageInfo>

    /**
     * 获取存储统计
     *
     * @return 存储统计信息
     */
    suspend fun getStorageStatistics(): Result<StorageStatistics>

    /**
     * 检查存储空间
     *
     * @param requiredBytes 需要的字节数
     * @return 是否有足够空间
     */
    suspend fun checkStorageSpace(requiredBytes: Long): Boolean

    /**
     * 清理存储
     *
     * @param options 清理选项
     * @return 清理结果
     */
    suspend fun cleanupStorage(options: CleanupOptions = CleanupOptions()): Result<CleanupResult>

    /**
     * 获取磁盘镜像管理器
     *
     * @return 磁盘镜像管理器
     */
    fun getDiskImageManager(): DiskImageManager

    /**
     * 获取加密管理器
     *
     * @return 加密管理器
     */
    fun getEncryptionManager(): EncryptionManager

    /**
     * 获取配额管理器
     *
     * @return 配额管理器
     */
    fun getQuotaManager(): QuotaManager

    /**
     * 获取持久化管理器
     *
     * @return 持久化管理器
     */
    fun getPersistenceManager(): PersistenceManager

    /**
     * 获取SAF存储提供者
     *
     * @return SAF存储提供者
     */
    fun getSafStorageProvider(): SafStorageProvider

    /**
     * 设置存储配额
     *
     * @param maxStorageBytes 最大存储字节数
     * @return 设置结果
     */
    suspend fun setStorageQuota(maxStorageBytes: Long): Result<Unit>

    /**
     * 启用/禁用加密
     *
     * @param enable 是否启用
     * @return 操作结果
     */
    suspend fun setEncryptionEnabled(enable: Boolean): Result<Unit>

    /**
     * 监听存储状态变化
     *
     * @return 存储状态流
     */
    fun observeStorageState(): Flow<StorageState>

    /**
     * 检查存储健康状态
     *
     * @return 健康检查结果
     */
    suspend fun checkStorageHealth(): Result<StorageHealthStatus>

    /**
     * 获取存储配置
     *
     * @return 存储配置
     */
    fun getConfig(): StorageConfig?

    /**
     * 检查是否已初始化
     *
     * @return 是否已初始化
     */
    fun isInitialized(): Boolean

    /**
     * 迁移存储
     *
     * @param targetPath 目标路径
     * @return 迁移结果
     */
    suspend fun migrateStorage(targetPath: String): Result<Unit>

    /**
     * 备份存储
     *
     * @param backupPath 备份路径
     * @return 备份结果
     */
    suspend fun backupStorage(backupPath: String): Result<Unit>

    /**
     * 恢复存储
     *
     * @param backupPath 备份路径
     * @return 恢复结果
     */
    suspend fun restoreStorage(backupPath: String): Result<Unit>

    /**
     * 释放资源
     */
    suspend fun release()
}

/**
 * 存储状态
 */
data class StorageState(
    val isInitialized: Boolean = false,
    val storageInfo: StorageInfo? = null,
    val isEncrypted: Boolean = false,
    val usagePercent: Int = 0,
    val needsCleanup: Boolean = false,
    val error: String? = null
) {
    val isReady: Boolean
        get() = isInitialized && error == null

    val isLowSpace: Boolean
        get() = usagePercent >= 80

    val isCriticalSpace: Boolean
        get() = usagePercent >= 95
}

/**
 * 存储健康状态
 */
data class StorageHealthStatus(
    val isHealthy: Boolean,
    val totalSpace: Long,
    val usedSpace: Long,
    val availableSpace: Long,
    val imageCount: Int,
    val snapshotCount: Int,
    val encryptedDataSize: Long,
    val issues: List<StorageIssue> = emptyList()
) {
    val usagePercent: Int
        get() = if (totalSpace > 0) ((usedSpace * 100) / totalSpace).toInt() else 0

    val hasIssues: Boolean
        get() = issues.isNotEmpty()
}

/**
 * 存储问题
 */
data class StorageIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val message: String,
    val suggestedAction: String? = null
)

/**
 * 问题类型枚举
 */
enum class IssueType {
    LOW_SPACE,
    CRITICAL_SPACE,
    ENCRYPTION_ERROR,
    CORRUPTED_IMAGE,
    ORPHANED_FILE,
    QUOTA_EXCEEDED,
    PERMISSION_DENIED,
    IO_ERROR
}

/**
 * 问题严重程度枚举
 */
enum class IssueSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}