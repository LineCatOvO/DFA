package com.dfa.core.vm.qemu

/**
 * QEMU磁盘配置
 *
 * 定义QEMU虚拟机的磁盘配置参数
 *
 * @property path 磁盘镜像文件路径
 * @property format 磁盘格式
 * @property sizeGb 磁盘大小（GB），仅用于创建新磁盘
 * @property interface 磁盘接口类型
 * @property cacheMode 缓存模式
 * @property readOnly 是否只读
 * @property discard 是否启用discard支持
 * @property bootIndex 启动顺序索引
 */
data class QemuDiskConfig(
    val path: String,
    val format: QemuDiskFormat = QemuDiskFormat.QCOW2,
    val sizeGb: Int = 10,
    val `interface`: String = "virtio",
    val cacheMode: String = "writeback",
    val readOnly: Boolean = false,
    val discard: Boolean = false,
    val bootIndex: Int? = null
) {
    /**
     * 验证磁盘配置
     */
    fun validate(): Boolean {
        return path.isNotEmpty() &&
                sizeGb > 0 &&
                bootIndex == null || bootIndex!! >= 0
    }

    /**
     * 转换为QEMU命令行参数
     */
    fun toQemuArgs(): List<String> {
        val args = mutableListOf<String>()
        val driveOptions = mutableListOf<String>()

        driveOptions.add("file=$path")
        driveOptions.add("format=${format.toQemuArg()}")
        driveOptions.add("cache=$cacheMode")

        if (readOnly) {
            driveOptions.add("readonly=on")
        }
        if (discard) {
            driveOptions.add("discard=unmap")
        }

        args.add("-drive")
        args.add(driveOptions.joinToString(","))

        return args
    }
}

/**
 * QEMU端口转发配置
 *
 * 定义用户模式网络下的端口转发规则
 *
 * @property protocol 协议类型（tcp/udp）
 * @property hostPort 主机端口
 * @property guestPort 虚拟机端口
 * @property hostAddress 主机绑定地址
 */
data class QemuPortForward(
    val protocol: String = "tcp",
    val hostPort: Int,
    val guestPort: Int,
    val hostAddress: String = "0.0.0.0"
) {
    /**
     * 验证端口转发配置
     */
    fun validate(): Boolean {
        return hostPort in 1..65535 &&
                guestPort in 1..65535 &&
                protocol in listOf("tcp", "udp")
    }

    /**
     * 转换为QEMU端口转发参数
     */
    fun toQemuArg(): String = "hostfwd=${protocol}:${hostAddress}:${hostPort}-:${guestPort}"
}

/**
 * QEMU网络配置
 *
 * 定义QEMU虚拟机的网络配置参数
 *
 * @property mode 网络模式
 * @property device 网络设备类型
 * @property macAddress MAC地址（可选，自动生成）
 * @property bridgeName 桥接网络名称（桥接模式使用）
 * @property tapDevice TAP设备名称（TAP模式使用）
 * @property portForwards 端口转发列表（用户模式使用）
 * @property hostname 虚拟机主机名（用户模式使用）
 * @property dns DNS服务器地址（用户模式使用）
 * @property enableDhcp 是否启用DHCP
 */
