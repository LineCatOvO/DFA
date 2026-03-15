package com.dfa.core.docker

/**
 * Docker客户端接口
 * 
 * 定义与Docker守护进程交互的API契约。
 * 所有操作均为异步操作，使用Kotlin协程实现。
 * 
 * 设计模式：Repository Pattern
 * 
 * @since 1.0.0
 */
interface DockerClient {
    
    // ==================== 连接管理 ====================
    
    /**
     * 连接到Docker守护进程
     * @return 连接结果
     */
    suspend fun connect(): Result<Unit>
    
    /**
     * 断开与Docker守护进程的连接
     */
    suspend fun disconnect()
    
    /**
     * 检查是否已连接
     * @return 连接状态
     */
    fun isConnected(): Boolean
    
    /**
     * 获取Docker版本信息
     * @return 版本信息
     */
    suspend fun version(): Result<DockerVersion>
    
    /**
     * 获取Docker系统信息
     * @return 系统信息
     */
    suspend fun info(): Result<DockerSystemInfo>
    
    /**
     * 执行健康检查（Ping Docker守护进程）
     * @return 健康检查结果
     */
    suspend fun ping(): Result<Boolean>
    
    // ==================== 容器操作 ====================
    
    /**
     * 创建容器
     * @param config 容器配置
     * @return 创建结果，包含容器ID
     */
    suspend fun createContainer(config: ContainerConfig): Result<ContainerCreateResult>
    
    /**
     * 启动容器
     * @param containerId 容器ID或名称
     * @return 操作结果
     */
    suspend fun startContainer(containerId: String): Result<Unit>
    
    /**
     * 停止容器
     * @param containerId 容器ID或名称
     * @param timeout 停止超时时间（秒），默认10秒
     * @return 操作结果
     */
    suspend fun stopContainer(containerId: String, timeout: Int = 10): Result<Unit>
    
    /**
     * 重启容器
     * @param containerId 容器ID或名称
     * @param timeout 重启超时时间（秒），默认10秒
     * @return 操作结果
     */
    suspend fun restartContainer(containerId: String, timeout: Int = 10): Result<Unit>
    
    /**
     * 暂停容器
     * @param containerId 容器ID或名称
     * @return 操作结果
     */
    suspend fun pauseContainer(containerId: String): Result<Unit>
    
    /**
     * 恢复容器
     * @param containerId 容器ID或名称
     * @return 操作结果
     */
    suspend fun unpauseContainer(containerId: String): Result<Unit>
    
    /**
     * 删除容器
     * @param containerId 容器ID或名称
     * @param force 是否强制删除
     * @param removeVolumes 是否删除关联的卷
     * @return 操作结果
     */
    suspend fun removeContainer(
        containerId: String,
        force: Boolean = false,
        removeVolumes: Boolean = false
    ): Result<Unit>
    
    /**
     * 列出容器
     * @param all 是否显示所有容器（包括已停止的）
     * @return 容器列表
     */
    suspend fun listContainers(all: Boolean = false): Result<List<ContainerInfo>>
    
    /**
     * 获取容器详细信息
     * @param containerId 容器ID或名称
     * @return 容器详细信息
     */
    suspend fun inspectContainer(containerId: String): Result<ContainerDetails>
    
    /**
     * 获取容器日志
     * @param containerId 容器ID或名称
     * @param options 日志选项
     * @return 日志内容
     */
    suspend fun getContainerLogs(
        containerId: String,
        options: ContainerLogsOptions = ContainerLogsOptions()
    ): Result<String>
    
    /**
     * 在容器中执行命令
     * @param containerId 容器ID或名称
     * @param command 要执行的命令
     * @param options 执行选项
     * @return 执行结果
     */
    suspend fun execInContainer(
        containerId: String,
        command: List<String>,
        options: ExecOptions = ExecOptions()
    ): Result<ExecResult>
    
    // ==================== 镜像操作 ====================
    
