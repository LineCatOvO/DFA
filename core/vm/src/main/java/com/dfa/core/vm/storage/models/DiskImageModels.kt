package com.dfa.core.vm.storage.models

/**
 * 磁盘镜像格式枚举
 */
enum class DiskImageFormat {
    /** RAW格式 */
    RAW,
    /** QCOW2格式 */
    QCOW2,
    /** VDI格式 */
    VDI,
    /** VMDK格式 */
    VMDK,
    /** VHD格式 */
    VHD,
    /** 未知格式 */
    UNKNOWN
}

/**
 * 磁盘镜像状态枚举
 */
enum class DiskImageState {
    /** 创建中 */
    CREATING,
    /** 就绪 */
    READY,
    /** 使用中 */
    IN_USE,
    /** 锁定 */
    LOCKED,
    /** 损坏 */
    CORRUPTED,
    /** 删除中 */
    DELETING,
    /** 错误 */
    ERROR
}

/**
 * 磁盘镜像信息
 *
 * @property id 镜像唯一标识
 * @property name 镜像名称
 * @property path 镜像路径
 * @property format 镜像格式
 * @property virtualSizeBytes 虚拟大小（字节）
 * @property actualSizeBytes 实际大小（字节）
 * @property state 镜像状态
 * @property isEncrypted 是否加密
 * @property checksum 校验和
 * @property checksumType 校验和类型
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 * @property vmId 关联的虚拟机ID
 * @property errorMessage 错误信息
 */
data class DiskImageInfo(
    val id: String,
    val name: String,
    val path: String,
    val format: DiskImageFormat,
    val virtualSizeBytes: Long,
    val actualSizeBytes: Long = 0,
    val state: DiskImageState = DiskImageState.READY,
    val isEncrypted: Boolean = false,
    val checksum: String? = null,
    val checksumType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val vmId: String? = null,
    val errorMessage: String? = null
) {
    /**
     * 是否可用
     */
    val isReady: Boolean
        get() = state == DiskImageState.READY || state == DiskImageState.IN_USE

    /**
     * 是否正在使用
     */
    val isInUse: Boolean
        get() = state == DiskImageState.IN_USE

    /**
     * 压缩率
     */
    val compressionRatio: Float
        get() = if (virtualSizeBytes > 0 && actualSizeBytes > 0) {
            actualSizeBytes.toFloat() / virtualSizeBytes
        } else 1.0f

    /**
     * 是否为稀疏镜像
     */
    val isSparse: Boolean
        get() = actualSizeBytes < virtualSizeBytes
}

/**
 * 创建磁盘镜像请求
 *
 * @property name 镜像名称
 * @property format 镜像格式
 * @property sizeBytes 大小（字节）
 * @property path 存储路径
 * @property enableEncryption 是否启用加密
 * @property preallocate 是否预分配空间
 * @property description 描述
 */
data class CreateDiskImageRequest(
    val name: String,
    val format: DiskImageFormat = DiskImageFormat.QCOW2,
    val sizeBytes: Long,
    val path: String? = null,
    val enableEncryption: Boolean = false,
    val preallocate: Boolean = false,
    val description: String? = null
) {
    fun validate(): Boolean {
        return name.isNotEmpty() &&
                sizeBytes > 0 &&
                format != DiskImageFormat.UNKNOWN
    }
}

/**
 * 创建磁盘镜像结果
 *
 * @property imageInfo 创建的镜像信息
 * @property success 是否成功
 * @property errorMessage 错误信息
 */
data class CreateDiskImageResult(
    val imageInfo: DiskImageInfo? = null,
    val success: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success(imageInfo: DiskImageInfo): CreateDiskImageResult {
            return CreateDiskImageResult(
                imageInfo = imageInfo,
                success = true
            )
        }

        fun failure(error: String): CreateDiskImageResult {
            return CreateDiskImageResult(
                success = false,
                errorMessage = error
            )
        }
    }
}

/**
 * 镜像转换请求
 *
 * @property sourcePath 源镜像路径
 * @property targetPath 目标镜像路径
 * @property targetFormat 目标格式
 * @property compress 是否压缩
 * @property enableEncryption 是否启用加密
 */
data class ConvertImageRequest(
    val sourcePath: String,
    val targetPath: String,
    val targetFormat: DiskImageFormat,
    val compress: Boolean = true,
    val enableEncryption: Boolean = false
) {
    fun validate(): Boolean {
        return sourcePath.isNotEmpty() &&
                targetPath.isNotEmpty() &&
                targetFormat != DiskImageFormat.UNKNOWN
    }
}

/**
 * 镜像转换进度
 *
 * @property sourcePath 源路径
 * @property targetPath 目标路径
 * @property progress 进度（0-100）
 * @property bytesProcessed 已处理字节数
 * @property totalBytes 总字节数
 * @property state 转换状态
 */
data class ConvertImageProgress(
    val sourcePath: String,
    val targetPath: String,
    val progress: Int = 0,
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val state: ConvertState = ConvertState.PENDING
) {
    enum class ConvertState {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }

    val isCompleted: Boolean
        get() = state == ConvertState.COMPLETED

    val isFailed: Boolean
        get() = state == ConvertState.FAILED
}

/**
 * 快照信息
 *
 * @property id 快照ID
 * @property imageId 关联的镜像ID
 * @property name 快照名称
 * @property path 快照路径
 * @property sizeBytes 大小
 * @property createdAt 创建时间
 * @property description 描述
 * @property isEncrypted 是否加密
 */
data class SnapshotInfo(
    val id: String,
    val imageId: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val description: String? = null,
    val isEncrypted: Boolean = false
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

/**
 * 创建快照请求
 *
 * @property imageId 镜像ID
 * @property name 快照名称
 * @property description 描述
 */
data class CreateSnapshotRequest(
    val imageId: String,
    val name: String,
    val description: String? = null
) {
    fun validate(): Boolean {
        return imageId.isNotEmpty() && name.isNotEmpty()
    }
}

/**
 * 镜像验证结果
 *
 * @property imageId 镜像ID
 * @property isValid 是否有效
 * @property hasErrors 是否有错误
 * @property errors 错误列表
 * @property warnings 警告列表
 */
data class ImageValidationResult(
    val imageId: String,
    val isValid: Boolean,
    val hasErrors: Boolean = false,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun valid(imageId: String): ImageValidationResult {
            return ImageValidationResult(
                imageId = imageId,
                isValid = true
            )
        }

        fun invalid(imageId: String, errors: List<String>): ImageValidationResult {
            return ImageValidationResult(
                imageId = imageId,
                isValid = false,
                hasErrors = true,
                errors = errors
            )
        }
    }
}