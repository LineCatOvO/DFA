package com.dfa.core.vm.storage

import kotlinx.coroutines.flow.Flow

/**
 * 持久化管理器接口
 *
 * 提供虚拟机状态和数据的持久化存储功能
 */
interface PersistenceManager {

    /**
     * 初始化持久化管理器
     *
     * @param storagePath 存储路径
     * @return 初始化结果
     */
    suspend fun initialize(storagePath: String): Result<Unit>

    /**
     * 保存虚拟机状态
     *
     * @param vmId 虚拟机ID
     * @param stateData 状态数据
     * @return 保存结果
     */
    suspend fun saveVmState(vmId: String, stateData: VmStateData): Result<PersistenceResult>

    /**
     * 加载虚拟机状态
     *
     * @param vmId 虚拟机ID
     * @return 状态数据
     */
    suspend fun loadVmState(vmId: String): Result<VmStateData?>

    /**
     * 删除虚拟机状态
     *
     * @param vmId 虚拟机ID
     * @return 删除结果
     */
    suspend fun deleteVmState(vmId: String): Result<Unit>

    /**
     * 检查虚拟机状态是否存在
     *
     * @param vmId 虚拟机ID
     * @return 是否存在
     */
    suspend fun hasVmState(vmId: String): Boolean

    /**
     * 创建快照
     *
     * @param vmId 虚拟机ID
     * @param snapshotName 快照名称
     * @param description 描述
     * @return 快照元数据
     */
    suspend fun createSnapshot(
        vmId: String,
        snapshotName: String,
        description: String? = null
    ): Result<SnapshotMetadata>

    /**
     * 恢复快照
     *
     * @param snapshotId 快照ID
     * @return 恢复的状态数据
     */
    suspend fun restoreSnapshot(snapshotId: String): Result<VmStateData>

    /**
     * 删除快照
     *
     * @param snapshotId 快照ID
     * @return 删除结果
     */
    suspend fun deleteSnapshot(snapshotId: String): Result<Unit>

    /**
     * 列出虚拟机的所有快照
     *
     * @param vmId 虚拟机ID
     * @return 快照列表
     */
    suspend fun listSnapshots(vmId: String): Result<List<SnapshotMetadata>>

    /**
     * 获取快照元数据
     *
     * @param snapshotId 快照ID
     * @return 快照元数据
     */
    suspend fun getSnapshotMetadata(snapshotId: String): Result<SnapshotMetadata?>

    /**
     * 保存键值对数据
     *
     * @param key 键
     * @param value 值
     * @return 保存结果
     */
    suspend fun saveData(key: String, value: ByteArray): Result<Unit>

    /**
     * 加载键值对数据
     *
     * @param key 键
     * @return 值
     */
    suspend fun loadData(key: String): Result<ByteArray?>

    /**
     * 删除键值对数据
     *
     * @param key 键
     * @return 删除结果
     */
    suspend fun deleteData(key: String): Result<Unit>

    /**
     * 检查数据是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    suspend fun hasData(key: String): Boolean

    /**
     * 清除所有数据
     *
     * @return 清除结果
     */
    suspend fun clearAll(): Result<Unit>

    /**
     * 获取存储使用情况
     *
     * @return 使用字节数
     */
    suspend fun getStorageUsage(): Long

    /**
     * 获取持久化状态
     *
     * @return 状态流
     */
    fun getPersistenceState(): Flow<PersistenceState>

    /**
     * 检查是否已初始化
     *
     * @return 是否已初始化
     */
    fun isInitialized(): Boolean

    /**
     * 导出数据
     *
     * @param targetPath 目标路径
     * @return 导出结果
     */
    suspend fun exportData(targetPath: String): Result<Unit>

    /**
     * 导入数据
     *
     * @param sourcePath 源路径
     * @return 导入结果
     */
    suspend fun importData(sourcePath: String): Result<Unit>

    /**
     * 压缩存储
     *
     * @return 压缩结果
     */
    suspend fun compact(): Result<Unit>

    /**
     * 验证数据完整性
     *
     * @return 验证结果
     */
    suspend fun verifyIntegrity(): Result<Boolean>

    /**
     * 释放资源
     */
    suspend fun release()
}

/**
 * 持久化状态
 */
data class PersistenceState(
    val isInitialized: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val lastSaveTime: Long? = null,
    val lastLoadTime: Long? = null,
    val totalBytesStored: Long = 0,
    val error: String? = null
) {
    val isReady: Boolean
        get() = isInitialized && !isSaving && !isLoading
}

/**
 * 持久化结果
 */
data class PersistenceResult(
    val success: Boolean,
    val bytesWritten: Long = 0,
    val durationMs: Long = 0,
    val errorMessage: String? = null
) {
    companion object {
        fun success(bytesWritten: Long, durationMs: Long): PersistenceResult {
            return PersistenceResult(
                success = true,
                bytesWritten = bytesWritten,
                durationMs = durationMs
            )
        }

        fun failure(error: String): PersistenceResult {
            return PersistenceResult(
                success = false,
                errorMessage = error
            )
        }
    }
}

/**
 * 虚拟机状态数据
 *
 * @property vmId 虚拟机ID
 * @property configJson 配置JSON
 * @property memoryState 内存状态（可选）
 * @property cpuState CPU状态（可选）
 * @property deviceStates 设备状态
 * @property customData 自定义数据
 * @property timestamp 时间戳
 */
data class VmStateData(
    val vmId: String,
    val configJson: String,
    val memoryState: ByteArray? = null,
    val cpuState: ByteArray? = null,
    val deviceStates: Map<String, ByteArray> = emptyMap(),
    val customData: Map<String, ByteArray> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 总大小
     */
    val totalSize: Long
        get() {
            var size = configJson.toByteArray().size.toLong()
            memoryState?.let { size += it.size }
            cpuState?.let { size += it.size }
            deviceStates.values.forEach { size += it.size }
            customData.values.forEach { size += it.size }
            return size
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VmStateData

        if (vmId != other.vmId) return false
        if (configJson != other.configJson) return false
        if (memoryState != null) {
            if (other.memoryState == null) return false
            if (!memoryState.contentEquals(other.memoryState)) return false
        } else if (other.memoryState != null) return false
        if (cpuState != null) {
            if (other.cpuState == null) return false
            if (!cpuState.contentEquals(other.cpuState)) return false
        } else if (other.cpuState != null) return false
        if (deviceStates != other.deviceStates) return false
        if (customData != other.customData) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vmId.hashCode()
        result = 31 * result + configJson.hashCode()
        result = 31 * result + (memoryState?.contentHashCode() ?: 0)
        result = 31 * result + (cpuState?.contentHashCode() ?: 0)
        result = 31 * result + deviceStates.hashCode()
        result = 31 * result + customData.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * 快照元数据
 *
 * @property id 快照ID
 * @property vmId 虚拟机ID
 * @property name 快照名称
 * @property description 描述
 * @property sizeBytes 大小
 * @property createdAt 创建时间
 * @property isEncrypted 是否加密
 * @property checksum 校验和
 */
data class SnapshotMetadata(
    val id: String,
    val vmId: String,
    val name: String,
    val description: String? = null,
    val sizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val checksum: String? = null
) {
    /**
     * 格式化创建时间
     */
    fun getFormattedCreatedAt(): String {
        val date = java.util.Date(createdAt)
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(date)
    }
}