data class QemuNetworkConfig(
    val mode: QemuNetworkMode = QemuNetworkMode.USER,
    val device: String = "virtio-net-pci",
    val macAddress: String? = null,
    val bridgeName: String? = null,
    val tapDevice: String? = null,
    val portForwards: List<QemuPortForward> = emptyList(),
    val hostname: String? = null,
    val dns: String? = null,
    val enableDhcp: Boolean = true
) {
    /**
     * 验证网络配置
     */
    fun validate(): Boolean {
        if (mode == QemuNetworkMode.BRIDGE && bridgeName.isNullOrBlank()) {
            return false
        }
        if (mode == QemuNetworkMode.TAP && tapDevice.isNullOrBlank()) {
            return false
        }
        return portForwards.all { it.validate() }
    }

    /**
     * 转换为QEMU命令行参数
     */
    fun toQemuArgs(): List<String> {
        val args = mutableListOf<String>()

        when (mode) {
            QemuNetworkMode.NONE -> {
                args.add("-net")
                args.add("none")
            }
            QemuNetworkMode.USER -> {
                val netOptions = mutableListOf<String>()
                netOptions.add("user")

                portForwards.forEach { pf ->
                    netOptions.add(pf.toQemuArg())
                }

                hostname?.let { netOptions.add("hostname=$it") }
                dns?.let { netOptions.add("dns=$it") }

                args.add("-netdev")
                args.add(netOptions.joinToString(","))

                val deviceOptions = mutableListOf<String>()
                deviceOptions.add(device)
                macAddress?.let { deviceOptions.add("mac=$it") }
                deviceOptions.add("netdev=user.0")

                args.add("-device")
                args.add(deviceOptions.joinToString(","))
            }
            QemuNetworkMode.BRIDGE -> {
                val netOptions = mutableListOf<String>()
                netOptions.add("bridge")
                netOptions.add("br=$bridgeName")

                args.add("-netdev")
                args.add(netOptions.joinToString(","))

                val deviceOptions = mutableListOf<String>()
                deviceOptions.add(device)
                macAddress?.let { deviceOptions.add("mac=$it") }
                deviceOptions.add("netdev=bridge.0")

                args.add("-device")
                args.add(deviceOptions.joinToString(","))
            }
            QemuNetworkMode.TAP -> {
                val netOptions = mutableListOf<String>()
                netOptions.add("tap")
                netOptions.add("ifname=$tapDevice")

                args.add("-netdev")
                args.add(netOptions.joinToString(","))

                val deviceOptions = mutableListOf<String>()
                deviceOptions.add(device)
                macAddress?.let { deviceOptions.add("mac=$it") }
                deviceOptions.add("netdev=tap.0")

                args.add("-device")
                args.add(deviceOptions.joinToString(","))
            }
            QemuNetworkMode.SOCKET -> {
                args.add("-netdev")
                args.add("socket,id=net0")
                args.add("-device")
                args.add("$device,netdev=net0")
            }
            QemuNetworkMode.VDE -> {
                args.add("-netdev")
                args.add("vde,id=net0")
                args.add("-device")
                args.add("$device,netdev=net0")
            }
        }

        return args
    }
}

/**
 * QEMU显示配置
 *
 * 定义QEMU虚拟机的显示配置参数
 *
 * @property type 显示类型
 * @property vncDisplay VNC显示号（VNC模式使用）
 * @property vncPassword VNC密码（可选）
 * @property spicePort SPICE端口（SPICE模式使用）
 * @property spicePassword SPICE密码（可选）
 * @property enableGl 是否启用OpenGL
 * @property glRenderer OpenGL渲染器
 */
data class QemuDisplayConfig(
    val type: QemuDisplayType = QemuDisplayType.NONE,
    val vncDisplay: Int = 0,
    val vncPassword: String? = null,
    val spicePort: Int = 5900,
    val spicePassword: String? = null,
    val enableGl: Boolean = false,
    val glRenderer: String? = null
) {
    /**
     * 验证显示配置
     */
    fun validate(): Boolean {
        return when (type) {
            QemuDisplayType.VNC -> vncDisplay >= 0
            QemuDisplayType.SPICE -> spicePort in 1..65535
            else -> true
        }
    }

    /**
     * 转换为QEMU命令行参数
     */
    fun toQemuArgs(): List<String> {
        val args = mutableListOf<String>()

        when (type) {
            QemuDisplayType.NONE -> {
                args.add("-display")
                args.add("none")
            }
            QemuDisplayType.VNC -> {
                val vncArg = buildString {
                    append(":$vncDisplay")
                    vncPassword?.let { append(",password=on") }
                }
                args.add("-vnc")
                args.add(vncArg)
            }
            QemuDisplayType.SPICE -> {
                args.add("-spice")
                val spiceArg = buildString {
                    append("port=$spicePort")
                    append(",addr=0.0.0.0")
                    append(",disable-ticketing=${spicePassword == null}")
                    spicePassword?.let { append(",password=on") }
                }
                args.add(spiceArg)
                args.add("-device")
                args.add("qxl-vga")
            }
            QemuDisplayType.SDL -> {
                args.add("-display")
                args.add("sdl")
                if (enableGl) {
                    args.add("-device")
                    args.add("virtio-gpu-pci,gl=on")
                }
            }
            QemuDisplayType.GTK -> {
                args.add("-display")
                args.add("gtk")
                if (enableGl) {
                    args.add("-device")
                    args.add("virtio-gpu-pci,gl=on")
                }
            }
            QemuDisplayType.COCOA -> {
                args.add("-display")
                args.add("cocoa")
            }
            QemuDisplayType.EGL_HEADLESS -> {
                args.add("-display")
                args.add("egl-headless")
                args.add("-device")
                args.add("virtio-gpu-pci,gl=on")
            }
            QemuDisplayType.DEFAULT -> {
                // 使用默认显示
            }
        }

        return args
    }
}

