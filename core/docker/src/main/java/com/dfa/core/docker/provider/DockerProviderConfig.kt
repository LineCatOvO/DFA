package com.dfa.core.docker.provider

/**
 * Docker Provider配置基类
 *
 * 使用sealed class定义不同类型Provider的配置。
 * 每种Provider类型有对应的配置子类。
 *
 * @property providerId Provider唯一标识符
 * @property autoStart 是否自动启动
 * @property connectionTimeout 连接超时时间（毫秒）
 * @property requestTimeout 请求超时时间（毫秒）
 * @since 1.0.0
 */
sealed class DockerProviderConfig {
    /**
     * Provider唯一标识符
     */
    abstract val providerId: String

    /**
     * 是否在初始化后自动启动
     */
    abstract val autoStart: Boolean

    /**
     * 连接超时时间（毫秒）
     */
    abstract val connectionTimeout: Long

    /**
     * 请求超时时间（毫秒）
     */
    abstract val requestTimeout: Long

    /**
     * 获取Provider类型
     *
     * @return 对应的DockerProviderType
     */
    abstract fun getProviderType(): DockerProviderType
}

/**
 * QEMU Docker Provider配置
 *
 * 用于配置在QEMU虚拟机中运行的Docker环境。
 *
 * @property providerId Provider唯一标识符
 * @property vmId 虚拟机ID
 * @property socketPath Docker套接字路径
 * @property sshHost SSH主机地址
 * @property sshPort SSH端口
 * @property sshUser SSH用户名
 * @property sshKeyPath SSH私钥路径
 * @property autoStart 是否自动启动
 * @property connectionTimeout 连接超时时间（毫秒）
 * @property requestTimeout 请求超时时间（毫秒）
 * @property memoryMB 虚拟机内存大小（MB）
 * @property cpus 虚拟机CPU核心数
 * @property diskSizeGB 虚拟机磁盘大小（GB）
 * @property imageDir 镜像存储目录
 * @since 1.0.0
 */
data class QemuDockerProviderConfig(
    override val providerId: String,
    val vmId: String,
    val socketPath: String = "/var/run/docker.sock",
    val sshHost: String = "localhost",
    val sshPort: Int = 22,
    val sshUser: String = "root",
    val sshKeyPath: String? = null,
    override val autoStart: Boolean = true,
    override val connectionTimeout: Long = 30000L,
    override val requestTimeout: Long = 60000L,
    val memoryMB: Int = 4096,
    val cpus: Int = 4,
    val diskSizeGB: Int = 50,
    val imageDir: String = "/var/lib/docker"
) : DockerProviderConfig() {
    override fun getProviderType(): DockerProviderType = DockerProviderType.QEMU

    /**
     * 获取SSH连接字符串
     */
    val sshConnectionString: String
        get() = "$sshUser@$sshHost:$sshPort"

    /**
     * 获取Docker主机地址
     */
    val dockerHost: String
        get() = "tcp://$sshHost:2375"
}

/**
 * AVF Docker Provider配置
 *
 * 用于配置在Apple Virtualization Framework中运行的Docker环境。
 *
 * @property providerId Provider唯一标识符
 * @property vmId 虚拟机ID
 * @property socketPath Docker套接字路径
 * @property vmBundlePath 虚拟机Bundle路径
 * @property autoStart 是否自动启动
 * @property connectionTimeout 连接超时时间（毫秒）
 * @property requestTimeout 请求超时时间（毫秒）
 * @property memoryMB 虚拟机内存大小（MB）
 * @property cpus 虚拟机CPU核心数
 * @property diskSizeGB 虚拟机磁盘大小（GB）
 * @property useRosetta 是否使用Rosetta进行x86_64模拟
 * @property networkMode 网络模式
 * @since 1.0.0
 */
data class AvfDockerProviderConfig(
    override val providerId: String,
    val vmId: String,
    val socketPath: String = "/var/run/docker.sock",
    val vmBundlePath: String,
    override val autoStart: Boolean = true,
    override val connectionTimeout: Long = 30000L,
    override val requestTimeout: Long = 60000L,
    val memoryMB: Int = 4096,
    val cpus: Int = 4,
    val diskSizeGB: Int = 50,
    val useRosetta: Boolean = true,
    val networkMode: AvfNetworkMode = AvfNetworkMode.BRIDGED
) : DockerProviderConfig() {
    override fun getProviderType(): DockerProviderType = DockerProviderType.AVF

    /**
     * 获取虚拟机配置摘要
     */
    val vmConfigSummary: String
        get() = "AVF VM[$vmId]: ${cpus}CPUs, ${memoryMB}MB RAM, ${diskSizeGB}GB Disk"
}

/**
 * 本地Docker Provider配置
 *
 * 用于配置直接运行在主机上的Docker环境。
 *
 * @property providerId Provider唯一标识符
 * @property socketPath Docker套接字路径
 * @property host Docker主机地址
 * @property tlsConfig TLS配置
 * @property autoStart 是否自动启动
 * @property connectionTimeout 连接超时时间（毫秒）
 * @property requestTimeout 请求超时时间（毫秒）
 * @since 1.0.0
 */
data class LocalDockerProviderConfig(
    override val providerId: String,
    val socketPath: String = "/var/run/docker.sock",
    val host: String? = null,
    val tlsConfig: DockerTlsConfig? = null,
    override val autoStart: Boolean = true,
    override val connectionTimeout: Long = 10000L,
    override val requestTimeout: Long = 30000L
) : DockerProviderConfig() {
    override fun getProviderType(): DockerProviderType = DockerProviderType.LOCAL

    /**
     * 获取Docker主机地址
     */
    val dockerHost: String
        get() = host ?: "unix://$socketPath"

    /**
     * 是否使用TLS
     */
    val useTls: Boolean
        get() = tlsConfig != null
}

/**
 * AVF网络模式
 *
 * @since 1.0.0
 */
enum class AvfNetworkMode {
    /**
     * 桥接模式
     */
    BRIDGED,

    /**
     * NAT模式
     */
    NAT,

    /**
     * 仅主机模式
     */
    HOST_ONLY
}

/**
 * Docker TLS配置
 *
 * @property certPath 证书目录路径
 * @property certFile 客户端证书文件
 * @property keyFile 客户端私钥文件
 * @property caFile CA证书文件
 * @property verify 是否验证服务器证书
 * @since 1.0.0
 */
data class DockerTlsConfig(
    val certPath: String,
    val certFile: String = "cert.pem",
    val keyFile: String = "key.pem",
    val caFile: String = "ca.pem",
    val verify: Boolean = true
) {
    /**
     * 获取完整证书路径
     */
    val fullCertPath: String
        get() = "$certPath/$certFile"

    /**
     * 获取完整私钥路径
     */
    val fullKeyPath: String
        get() = "$certPath/$keyFile"

    /**
     * 获取完整CA路径
     */
    val fullCaPath: String
        get() = "$certPath/$caFile"
}