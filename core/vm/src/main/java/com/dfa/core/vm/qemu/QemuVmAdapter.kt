package com.dfa.core.vm.qemu

import com.dfa.core.vm.VmAdapter
import com.dfa.core.vm.VmBackendType
import com.dfa.core.vm.VmCallback
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmHandle

/**
 * QEMU虚拟机适配器接口
 *
 * 提供与QEMU虚拟机交互的抽象接口，继承通用VmAdapter接口
 * 支持QEMU特定的功能，如快照管理、虚拟机迁移、QEMU监控等
 */
interface QemuVmAdapter : VmAdapter {

    // ==================== VmAdapter 基础实现 ====================

    /**
     * 获取后端类型
     *
     * @return 始终返回 VmBackendType.QEMU
     */
    override val backendType: VmBackendType
        get() = VmBackendType.QEMU

    // ==================== QEMU 特定方法 ====================

    /**
     * 检查QEMU是否可用
     *
     * 检查系统中是否安装了QEMU，以及是否可以正常执行
     *
     * @return QEMU是否可用
     */
    suspend fun isQemuAvailable(): Boolean

    /**
     * 获取QEMU版本
     *
     * 获取已安装的QEMU版本信息
     *
     * @return 版本字符串，失败时返回错误信息
     */
    suspend fun getQemuVersion(): Result<String>

    /**
     * 获取支持的架构列表
     *
     * 获取当前系统上QEMU支持的目标架构
     *
     * @return 支持的架构列表
     */
    suspend fun getSupportedArchitectures(): List<QemuTargetArch>

    /**
     * 创建虚拟机快照
     *
     * 为指定虚拟机创建一个快照，需要磁盘格式支持快照（如qcow2）
     *
     * @param handle 虚拟机句柄
     * @param name 快照名称
     * @return 创建结果
     */
    suspend fun createSnapshot(handle: VmHandle, name: String): Result<Unit>

    /**
     * 恢复虚拟机快照
     *
     * 将虚拟机恢复到指定快照状态
     *
     * @param handle 虚拟机句柄
     * @param name 快照名称
     * @return 恢复结果
     */
    suspend fun restoreSnapshot(handle: VmHandle, name: String): Result<Unit>

    /**
     * 删除虚拟机快照
     *
     * 删除指定的虚拟机快照
     *
     * @param handle 虚拟机句柄
     * @param name 快照名称
     * @return 删除结果
     */
    suspend fun deleteSnapshot(handle: VmHandle, name: String): Result<Unit>

    /**
     * 列出虚拟机快照
     *
     * 获取指定虚拟机的所有快照列表
     *
     * @param handle 虚拟机句柄
     * @return 快照名称列表
     */
    suspend fun listSnapshots(handle: VmHandle): Result<List<String>>

    /**
     * 迁移虚拟机
     *
     * 将运行中的虚拟机迁移到目标主机
     *
     * @param handle 虚拟机句柄
     * @param targetUri 目标URI（如：tcp://target-host:4444）
     * @param liveMigration 是否执行实时迁移（默认true）
     * @return 迁移结果
     */
    suspend fun migrateVm(
        handle: VmHandle,
        targetUri: String,
        liveMigration: Boolean = true
    ): Result<Unit>

    /**
     * 获取QEMU监控接口
     *
     * 获取与QEMU Monitor Protocol (QMP)交互的接口
     *
     * @param handle 虚拟机句柄
     * @return QEMU监控接口
     */
    suspend fun getQemuMonitor(handle: VmHandle): Result<QemuMonitor>

    /**
     * 获取QEMU进程信息
     *
     * 获取QEMU进程的详细信息，包括PID、内存使用、CPU使用等
     *
     * @param handle 虚拟机句柄
     * @return 进程信息
     */
    suspend fun getQemuProcessInfo(handle: VmHandle): Result<QemuProcessInfo>

    /**
     * 设置虚拟机CPU数量
     *
     * 热插拔CPU核心（如果支持）
     *
     * @param handle 虚拟机句柄
     * @param cpuCount 目标CPU核心数
     * @return 设置结果
     */
    suspend fun setCpuCount(handle: VmHandle, cpuCount: Int): Result<Unit>

