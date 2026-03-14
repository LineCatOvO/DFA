package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.image.ImageFormatDetector
import com.dfa.core.vm.storage.image.Qcow2Handler
import com.dfa.core.vm.storage.image.RawImageHandler
import com.dfa.core.vm.storage.models.ConvertImageProgress
import com.dfa.core.vm.storage.models.CreateDiskImageRequest
import com.dfa.core.vm.storage.models.CreateDiskImageResult
import com.dfa.core.vm.storage.models.CreateSnapshotRequest
import com.dfa.core.vm.storage.models.DiskImageFormat
import com.dfa.core.vm.storage.models.DiskImageInfo
import com.dfa.core.vm.storage.models.DiskImageState
import com.dfa.core.vm.storage.models.ImageValidationResult
import com.dfa.core.vm.storage.models.SnapshotInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 磁盘镜像管理器实现
 *
 * 协调各种镜像处理器，提供统一的镜像管理接口
 */
@Singleton
class DiskImageManagerImpl @Inject constructor(
    private val qcow2Handler: Qcow2Handler,
    private val rawImageHandler: RawImageHandler,
    private val imageFormatDetector: ImageFormatDetector,
    private val storageConfig: StorageConfigProvider
) : DiskImageManager {

    private val mutex = Mutex()
    private val imageCache = mutableMapOf<String, DiskImageInfo>()
    private val snapshotCache = mutableMapOf<String, MutableList<SnapshotInfo>>()

    override suspend fun createImage(request: CreateDiskImageRequest): CreateDiskImageResult {
        if (!request.validate()) {
            return CreateDiskImageResult.failure("Invalid image creation request")
        }

        return mutex.withLock {
            try {
                val storagePath = request.path ?: storageConfig.getDefaultImagePath()
                val fileName = "${request.name}.${getExtension(request.format)}"
                val fullPath = File(storagePath, fileName).absolutePath

                // 检查文件是否已存在
                if (File(fullPath).exists()) {
                    return@withLock CreateDiskImageResult.failure("Image file already exists: $fullPath")
                }

                val result = when (request.format) {
                    DiskImageFormat.QCOW2 -> qcow2Handler.createImage(
                        path = fullPath,
                        virtualSizeBytes = request.sizeBytes,
                        preallocate = request.preallocate
                    )
                    DiskImageFormat.RAW -> rawImageHandler.createImage(
                        path = fullPath,
                        sizeBytes = request.sizeBytes,
                        preallocate = request.preallocate
                    )
                    else -> return@withLock CreateDiskImageResult.failure(
                        "Unsupported image format: ${request.format}"
                    )
                }

                if (result.isFailure) {
                    return@withLock CreateDiskImageResult.failure(
                        result.exceptionOrNull()?.message ?: "Failed to create image"
                    )
                }

                val imageInfo = result.getOrThrow()
                imageCache[imageInfo.id] = imageInfo

                CreateDiskImageResult.success(imageInfo)
            } catch (e: Exception) {
                CreateDiskImageResult.failure("Failed to create image: ${e.message}")
            }
        }
    }

    override suspend fun deleteImage(imageId: String): Result<Unit> = mutex.withLock {
        return try {
            val imageInfo = imageCache[imageId]
                ?: return Result.failure(
                    StorageException.DiskImageException("Image not found: $imageId")
                )

            if (imageInfo.state == DiskImageState.IN_USE) {
                return Result.failure(
                    StorageException.DiskImageException("Cannot delete image in use: $imageId")
                )
            }

            val file = File(imageInfo.path)
            if (file.exists()) {
                file.delete()
            }

            imageCache.remove(imageId)
            snapshotCache.remove(imageId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to delete image: ${e.message}",
                    imageId
                )
            )
        }
    }

    override suspend fun getImageInfo(imageId: String): Result<DiskImageInfo> {
        return imageCache[imageId]?.let {
            Result.success(it)
        } ?: Result.failure(
            StorageException.DiskImageException("Image not found: $imageId")
        )
    }

    override suspend fun getImageInfoByPath(path: String): Result<DiskImageInfo> {
        // 先在缓存中查找
        val cachedImage = imageCache.values.find { it.path == path }
        if (cachedImage != null) {
            return Result.success(cachedImage)
        }

        // 从文件系统读取
        return try {
            val format = imageFormatDetector.detectFormat(path).getOrNull()
                ?: return Result.failure(
                    StorageException.ImageFormatException("Cannot detect image format")
                )

            val result = when (format) {
                DiskImageFormat.QCOW2 -> qcow2Handler.readImageInfo(path)
                DiskImageFormat.RAW -> rawImageHandler.readImageInfo(path)
                else -> return Result.failure(
                    StorageException.ImageFormatException("Unsupported format: $format")
                )
            }

            if (result.isSuccess) {
                val imageInfo = result.getOrThrow()
                imageCache[imageInfo.id] = imageInfo
                Result.success(imageInfo)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to get image info: ${e.message}",
                    path
                )
            )
        }
    }

    override suspend fun listImages(): Result<List<DiskImageInfo>> {
        return Result.success(imageCache.values.toList())
    }

    override suspend fun listImagesByVm(vmId: String): Result<List<DiskImageInfo>> {
        val images = imageCache.values.filter { it.vmId == vmId }
        return Result.success(images)
    }

    override suspend fun validateImage(imageId: String): Result<ImageValidationResult> {
        val imageInfo = imageCache[imageId]
            ?: return Result.failure(
                StorageException.DiskImageException("Image not found: $imageId")
            )

        val errors = mutableListOf<String>()

        // 检查文件是否存在
        val file = File(imageInfo.path)
        if (!file.exists()) {
            errors.add("Image file does not exist: ${imageInfo.path}")
        }

        // 验证格式
        val formatResult = imageFormatDetector.detectFormat(imageInfo.path)
        if (formatResult.isFailure || formatResult.getOrNull() != imageInfo.format) {
            errors.add("Image format mismatch")
        }

        // 使用对应的处理器验证
        val handlerResult = when (imageInfo.format) {
            DiskImageFormat.QCOW2 -> qcow2Handler.validateImage(imageInfo.path)
            DiskImageFormat.RAW -> rawImageHandler.validateImage(imageInfo.path)
            else -> Result.success(true)
        }

        if (handlerResult.isFailure || handlerResult.getOrNull() != true) {
            errors.add("Image validation failed")
        }

        return if (errors.isEmpty()) {
            Result.success(ImageValidationResult.valid(imageId))
        } else {
            Result.success(ImageValidationResult.invalid(imageId, errors))
        }
    }

    override suspend fun resizeImage(imageId: String, newSizeBytes: Long): Result<DiskImageInfo> =
        mutex.withLock {
            val imageInfo = imageCache[imageId]
                ?: return Result.failure(
                    StorageException.DiskImageException("Image not found: $imageId")
                )

            if (imageInfo.state == DiskImageState.IN_USE) {
                return Result.failure(
                    StorageException.DiskImageException("Cannot resize image in use")
                )
            }

            val result = when (imageInfo.format) {
                DiskImageFormat.QCOW2 -> qcow2Handler.resizeImage(imageInfo.path, newSizeBytes)
                DiskImageFormat.RAW -> rawImageHandler.resizeImage(imageInfo.path, newSizeBytes)
                else -> return Result.failure(
                    StorageException.ImageFormatException("Unsupported format for resize")
                )
            }

            if (result.isSuccess) {
                val updatedInfo = result.getOrThrow()
                imageCache[imageId] = updatedInfo
                Result.success(updatedInfo)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Resize failed"))
            }
        }

    override fun convertImage(
        sourcePath: String,
        targetPath: String,
        targetFormat: DiskImageFormat
    ): Flow<ConvertImageProgress> = flow {
        emit(ConvertImageProgress(sourcePath, targetPath, state = ConvertImageProgress.ConvertState.PENDING))

        try {
            // 读取源镜像信息
            val sourceFormat = imageFormatDetector.detectFormat(sourcePath).getOrNull()
            if (sourceFormat == null || sourceFormat == DiskImageFormat.UNKNOWN) {
                emit(
                    ConvertImageProgress(
                        sourcePath,
                        targetPath,
                        state = ConvertImageProgress.ConvertState.FAILED
                    )
                )
                return@flow
            }

            val sourceFile = File(sourcePath)
            val totalBytes = sourceFile.length()

            emit(
                ConvertImageProgress(
                    sourcePath,
                    targetPath,
                    state = ConvertImageProgress.ConvertState.IN_PROGRESS
                )
            )

            // 简化实现：直接复制文件（实际应使用qemu-img等工具）
            if (targetFormat == DiskImageFormat.RAW) {
                val result = rawImageHandler.copyImage(sourcePath, targetPath)
                if (result.isSuccess) {
                    emit(
                        ConvertImageProgress(
                            sourcePath,
                            targetPath,
                            progress = 100,
                            bytesProcessed = totalBytes,
                            totalBytes = totalBytes,
                            state = ConvertImageProgress.ConvertState.COMPLETED
                        )
                    )
                } else {
                    emit(
                        ConvertImageProgress(
                            sourcePath,
                            targetPath,
                            state = ConvertImageProgress.ConvertState.FAILED
                        )
                    )
                }
            } else {
                // 其他格式暂不支持
                emit(
                    ConvertImageProgress(
                        sourcePath,
                        targetPath,
                        state = ConvertImageProgress.ConvertState.FAILED
                    )
                )
            }
        } catch (e: Exception) {
            emit(
                ConvertImageProgress(
                    sourcePath,
                    targetPath,
                    state = ConvertImageProgress.ConvertState.FAILED
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun createSnapshot(request: CreateSnapshotRequest): Result<SnapshotInfo> =
        mutex.withLock {
            if (!request.validate()) {
                return Result.failure(
                    StorageException.DiskImageException("Invalid snapshot request")
                )
            }

            val imageInfo = imageCache[request.imageId]
                ?: return Result.failure(
                    StorageException.DiskImageException("Image not found: ${request.imageId}")
                )

            // 检查格式是否支持快照
            if (!imageFormatDetector.supportsSnapshots(imageInfo.format)) {
                return Result.failure(
                    StorageException.DiskImageException("Format does not support snapshots")
                )
            }

            val snapshotId = UUID.randomUUID().toString()
            val snapshotPath = "${imageInfo.path}.snapshot.$snapshotId"

            val snapshot = SnapshotInfo(
                id = snapshotId,
                imageId = request.imageId,
                name = request.name,
                path = snapshotPath,
                sizeBytes = imageInfo.actualSizeBytes,
                description = request.description
            )

            snapshotCache.getOrPut(request.imageId) { mutableListOf() }.add(snapshot)

            Result.success(snapshot)
        }

    override suspend fun deleteSnapshot(snapshotId: String): Result<Unit> = mutex.withLock {
        for ((imageId, snapshots) in snapshotCache) {
            val index = snapshots.indexOfFirst { it.id == snapshotId }
            if (index >= 0) {
                val snapshot = snapshots[index]
                File(snapshot.path).delete()
                snapshots.removeAt(index)
                return Result.success(Unit)
            }
        }
        return Result.failure(
            StorageException.DiskImageException("Snapshot not found: $snapshotId")
        )
    }

    override suspend fun listSnapshots(imageId: String): Result<List<SnapshotInfo>> {
        return Result.success(snapshotCache[imageId] ?: emptyList())
    }

    override suspend fun restoreSnapshot(snapshotId: String): Result<DiskImageInfo> = mutex.withLock {
        for ((imageId, snapshots) in snapshotCache) {
            val snapshot = snapshots.find { it.id == snapshotId }
            if (snapshot != null) {
                val imageInfo = imageCache[imageId]
                    ?: return Result.failure(
                        StorageException.DiskImageException("Image not found")
                    )

                // 简化实现：复制快照文件
                val copyResult = rawImageHandler.copyImage(snapshot.path, imageInfo.path)
                return if (copyResult.isSuccess) {
                    Result.success(imageInfo)
                } else {
                    Result.failure(
                        StorageException.DiskImageException("Failed to restore snapshot")
                    )
                }
            }
        }
        return Result.failure(
            StorageException.DiskImageException("Snapshot not found: $snapshotId")
        )
    }

    override suspend fun lockImage(imageId: String, vmId: String): Result<Unit> = mutex.withLock {
        val imageInfo = imageCache[imageId]
            ?: return Result.failure(
                StorageException.DiskImageException("Image not found: $imageId")
            )

        if (imageInfo.state == DiskImageState.LOCKED || imageInfo.state == DiskImageState.IN_USE) {
            return Result.failure(
                StorageException.DiskImageException("Image is already locked or in use")
            )
        }

        imageCache[imageId] = imageInfo.copy(
            state = DiskImageState.IN_USE,
            vmId = vmId
        )

        Result.success(Unit)
    }

    override suspend fun unlockImage(imageId: String): Result<Unit> = mutex.withLock {
        val imageInfo = imageCache[imageId]
            ?: return Result.failure(
                StorageException.DiskImageException("Image not found: $imageId")
            )

        imageCache[imageId] = imageInfo.copy(
            state = DiskImageState.READY,
            vmId = null
        )

        Result.success(Unit)
    }

    override suspend fun detectFormat(path: String): Result<DiskImageFormat> {
        return imageFormatDetector.detectFormat(path)
    }

    override suspend fun copyImage(sourceId: String, targetName: String): Result<DiskImageInfo> =
        mutex.withLock {
            val sourceInfo = imageCache[sourceId]
                ?: return Result.failure(
                    StorageException.DiskImageException("Source image not found: $sourceId")
                )

            val targetPath = "${File(sourceInfo.path).parent}/$targetName.${getExtension(sourceInfo.format)}"

            val result = when (sourceInfo.format) {
                DiskImageFormat.RAW -> rawImageHandler.copyImage(sourceInfo.path, targetPath)
                else -> rawImageHandler.copyImage(sourceInfo.path, targetPath)
            }

            return if (result.isSuccess) {
                val newInfo = result.getOrThrow()
                imageCache[newInfo.id] = newInfo
                Result.success(newInfo)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Copy failed"))
            }
        }

    override suspend fun importImage(sourcePath: String, targetName: String): Result<DiskImageInfo> =
        mutex.withLock {
            val format = imageFormatDetector.detectFormat(sourcePath).getOrNull()
                ?: return Result.failure(
                    StorageException.ImageFormatException("Cannot detect image format")
                )

            val storagePath = storageConfig.getDefaultImagePath()
            val targetPath = "$storagePath/$targetName.${getExtension(format)}"

            val result = rawImageHandler.copyImage(sourcePath, targetPath)

            return if (result.isSuccess) {
                val imageInfo = result.getOrThrow().copy(format = format)
                imageCache[imageInfo.id] = imageInfo
                Result.success(imageInfo)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Import failed"))
            }
        }

    override suspend fun exportImage(imageId: String, targetPath: String): Result<Unit> {
        val imageInfo = imageCache[imageId]
            ?: return Result.failure(
                StorageException.DiskImageException("Image not found: $imageId")
            )

        val result = rawImageHandler.copyImage(imageInfo.path, targetPath)
        return if (result.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Export failed"))
        }
    }

    override suspend fun getTotalImageSize(): Long {
        return imageCache.values.sumOf { it.actualSizeBytes }
    }

    override suspend fun cleanupUnusedImages(): Result<Int> = mutex.withLock {
        var count = 0
        val toRemove = mutableListOf<String>()

        for ((id, info) in imageCache) {
            if (info.state != DiskImageState.IN_USE && info.vmId == null) {
                val file = File(info.path)
                if (file.exists() && !file.name.startsWith("system_")) {
                    file.delete()
                    toRemove.add(id)
                    count++
                }
            }
        }

        toRemove.forEach { imageCache.remove(it) }

        Result.success(count)
    }

    // 私有方法

    private fun getExtension(format: DiskImageFormat): String {
        return when (format) {
            DiskImageFormat.QCOW2 -> "qcow2"
            DiskImageFormat.RAW -> "raw"
            DiskImageFormat.VDI -> "vdi"
            DiskImageFormat.VMDK -> "vmdk"
            DiskImageFormat.VHD -> "vhd"
            DiskImageFormat.UNKNOWN -> "img"
        }
    }
}

/**
 * 存储配置提供者接口
 */
interface StorageConfigProvider {
    fun getDefaultImagePath(): String
    fun getStoragePath(): String
    fun getMaxStorageBytes(): Long
}