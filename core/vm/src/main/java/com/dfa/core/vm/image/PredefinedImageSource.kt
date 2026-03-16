package com.dfa.core.vm.image

/**
 * 镜像架构枚举
 */
enum class ImageArchitecture {
    ARM64,
    AMD64,
    X86,
    ARMV7
}

/**
 * 操作系统类型枚举
 */
enum class OsType {
    DEBIAN,
    UBUNTU,
    ALPINE,
    FEDORA,
    CIRROS,
    CENTOS,
    ROCKY,
    ALMALINUX
}

/**
 * 校验和类型枚举
 */
enum class ChecksumType {
    SHA256,
    SHA512,
    MD5
}

/**
 * 预定义镜像来源
 *
 * 包含官方QCOW2镜像的完整元数据，供用户选择下载
 *
 * @property id 唯一标识
 * @property name 显示名称
 * @property description 描述
 * @property url 下载URL
 * @property architecture 架构
 * @property osType 操作系统类型
 * @property osVersion 操作系统版本
 * @property format 镜像格式
 * @property estimatedSizeBytes 预估大小（字节）
 * @property checksum 校验和
 * @property checksumType 校验和类型
 * @property loginAccount 默认登录账户
 * @property loginPassword 默认登录密码（可选）
 * @property isMinimal 是否为最小镜像
 * @property tags 标签集合
 * @property officialUrl 官方文档URL
 * @property lastVerified 最后验证时间
 */
data class PredefinedImageSource(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val architecture: ImageArchitecture,
    val osType: OsType,
    val osVersion: String,
    val format: String = "qcow2",
    val estimatedSizeBytes: Long = 0,
    val checksum: String? = null,
    val checksumType: ChecksumType = ChecksumType.SHA256,
    val loginAccount: String,
    val loginPassword: String? = null,
    val isMinimal: Boolean = false,
    val tags: Set<String> = emptySet(),
    val officialUrl: String? = null,
    val lastVerified: Long = System.currentTimeMillis()
) {
    /**
     * 格式化预估大小
     */
    val formattedSize: String
        get() = formatFileSize(estimatedSizeBytes)

    /**
     * 是否为ARM架构
     */
    val isArm: Boolean
        get() = architecture == ImageArchitecture.ARM64 || architecture == ImageArchitecture.ARMV7

    /**
     * 是否推荐用于Docker
     */
    val isRecommendedForDocker: Boolean
        get() = "docker" in tags || "cloud" in tags

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}