    /**
     * 设置虚拟机内存大小
     *
     * 热插拔内存（如果支持）
     *
     * @param handle 虚拟机句柄
     * @param memoryMb 目标内存大小（MB）
     * @return 设置结果
     */
    suspend fun setMemorySize(handle: VmHandle, memoryMb: Int): Result<Unit>

    /**
     * 获取虚拟机截图
     *
     * 获取虚拟机当前显示的截图
     *
     * @param handle 虚拟机句柄
     * @param format 图像格式（如：png、ppm）
     * @return 截图数据
     */
    suspend fun takeScreenshot(handle: VmHandle, format: String = "png"): Result<ByteArray>

    /**
     * 发送按键事件
     *
     * 向虚拟机发送按键事件
     *
     * @param handle 虚拟机句柄
     * @param keys 按键码列表
     * @return 发送结果
     */
    suspend fun sendKeys(handle: VmHandle, keys: List<Int>): Result<Unit>

    /**
     * 发送鼠标事件
     *
     * 向虚拟机发送鼠标移动或点击事件
     *
     * @param handle 虚拟机句柄
     * @param x X坐标
     * @param y Y坐标
     * @param buttons 按钮状态（1=左键，2=右键，4=中键）
     * @return 发送结果
     */
    suspend fun sendMouseEvent(
        handle: VmHandle,
        x: Int,
        y: Int,
        buttons: Int = 0
    ): Result<Unit>

    /**
     * 检查KVM是否可用
     *
     * 检查系统是否支持KVM硬件加速
     *
     * @return KVM是否可用
     */
    suspend fun isKvmAvailable(): Boolean

    /**
     * 获取支持的加速器列表
     *
     * 获取当前系统上QEMU支持的硬件加速器
     *
     * @return 支持的加速器列表
     */
    suspend fun getSupportedAccelerators(): List<QemuAccelerator>

    /**
     * 创建磁盘镜像
     *
     * 创建一个新的QEMU磁盘镜像文件
     *
     * @param path 镜像文件路径
     * @param format 磁盘格式
     * @param sizeGb 大小（GB）
     * @return 创建结果
     */
    suspend fun createDiskImage(
        path: String,
        format: QemuDiskFormat = QemuDiskFormat.QCOW2,
        sizeGb: Int
    ): Result<Unit>

    /**
     * 转换磁盘镜像格式
     *
     * 将磁盘镜像从一种格式转换为另一种格式
     *
     * @param sourcePath 源镜像路径
     * @param targetPath 目标镜像路径
     * @param targetFormat 目标格式
     * @return 转换结果
     */
    suspend fun convertDiskImage(
        sourcePath: String,
        targetPath: String,
        targetFormat: QemuDiskFormat
    ): Result<Unit>

    /**
     * 获取磁盘镜像信息
     *
     * 获取磁盘镜像文件的详细信息
     *
     * @param path 镜像文件路径
     * @return 镜像信息
     */
    suspend fun getDiskImageInfo(path: String): Result<QemuDiskImageInfo>
}

/**
 * QEMU进程信息
 *
 * 包含QEMU进程的运行时信息
 *
 * @property pid 进程ID
 * @property memoryUsageMb 内存使用量（MB）
 * @property cpuUsagePercent CPU使用率（百分比）
 * @property uptimeSeconds 运行时间（秒）
 * @property threads 线程数
 * @property fileDescriptors 打开的文件描述符数
 * @property networkBytesReceived 接收的网络字节数
 * @property networkBytesSent 发送的网络字节数
 * @property diskReadBytes 磁盘读取字节数
 * @property diskWriteBytes 磁盘写入字节数
 */
