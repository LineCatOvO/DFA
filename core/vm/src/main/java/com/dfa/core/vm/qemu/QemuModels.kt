package com.dfa.core.vm.qemu

/**
 * QEMU加速器类型枚举
 *
 * 定义QEMU支持的不同硬件加速器类型
 */
enum class QemuAccelerator {
    /** TCG (Tiny Code Generator) - 纯软件模拟，无硬件加速 */
    TCG,
    /** KVM (Kernel-based Virtual Machine) - Linux内核虚拟化 */
    KVM,
    /** HVF (Hypervisor Framework) - macOS虚拟化框架 */
    HVF,
    /** WHPX (Windows Hypervisor Platform) - Windows虚拟化平台 */
    WHPX,
    /** HAX (Hardware Accelerated Execution) - Intel硬件加速执行 */
    HAX;

    /**
     * 获取加速器的QEMU命令行参数
     */
    fun toQemuArg(): String = when (this) {
        TCG -> "tcg"
        KVM -> "kvm"
        HVF -> "hvf"
        WHPX -> "whpx"
        HAX -> "hax"
    }

    /**
     * 检查加速器是否需要特定平台
     */
    val requiredPlatform: String? get() = when (this) {
        KVM -> "Linux"
        HVF -> "macOS"
        WHPX -> "Windows"
        HAX -> "Windows/macOS"
        TCG -> null // 跨平台
    }
}

/**
 * QEMU机器类型枚举
 *
 * 定义QEMU支持的不同机器类型
 */
enum class QemuMachineType {
    // x86架构
    /** 标准PC (i440FX芯片组) */
    PC("pc"),
    /** Q35芯片组PC */
    Q35("q35"),
    /** 微型机器 */
    MICROVM("microvm"),
    /** ISA PC */
    ISAPC("isapc"),

    // ARM架构
    /** ARM虚拟机器 */
    VIRT("virt"),
    /** ARM快速模型 */
    VIRT_2_10("virt-2.10"),
    /** ARM快速模型 2.11 */
    VIRT_2_11("virt-2.11"),
    /** ARM快速模型 2.12 */
    VIRT_2_12("virt-2.12"),
    /** ARM快速模型 3.0 */
    VIRT_3_0("virt-3.0"),
    /** ARM快速模型 4.0 */
    VIRT_4_0("virt-4.0"),
    /** Raspberry Pi 2 */
    RASPI2("raspi2"),
    /** Raspberry Pi 3 */
    RASPI3("raspi3"),

    // RISC-V架构
    /** RISC-V虚拟机器 */
    RISCV_VIRT("virt"),

    // 其他架构
    /** 默认机器类型 */
    DEFAULT("none");

    /** 机器类型标识符 */
    val machineName: String

    constructor(machineName: String) {
        this.machineName = machineName
    }

    /**
     * 获取QEMU命令行参数
     */
    fun toQemuArg(): String = machineName

    /**
     * 检查是否为x86架构机器
     */
    val isX86: Boolean get() = this in listOf(PC, Q35, MICROVM, ISAPC)

    /**
     * 检查是否为ARM架构机器
     */
    val isArm: Boolean get() = this in listOf(VIRT, VIRT_2_10, VIRT_2_11, VIRT_2_12, VIRT_3_0, VIRT_4_0, RASPI2, RASPI3)
}

/**
 * QEMU CPU型号枚举
 *
 * 定义QEMU支持的不同CPU型号
 */
enum class QemuCpuModel {
    // x86_64架构
    /** QEMU默认64位CPU */
    QEMU64("qemu64"),
    /** QEMU默认32位CPU */
    QEMU32("qemu32"),
    /** 主机CPU透传 */
    HOST("host"),
    /** Intel Haswell */
    HASWELL("Haswell"),
    /** Intel Broadwell */
    BROADWELL("Broadwell"),
    /** Intel Skylake */
    SKYLAKE("Skylake"),
    /** Intel Cascadelake-Server */
    CASCADLAKE_SERVER("Cascadelake-Server"),
    /** AMD EPYC */
    EPYC("EPYC"),
    /** AMD EPYC-Rome */
    EPYC_ROME("EPYC-Rome"),
    /** 486 CPU */
    I486("486"),
    /** Pentium */
    PENTIUM("pentium"),
    /** Pentium 2 */
    PENTIUM2("pentium2"),
    /** Pentium 3 */
    PENTIUM3("pentium3"),

    // ARM架构
    /** ARM Cortex-A57 */
    CORTEX_A57("cortex-a57"),
    /** ARM Cortex-A53 */
    CORTEX_A53("cortex-a53"),
    /** ARM Cortex-A72 */
    CORTEX_A72("cortex-a72"),
    /** ARM Cortex-A15 */
    CORTEX_A15("cortex-a15"),

    // RISC-V架构
    /** RISC-V RV64 */
    RV64("rv64"),
    /** RISC-V RV32 */
    RV32("rv32"),

    // 默认
    /** 默认CPU */
    DEFAULT("default");

    /** CPU型号标识符 */
    val cpuName: String

