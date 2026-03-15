package com.dfa.core.vm.qemu

/**
 * QEMU命令行构建器
 *
 * 将QemuConfig转换为QEMU命令行参数
 *
 * @property config QEMU虚拟机配置
 */
class QemuCommandLine(private val config: QemuConfig) {

    /**
     * 构建完整的QEMU命令行
     *
     * @return QEMU命令行参数列表
     */
    fun build(): List<String> {
        val args = mutableListOf<String>()

        // 基本参数
        args.addAll(buildBasicArgs())

        // 内存和CPU
        args.addAll(buildMemoryArgs())
        args.addAll(buildCpuArgs())

        // 加速器
        args.addAll(buildAcceleratorArgs())

        // 机器类型
        args.addAll(buildMachineArgs())

        // 磁盘
        args.addAll(buildDiskArgs())

        // 网络
        args.addAll(buildNetworkArgs())

        // 显示
        args.addAll(buildDisplayArgs())

        // 串口
        args.addAll(buildSerialArgs())

        // 音频
        args.addAll(buildAudioArgs())

        // 启动配置
        args.addAll(buildBootArgs())

        // USB
        args.addAll(buildUsbArgs())

        // GPU
        args.addAll(buildGpuArgs())

        // 监控
        args.addAll(buildMonitorArgs())

        // 额外参数
        args.addAll(config.extraArgs)

        return args
    }

