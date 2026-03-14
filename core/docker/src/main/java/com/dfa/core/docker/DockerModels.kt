package com.dfa.core.docker

/**
 * 容器状态枚举
 */
enum class ContainerState {
    CREATED,
    RUNNING,
    PAUSED,
    RESTARTING,
    REMOVING,
    EXITED,
    DEAD
}

/**
 * 容器信息
 */
data class ContainerInfo(
    val id: String,
    val name: String,
    val image: String,
    val state: ContainerState,
    val status: String,
    val ports: List<PortMapping> = emptyList()
)

/**
 * 端口映射
 */
data class PortMapping(
    val containerPort: Int,
    val hostPort: Int,
    val protocol: String = "tcp"
)

/**
 * 镜像信息
 */
data class ImageInfo(
    val id: String,
    val name: String,
    val tag: String,
    val size: Long,
    val createdAt: String
)