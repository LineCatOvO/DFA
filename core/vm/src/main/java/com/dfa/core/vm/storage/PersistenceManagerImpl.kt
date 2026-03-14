package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.PersistenceResult
import com.dfa.core.vm.storage.models.PersistenceState
import com.dfa.core.vm.storage.models.SnapshotMetadata
import com.dfa.core.vm.storage.models.VmStateData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 持久化管理器实现
 *
 * 提供虚拟机状态和数据的持久化存储功能
 */
@Singleton
class PersistenceManagerImpl @Inject constructor(
    private val encryptionManager: EncryptionManager
) : PersistenceManager {

    private val mutex = Mutex()
    private var storagePath: String? = null
    private var isReady = false

    private val _persistenceState = MutableStateFlow(PersistenceState())
    private val stateFileCache = mutableMapOf<String, VmStateData>()
    private val snapshotCache = mutableMapOf<String, SnapshotMetadata>()
    private val dataCache = mutableMapOf<String, ByteArray>()

    override suspend fun initialize(storagePath: String): Result<Unit> = mutex.withLock {
        return try {
            val dir = File(storagePath)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            // 创建子目录
            File(dir, VM_STATE_DIR).mkdirs()
            File(dir, SNAPSHOTS_DIR).mkdirs()
            File(dir, DATA_DIR).mkdirs()

            this.storagePath = storagePath
            this.isReady = true

            _persistenceState.value = _persistenceState.value.copy(
                isInitialized = true
            )

            // 加载缓存
            loadCaches()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to initialize persistence manager: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun saveVmState(vmId: String, stateData: VmStateData): Result<PersistenceResult> = mutex.withLock {
        val startTime = System.currentTimeMillis()

        return try {
            if (!isReady) {
                return Result.failure(
                    StorageException.PersistenceException("Persistence manager not initialized")
                )
            }

            _persistenceState.value = _persistenceState.value.copy(isSaving = true)

            val path = storagePath!!
            val stateFile = File(path, "$VM_STATE_DIR/$vmId.dat")

            // 序列化状态数据
            val serialized = serializeVmState(stateData)

            // 加密（如果启用）
            val dataToSave = if (encryptionManager.isInitialized()) {
                val encrypted = encryptionManager.encryptData(serialized).getOrThrow()
                encrypted.toByteArray()
            } else {
                serialized
            }

            // 写入文件
            FileOutputStream(stateFile).use { output ->
                output.write(dataToSave)
            }

            // 更新缓存
            stateFileCache[vmId] = stateData

            val durationMs = System.currentTimeMillis() - startTime

            _persistenceState.value = _persistenceState.value.copy(
                isSaving = false,
                lastSaveTime = System.currentTimeMillis(),
                totalBytesStored = getStorageUsage()
            )

            Result.success(
                PersistenceResult.success(
                    bytesWritten = dataToSave.size.toLong(),
                    durationMs = durationMs
                )
            )
        } catch (e: Exception) {
            _persistenceState.value = _persistenceState.value.copy(
                isSaving = false,
                error = e.message
            )
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to save VM state: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun loadVmState(vmId: String): Result<VmStateData?> = mutex.withLock {
        return try {
            if (!isReady) {
                return Result.failure(
                    StorageException.PersistenceException("Persistence manager not initialized")
                )
            }

            _persistenceState.value = _persistenceState.value.copy(isLoading = true)

            // 先检查缓存
            stateFileCache[vmId]?.let {
                _persistenceState.value = _persistenceState.value.copy(
                    isLoading = false,
                    lastLoadTime = System.currentTimeMillis()
                )
                return Result.success(it)
            }

            val path = storagePath!!
            val stateFile = File(path, "$VM_STATE_DIR/$vmId.dat")

            if (!stateFile.exists()) {
                _persistenceState.value = _persistenceState.value.copy(isLoading = false)
                return Result.success(null)
            }

            // 读取文件
            val data = FileInputStream(stateFile).use { input ->
                input.readBytes()
            }

            // 解密（如果启用）
            val decrypted = if (encryptionManager.isInitialized()) {
                encryptionManager.decryptData(data).getOrThrow().data!!
            } else {
                data
            }

            // 反序列化
            val stateData = deserializeVmState(decrypted)

            // 更新缓存
            stateFileCache[vmId] = stateData

            _persistenceState.value = _persistenceState.value.copy(
                isLoading = false,
                lastLoadTime = System.currentTimeMillis()
            )

            Result.success(stateData)
        } catch (e: Exception) {
            _persistenceState.value = _persistenceState.value.copy(
                isLoading = false,
                error = e.message
            )
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to load VM state: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun deleteVmState(vmId: String): Result<Unit> = mutex.withLock {
        return try {
            val path = storagePath ?: return Result.failure(
                StorageException.PersistenceException("Persistence manager not initialized")
            )

            val stateFile = File(path, "$VM_STATE_DIR/$vmId.dat")
            if (stateFile.exists()) {
                stateFile.delete()
            }

            stateFileCache.remove(vmId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to delete VM state: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun hasVmState(vmId: String): Boolean {
        return stateFileCache.containsKey(vmId) || run {
            val path = storagePath ?: return false
            File(path, "$VM_STATE_DIR/$vmId.dat").exists()
        }
    }

    override suspend fun createSnapshot(
        vmId: String,
        snapshotName: String,
        description: String?
    ): Result<SnapshotMetadata> = mutex.withLock {
        return try {
            if (!isReady) {
                return Result.failure(
                    StorageException.PersistenceException("Persistence manager not initialized")
                )
            }

            // 加载当前状态
            val stateData = loadVmState(vmId).getOrNull()
                ?: return Result.failure(
                    StorageException.PersistenceException("VM state not found: $vmId")
                )

            val snapshotId = UUID.randomUUID().toString()
            val path = storagePath!!
            val snapshotFile = File(path, "$SNAPSHOTS_DIR/$snapshotId.dat")

            // 序列化并保存
            val serialized = serializeVmState(stateData)

            // 加密（如果启用）
            val dataToSave = if (encryptionManager.isInitialized()) {
                val encrypted = encryptionManager.encryptData(serialized).getOrThrow()
                encrypted.toByteArray()
            } else {
                serialized
            }

            FileOutputStream(snapshotFile).use { output ->
                output.write(dataToSave)
            }

            // 计算校验和
            val checksum = calculateChecksum(dataToSave)

            val metadata = SnapshotMetadata(
                id = snapshotId,
                vmId = vmId,
                name = snapshotName,
                description = description,
                sizeBytes = dataToSave.size.toLong(),
                isEncrypted = encryptionManager.isInitialized(),
                checksum = checksum
            )

            // 保存元数据
            val metadataFile = File(path, "$SNAPSHOTS_DIR/$snapshotId.meta")
            FileOutputStream(metadataFile).use { output ->
                output.write(serializeMetadata(metadata))
            }

            snapshotCache[snapshotId] = metadata

            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to create snapshot: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun restoreSnapshot(snapshotId: String): Result<VmStateData> = mutex.withLock {
        return try {
            if (!isReady) {
                return Result.failure(
                    StorageException.PersistenceException("Persistence manager not initialized")
                )
            }

            val path = storagePath!!
            val snapshotFile = File(path, "$SNAPSHOTS_DIR/$snapshotId.dat")

            if (!snapshotFile.exists()) {
                return Result.failure(
                    StorageException.PersistenceException("Snapshot not found: $snapshotId")
                )
            }

            // 读取快照数据
            val data = FileInputStream(snapshotFile).use { input ->
                input.readBytes()
            }

            // 解密（如果启用）
            val decrypted = if (encryptionManager.isInitialized()) {
                encryptionManager.decryptData(data).getOrThrow().data!!
            } else {
                data
            }

            // 反序列化
            val stateData = deserializeVmState(decrypted)

            Result.success(stateData)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to restore snapshot: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun deleteSnapshot(snapshotId: String): Result<Unit> = mutex.withLock {
        return try {
            val path = storagePath ?: return Result.failure(
                StorageException.PersistenceException("Persistence manager not initialized")
            )

            val snapshotFile = File(path, "$SNAPSHOTS_DIR/$snapshotId.dat")
            val metadataFile = File(path, "$SNAPSHOTS_DIR/$snapshotId.meta")

            if (snapshotFile.exists()) snapshotFile.delete()
            if (metadataFile.exists()) metadataFile.delete()

            snapshotCache.remove(snapshotId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to delete snapshot: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun listSnapshots(vmId: String): Result<List<SnapshotMetadata>> {
        val snapshots = snapshotCache.values.filter { it.vmId == vmId }
        return Result.success(snapshots)
    }

    override suspend fun getSnapshotMetadata(snapshotId: String): Result<SnapshotMetadata?> {
        return Result.success(snapshotCache[snapshotId])
    }

    override suspend fun saveData(key: String, value: ByteArray): Result<Unit> = mutex.withLock {
        return try {
            if (!isReady) {
                return Result.failure(
                    StorageException.PersistenceException("Persistence manager not initialized")
                )
            }

            val path = storagePath!!
            val dataFile = File(path, "$DATA_DIR/$key.dat")

            FileOutputStream(dataFile).use { output ->
                output.write(value)
            }

            dataCache[key] = value

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to save data: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun loadData(key: String): Result<ByteArray?> = mutex.withLock {
        return try {
            // 先检查缓存
            dataCache[key]?.let { return Result.success(it) }

            val path = storagePath ?: return Result.failure(
                StorageException.PersistenceException("Persistence manager not initialized")
            )

            val dataFile = File(path, "$DATA_DIR/$key.dat")

            if (!dataFile.exists()) {
                return Result.success(null)
            }

            val data = FileInputStream(dataFile).use { input ->
                input.readBytes()
            }

            dataCache[key] = data

            Result.success(data)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to load data: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun deleteData(key: String): Result<Unit> = mutex.withLock {
        return try {
            val path = storagePath ?: return Result.failure(
                StorageException.PersistenceException("Persistence manager not initialized")
            )

            val dataFile = File(path, "$DATA_DIR/$key.dat")
            if (dataFile.exists()) {
                dataFile.delete()
            }

            dataCache.remove(key)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to delete data: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun hasData(key: String): Boolean {
        return dataCache.containsKey(key) || run {
            val path = storagePath ?: return false
            File(path, "$DATA_DIR/$key.dat").exists()
        }
    }

    override suspend fun clearAll(): Result<Unit> = mutex.withLock {
        return try {
            val path = storagePath ?: return Result.failure(
                StorageException.PersistenceException("Persistence manager not initialized")
            )

            File(path, VM_STATE_DIR).deleteRecursively()
            File(path, SNAPSHOTS_DIR).deleteRecursively()
            File(path, DATA_DIR).deleteRecursively()

            // 重新创建目录
            File(path, VM_STATE_DIR).mkdirs()
            File(path, SNAPSHOTS_DIR).mkdirs()
            File(path, DATA_DIR).mkdirs()

            stateFileCache.clear()
            snapshotCache.clear()
            dataCache.clear()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to clear all: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun getStorageUsage(): Long = withContext(Dispatchers.IO) {
        val path = storagePath ?: return@withContext 0L

        var totalSize = 0L

        File(path, VM_STATE_DIR).walkTopDown().forEach { file ->
            if (file.isFile) totalSize += file.length()
        }

        File(path, SNAPSHOTS_DIR).walkTopDown().forEach { file ->
            if (file.isFile) totalSize += file.length()
        }

        File(path, DATA_DIR).walkTopDown().forEach { file ->
            if (file.isFile) totalSize += file.length()
        }

        totalSize
    }

    override fun getPersistenceState(): Flow<PersistenceState> = _persistenceState.asStateFlow()

    override fun isInitialized(): Boolean = isReady

    override suspend fun exportData(targetPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val path = storagePath ?: return@withContext Result.failure(
                StorageException.PersistenceException("Persistence manager not initialized")
            )

            val targetFile = File(targetPath)
            targetFile.parentFile?.mkdirs()

            ZipOutputStream(FileOutputStream(targetFile)).use { zip ->
                // 导出VM状态
                File(path, VM_STATE_DIR).walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val entry = ZipEntry("$VM_STATE_DIR/${file.name}")
                        zip.putNextEntry(entry)
                        FileInputStream(file).use { input ->
                            input.copyTo(zip)
                        }
                        zip.closeEntry()
                    }
                }

                // 导出快照
                File(path, SNAPSHOTS_DIR).walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val entry = ZipEntry("$SNAPSHOTS_DIR/${file.name}")
                        zip.putNextEntry(entry)
                        FileInputStream(file).use { input ->
                            input.copyTo(zip)
                        }
                        zip.closeEntry()
                    }
                }

                // 导出数据
                File(path, DATA_DIR).walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val entry = ZipEntry("$DATA_DIR/${file.name}")
                        zip.putNextEntry(entry)
                        FileInputStream(file).use { input ->
                            input.copyTo(zip)
                        }
                        zip.closeEntry()
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to export data: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun importData(sourcePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val path = storagePath ?: return@withContext Result.failure(
                StorageException.PersistenceException("Persistence manager not initialized")
            )

            ZipInputStream(FileInputStream(sourcePath)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val file = File(path, entry.name)
                    file.parentFile?.mkdirs()

                    FileOutputStream(file).use { output ->
                        zip.copyTo(output)
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // 重新加载缓存
            loadCaches()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to import data: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun compact(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 简化实现：删除临时文件和无效快照
            val path = storagePath ?: return@withContext Result.failure(
                StorageException.PersistenceException("Persistence manager not initialized")
            )

            // 清理临时文件
            File(path).walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "tmp") {
                    file.delete()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to compact: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun verifyIntegrity(): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val path = storagePath ?: return@withContext Result.failure(
                StorageException.PersistenceException("Persistence manager not initialized")
            )

            // 验证快照校验和
            for ((_, metadata) in snapshotCache) {
                val snapshotFile = File(path, "$SNAPSHOTS_DIR/${metadata.id}.dat")
                if (snapshotFile.exists() && metadata.checksum != null) {
                    val data = FileInputStream(snapshotFile).use { it.readBytes() }
                    val actualChecksum = calculateChecksum(data)
                    if (actualChecksum != metadata.checksum) {
                        return@withContext Result.success(false)
                    }
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to verify integrity: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun release() = mutex.withLock {
        storagePath = null
        isReady = false
        stateFileCache.clear()
        snapshotCache.clear()
        dataCache.clear()

        _persistenceState.value = PersistenceState()
    }

    // 私有方法

    private suspend fun loadCaches() = withContext(Dispatchers.IO) {
        val path = storagePath ?: return@withContext

        // 加载快照元数据
        File(path, SNAPSHOTS_DIR).walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "meta") {
                try {
                    val data = FileInputStream(file).use { it.readBytes() }
                    val metadata = deserializeMetadata(data)
                    snapshotCache[metadata.id] = metadata
                } catch (e: Exception) {
                    // 忽略无效的元数据文件
                }
            }
        }
    }

    private fun serializeVmState(stateData: VmStateData): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val dos = java.io.DataOutputStream(output)

        dos.writeUTF(stateData.vmId)
        dos.writeUTF(stateData.configJson)
        dos.writeLong(stateData.timestamp)

        // 内存状态
        stateData.memoryState?.let {
            dos.writeInt(it.size)
            dos.write(it)
        } ?: dos.writeInt(0)

        // CPU状态
        stateData.cpuState?.let {
            dos.writeInt(it.size)
            dos.write(it)
        } ?: dos.writeInt(0)

        // 设备状态
        dos.writeInt(stateData.deviceStates.size)
        stateData.deviceStates.forEach { (key, value) ->
            dos.writeUTF(key)
            dos.writeInt(value.size)
            dos.write(value)
        }

        // 自定义数据
        dos.writeInt(stateData.customData.size)
        stateData.customData.forEach { (key, value) ->
            dos.writeUTF(key)
            dos.writeInt(value.size)
            dos.write(value)
        }

        return output.toByteArray()
    }

    private fun deserializeVmState(data: ByteArray): VmStateData {
        val input = java.io.ByteArrayInputStream(data)
        val dis = java.io.DataInputStream(input)

        val vmId = dis.readUTF()
        val configJson = dis.readUTF()
        val timestamp = dis.readLong()

        // 内存状态
        val memoryStateSize = dis.readInt()
        val memoryState = if (memoryStateSize > 0) {
            ByteArray(memoryStateSize).also { dis.readFully(it) }
        } else null

        // CPU状态
        val cpuStateSize = dis.readInt()
        val cpuState = if (cpuStateSize > 0) {
            ByteArray(cpuStateSize).also { dis.readFully(it) }
        } else null

        // 设备状态
        val deviceStatesCount = dis.readInt()
        val deviceStates = mutableMapOf<String, ByteArray>()
        repeat(deviceStatesCount) {
            val key = dis.readUTF()
            val size = dis.readInt()
            val value = ByteArray(size).also { dis.readFully(it) }
            deviceStates[key] = value
        }

        // 自定义数据
        val customDataCount = dis.readInt()
        val customData = mutableMapOf<String, ByteArray>()
        repeat(customDataCount) {
            val key = dis.readUTF()
            val size = dis.readInt()
            val value = ByteArray(size).also { dis.readFully(it) }
            customData[key] = value
        }

        return VmStateData(
            vmId = vmId,
            configJson = configJson,
            memoryState = memoryState,
            cpuState = cpuState,
            deviceStates = deviceStates,
            customData = customData,
            timestamp = timestamp
        )
    }

    private fun serializeMetadata(metadata: SnapshotMetadata): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val dos = java.io.DataOutputStream(output)

        dos.writeUTF(metadata.id)
        dos.writeUTF(metadata.vmId)
        dos.writeUTF(metadata.name)
        dos.writeUTF(metadata.description ?: "")
        dos.writeLong(metadata.sizeBytes)
        dos.writeLong(metadata.createdAt)
        dos.writeBoolean(metadata.isEncrypted)
        dos.writeUTF(metadata.checksum ?: "")

        return output.toByteArray()
    }

    private fun deserializeMetadata(data: ByteArray): SnapshotMetadata {
        val input = java.io.ByteArrayInputStream(data)
        val dis = java.io.DataInputStream(input)

        return SnapshotMetadata(
            id = dis.readUTF(),
            vmId = dis.readUTF(),
            name = dis.readUTF(),
            description = dis.readUTF().takeIf { it.isNotEmpty() },
            sizeBytes = dis.readLong(),
            createdAt = dis.readLong(),
            isEncrypted = dis.readBoolean(),
            checksum = dis.readUTF().takeIf { it.isNotEmpty() }
        )
    }

    private fun calculateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val VM_STATE_DIR = "vm_states"
        private const val SNAPSHOTS_DIR = "snapshots"
        private const val DATA_DIR = "data"
    }
}