/**
 * QEMU串口配置
 *
 * 定义QEMU虚拟机的串口配置参数
 *
 * @property enabled 是否启用串口
 * @property mode 串口模式
 * @property numPorts 串口数量
 */
data class QemuSerialConfig(
    val enabled: Boolean = true,
    val mode: QemuSerialMode = QemuSerialMode.Stdio,
    val numPorts: Int = 1
) {
    /**
     * 验证串口配置
     */
    fun validate(): Boolean {
        return numPorts in 1..4
    }

    /**
     * 转换为QEMU命令行参数
     */
    fun toQemuArgs(): List<String> {
        val args = mutableListOf<String>()

        if (!enabled) {
            return args
        }

        for (i in 0 until numPorts) {
            args.add("-serial")
            args.add(mode.toQemuArg())
        }

        return args
    }
}

/**
 * QEMU音频配置
 *
 * 定义QEMU虚拟机的音频配置参数
 *
 * @property enabled 是否启用音频
 * @property backend 音频后端
 * @property deviceId 音频设备ID
 */
data class QemuAudioConfig(
    val enabled: Boolean = false,
    val backend: QemuAudioBackend = QemuAudioBackend.NONE,
    val deviceId: String = "intel-hda"
) {
    /**
     * 转换为QEMU命令行参数
     */
    fun toQemuArgs(): List<String> {
        val args = mutableListOf<String>()

        if (!enabled || backend == QemuAudioBackend.NONE) {
            args.add("-audiodev")
            args.add("none,id=audio0")
            return args
        }

        args.add("-audiodev")
        args.add("${backend.toEnvVar()},id=audio0")

        when (deviceId) {
            "intel-hda" -> {
                args.add("-device")
                args.add("intel-hda")
                args.add("-device")
                args.add("hda-duplex,audiodev=audio0")
            }
            "ac97" -> {
                args.add("-device")
                args.add("AC97,audiodev=audio0")
            }
        }

        return args
    }
}

/**
 * QEMU虚拟机配置
 *
 * 定义QEMU虚拟机的完整配置参数
 *
 * @property id 虚拟机唯一标识符
 * @property name 虚拟机名称
 * @property targetArch 目标架构
 * @property accelerator 加速器类型
 * @property machineType 机器类型
 * @property cpuModel CPU型号
 * @property memoryMb 内存大小（MB）
 * @property cpuCores CPU核心数
 * @property disks 磁盘配置列表
 * @property network 网络配置
 * @property display 显示配置
 * @property serial 串口配置
 * @property audio 音频配置
 * @property bootImage 启动镜像路径
 * @property kernelImage 内核镜像路径
 * @property initrdImage initrd镜像路径
 * @property kernelArgs 内核启动参数
 * @property biosFile BIOS文件路径
 * @property enableKvm 是否启用KVM
 * @property enableUsb 是否启用USB
 * @property enableGpu 是否启用GPU
 * @property monitorPath QEMU监控套接字路径
 * @property qemuPath QEMU可执行文件路径
 * @property extraArgs 额外的QEMU参数
 */
