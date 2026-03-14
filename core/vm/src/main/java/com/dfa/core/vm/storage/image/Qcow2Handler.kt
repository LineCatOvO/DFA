package com.dfa.core.vm.storage.image

import com.dfa.core.vm.storage.models.DiskImageFormat
import com.dfa.core.vm.storage.models.DiskImageInfo
import com.dfa.core.vm.storage.models.DiskImageState
import com.dfa.core.vm.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QCOW2镜像处理器
 *
 * 提供QCOW2格式磁盘镜像的创建、读取和管理功能
 */
@Singleton
class Qcow2Handler @Inject constructor() {

    /**
     * 创建QCOW2镜像
     *
     * @param path 镜像路径
     * @param virtualSizeBytes 虚拟大小
     * @param preallocate 是否预分配
     * @return 创建的镜像信息
     */
    suspend fun createImage(
        path: String,
        virtualSizeBytes: Long,
        preallocate: Boolean = false
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

            // 创建QCOW2头部
            val header = createQcow2Header(virtualSizeBytes)

            RandomAccessFile(file, "rw").use { raf ->
                raf.write(header)

                if (preallocate) {
                    // 预分配空间
                    raf.setLength(virtualSizeBytes)
                }
            }

            val imageInfo = DiskImageInfo(
                id = generateImageId(path),
                name = file.nameWithoutExtension,
                path = path,
                format = DiskImageFormat.QCOW2,
                virtualSizeBytes = virtualSizeBytes,
                actualSizeBytes = if (preallocate) virtualSizeBytes else header.size.toLong(),
                state = DiskImageState.READY
            )

            Result.success(imageInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to create QCOW2 image: ${e.message}",
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

            val header = readQcow2Header(file)

            val imageInfo = DiskImageInfo(
                id = generateImageId(path),
                name = file.nameWithoutExtension,
                path = path,
                format = DiskImageFormat.QCOW2,
                virtualSizeBytes = header.virtualSize,
                actualSizeBytes = file.length(),
                state = DiskImageState.READY
            )

            Result.success(imageInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to read QCOW2 image info: ${e.message}",
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

            val magic = ByteArray(4)
            RandomAccessFile(file, "r").use { raf ->
                raf.read(magic)
            }

            // QCOW2魔数: 0x514649fb ("QFI\xfb")
            val isValid = magic.contentEquals(QCOW2_MAGIC)
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to validate QCOW2 image: ${e.message}",
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

            // 读取现有头部
            val header = readQcow2Header(file)

            // 更新虚拟大小
            header.virtualSize = newSizeBytes

            // 写回头部
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(24) // virtual_size offset
                raf.writeLong(newSizeBytes)
            }

            val imageInfo = DiskImageInfo(
                id = generateImageId(path),
                name = file.nameWithoutExtension,
                path = path,
                format = DiskImageFormat.QCOW2,
                virtualSizeBytes = newSizeBytes,
                actualSizeBytes = file.length(),
                state = DiskImageState.READY
            )

            Result.success(imageInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.DiskImageException(
                    "Failed to resize QCOW2 image: ${e.message}",
                    path,
                    e
                )
            )
        }
    }

    // 私有方法

    private fun createQcow2Header(virtualSize: Long): ByteArray {
        val buffer = ByteBuffer.allocate(104) // QCOW2 header size
        buffer.order(ByteOrder.BIG_ENDIAN)

        // Magic number
        buffer.put(QCOW2_MAGIC)
        // Version (2 or 3)
        buffer.putInt(QCOW2_VERSION)
        // Backing file offset
        buffer.putLong(0)
        // Backing file size
        buffer.putInt(0)
        // Cluster bits (default 16 = 64KB clusters)
        buffer.putInt(16)
        // Virtual size
        buffer.putLong(virtualSize)
        // Encryption method (0 = none)
        buffer.putInt(0)
        // L1 size
        buffer.putInt(calculateL1Size(virtualSize))
        // L1 table offset
        buffer.putLong(104) // After header
        // Refcount table offset
        buffer.putLong(0)
        // Refcount table clusters
        buffer.putInt(0)
        // Number of snapshots
        buffer.putInt(0)
        // Snapshots offset
        buffer.putLong(0)

        return buffer.array()
    }

    private fun readQcow2Header(file: File): Qcow2Header {
        RandomAccessFile(file, "r").use { raf ->
            val buffer = ByteArray(104)
            raf.read(buffer)

            val byteBuffer = ByteBuffer.wrap(buffer)
            byteBuffer.order(ByteOrder.BIG_ENDIAN)

            // Skip magic (4 bytes)
            byteBuffer.position(4)
            // Version
            val version = byteBuffer.int
            // Skip backing file offset (8 bytes) and size (4 bytes)
            byteBuffer.position(byteBuffer.position() + 12)
            // Cluster bits
            val clusterBits = byteBuffer.int
            // Virtual size
            val virtualSize = byteBuffer.long

            return Qcow2Header(
                version = version,
                virtualSize = virtualSize,
                clusterBits = clusterBits
            )
        }
    }

    private fun calculateL1Size(virtualSize: Long): Int {
        val clusterBits = 16
        val clusterSize = 1L shl clusterBits
        val l2Entries = clusterSize / 8 // Each L2 entry is 8 bytes
        val l2Coverage = l2Entries * clusterSize

        return ((virtualSize + l2Coverage - 1) / l2Coverage).toInt()
    }

    private fun generateImageId(path: String): String {
        return "qcow2-${path.hashCode().toString(16)}-${System.currentTimeMillis()}"
    }

    private data class Qcow2Header(
        val version: Int,
        val virtualSize: Long,
        val clusterBits: Int
    )

    companion object {
        private val QCOW2_MAGIC = byteArrayOf(0x51, 0x46, 0x49, 0xfb) // "QFI\xfb"
        private const val QCOW2_VERSION = 3
    }
}