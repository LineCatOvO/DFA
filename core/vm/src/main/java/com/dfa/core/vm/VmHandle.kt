package com.dfa.core.vm

/**
 * 通用虚拟机句柄
 *
 * 用于标识和管理虚拟机实例，支持多种虚拟化后端（AVF、QEMU等）
 *
 * @property vmId 虚拟机唯一标识符
 * @property backendType 后端类型（AVF、QEMU等）
 * @property processId 进程ID（如果适用）
 * @property socketPath Unix套接字路径（如果适用）
 * @property sshPort SSH端口（如果适用）
 * @property vncPort VNC端口（如果适用）
 * @property monitorPath 监控套接字路径（如果适用）
 * @property createdAt 创建时间戳
 * @property lastUpdated 最后更新时间戳
 * @property metadata 扩展元数据（用于存储后端特定信息）
 */
data class VmHandle(
    val vmId: String,
    val backendType: VmBackendType = VmBackendType.AVF,
    val processId: Int? = null,
    val socketPath: String? = null,
    val sshPort: Int? = null,
    val vncPort: Int? = null,
    val monitorPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 更新最后修改时间
     */
    fun touch(): VmHandle = copy(lastUpdated = System.currentTimeMillis())

    /**
     * 添加元数据
     */
    fun withMetadata(key: String, value: Any): VmHandle =
        copy(metadata = metadata + (key to value), lastUpdated = System.currentTimeMillis())

    /**
     * 检查句柄是否有效
     */
    val isValid: Boolean
        get() = vmId.isNotEmpty()

    /**
     * 获取连接信息摘要
     */
    val connectionSummary: String
        get() = buildString {
            append("VM[$vmId]")
            processId?.let { append(" PID=$it") }
            sshPort?.let { append(" SSH=$it") }
            vncPort?.let { append(" VNC=$it") }
        }
}

/**
 * 虚拟机后端类型枚举
 */
enum class VmBackendType {
    /** Android Virtualization Framework */
    AVF,
    /** QEMU虚拟机 */
    QEMU,
    /** Docker容器 */
    DOCKER,
    /** 其他/未知 */
    UNKNOWN
}

/**
 * VmHandle扩展函数：转换为AvfVmHandle（向后兼容）
 */
fun VmHandle.toAvfVmHandle(): AvfVmHandle = AvfVmHandle(
    vmId = vmId,
    processId = processId,
    socketPath = socketPath,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)

/**
 * AvfVmHandle扩展函数：转换为VmHandle
 */
fun AvfVmHandle.toVmHandle(): VmHandle = VmHandle(
    vmId = vmId,
    backendType = VmBackendType.AVF,
    processId = processId,
    socketPath = socketPath,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)