    /**
     * 拉取镜像
     * @param imageName 镜像名称（如 nginx:latest）
     * @param options 拉取选项
     * @return 拉取结果
     */
    suspend fun pullImage(
        imageName: String,
        options: ImagePullOptions = ImagePullOptions()
    ): Result<ImagePullResult>
    
    /**
     * 推送镜像
     * @param imageName 镜像名称
     * @param options 推送选项
     * @return 推送结果
     */
    suspend fun pushImage(
        imageName: String,
        options: ImagePushOptions = ImagePushOptions()
    ): Result<ImagePushResult>
    
    /**
     * 构建镜像
     * @param contextPath 构建上下文路径
     * @param options 构建选项
     * @return 构建结果
     */
    suspend fun buildImage(
        contextPath: String,
        options: ImageBuildOptions = ImageBuildOptions()
    ): Result<ImageBuildResult>
    
    /**
     * 删除镜像
     * @param imageId 镜像ID或名称
     * @param force 是否强制删除
     * @param prune 是否清理未引用的镜像
     * @return 操作结果
     */
    suspend fun removeImage(
        imageId: String,
        force: Boolean = false,
        prune: Boolean = false
    ): Result<Unit>
    
    /**
     * 列出镜像
     * @param all 是否显示所有镜像（包括中间层）
     * @param filters 过滤条件
     * @return 镜像列表
     */
    suspend fun listImages(
        all: Boolean = false,
        filters: Map<String, String> = emptyMap()
    ): Result<List<ImageInfo>>
    
    /**
     * 获取镜像详细信息
     * @param imageId 镜像ID或名称
     * @return 镜像详细信息
     */
    suspend fun inspectImage(imageId: String): Result<ImageDetails>
    
    /**
     * 标记镜像
     * @param sourceImage 源镜像
     * @param targetImage 目标镜像
     * @return 操作结果
     */
    suspend fun tagImage(sourceImage: String, targetImage: String): Result<Unit>
    
    // ==================== 网络操作 ====================
    
    /**
     * 创建网络
     * @param config 网络配置
     * @return 创建结果
     */
    suspend fun createNetwork(config: NetworkConfig): Result<NetworkCreateResult>
    
    /**
     * 删除网络
     * @param networkId 网络ID或名称
     * @return 操作结果
     */
    suspend fun removeNetwork(networkId: String): Result<Unit>
    
    /**
     * 列出网络
     * @param filters 过滤条件
     * @return 网络列表
     */
    suspend fun listNetworks(
        filters: Map<String, String> = emptyMap()
    ): Result<List<NetworkInfo>>
    
    /**
     * 获取网络详细信息
     * @param networkId 网络ID或名称
     * @return 网络详细信息
     */
    suspend fun inspectNetwork(networkId: String): Result<NetworkDetails>
    
    /**
     * 将容器连接到网络
     * @param networkId 网络ID或名称
     * @param containerId 容器ID或名称
     * @param config 连接配置
     * @return 操作结果
     */
    suspend fun connectToNetwork(
        networkId: String,
        containerId: String,
        config: NetworkConnectConfig = NetworkConnectConfig()
    ): Result<Unit>
    
    /**
     * 将容器从网络断开
     * @param networkId 网络ID或名称
     * @param containerId 容器ID或名称
     * @param force 是否强制断开
     * @return 操作结果
     */
    suspend fun disconnectFromNetwork(
        networkId: String,
        containerId: String,
        force: Boolean = false
    ): Result<Unit>
    
    // ==================== 卷操作 ====================
    
    /**
     * 创建卷
     * @param config 卷配置
     * @return 创建结果
     */
    suspend fun createVolume(config: VolumeConfig = VolumeConfig()): Result<VolumeCreateResult>
    
    /**
     * 删除卷
     * @param volumeName 卷名称
     * @param force 是否强制删除
     * @return 操作结果
     */
    suspend fun removeVolume(volumeName: String, force: Boolean = false): Result<Unit>
    
    /**
     * 列出卷
     * @param filters 过滤条件
     * @return 卷列表
     */
    suspend fun listVolumes(
        filters: Map<String, String> = emptyMap()
    ): Result<List<VolumeInfo>>
    