    /**
     * 构建命令行字符串
     *
     * @return 完整的命令行字符串
     */
    fun buildCommandLineString(): String {
        val executable = config.getQemuExecutable()
        val args = build()
        return buildString {
            append(executable)
            args.forEach { arg ->
                append(" ")
                if (arg.contains(" ") || arg.contains("\"")) {
                    append("\"${arg.replace("\"", "\\\"")}\"")
                } else {
                    append(arg)
                }
            }
        }
    }

    /**
     * 构建基本参数
     */
    private fun buildBasicArgs(): List<String> {
        val args = mutableListOf<String>()
        args.add("-name")
        args.add(config.name)
        args.add("-uuid")
        args.add(config.id)
        return args
    }

    /**
     * 构建内存参数
     */
    private fun buildMemoryArgs(): List<String> {
        val args = mutableListOf<String>()
        args.add("-m")
        args.add("${config.memoryMb}M")
        return args
    }

    /**
     * 构建CPU参数
     */
    private fun buildCpuArgs(): List<String> {
        val args = mutableListOf<String>()
        args.add("-smp")
        args.add("${config.cpuCores}")
        args.add("-cpu")
        args.add(config.cpuModel.toQemuArg())
        return args
    }

    /**
     * 构建加速器参数
     */
    private fun buildAcceleratorArgs(): List<String> {
        val args = mutableListOf<String>()
        args.add("-accel")
        args.add(config.accelerator.toQemuArg())
        return args
    }

    /**
     * 构建机器类型参数
     */
    private fun buildMachineArgs(): List<String> {
        val args = mutableListOf<String>()
        args.add("-machine")
        args.add(config.machineType.toQemuArg())
        return args
    }

    /**
     * 构建磁盘参数
     */
    private fun buildDiskArgs(): List<String> {
        val args = mutableListOf<String>()
        config.disks.forEach { disk ->
            args.addAll(disk.toQemuArgs())
        }
        return args
    }

    /**
     * 构建网络参数
     */
    private fun buildNetworkArgs(): List<String> {
        return config.network.toQemuArgs()
    }

    /**
     * 构建显示参数
     */
    private fun buildDisplayArgs(): List<String> {
        return config.display.toQemuArgs()
    }

    /**
     * 构建串口参数
     */
    private fun buildSerialArgs(): List<String> {
        return config.serial.toQemuArgs()
    }

    /**
     * 构建音频参数
     */
    private fun buildAudioArgs(): List<String> {
        return config.audio.toQemuArgs()
    }

    /**
     * 构建启动参数
     */
    private fun buildBootArgs(): List<String> {
        val args = mutableListOf<String>()

        // BIOS文件
        config.biosFile?.let { bios ->
            args.add("-bios")
            args.add(bios)
        }

        // 启动镜像
        config.bootImage?.let { image ->
            args.add("-drive")
            args.add("file=$image,media=cdrom")
        }

        // 内核镜像
        config.kernelImage?.let { kernel ->
            args.add("-kernel")
            args.add(kernel)
        }

        // initrd镜像
        config.initrdImage?.let { initrd ->
            args.add("-initrd")
            args.add(initrd)
        }

        // 内核参数
        config.kernelArgs?.let { kernelArgs ->
            args.add("-append")
            args.add(kernelArgs)
        }

        return args
    }

    /**
     * 构建USB参数
     */
    private fun buildUsbArgs(): List<String> {
        val args = mutableListOf<String>()
        if (config.enableUsb) {
            args.add("-usb")
        }
        return args
    }

    /**
     * 构建GPU参数
     */
    private fun buildGpuArgs(): List<String> {
        val args = mutableListOf<String>()
        if (config.enableGpu) {
            args.add("-device")
            args.add("virtio-gpu-pci")
        }
        return args
    }

    /**
     * 构建监控参数
     */
    private fun buildMonitorArgs(): List<String> {
        val args = mutableListOf<String>()
        config.monitorPath?.let { path ->
            args.add("-monitor")
            args.add("unix:$path,server=on,wait=off")
        }
        return args
    }

    /**
     * 验证配置
     *
     * @return 验证结果，包含错误信息列表
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // 验证基本配置
        if (config.id.isEmpty()) {
            errors.add("VM id is required")
        }
        if (config.name.isEmpty()) {
            errors.add("VM name is required")
        }

        // 验证资源配置
        if (config.memoryMb <= 0) {
            errors.add("Memory must be positive (current: ${config.memoryMb}MB)")
        }
        if (config.cpuCores <= 0) {
            errors.add("CPU cores must be positive (current: ${config.cpuCores})")
        }

        // 验证加速器兼容性
        if (config.accelerator == QemuAccelerator.KVM && config.enableKvm) {
            // KVM需要Linux环境
        }

        // 验证架构和机器类型兼容性
        if (config.machineType.isX86 && !config.targetArch.is64Bit && config.targetArch != QemuTargetArch.I386) {
            errors.add("Machine type ${config.machineType} is not compatible with architecture ${config.targetArch}")
        }

        // 验证磁盘配置
        config.disks.forEachIndexed { index, disk ->
            if (!disk.validate()) {
                errors.add("Disk $index configuration is invalid")
            }
        }

        // 验证网络配置
        if (!config.network.validate()) {
            errors.add("Network configuration is invalid")
        }

        // 验证显示配置
        if (!config.display.validate()) {
            errors.add("Display configuration is invalid")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * 验证结果
     *
     * @property isValid 是否有效
     * @property errors 错误信息列表
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    ) {
        /**
         * 获取错误摘要
         */
        fun getErrorSummary(): String {
            return if (errors.isEmpty()) {
                "No errors"
            } else {
                errors.joinToString("; ")
            }
        }
    }

    companion object {
        /**
         * 创建QEMU命令行构建器
         *
         * @param config QEMU配置
         * @return 命令行构建器实例
         */
        fun from(config: QemuConfig): QemuCommandLine {
            return QemuCommandLine(config)
        }

        /**
         * 快速构建QEMU命令行
         *
         * @param config QEMU配置
         * @return 命令行参数列表
         */
        fun buildArgs(config: QemuConfig): List<String> {
            return QemuCommandLine(config).build()
        }

        /**
         * 快速构建QEMU命令行字符串
         *
         * @param config QEMU配置
         * @return 命令行字符串
         */
        fun buildString(config: QemuConfig): String {
            return QemuCommandLine(config).buildCommandLineString()
        }

        /**
         * 验证QEMU配置
         *
         * @param config QEMU配置
         * @return 验证结果
         */
        fun validateConfig(config: QemuConfig): ValidationResult {
            return QemuCommandLine(config).validate()
        }
    }
}

/**
 * 扩展函数：将QemuConfig转换为命令行参数
 */
fun QemuConfig.toCommandLine(): List<String> {
    return QemuCommandLine.buildArgs(this)
}

/**
 * 扩展函数：将QemuConfig转换为命令行字符串
 */
fun QemuConfig.toCommandLineString(): String {
    return QemuCommandLine.buildString(this)
}

/**
 * 扩展函数：验证QemuConfig
 */
fun QemuConfig.validateCommandLine(): QemuCommandLine.ValidationResult {
    return QemuCommandLine.validateConfig(this)
}