    constructor(cpuName: String) {
        this.cpuName = cpuName
    }

    /**
     * 获取QEMU命令行参数
     */
    fun toQemuArg(): String = cpuName

    /**
     * 检查是否为x86架构CPU
     */
    val isX86: Boolean get() = this in listOf(
        QEMU64, QEMU32, HOST, HASWELL, BROADWELL, SKYLAKE,
        CASCADLAKE_SERVER, EPYC, EPYC_ROME, I486, PENTIUM, PENTIUM2, PENTIUM3
    )

    /**
     * 检查是否为ARM架构CPU
     */
    val isArm: Boolean get() = this in listOf(
        CORTEX_A57, CORTEX_A53, CORTEX_A72, CORTEX_A15
    )
}

/**
 * QEMU磁盘格式枚举
 *
 * 定义QEMU支持的不同磁盘镜像格式
 */
enum class QemuDiskFormat {
    /** QEMU Copy-On-Write格式 - 支持快照、压缩 */
    QCOW2("qcow2"),
    /** 原始格式 - 直接映射，性能最佳 */
    RAW("raw"),
    /** VirtualBox磁盘格式 */
    VDI("vdi"),
    /** VMware磁盘格式 */
    VMDK("vmdk"),
    /** VHD/VHDX格式 */
    VHD("vhd"),
    /** VHDX格式 */
    VHDX("vhdx"),
    /** QEMU Copy-On-Write版本1 */
    QCOW("qcow"),
    /** Bochs磁盘格式 */
    BOCHS("bochs"),
    /** COW格式 */
    COW("cow"),
    /** VPC格式 */
    VPC("vpc");

    /** 格式标识符 */
    val formatName: String

    constructor(formatName: String) {
        this.formatName = formatName
    }

    /**
     * 获取QEMU命令行参数
     */
    fun toQemuArg(): String = formatName

    /**
     * 检查是否支持快照
     */
    val supportsSnapshots: Boolean get() = this in listOf(QCOW2, QCOW)

    /**
     * 检查是否支持压缩
     */
    val supportsCompression: Boolean get() = this == QCOW2

    /**
     * 检查是否为稀疏格式
     */
    val isSparse: Boolean get() = this in listOf(QCOW2, QCOW, VDI, VMDK, VHD, VHDX)
}

/**
 * QEMU网络模式枚举
 *
 * 定义QEMU支持的不同网络配置模式
 */
enum class QemuNetworkMode {
    /** 用户模式网络 - NAT方式，无需root权限 */
    USER("user"),
    /** 桥接模式 - 直接连接物理网络 */
    BRIDGE("bridge"),
    /** Socket模式 - 通过socket连接 */
    SOCKET("socket"),
    /** TAP设备模式 - 高性能网络 */
    TAP("tap"),
    /** VDE虚拟网络 */
    VDE("vde"),
    /** 无网络 */
    NONE("none");

    /** 模式标识符 */
    val modeName: String

    constructor(modeName: String) {
        this.modeName = modeName
    }

    /**
     * 获取QEMU命令行参数
     */
    fun toQemuArg(): String = modeName

    /**
     * 检查是否需要root权限
     */
    val requiresRoot: Boolean get() = this in listOf(BRIDGE, TAP)

    /**
     * 检查是否支持端口转发
     */
    val supportsPortForwarding: Boolean get() = this == USER
}

/**
 * QEMU显示类型枚举
 *
 * 定义QEMU支持的不同显示输出方式
 */
enum class QemuDisplayType {
    /** 无显示 - 无头模式 */
    NONE("none"),
    /** VNC远程显示 */
    VNC("vnc"),
    /** SPICE远程显示 */
    SPICE("spice"),
    /** SDL窗口显示 */
    SDL("sdl"),
    /** GTK窗口显示 */
    GTK("gtk"),
    /** Cocoa窗口显示 (macOS) */
    COCOA("cocoa"),
    /** EGL无头渲染 */
    EGL_HEADLESS("egl-headless"),
    /** 默认显示 */
    DEFAULT("default");

    /** 类型标识符 */
    val typeName: String

    constructor(typeName: String) {
        this.typeName = typeName
    }

    /**
     * 获取QEMU命令行参数
     */
    fun toQemuArg(): String = typeName

    /**
     * 检查是否为远程显示
     */
    val isRemote: Boolean get() = this in listOf(VNC, SPICE)

    /**
     * 检查是否为本地显示
     */
    val isLocal: Boolean get() = this in listOf(SDL, GTK, COCOA)

    /**
     * 检查是否为无头模式
     */
    val isHeadless: Boolean get() = this in listOf(NONE, EGL_HEADLESS)
}

/**
 * QEMU串口模式 sealed class
 *
 * 定义QEMU串口的不同输出模式
 */
sealed class QemuSerialMode {
    /**
     * 标准输入输出模式
     */
    data object Stdio : QemuSerialMode() {
        override fun toQemuArg(): String = "stdio"
    }

    /**
     * 文件输出模式
     *
     * @property path 输出文件路径
     */
    data class File(val path: String) : QemuSerialMode() {
        override fun toQemuArg(): String = "file:$path"
    }