    /**
     * 获取卷详细信息
     * @param volumeName 卷名称
     * @return 卷详细信息
     */
    suspend fun inspectVolume(volumeName: String): Result<VolumeDetails>
    
    /**
     * 清理未使用的卷
     * @return 清理结果
     */
    suspend fun pruneVolumes(): Result<VolumePruneResult>
}

// ==================== 数据模型 ====================

/**
 * Docker版本信息
 */
data class DockerVersion(
    val version: String,
    val apiVersion: String,
    val gitCommit: String,
    val goVersion: String,
    val os: String,
    val arch: String,
    val kernelVersion: String
)

/**
 * Docker系统信息
 */
data class DockerSystemInfo(
    val containers: Int,
    val containersRunning: Int,
    val containersStopped: Int,
    val containersPaused: Int,
    val images: Int,
    val operatingSystem: String,
    val architecture: String,
    val cpus: Int,
    val memory: Long,
    val dockerRootDir: String,
    val driver: String
)

/**
 * 容器创建结果
 */
data class ContainerCreateResult(
    val id: String,
    val warnings: List<String> = emptyList()
)

/**
 * 容器配置
 */
data class ContainerConfig(
    val name: String? = null,
    val image: String,
    val command: List<String> = emptyList(),
    val entrypoint: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap(),
    val ports: List<PortBinding> = emptyList(),
    val volumes: List<VolumeMount> = emptyList(),
    val networks: List<String> = emptyList(),
    val hostname: String? = null,
    val domainName: String? = null,
    val user: String? = null,
    val workingDir: String? = null,
    val labels: Map<String, String> = emptyMap(),
    val restartPolicy: RestartPolicy = RestartPolicy(),
    val resources: ResourceLimits = ResourceLimits(),
    val healthCheck: HealthCheck? = null,
    val privileged: Boolean = false,
    val capabilities: ContainerCapabilities = ContainerCapabilities()
)

/**
 * 端口绑定
 */
data class PortBinding(
    val containerPort: Int,
    val hostPort: Int,
    val hostIp: String = "0.0.0.0",
    val protocol: String = "tcp"
)

/**
 * 卷挂载
 */
data class VolumeMount(
    val source: String,
    val destination: String,
    val mode: String = "rw",
    val type: String = "bind"
)

/**
 * 重启策略
 */
data class RestartPolicy(
    val name: String = "no",
    val maximumRetryCount: Int = 0
)

/**
 * 资源限制
 */
data class ResourceLimits(
    val cpuShares: Long? = null,
    val memory: Long? = null,
    val memorySwap: Long? = null,
    val cpuPeriod: Long? = null,
    val cpuQuota: Long? = null,
    val cpusetCpus: String? = null,
    val cpusetMems: String? = null
)

/**
 * 健康检查配置
 */
data class HealthCheck(
    val test: List<String>,
    val interval: Long = 30000000000, // 30 seconds in nanoseconds
    val timeout: Long = 30000000000, // 30 seconds in nanoseconds
    val retries: Int = 3,
    val startPeriod: Long = 0
)

/**
 * 容器能力
 */
data class ContainerCapabilities(
    val add: List<String> = emptyList(),
    val drop: List<String> = emptyList()
)

/**
 * 容器详细信息
 */
data class ContainerDetails(
    val id: String,
    val name: String,
    val image: String,
    val imageId: String,
    val state: ContainerState,
    val status: String,
    val created: Long,
    val ports: List<PortMapping>,
    val mounts: List<MountInfo>,
    val networkSettings: NetworkSettings,
    val config: ContainerConfigInfo,
    val hostConfig: HostConfigInfo
)

/**
 * 挂载信息
 */
data class MountInfo(
    val type: String,
    val source: String,
    val destination: String,
    val mode: String,
    val rw: Boolean
)

/**
 * 网络设置
 */
data class NetworkSettings(
    val networks: Map<String, NetworkEndpoint>,
    val ipAddress: String,
    val ipPrefixLen: Int,
    val gateway: String,
    val macAddress: String
)

