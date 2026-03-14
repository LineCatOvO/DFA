package com.dfa.core.vm.storage.models

/**
 * 存储状态枚举
 */
enum class StorageState {
    /** 初始化中 */
    INITIALIZING,
    /** 就绪 */
    READY,
    /** 错误 */
    ERROR,
    /** 已释放 */
    RELEASED
}

/**
 * 存储类型枚举
 */
enum class StorageType {
    /** 内部存储 */
    INTERNAL,
    /** 外部存储 */
    EXTERNAL,
    /** SAF存储 */
    SAF,
    /** 加密存储 */
    ENCRYPTED
}

/**
 * 存储配置
 *
 * @property storagePath 存储路径
 * @property maxStorageBytes 最大存储字节数
 * @property enableEncryption 是否启用加密
 * @property encryptionKeyAlias 加密密钥别名
 * @property autoCleanup 是否自动清理
 * @property cleanupThreshold 清理阈值（百分比）
 */
data class StorageConfig(
    val storagePath: String,
    val maxStorageBytes: Long = 10L * 1024 * 1024 * 1024, // 10GB
    val enableEncryption: Boolean = false,
    val encryptionKeyAlias: String? = null,
    val autoCleanup: Boolean = true,
    val cleanupThreshold: Int = 80 // 80%
) {
    fun validate(): Boolean {
        return storagePath.isNotEmpty() &&
                maxStorageBytes > 0 &&
                cleanupThreshold in 0..100 &&
                (!enableEncryption || !encryptionKeyAlias.isNullOrEmpty())
    }
}

/**
 * 存储信息
 *
 * @property path 存储路径
 * @property type 存储类型
 * @property totalBytes 总字节数
 * @property usedBytes 已使用字节数
 * @property availableBytes 可用字节数
 * @property state 存储状态
 * @property isEncrypted 是否加密
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
data class StorageInfo(
    val path: String,
    val type: StorageType,
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val state: StorageState = StorageState.READY,
    val isEncrypted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 使用百分比（0-100）
     */
    val usagePercent: Int
        get() = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    /**
     * 是否有足够空间
     */
    fun hasEnoughSpace(requiredBytes: Long): Boolean = availableBytes >= requiredBytes

    /**
     * 是否需要清理
     */
    fun needsCleanup(threshold: Int = 80): Boolean = usagePercent >= threshold

    /**
     * 是否就绪
     */
    val isReady: Boolean
        get() = state == StorageState.READY
}

/**
 * 存储操作结果
 *
 * @property success 是否成功
 * @property bytesProcessed 处理的字节数
 * @property message 消息
 * @property error 错误信息
 */
data class StorageOperationResult(
    val success: Boolean,
    val bytesProcessed: Long = 0,
    val message: String? = null,
    val error: String? = null
) {
    companion object {
        fun success(bytesProcessed: Long = 0, message: String? = null): StorageOperationResult {
            return StorageOperationResult(
                success = true,
                bytesProcessed = bytesProcessed,
                message = message
            )
        }

        fun failure(error: String, bytesProcessed: Long = 0): StorageOperationResult {
            return StorageOperationResult(
                success = false,
                bytesProcessed = bytesProcessed,
                error = error
            )
        }
    }
}

/**
 * 存储统计信息
 *
 * @property totalImages 镜像总数
 * @property totalImageBytes 镜像总大小
 * @property totalSnapshots 快照总数
 * @property totalSnapshotBytes 快照总大小
 * @property encryptedDataBytes 加密数据大小
 * @property lastCleanupTime 最后清理时间
 */
data class StorageStatistics(
    val totalImages: Int = 0,
    val totalImageBytes: Long = 0,
    val totalSnapshots: Int = 0,
    val totalSnapshotBytes: Long = 0,
    val encryptedDataBytes: Long = 0,
    val lastCleanupTime: Long? = null
) {
    /**
     * 总存储使用量
     */
    val totalUsedBytes: Long
        get() = totalImageBytes + totalSnapshotBytes + encryptedDataBytes
}

/**
 * 清理选项
 *
 * @property cleanUnusedImages 清理未使用的镜像
 * @property cleanOldSnapshots 清理旧快照
 * @property cleanTempFiles 清理临时文件
 * @property keepRecentSnapshots 保留最近N个快照
 * @property dryRun 仅模拟，不实际执行
 */
data class CleanupOptions(
    val cleanUnusedImages: Boolean = true,
    val cleanOldSnapshots: Boolean = true,
    val cleanTempFiles: Boolean = true,
    val keepRecentSnapshots: Int = 3,
    val dryRun: Boolean = false
)

/**
 * 清理结果
 *
 * @property imagesCleaned 清理的镜像数
 * @property snapshotsCleaned 清理的快照数
 * @property tempFilesCleaned 清理的临时文件数
 * @property bytesReclaimed 回收的字节数
 * @property errors 错误列表
 */
data class CleanupResult(
    val imagesCleaned: Int = 0,
    val snapshotsCleaned: Int = 0,
    val tempFilesCleaned: Int = 0,
    val bytesReclaimed: Long = 0,
    val errors: List<String> = emptyList()
) {
    val isSuccess: Boolean
        get() = errors.isEmpty()

    val totalItemsCleaned: Int
        get() = imagesCleaned + snapshotsCleaned + tempFilesCleaned
}