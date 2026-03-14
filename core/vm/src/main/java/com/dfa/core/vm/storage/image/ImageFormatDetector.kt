package com.dfa.core.vm.storage.image

import com.dfa.core.vm.storage.models.DiskImageFormat
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
 * 镜像格式检测器
 *
 * 自动检测磁盘镜像的格式
 */
@Singleton
class ImageFormatDetector @Inject constructor() {

    /**
     * 检测镜像格式
     *
     * @param path 镜像路径
     * @return 检测到的格式
     */
    suspend fun detectFormat(path: String): Result<DiskImageFormat> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(
                    StorageException.ImageFormatException(
                        "Image file not found: $path",
                        null
                    )
                )
            }

            if (file.length() < MIN_HEADER_SIZE) {
                return@withContext Result.failure(
                    StorageException.ImageFormatException(
                        "File too small to determine format",
                        null
                    )
                )
            }

            val header = ByteArray(MIN_HEADER_SIZE)
            RandomAccessFile(file, "r").use { raf ->
                raf.read(header)
            }

            val format = detectFormatFromHeader(header)
                ?: detectFormatFromExtension(file)
                ?: DiskImageFormat.UNKNOWN

            Result.success(format)
        } catch (e: Exception) {
            Result.failure(
                StorageException.ImageFormatException(
                    "Failed to detect image format: ${e.message}",
                    null,
                    e
                )
            )
        }
    }

    /**
     * 检测镜像格式（从字节数组）
     *
     * @param header 文件头部字节
     * @return 检测到的格式
     */
    fun detectFormatFromHeader(header: ByteArray): DiskImageFormat? {
        if (header.size < MIN_HEADER_SIZE) {
            return null
        }

        // QCOW2: 0x514649fb ("QFI\xfb")
        if (header.size >= 4 && header.copyOf(4).contentEquals(QCOW2_MAGIC)) {
            return DiskImageFormat.QCOW2
        }

        // VMDK: "KDMV" (little endian) or "VMDK"
        if (header.size >= 4) {
            val magic = String(header.copyOf(4), Charsets.US_ASCII)
            if (magic == "KDMV" || magic == "VMDK") {
                return DiskImageFormat.VMDK
            }
        }

        // VDI: "\x7f\x10\xda\xbe"
        if (header.size >= 4 && header.copyOf(4).contentEquals(VDI_MAGIC)) {
            return DiskImageFormat.VDI
        }

        // VHD: "connectix" at offset 0
        if (header.size >= 9) {
            val magic = String(header.copyOfRange(0, 9), Charsets.US_ASCII)
            if (magic == "connectix") {
                return DiskImageFormat.VHD
            }
        }

        // VHDX: "vhdxfile"
        if (header.size >= 8) {
            val magic = String(header.copyOfRange(0, 8), Charsets.US_ASCII)
            if (magic == "vhdxfile") {
                return DiskImageFormat.VHD
            }
        }

        return null
    }

    /**
     * 从文件扩展名检测格式
     *
     * @param file 文件
     * @return 检测到的格式
     */
    fun detectFormatFromExtension(file: File): DiskImageFormat? {
        val extension = file.extension.lowercase()
        return when (extension) {
            "raw", "img", "bin" -> DiskImageFormat.RAW
            "qcow2", "qcow" -> DiskImageFormat.QCOW2
            "vmdk" -> DiskImageFormat.VMDK
            "vdi" -> DiskImageFormat.VDI
            "vhd", "vhdx" -> DiskImageFormat.VHD
            else -> null
        }
    }

    /**
     * 验证镜像格式
     *
     * @param path 镜像路径
     * @param expectedFormat 预期格式
     * @return 是否匹配
     */
    suspend fun validateFormat(
        path: String,
        expectedFormat: DiskImageFormat
    ): Result<Boolean> {
        return try {
            val detectedFormat = detectFormat(path).getOrNull()
            Result.success(detectedFormat == expectedFormat)
        } catch (e: Exception) {
            Result.failure(
                StorageException.ImageFormatException(
                    "Failed to validate format: ${e.message}",
                    expectedFormat.name,
                    e
                )
            )
        }
    }

    /**
     * 获取镜像格式描述
     *
     * @param format 格式
     * @return 描述
     */
    fun getFormatDescription(format: DiskImageFormat): String {
        return when (format) {
            DiskImageFormat.RAW -> "Raw disk image - simple byte-for-byte copy"
            DiskImageFormat.QCOW2 -> "QEMU Copy-On-Write v2 - supports snapshots, compression, encryption"
            DiskImageFormat.VDI -> "VirtualBox Disk Image - VirtualBox native format"
            DiskImageFormat.VMDK -> "VMware Virtual Disk - VMware native format"
            DiskImageFormat.VHD -> "Virtual Hard Disk - Microsoft Hyper-V format"
            DiskImageFormat.UNKNOWN -> "Unknown or unsupported format"
        }
    }

    /**
     * 检查格式是否支持快照
     *
     * @param format 格式
     * @return 是否支持
     */
    fun supportsSnapshots(format: DiskImageFormat): Boolean {
        return when (format) {
            DiskImageFormat.QCOW2 -> true
            DiskImageFormat.VDI -> true
            DiskImageFormat.VMDK -> true
            DiskImageFormat.RAW -> false
            DiskImageFormat.VHD -> false
            DiskImageFormat.UNKNOWN -> false
        }
    }

    /**
     * 检查格式是否支持压缩
     *
     * @param format 格式
     * @return 是否支持
     */
    fun supportsCompression(format: DiskImageFormat): Boolean {
        return when (format) {
            DiskImageFormat.QCOW2 -> true
            DiskImageFormat.VMDK -> true
            DiskImageFormat.RAW -> false
            DiskImageFormat.VDI -> false
            DiskImageFormat.VHD -> false
            DiskImageFormat.UNKNOWN -> false
        }
    }

    /**
     * 检查格式是否支持加密
     *
     * @param format 格式
     * @return 是否支持
     */
    fun supportsEncryption(format: DiskImageFormat): Boolean {
        return when (format) {
            DiskImageFormat.QCOW2 -> true
            DiskImageFormat.RAW -> false
            DiskImageFormat.VDI -> false
            DiskImageFormat.VMDK -> false
            DiskImageFormat.VHD -> false
            DiskImageFormat.UNKNOWN -> false
        }
    }

    companion object {
        private const val MIN_HEADER_SIZE = 512

        // QCOW2 magic: "QFI\xfb"
        private val QCOW2_MAGIC = byteArrayOf(0x51, 0x46, 0x49, 0xfb.toByte())

        // VDI magic: "\x7f\x10\xda\xbe"
        private val VDI_MAGIC = byteArrayOf(0x7f, 0x10, 0xda.toByte(), 0xbe.toByte())
    }
}