/**
 * 网络端点
 */
data class NetworkEndpoint(
    val networkId: String,
    val endpointId: String,
    val gateway: String,
    val ipAddress: String,
    val ipPrefixLen: Int,
    val macAddress: String
)

/**
 * 容器配置信息
 */
data class ContainerConfigInfo(
    val hostname: String,
    val domainName: String,
    val user: String,
    val attachStdin: Boolean,
    val attachStdout: Boolean,
    val attachStderr: Boolean,
    val exposedPorts: Map<String, Any>,
    val tty: Boolean,
    val openStdin: Boolean,
    val stdinOnce: Boolean,
    val env: List<String>,
    val cmd: List<String>,
    val image: String,
    val volumes: Map<String, Any>,
    val workingDir: String,
    val entrypoint: List<String>,
    val labels: Map<String, String>
)

/**
 * 主机配置信息
 */
data class HostConfigInfo(
    val binds: List<String>,
    val portBindings: Map<String, List<Map<String, String>>>,
    val restartPolicy: Map<String, Any>,
    val networkMode: String,
    val privileged: Boolean,
    val readonlyRootfs: Boolean
)

/**
 * 容器日志选项
 */
data class ContainerLogsOptions(
    val follow: Boolean = false,
    val stdout: Boolean = true,
    val stderr: Boolean = true,
    val since: Long = 0,
    val until: Long = 0,
    val timestamps: Boolean = false,
    val tail: String = "all"
)

/**
 * 执行选项
 */
data class ExecOptions(
    val attachStdin: Boolean = false,
    val attachStdout: Boolean = true,
    val attachStderr: Boolean = true,
    val detachKeys: String? = null,
    val tty: Boolean = false,
    val env: Map<String, String> = emptyMap(),
    val cwd: String? = null,
    val privileged: Boolean = false,
    val user: String? = null
)

/**
 * 执行结果
 */
data class ExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

/**
 * 镜像拉取选项
 */
data class ImagePullOptions(
    val registry: String? = null,
    val platform: String? = null,
    val auth: RegistryAuth? = null
)

/**
 * 镜像拉取结果
 */
data class ImagePullResult(
    val imageId: String,
    val status: String
)

/**
 * 镜像推送选项
 */
data class ImagePushOptions(
    val registry: String? = null,
    val auth: RegistryAuth? = null
)

/**
 * 镜像推送结果
 */
data class ImagePushResult(
    val status: String,
    val digest: String? = null
)

/**
 * 镜像构建选项
 */
data class ImageBuildOptions(
    val dockerfile: String = "Dockerfile",
    val tags: List<String> = emptyList(),
    val buildArgs: Map<String, String> = emptyMap(),
    val cacheFrom: List<String> = emptyList(),
    val noCache: Boolean = false,
    val pull: Boolean = false,
    val platform: String? = null,
    val target: String? = null,
    val labels: Map<String, String> = emptyMap()
)

/**
 * 镜像构建结果
 */
data class ImageBuildResult(
    val imageId: String,
    val warnings: List<String> = emptyList()
)

/**
 * 镜像详细信息
 */
data class ImageDetails(
    val id: String,
    val repoTags: List<String>,
    val repoDigests: List<String>,
    val created: Long,
    val size: Long,
    val virtualSize: Long,
    val architecture: String,
    val os: String,
    val author: String,
    val config: ImageConfigInfo
)

/**
 * 镜像配置信息
 */
data class ImageConfigInfo(
    val hostname: String,
    val domainName: String,
    val user: String,
    val attachStdin: Boolean,
    val attachStdout: Boolean,
    val attachStderr: Boolean,
    val exposedPorts: Map<String, Any>,
    val tty: Boolean,
    val openStdin: Boolean,
    val stdinOnce: Boolean,
    val env: List<String>,
    val cmd: List<String>,
    val volumes: Map<String, Any>,
    val workingDir: String,
    val entrypoint: List<String>,
    val labels: Map<String, String>
)

/**
 * 注册表认证信息
 */