data class QemuProcessInfo(
    val pid: Int,
    val memoryUsageMb: Long,
    val cpuUsagePercent: Double,
    val uptimeSeconds: Long,
    val threads: Int,
    val fileDescriptors: Int,
    val networkBytesReceived: Long,
    val networkBytesSent: Long,
    val diskReadBytes: Long,
    val diskWriteBytes: Long
) {
    /**
     * 格式化的运行时间
     */
    val formattedUptime: String
        get() {
            val hours = uptimeSeconds / 3600
            val minutes = (uptimeSeconds % 3600) / 60
            val seconds = uptimeSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
}

/**
 * QEMU磁盘镜像信息
 *
 * 包含磁盘镜像文件的详细信息
 *
 * @property path 镜像文件路径
 * @property format 磁盘格式
 * @property virtualSizeGb 虚拟大小（GB）
 * @property actualSizeGb 实际大小（GB）
 * @property clusterSize 簇大小（字节）
 * @property backingFile 后备文件路径（如果有）
 * @property hasSnapshots 是否有快照
 * @property snapshotCount 快照数量
 * @property encrypted 是否加密
 * @property compressed 是否压缩
 * @property createdAt 创建时间
 * @property modifiedAt 最后修改时间
 */
data class QemuDiskImageInfo(
    val path: String,
    val format: QemuDiskFormat,
    val virtualSizeGb: Double,
    val actualSizeGb: Double,
    val clusterSize: Int? = null,
    val backingFile: String? = null,
    val hasSnapshots: Boolean = false,
    val snapshotCount: Int = 0,
    val encrypted: Boolean = false,
    val compressed: Boolean = false,
    val createdAt: Long? = null,
    val modifiedAt: Long? = null
) {
    /**
     * 压缩率
     */
    val compressionRatio: Double
        get() = if (virtualSizeGb > 0) actualSizeGb / virtualSizeGb else 0.0

    /**
     * 格式化的虚拟大小
     */
    val formattedVirtualSize: String
        get() = String.format("%.2f GB", virtualSizeGb)

    /**
     * 格式化的实际大小
     */
    val formattedActualSize: String
        get() = String.format("%.2f GB", actualSizeGb)
}

/**
 * QEMU虚拟机资源信息
 *
 * 包含QEMU特定资源信息
 *
 * @property totalMemoryMb 总内存（MB）
 * @property availableMemoryMb 可用内存（MB）
 * @property totalCpuCores 总CPU核心数
 * @property availableCpuCores 可用CPU核心数
 * @property totalDiskSpaceGb 总磁盘空间（GB）
 * @property availableDiskSpaceGb 可用磁盘空间（GB）
 * @property kvmAvailable KVM是否可用
 * @property kvmEnabled KVM是否启用
 * @property supportedArchitectures 支持的架构列表
 * @property supportedAccelerators 支持的加速器列表
 * @property qemuVersion QEMU版本
 * @property qemuPath QEMU可执行文件路径
 */
data class QemuResources(
    val totalMemoryMb: Long,
    val availableMemoryMb: Long,
    val totalCpuCores: Int,
    val availableCpuCores: Int,
    val totalDiskSpaceGb: Long,
    val availableDiskSpaceGb: Long,
    val kvmAvailable: Boolean,
    val kvmEnabled: Boolean,
    val supportedArchitectures: List<QemuTargetArch>,
    val supportedAccelerators: List<QemuAccelerator>,
    val qemuVersion: String,
    val qemuPath: String
) {
    /**
     * 检查是否有足够资源
     */
    val hasEnoughResources: Boolean
        get() = availableMemoryMb > 0 && availableCpuCores > 0 && availableDiskSpaceGb > 0

    /**
     * 检查是否有足够资源运行指定配置的虚拟机
     *
     * @param config QEMU配置
     * @return 是否有足够资源
     */
    fun hasEnoughResourcesFor(config: QemuConfig): Boolean {
        return availableMemoryMb >= config.memoryMb &&
                availableCpuCores >= config.cpuCores &&
                availableDiskSpaceGb > 0
    }
}

/**
 * QEMU虚拟机信息
 *
 * 包含QEMU特定信息
 *
 * @property vmId 虚拟机ID
 * @property name 虚拟机名称
 * @property status 虚拟机状态
 * @property backendType 后端类型
 * @property config QEMU配置
 * @property qemuPid QEMU进程PID
 * @property monitorPath 监控套接字路径
 * @property vncPort VNC端口
 * @property sshPort SSH端口
 * @property uptimeSeconds 运行时间（秒）
 * @property cpuUsage CPU使用率
 * @property memoryUsageMb 内存使用量（MB）
 */
data class QemuVmInfo(
    val vmId: String,
    val name: String,
    val status: QemuVmStatus,
    val backendType: VmBackendType = VmBackendType.QEMU,
    val config: QemuConfig,
    val qemuPid: Int? = null,
    val monitorPath: String? = null,
    val vncPort: Int? = null,
    val sshPort: Int? = null,
    val uptimeSeconds: Long = 0,
    val cpuUsage: Double = 0.0,
    val memoryUsageMb: Long = 0
) {
    /**
     * 是否运行中
     */
    val isRunning: Boolean
        get() = status == QemuVmStatus.RUNNING

    /**
     * 是否已停止
     */
    val isStopped: Boolean
        get() = status == QemuVmStatus.STOPPED || status == QemuVmStatus.ERROR

    /**
     * 是否可启动
     */
    val canStart: Boolean
        get() = status == QemuVmStatus.CREATED || status == QemuVmStatus.STOPPED

    /**
     * 是否可停止
     */
    val canStop: Boolean
        get() = status == QemuVmStatus.RUNNING || status == QemuVmStatus.PAUSED

    /**
     * 是否可暂停
     */
    val canPause: Boolean
        get() = status == QemuVmStatus.RUNNING

    /**
     * 是否可恢复
     */
    val canResume: Boolean
        get() = status == QemuVmStatus.PAUSED

    /**
     * 格式化的运行时间
     */
    val formattedUptime: String
        get() {
            val hours = uptimeSeconds / 3600
            val minutes = (uptimeSeconds % 3600) / 60
            val seconds = uptimeSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
}

/**
 * QEMU虚拟机状态枚举
 */
enum class QemuVmStatus {
    /** 未创建 */
    NOT_CREATED,
    /** 已创建，未启动 */
    CREATED,
    /** 正在启动 */
    STARTING,
    /** 运行中 */
    RUNNING,
    /** 正在暂停 */
    PAUSING,
    /** 已暂停 */
    PAUSED,
    /** 正在恢复 */
    RESUMING,
    /** 正在停止 */
    STOPPING,
    /** 已停止 */
    STOPPED,
    /** 正在迁移 */
    MIGRATING,
    /** 迁移完成 */
    MIGRATED,
    /** 错误状态 */
    ERROR,
    /** 未知状态 */
    UNKNOWN;

    /**
     * 检查是否为活跃状态
     */
    val isActive: Boolean
        get() = this in listOf(RUNNING, STARTING, PAUSING, RESUMING, STOPPING, MIGRATING)

    /**
     * 检查是否为可恢复状态
     */
    val isRecoverable: Boolean
        get() = this in listOf(PAUSED, STOPPED, ERROR)

    /**
     * 检查是否为可操作状态
     */
    val isOperational: Boolean
        get() = this in listOf(RUNNING, PAUSED)
}

/**
 * QEMU适配器工厂接口
 *
 * 用于创建QemuVmAdapter实例
 */
interface QemuVmAdapterFactory {
    /**
     * 创建QEMU适配器实例
     *
     * @return QemuVmAdapter实例
     */
    fun create(): QemuVmAdapter

    /**
     * 检查QEMU后端是否可用
     *
     * @return QEMU后端是否可用
     */
    suspend fun isQemuBackendAvailable(): Boolean

    /**
     * 获取QEMU版本
     *
     * @return QEMU版本字符串
     */
    suspend fun getQemuVersion(): String?
}

/**
 * QEMU虚拟机特性枚举
 *
 * 定义QEMU支持的各种特性
 */
enum class QemuVmFeature {
    /** 快照支持 */
    SNAPSHOTS,
    /** 实时迁移 */
    LIVE_MIGRATION,
    /** CPU热插拔 */
    CPU_HOTPLUG,
    /** 内存热插拔 */
    MEMORY_HOTPLUG,
    /** 设备热插拔 */
    DEVICE_HOTPLUG,
    /** KVM加速 */
    KVM_ACCELERATION,
    /** VNC显示 */
    VNC_DISPLAY,
    /** SPICE显示 */
    SPICE_DISPLAY,
    /** QMP监控 */
    QMP_MONITOR,
    /** 串口控制台 */
    SERIAL_CONSOLE,
    /** USB透传 */
    USB_PASSTHROUGH,
    /** PCI透传 */
    PCI_PASSTHROUGH,
    /** GPU透传 */
    GPU_PASSTHROUGH,
    /** 网络桥接 */
    NETWORK_BRIDGE,
    /** 端口转发 */
    PORT_FORWARDING,
    /** 共享文件夹 */
    SHARED_FOLDERS,
    /** 剪贴板共享 */
    CLIPBOARD_SHARING,
    /** 拖放支持 */
    DRAG_AND_DROP
}