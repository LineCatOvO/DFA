package com.dfa.core.vm.image

import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 镜像验证器
 *
 * 提供镜像文件验证功能，包括校验和验证和格式验证
 */
@Singleton
class ImageValidator @Inject constructor() {

    /**
     * 验证镜像文件
     *
     * @param imageInfo 镜像信息
     * @return 验证结果
     */
    suspend fun validate(imageInfo: ImageInfo): ImageValidationResult {
        val localPath = imageInfo.localPath
        if (localPath == null) {
            return ImageValidationResult(
                imageId = imageInfo.id,
                isValid = false,
                errorMessage = "Local path is null"
            )
        }

        val file = File(localPath)
        if (!file.exists()) {
            return ImageValidationResult(
                imageId = imageInfo.id,
                isValid = false,
                errorMessage = "File does not exist: $localPath"
            )
        }

        // 验证文件格式
        val formatValid = validateFormat(file)
        if (!formatValid) {
            return ImageValidationResult(
                imageId = imageInfo.id,
                isValid = false,
                fileSize = file.length(),
                errorMessage = "Invalid image format"
            )
        }

        // 验证校验和（如果提供）
        val checksumMatch = if (imageInfo.checksum != null && imageInfo.checksumType != null) {
            val actualChecksum = calculateChecksum(file, imageInfo.checksumType)
            actualChecksum.equals(imageInfo.checksum, ignoreCase = true)
        } else {
            true // 没有提供校验和，默认通过
        }

        return ImageValidationResult(
            imageId = imageInfo.id,
            isValid = checksumMatch,
            checksumMatch = checksumMatch,
            fileSize = file.length(),
            errorMessage = if (!checksumMatch) "Checksum mismatch" else null
        )
    }

    /**
     * 验证镜像格式
     *
     * @param file 镜像文件
     * @return 是否为有效格式
     */
    fun validateFormat(file: File): Boolean {
        val fileName = file.name.lowercase()
        val extension = fileName.substringAfterLast(".", "")

        // 检查扩展名
        if (extension !in ImageConstants.SUPPORTED_FORMATS) {
            return false
        }

        // 检查文件大小（至少1MB）
        if (file.length() < 1024 * 1024) {
            return false
        }

        // 检查QCOW2文件头（如果扩展名是qcow2）
        if (extension == "qcow2") {
            return validateQcow2Header(file)
        }

        return true
    }

    /**
     * 验证QCOW2文件头
     *
     * @param file 文件
     * @return 是否为有效的QCOW2文件
     */
    private fun validateQcow2Header(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                val bytesRead = input.read(header)
                if (bytesRead < 4) return false

                // QCOW2 magic number: 0x514649fb (QFI\xfb)
                header[0] == 0x51.toByte() &&
                    header[1] == 0x46.toByte() &&
                    header[2] == 0x49.toByte() &&
                    header[3] == 0xfb.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 计算文件校验和
     *
     * @param file 文件
     * @param algorithm 算法（如sha256）
     * @return 校验和（十六进制字符串）
     */
    suspend fun calculateChecksum(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm.uppercase())

        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 快速验证镜像（仅检查文件头）
     *
     * @param filePath 文件路径
     * @return 是否为有效镜像
     */
    fun quickValidate(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists()) return false

        return validateFormat(file)
    }

    /**
     * 获取镜像文件信息
     *
     * @param filePath 文件路径
     * @return 镜像信息（如果有效）
     */
    suspend fun getImageInfo(filePath: String): ImageFormatInfo? {
        val file = File(filePath)
        if (!file.exists()) return null

        val extension = file.name.substringAfterLast(".", "").lowercase()

        return when (extension) {
            "qcow2" -> getQcow2Info(file)
            "img", "raw" -> getRawInfo(file)
            else -> null
        }
    }

    /**
     * 获取QCOW2镜像信息
     */
    private fun getQcow2Info(file: File): ImageFormatInfo? {
        return try {
            file.inputStream().use { input ->
                // 读取QCOW2头信息
                val header = ByteArray(72)
                if (input.read(header) < 72) return null

                // 解析版本
                val version = ((header[4].toInt() and 0xFF) shl 24) or
                    ((header[5].toInt() and 0xFF) shl 16) or
                    ((header[6].toInt() and 0xFF) shl 8) or
                    (header[7].toInt() and 0xFF)

                // 解析虚拟大小
                val virtualSize = ((header[24].toLong() and 0xFF) shl 56) or
                    ((header[25].toLong() and 0xFF) shl 48) or
                    ((header[26].toLong() and 0xFF) shl 40) or
                    ((header[27].toLong() and 0xFF) shl 32) or
                    ((header[28].toLong() and 0xFF) shl 24) or
                    ((header[29].toLong() and 0xFF) shl 16) or
                    ((header[30].toLong() and 0xFF) shl 8) or
                    (header[31].toLong() and 0xFF)

                ImageFormatInfo(
                    format = "qcow2",
                    version = version,
                    virtualSize = virtualSize,
                    actualSize = file.length()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取RAW镜像信息
     */
    private fun getRawInfo(file: File): ImageFormatInfo {
        return ImageFormatInfo(
            format = "raw",
            version = 1,
            virtualSize = file.length(),
            actualSize = file.length()
        )
    }
}

/**
 * 镜像格式信息
 *
 * @property format 格式类型
 * @property version 格式版本
 * @property virtualSize 虚拟大小（字节）
 * @property actualSize 实际大小（字节）
 */
data class ImageFormatInfo(
    val format: String,
    val version: Int,
    val virtualSize: Long,
    val actualSize: Long
)