data class QemuConfig(
    val id: String,
    val name: String,
    val targetArch: QemuTargetArch = QemuTargetArch.X86_64,
    val accelerator: QemuAccelerator = QemuAccelerator.TCG,
    val machineType: QemuMachineType = QemuMachineType.PC,
    val cpuModel: QemuCpuModel = QemuCpuModel.QEMU64,
    val memoryMb: Int = 2048,
    val cpuCores: Int = 2,
    val disks: List<QemuDiskConfig> = emptyList(),
    val network: QemuNetworkConfig = QemuNetworkConfig(),
    val display: QemuDisplayConfig = QemuDisplayConfig(),
    val serial: QemuSerialConfig = QemuSerialConfig(),
    val audio: QemuAudioConfig = QemuAudioConfig(),
    val bootImage: String? = null,
    val kernelImage: String? = null,
    val initrdImage: String? = null,
    val kernelArgs: String? = null,
    val biosFile: String? = null,
    val enableKvm: Boolean = false,
    val enableUsb: Boolean = true,
    val enableGpu: Boolean = false,
    val monitorPath: String? = null,
    val qemuPath: String? = null,
    val workingDirectory: String? = null,
    val extraArgs: List<String> = emptyList()
) {
    /**
     * 验证配置有效性
     */
    fun validate(): Boolean {
        if (id.isEmpty() || name.isEmpty()) {
            return false
        }
        if (memoryMb <= 0 || cpuCores <= 0) {
            return false
        }
        if (!disks.all { it.validate() }) {
            return false
        }
        if (!network.validate()) {
            return false
        }
        if (!display.validate()) {
            return false
        }
        if (!serial.validate()) {
            return false
        }
        return true
    }

    /**
     * 获取QEMU可执行文件路径
     */
    fun getQemuExecutable(): String {
        return qemuPath ?: targetArch.getSystemCommand()
    }

    companion object {
        /**
         * 创建默认配置
         */
        fun default(id: String, name: String): QemuConfig {
            return QemuConfig(
                id = id,
                name = name,
                targetArch = QemuTargetArch.X86_64,
                accelerator = QemuAccelerator.TCG,
                machineType = QemuMachineType.PC,
                cpuModel = QemuCpuModel.QEMU64,
                memoryMb = 2048,
                cpuCores = 2
            )
        }
    }

    /**
     * Builder类用于构建QemuConfig
     */
    class Builder {
        private var id: String = ""
        private var name: String = ""
        private var targetArch: QemuTargetArch = QemuTargetArch.X86_64
        private var accelerator: QemuAccelerator = QemuAccelerator.TCG
        private var machineType: QemuMachineType = QemuMachineType.PC
        private var cpuModel: QemuCpuModel = QemuCpuModel.QEMU64
        private var memoryMb: Int = 2048
        private var cpuCores: Int = 2
        private val disks: MutableList<QemuDiskConfig> = mutableListOf()
        private var network: QemuNetworkConfig = QemuNetworkConfig()
        private var display: QemuDisplayConfig = QemuDisplayConfig()
        private var serial: QemuSerialConfig = QemuSerialConfig()
        private var audio: QemuAudioConfig = QemuAudioConfig()
        private var bootImage: String? = null
        private var kernelImage: String? = null
        private var initrdImage: String? = null
        private var kernelArgs: String? = null
        private var biosFile: String? = null
        private var enableKvm: Boolean = false
        private var enableUsb: Boolean = true
        private var enableGpu: Boolean = false
        private var monitorPath: String? = null
        private var qemuPath: String? = null
        private val extraArgs: MutableList<String> = mutableListOf()

        fun id(id: String) = apply { this.id = id }
        fun name(name: String) = apply { this.name = name }
        fun targetArch(targetArch: QemuTargetArch) = apply { this.targetArch = targetArch }
        fun accelerator(accelerator: QemuAccelerator) = apply { this.accelerator = accelerator }
        fun machineType(machineType: QemuMachineType) = apply { this.machineType = machineType }
        fun cpuModel(cpuModel: QemuCpuModel) = apply { this.cpuModel = cpuModel }
        fun memoryMb(memoryMb: Int) = apply { this.memoryMb = memoryMb }
        fun cpuCores(cpuCores: Int) = apply { this.cpuCores = cpuCores }

        fun addDisk(disk: QemuDiskConfig) = apply { disks.add(disk) }
        fun disks(disks: List<QemuDiskConfig>) = apply { this.disks.clear(); this.disks.addAll(disks) }

        fun network(network: QemuNetworkConfig) = apply { this.network = network }
        fun display(display: QemuDisplayConfig) = apply { this.display = display }
        fun serial(serial: QemuSerialConfig) = apply { this.serial = serial }
        fun audio(audio: QemuAudioConfig) = apply { this.audio = audio }

        fun bootImage(bootImage: String?) = apply { this.bootImage = bootImage }
        fun kernelImage(kernelImage: String?) = apply { this.kernelImage = kernelImage }
        fun initrdImage(initrdImage: String?) = apply { this.initrdImage = initrdImage }
        fun kernelArgs(kernelArgs: String?) = apply { this.kernelArgs = kernelArgs }
        fun biosFile(biosFile: String?) = apply { this.biosFile = biosFile }

        fun enableKvm(enableKvm: Boolean) = apply { this.enableKvm = enableKvm }
        fun enableUsb(enableUsb: Boolean) = apply { this.enableUsb = enableUsb }
        fun enableGpu(enableGpu: Boolean) = apply { this.enableGpu = enableGpu }

        fun monitorPath(monitorPath: String?) = apply { this.monitorPath = monitorPath }
        fun qemuPath(qemuPath: String?) = apply { this.qemuPath = qemuPath }

        fun addExtraArg(arg: String) = apply { extraArgs.add(arg) }
        fun extraArgs(args: List<String>) = apply { this.extraArgs.clear(); this.extraArgs.addAll(args) }

        /**
         * 构建QemuConfig实例
         *
         * @throws IllegalStateException 如果必需字段未设置
         */
        fun build(): QemuConfig {
            require(id.isNotEmpty()) { "VM id is required" }
            require(name.isNotEmpty()) { "VM name is required" }
            require(memoryMb > 0) { "Memory must be positive" }
            require(cpuCores > 0) { "CPU cores must be positive" }

            return QemuConfig(
                id = id,
                name = name,
                targetArch = targetArch,
                accelerator = accelerator,
                machineType = machineType,
                cpuModel = cpuModel,
                memoryMb = memoryMb,
                cpuCores = cpuCores,
                disks = disks.toList(),
                network = network,
                display = display,
                serial = serial,
                audio = audio,
                bootImage = bootImage,
                kernelImage = kernelImage,
                initrdImage = initrdImage,
                kernelArgs = kernelArgs,
                biosFile = biosFile,
                enableKvm = enableKvm,
                enableUsb = enableUsb,
                enableGpu = enableGpu,
                monitorPath = monitorPath,
                qemuPath = qemuPath,
                extraArgs = extraArgs.toList()
            )
        }
    }

    /**
     * 创建Builder实例
     */
    fun toBuilder(): Builder {
        return Builder()
            .id(id)
            .name(name)
            .targetArch(targetArch)
            .accelerator(accelerator)
            .machineType(machineType)
            .cpuModel(cpuModel)
            .memoryMb(memoryMb)
            .cpuCores(cpuCores)
            .disks(disks)
            .network(network)
            .display(display)
            .serial(serial)
            .audio(audio)
            .bootImage(bootImage)
            .kernelImage(kernelImage)
            .initrdImage(initrdImage)
            .kernelArgs(kernelArgs)
            .biosFile(biosFile)
            .enableKvm(enableKvm)
            .enableUsb(enableUsb)
            .enableGpu(enableGpu)
            .monitorPath(monitorPath)
            .qemuPath(qemuPath)
            .extraArgs(extraArgs)
    }
}

/**
 * 扩展函数：创建QemuConfig Builder
 */
fun qemuConfig(block: QemuConfig.Builder.() -> Unit): QemuConfig {
    return QemuConfig.Builder().apply(block).build()
}