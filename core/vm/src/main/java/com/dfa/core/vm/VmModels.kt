package com.dfa.core.vm

/**
 * 虚拟机状态枚举
 */
enum class VmState {
    CREATED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR,
    PAUSED,
    RESUMING,
    MIGRATING
}

/**
 * 虚拟机事件 sealed class
 */
sealed class VmEvent {
    data class Start(val config: VmConfig) : VmEvent()
    data object Stop : VmEvent()
    data object Pause : VmEvent()
    data object Resume : VmEvent()
    data class Migrate(val targetHost: String) : VmEvent()
    data class Error(val error: VmError) : VmEvent()
    data object Reset : VmEvent()
}

/**
 * 虚拟机错误 sealed class
 */
sealed class VmError : Throwable() {
    data class ConfigurationError(override val message: String) : VmError()
    data class ResourceError(override val message: String) : VmError()
    data class NetworkError(override val message: String) : VmError()
    data class PermissionError(override val message: String) : VmError()
    data class TimeoutError(override val message: String) : VmError()
    data class UnknownError(override val message: String, override val cause: Throwable? = null) : VmError()
}

/**
 * 虚拟机资源配置
 */
data class VmResourceConfig(
    val memoryMb: Int = 2048,
    val cpuCores: Int = 2,
    val diskSizeGb: Int = 10,
    val networkBandwidthMbps: Int = 100,
    val gpuEnabled: Boolean = false,
    val gpuMemoryMb: Int = 0
) {
    fun validate(): Boolean {
        return memoryMb > 0 &&
                cpuCores > 0 &&
                diskSizeGb > 0 &&
                networkBandwidthMbps > 0 &&
                (!gpuEnabled || gpuMemoryMb > 0)
    }
}

/**
 * AVF虚拟机句柄
 */
data class AvfVmHandle(
    val vmId: String,
    val processId: Int? = null,
    val socketPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * 虚拟机配置
 */
data class VmConfig(
    val id: String,
    val name: String,
    val memory: Int = 2048,
    val cpu: Int = 2,
    val diskSize: Int = 10, // GB
    val resources: VmResourceConfig = VmResourceConfig(memory, cpu, diskSize),
    val bootImage: String? = null,
    val kernelImage: String? = null,
    val initrdImage: String? = null,
    val kernelArgs: String? = null,
    val enableGpu: Boolean = false
)

/**
 * QEMU虚拟机句柄类型别名
 * 
 * 为了向后兼容，QemuVmHandle是VmHandle的类型别名
 */
typealias QemuVmHandle = VmHandle

/**
 * 虚拟机信息
 */
data class VmInfo(
    val config: VmConfig,
    val state: VmState,
    val ipAddress: String? = null,
    val uptime: Long = 0,
    val handle: VmHandle? = null,
    val errorMessage: String? = null
) {
    val isRunning: Boolean
        get() = state == VmState.RUNNING

    val isStopped: Boolean
        get() = state == VmState.STOPPED || state == VmState.ERROR

    val canStart: Boolean
        get() = state == VmState.CREATED || state == VmState.STOPPED

    val canStop: Boolean
        get() = state == VmState.RUNNING || state == VmState.PAUSED

    val canPause: Boolean
        get() = state == VmState.RUNNING

    val canResume: Boolean
        get() = state == VmState.PAUSED
}