    /**
     * Unix Socket模式
     *
     * @property path Socket路径
     * @property server 是否作为服务器
     * @property wait 是否等待连接
     */
    data class UnixSocket(
        val path: String,
        val server: Boolean = true,
        val wait: Boolean = false
    ) : QemuSerialMode() {
        override fun toQemuArg(): String = buildString {
            append("unix:$path")
            if (server) append(",server=on")
            if (wait) append(",wait=on") else append(",wait=off")
        }
    }

    /**
     * TCP Socket模式
     *
     * @property host 主机地址
     * @property port 端口号
     * @property server 是否作为服务器
     * @property wait 是否等待连接
     */
    data class TcpSocket(
        val host: String = "0.0.0.0",
        val port: Int,
        val server: Boolean = true,
        val wait: Boolean = false
    ) : QemuSerialMode() {
        override fun toQemuArg(): String = buildString {
            append("tcp:$host:$port")
            if (server) append(",server=on")
            if (wait) append(",wait=on") else append(",wait=off")
        }
    }

    /**
     * PTY模式
     */
    data object Pty : QemuSerialMode() {
        override fun toQemuArg(): String = "pty"
    }

    /**
     * 禁用串口
     */
    data object Disabled : QemuSerialMode() {
        override fun toQemuArg(): String = "none"
    }

    /**
     * 获取QEMU命令行参数
     */
    abstract fun toQemuArg(): String
}

/**
 * QEMU总线类型枚举
 *
 * 定义QEMU支持的不同设备总线类型
 */
enum class QemuBusType {
    /** PCI总线 */
    PCI,
    /** PCIe总线 */
    PCIE,
    /** ISA总线 */
    ISA,
    /** USB总线 */
    USB,
    /** Virtio总线 */
    VIRTIO,
    /** SCSI总线 */
    SCSI,
    /** IDE总线 */
    IDE,
    /** SD总线 */
    SD
}

/**
 * QEMU音频后端枚举
 *
 * 定义QEMU支持的不同音频后端
 */
enum class QemuAudioBackend {
    /** 无音频 */
    NONE,
    /** PulseAudio */
    PA,
    /** ALSA */
    ALSA,
    /** OSS */
    OSS,
    /** CoreAudio (macOS) */
    COREAUDIO,
    /** DirectSound (Windows) */
    DSOUND,
    /** SDL音频 */
    SDL,
    /** JACK */
    JACK,
    /** PipeWire */
    PIPEWIRE;

    /**
     * 获取QEMU环境变量名
     */
    fun toEnvVar(): String = when (this) {
        NONE -> "none"
        PA -> "pa"
        ALSA -> "alsa"
        OSS -> "oss"
        COREAUDIO -> "coreaudio"
        DSOUND -> "dsound"
        SDL -> "sdl"
        JACK -> "jack"
        PIPEWIRE -> "pipewire"
    }
}

/**
 * QEMU目标架构枚举
 *
 * 定义QEMU支持的不同目标架构
 */
enum class QemuTargetArch(
    /** 架构标识符 */
    val archName: String,
    /** QEMU系统模拟器名称 */
    val systemEmulator: String,
    /** QEMU用户模拟器名称 */
    val userEmulator: String
) {
    /** x86_64架构 */
    X86_64("x86_64", "qemu-system-x86_64", "qemu-x86_64"),
    /** i386架构 */
    I386("i386", "qemu-system-i386", "qemu-i386"),
    /** ARM 64位架构 */
    AARCH64("aarch64", "qemu-system-aarch64", "qemu-aarch64"),
    /** ARM 32位架构 */
    ARM("arm", "qemu-system-arm", "qemu-arm"),
    /** RISC-V 64位架构 */
    RISCV64("riscv64", "qemu-system-riscv64", "qemu-riscv64"),
    /** RISC-V 32位架构 */
    RISCV32("riscv32", "qemu-system-riscv32", "qemu-riscv32"),
    /** MIPS架构 */
    MIPS("mips", "qemu-system-mips", "qemu-mips"),
    /** MIPS64架构 */
    MIPS64("mips64", "qemu-system-mips64", "qemu-mips64"),
    /** PowerPC架构 */
    PPC("ppc", "qemu-system-ppc", "qemu-ppc"),
    /** PowerPC 64位架构 */
    PPC64("ppc64", "qemu-system-ppc64", "qemu-ppc64"),
    /** SPARC架构 */
    SPARC("sparc", "qemu-system-sparc", "qemu-sparc"),
    /** SPARC 64位架构 */
    SPARC64("sparc64", "qemu-system-sparc64", "qemu-sparc64");

    /**
     * 获取系统模拟器命令
     */
    fun getSystemCommand(): String = systemEmulator

    /**
     * 获取用户模拟器命令
     */
    fun getUserCommand(): String = userEmulator

    /**
     * 检查是否为64位架构
     */
    val is64Bit: Boolean get() = this in listOf(X86_64, AARCH64, RISCV64, MIPS64, PPC64, SPARC64)
}