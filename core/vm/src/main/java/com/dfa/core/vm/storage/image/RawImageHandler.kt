package com.dfa.core.vm.storage.image

import com.dfa.core.vm.storage.models.DiskImageFormat
import com.dfa.core.vm.storage.models.DiskImageInfo
import com.dfa.core.vm.storage.models.DiskImageState
import com.dfa.core.vm.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAW镜像处理器
 *
 * 提供RAW格式磁盘镜像的创建、读取和管理功能
 */
@Singleton
class RawImageHandler @Inject constructor() {

    /**
     * 创建RAW镜像
     *
     * @param path 镜像路径
     * @param sizeBytes 大小
     * @param preallocate 是否预分配
     * @return 创建的镜像信息
     */
    suspend fun createImage(
        path: String,
        sizeBytes: Long,
        preallocate: Boolean = true
    ): Result<DiskImageInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            if (file.exists()) {
                return@withContext Result.failure(
                    StorageException.DiskImageException(
                        "Image file already exists: $path",
                        path
                    )
                )
            }

            // 创建父目录
            file.parentFile?.mkdirs()

            if (preallocate) {
                // 预分配空间
                RandomAccessFile(file, "rw").use { raf ->
                    raf.setLength(sizeBytes)
                }
            } else {
                // 创建稀疏文件
                file.createNewFile()
            }

            val imageInfo = DiskImageInfo(
                id = generateImageId(path),
                name = file.nameWithoutExtension,
                path = path,
                format = DiskImageFormat.RAW,
                virtualSizeBytes = sizeBytes,
                actualSizeBytes = if (preallocate) sizeBytes else 0,
                state = DiskImageState.READY
            )

            Result.success(imageInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to create RAW image: ${e.message}",
                    path,
                    e
                )
            )
        }
    }

    /**
     * 读取镜像信息
     *
     * @param path 镜像路径
     * @return 镜像信息
     */
    suspend fun readImageInfo(path: String): Result<DiskImageInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(
                    StorageException.DiskImageException(
                        "Image file not found: $path",
                        path
                    )
                )
            }

            val size = file.length()

            val imageInfo = DiskImageInfo(
                id = generateImageId(path),
                name = file.nameWithoutExtension,
                path = path,
                format = DiskImageFormat.RAW,
                virtualSizeBytes = size,
                actualSizeBytes = size,
                state = DiskImageState.READY
            )

            Result.success(imageInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to read RAW image info: ${e.message}",
                    path,
                    e
                )
            )
        }
    }

    /**
     * 验证镜像
     *
     * @param path 镜像路径
     * @return 是否有效
     */
    suspend fun validateImage(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.success(false)
            }

            // RAW格式没有特定的魔数，只要文件存在且可读即可
            val canRead = file.canRead()
            Result.success(canRead)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to validate RAW image: ${e.message}",
                    path,
                    e
                )
            )
        }
    }

    /**
     * 调整镜像大小
     *
     * @param path 镜像路径
     * @param newSizeBytes 新大小
     * @return 操作结果
     */
    suspend fun resizeImage(
        path: String,
        newSizeBytes: Long
    ): Result<DiskImageInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(
                    StorageException.DiskImageException(
                        "Image file not found: $path",
                        path
                    )
                )
            }

            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(newSizeBytes)
            }

            val imageInfo = DiskImageInfo(
                id = generateImageId(path),
                name = file.nameWithoutExtension,
                path = path,
                format = DiskImageFormat.RAW,
                virtualSizeBytes = newSizeBytes,
                actualSizeBytes = newSizeBytes,
                state = DiskImageState.READY
            )

            Result.success(imageInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to resize RAW image: ${e.message}",
                    path,
                    e
                )
            )
        }
    }

    /**
     * 计算镜像校验和
     *
     * @param path 镜像路径
     * @param algorithm 算法（如SHA-256）
     * @return 校验和
     */
    suspend fun calculateChecksum(
        path: String,
        algorithm: String = "SHA-256"
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(
                    StorageException.DiskImageException(
                        "Image file not found: $path",
                        path
                    )
                )
            }

            val digest = MessageDigest.getInstance(algorithm)
            val buffer = ByteArray(BUFFER_SIZE)

            file.inputStream().use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            val checksum = digest.digest().joinToString("") { 
                "%02x".format(it) 
            }

            Result.success(checksum)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to calculate checksum: ${e.message}",
                    path,
                    e
                )
            )
        }
    }

    /**
     * 复制镜像
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @return 复制的镜像信息
     */
    suspend fun copyImage(
        sourcePath: String,
        targetPath: String
    ): Result<DiskImageInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(
                    StorageException.DiskImageException(
                        "Source image not found: $sourcePath",
                        sourcePath
                    )
                )
            }

            val targetFile = File(targetPath)
            targetFile.parentFile?.mkdirs()

            sourceFile.copyTo(targetFile, overwrite = true)

            val size = targetFile.length()

            val imageInfo = DiskImageInfo(
                id = generateImageId(targetPath),
                name = targetFile.nameWithoutExtension,
                path = targetPath,
                format = DiskImageFormat.RAW,
                virtualSizeBytes = size,
                actualSizeBytes = size,
                state = DiskImageState.READY
            )

            Result.success(imageInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to copy RAW image: ${e.message}",
                    sourcePath,
                    e
                )
            )
        }
    }

    /**
     * 清零镜像
     *
     * @param path 镜像路径
     * @return 操作结果
     */
    suspend fun zeroImage(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(
                    StorageException.DiskImageException(
                        "Image file not found: $path",
                        path
                    )
                )
            }

            val size = file.length()
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(size)
                // 文件内容已清零
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to zero RAW image: ${e.message}",
                    path,
                    e
                )
            )
        }
    }

    // 私有方法

    private fun generateImageId(path: String): String {
        return "raw-${path.hashCode().toString(16)}-${System.currentTimeMillis()}"
    }

    companion object {
        private const val BUFFER_SIZE = 8192
    }
}