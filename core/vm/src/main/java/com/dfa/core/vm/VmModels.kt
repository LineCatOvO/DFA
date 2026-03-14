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
    ERROR
}

/**
 * 虚拟机配置
 */
data class VmConfig(
    val id: String,
    val name: String,
    val memory: Int = 2048,
    val cpu: Int = 2,
    val diskSize: Int = 10 // GB
)

/**
 * 虚拟机信息
 */
data class VmInfo(
    val config: VmConfig,
    val state: VmState,
    val ipAddress: String? = null,
    val uptime: Long = 0
)