data class RegistryAuth(
    val username: String,
    val password: String,
    val email: String? = null,
    val serverAddress: String? = null
)

/**
 * 网络配置
 */
data class NetworkConfig(
    val name: String,
    val driver: String = "bridge",
    val scope: String = "local",
    val internal: Boolean = false,
    val attachable: Boolean = false,
    val ingress: Boolean = false,
    val enableIPv6: Boolean = false,
    val ipam: IpamConfig = IpamConfig(),
    val options: Map<String, String> = emptyMap(),
    val labels: Map<String, String> = emptyMap()
)

/**
 * IPAM配置
 */
data class IpamConfig(
    val driver: String = "default",
    val config: List<IpamConfigEntry> = emptyList()
)

/**
 * IPAM配置条目
 */
data class IpamConfigEntry(
    val subnet: String,
    val gateway: String? = null,
    val ipRange: String? = null
)

/**
 * 网络创建结果
 */
data class NetworkCreateResult(
    val id: String,
    val warning: String? = null
)

/**
 * 网络信息
 */
data class NetworkInfo(
    val id: String,
    val name: String,
    val driver: String,
    val scope: String,
    val internal: Boolean,
    val attachable: Boolean,
    val ingress: Boolean,
    val enableIPv6: Boolean
)

/**
 * 网络详细信息
 */
data class NetworkDetails(
    val id: String,
    val name: String,
    val driver: String,
    val scope: String,
    val internal: Boolean,
    val attachable: Boolean,
    val ingress: Boolean,
    val enableIPv6: Boolean,
    val ipam: IpamConfig,
    val containers: Map<String, NetworkContainer>,
    val options: Map<String, String>,
    val labels: Map<String, String>,
    val created: Long
)

/**
 * 网络中的容器
 */
data class NetworkContainer(
    val containerId: String,
    val name: String,
    val endpointId: String,
    val macAddress: String,
    val ipv4Address: String,
    val ipv6Address: String
)

/**
 * 网络连接配置
 */
data class NetworkConnectConfig(
    val container: String,
    val endpointConfig: EndpointConfig? = null
)

/**
 * 端点配置
 */
data class EndpointConfig(
    val ipamConfig: EndpointIpamConfig? = null,
    val links: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val networkId: String? = null,
    val endpointId: String? = null,
    val gateway: String? = null,
    val ipAddress: String? = null,
    val ipPrefixLen: Int? = null,
    val ipv6Gateway: String? = null,
    val globalIPv6Address: String? = null,
    val globalIPv6PrefixLen: Int? = null,
    val macAddress: String? = null
)

/**
 * 端点IPAM配置
 */
data class EndpointIpamConfig(
    val ipv4Address: String? = null,
    val ipv6Address: String? = null,
    val linkLocalIPs: List<String> = emptyList()
)

/**
 * 卷配置
 */
data class VolumeConfig(
    val name: String? = null,
    val driver: String = "local",
    val driverOpts: Map<String, String> = emptyMap(),
    val labels: Map<String, String> = emptyMap()
)

/**
 * 卷创建结果
 */
data class VolumeCreateResult(
    val name: String,
    val driver: String,
    val mountpoint: String,
    val scope: String,
    val options: Map<String, String>,
    val labels: Map<String, String>
)

/**
 * 卷信息
 */
data class VolumeInfo(
    val name: String,
    val driver: String,
    val mountpoint: String,
    val scope: String,
    val usageData: VolumeUsageData? = null
)

/**
 * 卷使用数据
 */
data class VolumeUsageData(
    val size: Long,
    val refCount: Int
)

/**
 * 卷详细信息
 */
data class VolumeDetails(
    val name: String,
    val driver: String,
    val mountpoint: String,
    val scope: String,
    val options: Map<String, String>,
    val labels: Map<String, String>,
    val createdAt: String,
    val usageData: VolumeUsageData? = null
)

/**
 * 卷清理结果
 */
data class VolumePruneResult(
    val volumesDeleted: List<String>,
    val spaceReclaimed: Long
)