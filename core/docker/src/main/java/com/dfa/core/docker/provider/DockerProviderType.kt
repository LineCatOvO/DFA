package com.dfa.core.docker.provider

/**
 * Docker Provider类型枚举
 *
 * 定义支持的Docker Provider类型，用于标识不同的Docker运行环境。
 *
 * @property displayLabel 显示标签，用于UI展示
 * @property description 描述信息，说明该Provider的特点
 * @since 1.0.0
 */
enum class DockerProviderType(
    val displayLabel: String,
    val description: String
) {
    /**
     * QEMU虚拟化环境
     *
     * 通过QEMU虚拟机运行Docker，适用于需要完整虚拟化的场景。
     */
    QEMU(
        displayLabel = "QEMU",
        description = "Docker running in QEMU virtual machine environment"
    ),

    /**
     * Apple Virtualization Framework
     *
     * 使用Apple的虚拟化框架运行Docker，适用于macOS平台。
     */
    AVF(
        displayLabel = "AVF",
        description = "Docker running via Apple Virtualization Framework"
    ),

    /**
     * 本地Docker环境
     *
     * 直接使用本地安装的Docker，无需虚拟化层。
     */
    LOCAL(
        displayLabel = "Local",
        description = "Docker running directly on the host system"
    ),

    /**
     * 未知类型
     *
     * 无法识别的Provider类型。
     */
    UNKNOWN(
        displayLabel = "Unknown",
        description = "Unknown or unsupported Docker provider type"
    );

    /**
     * 检查是否为虚拟化环境
     *
     * @return 如果是虚拟化环境返回true
     */
    fun isVirtualized(): Boolean = this == QEMU || this == AVF

    /**
     * 检查是否支持
     *
     * @return 如果是已知类型返回true
     */
    fun isSupported(): Boolean = this != UNKNOWN

    companion object {
        /**
         * 根据名称解析Provider类型
         *
         * @param name 类型名称（不区分大小写）
         * @return 对应的DockerProviderType，未找到则返回UNKNOWN
         */
        fun fromName(name: String): DockerProviderType {
            return values().find { it.name.equals(name, ignoreCase = true) } ?: UNKNOWN
        }
    }
}