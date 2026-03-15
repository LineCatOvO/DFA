package com.dfa.core.docker

import java.time.Duration

/**
 * Docker客户端配置
 * 
 * 用于配置Docker客户端的连接参数和行为。
 * 
 * @property host Docker守护进程地址
 * @property dockerHost Docker Host URL（如 unix:///var/run/docker.sock）
 * @property tlsVerify 是否启用TLS验证
 * @property certPath TLS证书路径
 * @property connectionTimeout 连接超时时间
 * @property readTimeout 读取超时时间
 * @property writeTimeout 写入超时时间
 * @property maxRetries 最大重试次数
 * @property retryDelay 重试延迟时间
 * @property enableMetrics 是否启用指标收集
 * @property apiVersion API版本
 * @property registryConfig 注册表配置
 * @since 1.0.0
 */
data class DockerConfig(
    val host: String = DEFAULT_HOST,
    val dockerHost: String? = null,
    val tlsVerify: Boolean = false,
    val certPath: String? = null,
    val connectionTimeout: Duration = Duration.ofSeconds(DEFAULT_CONNECTION_TIMEOUT_SECONDS),
    val readTimeout: Duration = Duration.ofSeconds(DEFAULT_READ_TIMEOUT_SECONDS),
    val writeTimeout: Duration = Duration.ofSeconds(DEFAULT_WRITE_TIMEOUT_SECONDS),
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val retryDelay: Duration = Duration.ofMillis(DEFAULT_RETRY_DELAY_MS),
    val enableMetrics: Boolean = false,
    val apiVersion: String? = null,
    val registryConfig: RegistryConfig = RegistryConfig()
) {
    companion object {
        /**
         * 默认Docker Host
         */
        const val DEFAULT_HOST = "localhost"
        
        /**
         * 默认Unix Socket路径
         */
        const val DEFAULT_UNIX_SOCKET = "unix:///var/run/docker.sock"
        
        /**
         * Windows默认命名管道路径
         */
        const val DEFAULT_WINDOWS_PIPE = "npipe:////./pipe/docker_engine"
        
        /**
         * 默认TCP端口
         */
        const val DEFAULT_TCP_PORT = 2375
        
        /**
         * 默认TLS端口
         */
        const val DEFAULT_TLS_PORT = 2376
        
        /**
         * 默认连接超时时间（秒）
         */
        const val DEFAULT_CONNECTION_TIMEOUT_SECONDS = 30L
        
        /**
         * 默认读取超时时间（秒）
         */
        const val DEFAULT_READ_TIMEOUT_SECONDS = 60L
        
        /**
         * 默认写入超时时间（秒）
         */
        const val DEFAULT_WRITE_TIMEOUT_SECONDS = 60L
        
        /**
         * 默认最大重试次数
         */
        const val DEFAULT_MAX_RETRIES = 3
        
        /**
         * 默认重试延迟（毫秒）
         */
        const val DEFAULT_RETRY_DELAY_MS = 1000L
        
        /**
         * 从环境变量创建配置
         * 
         * 支持的环境变量：
         * - DOCKER_HOST: Docker守护进程地址
         * - DOCKER_TLS_VERIFY: 是否启用TLS验证
         * - DOCKER_CERT_PATH: TLS证书路径
         * - DOCKER_API_VERSION: API版本
         * 
         * @return Docker配置实例
         */
        fun fromEnvironment(): DockerConfig {
            val dockerHost = System.getenv("DOCKER_HOST")
            val tlsVerify = System.getenv("DOCKER_TLS_VERIFY")?.toBoolean() ?: false
            val certPath = System.getenv("DOCKER_CERT_PATH")
            val apiVersion = System.getenv("DOCKER_API_VERSION")
            
            return DockerConfig(
                dockerHost = dockerHost,
                tlsVerify = tlsVerify,
                certPath = certPath,
                apiVersion = apiVersion
            )
        }
        
        /**
         * 创建默认配置
         * 
         * 根据操作系统自动选择合适的默认值。
         * 
         * @return Docker配置实例
         */
        fun default(): DockerConfig {
            val osName = System.getProperty("os.name").lowercase()
            val dockerHost = when {
                osName.contains("win") -> DEFAULT_WINDOWS_PIPE
                else -> DEFAULT_UNIX_SOCKET
            }
            
            return DockerConfig(dockerHost = dockerHost)
        }
        
        /**
         * 创建TCP连接配置
         * 
         * @param host 主机地址
         * @param port 端口号
         * @param tls 是否使用TLS
         * @return Docker配置实例
         */
        fun tcp(
            host: String = DEFAULT_HOST,
            port: Int = if (false) DEFAULT_TLS_PORT else DEFAULT_TCP_PORT,
            tls: Boolean = false
        ): DockerConfig {
            val protocol = if (tls) "https" else "http"
            return DockerConfig(
                host = host,
                dockerHost = "$protocol://$host:$port",
                tlsVerify = tls
            )
        }
        
        /**
         * 创建Unix Socket连接配置
         * 
         * @param socketPath Socket路径
         * @return Docker配置实例
         */
        fun unixSocket(socketPath: String = DEFAULT_UNIX_SOCKET): DockerConfig {
            return DockerConfig(dockerHost = socketPath)
        }
    }
    
    /**
     * 获取有效的Docker Host URL
     * 
     * @return Docker Host URL
     */
    fun getEffectiveDockerHost(): String {
        return dockerHost ?: run {
            val osName = System.getProperty("os.name").lowercase()
            when {
                osName.contains("win") -> DEFAULT_WINDOWS_PIPE
                else -> DEFAULT_UNIX_SOCKET
            }
        }
    }
    
    /**
     * 是否使用TLS
     * 
     * @return 是否使用TLS
     */
    fun isTlsEnabled(): Boolean {
        return tlsVerify || getEffectiveDockerHost().startsWith("https://")
    }
    
    /**
     * 验证配置
     * 
     * @throws DockerConfigException 如果配置无效
     */
    fun validate() {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(!connectionTimeout.isNegative) { "connectionTimeout must be positive" }
        require(!readTimeout.isNegative) { "readTimeout must be positive" }
        require(!writeTimeout.isNegative) { "writeTimeout must be positive" }
        require(!retryDelay.isNegative) { "retryDelay must be positive" }
        
        if (tlsVerify && certPath.isNullOrBlank()) {
            throw DockerConfigException("certPath is required when tlsVerify is enabled")
        }
    }
    
    /**
     * 创建构建器
     * 
     * @return 配置构建器
     */
    fun toBuilder(): Builder {
        return Builder(this)
    }
    
    /**
     * Docker配置构建器
     */
    class Builder {
        private var host: String = DEFAULT_HOST
        private var dockerHost: String? = null
        private var tlsVerify: Boolean = false
        private var certPath: String? = null
        private var connectionTimeout: Duration = Duration.ofSeconds(DEFAULT_CONNECTION_TIMEOUT_SECONDS)
        private var readTimeout: Duration = Duration.ofSeconds(DEFAULT_READ_TIMEOUT_SECONDS)
        private var writeTimeout: Duration = Duration.ofSeconds(DEFAULT_WRITE_TIMEOUT_SECONDS)
        private var maxRetries: Int = DEFAULT_MAX_RETRIES
        private var retryDelay: Duration = Duration.ofMillis(DEFAULT_RETRY_DELAY_MS)
        private var enableMetrics: Boolean = false
        private var apiVersion: String? = null
        private var registryConfig: RegistryConfig = RegistryConfig()
        
        constructor()
        
        constructor(config: DockerConfig) {
            this.host = config.host
            this.dockerHost = config.dockerHost
            this.tlsVerify = config.tlsVerify
            this.certPath = config.certPath
            this.connectionTimeout = config.connectionTimeout
            this.readTimeout = config.readTimeout
            this.writeTimeout = config.writeTimeout
            this.maxRetries = config.maxRetries
            this.retryDelay = config.retryDelay
            this.enableMetrics = config.enableMetrics
            this.apiVersion = config.apiVersion
            this.registryConfig = config.registryConfig
        }
        
        fun host(host: String) = apply { this.host = host }
        fun dockerHost(dockerHost: String?) = apply { this.dockerHost = dockerHost }
        fun tlsVerify(tlsVerify: Boolean) = apply { this.tlsVerify = tlsVerify }
        fun certPath(certPath: String?) = apply { this.certPath = certPath }
        fun connectionTimeout(timeout: Duration) = apply { this.connectionTimeout = timeout }
        fun readTimeout(timeout: Duration) = apply { this.readTimeout = timeout }
        fun writeTimeout(timeout: Duration) = apply { this.writeTimeout = timeout }
        fun maxRetries(retries: Int) = apply { this.maxRetries = retries }
        fun retryDelay(delay: Duration) = apply { this.retryDelay = delay }
        fun enableMetrics(enable: Boolean) = apply { this.enableMetrics = enable }
        fun apiVersion(version: String?) = apply { this.apiVersion = version }
        fun registryConfig(config: RegistryConfig) = apply { this.registryConfig = config }
        
        fun build(): DockerConfig {
            return DockerConfig(
                host = host,
                dockerHost = dockerHost,
                tlsVerify = tlsVerify,
                certPath = certPath,
                connectionTimeout = connectionTimeout,
                readTimeout = readTimeout,
                writeTimeout = writeTimeout,
                maxRetries = maxRetries,
                retryDelay = retryDelay,
                enableMetrics = enableMetrics,
                apiVersion = apiVersion,
                registryConfig = registryConfig
            )
        }
    }
}

/**
 * 注册表配置
 * 
 * 用于配置Docker镜像仓库的认证信息。
 * 
 * @property registries 注册表认证信息映射
 * @property defaultRegistry 默认注册表
 * @property insecureRegistries 不安全的注册表列表（HTTP）
 * @property mirrorRegistry 镜像仓库地址
 * @since 1.0.0
 */
data class RegistryConfig(
    val registries: Map<String, RegistryAuth> = emptyMap(),
    val defaultRegistry: String = "docker.io",
    val insecureRegistries: List<String> = emptyList(),
    val mirrorRegistry: String? = null
) {
    /**
     * 获取指定注册表的认证信息
     * 
     * @param registry 注册表地址
     * @return 认证信息，如果不存在则返回null
     */
    fun getAuth(registry: String): RegistryAuth? {
        return registries[registry] ?: registries[getNormalizedRegistry(registry)]
    }
    
    /**
     * 规范化注册表地址
     * 
     * @param registry 注册表地址
     * @return 规范化后的地址
     */
    private fun getNormalizedRegistry(registry: String): String {
        return when {
            registry.startsWith("http://") -> registry.removePrefix("http://")
            registry.startsWith("https://") -> registry.removePrefix("https://")
            else -> registry
        }.removeSuffix("/")
    }
    
    /**
     * 添加注册表认证
     * 
     * @param registry 注册表地址
     * @param auth 认证信息
     * @return 新的配置实例
     */
    fun withAuth(registry: String, auth: RegistryAuth): RegistryConfig {
        return copy(registries = registries + (registry to auth))
    }
    
    /**
     * 添加不安全的注册表
     * 
     * @param registry 注册表地址
     * @return 新的配置实例
     */
    fun withInsecureRegistry(registry: String): RegistryConfig {
        return copy(insecureRegistries = insecureRegistries + registry)
    }
    
    companion object {
        /**
         * Docker Hub官方注册表
         */
        const val DOCKER_HUB = "docker.io"
        
        /**
         * 从Docker配置文件创建
         * 
         * @param configPath 配置文件路径（默认为 ~/.docker/config.json）
         * @return 注册表配置
         */
        fun fromDockerConfig(configPath: String? = null): RegistryConfig {
            // 实际实现需要读取Docker配置文件
            // 这里返回默认配置
            return RegistryConfig()
        }
    }
}

/**
 * HTTP代理配置
 * 
 * @property httpProxy HTTP代理地址
 * @property httpsProxy HTTPS代理地址
 * @property noProxy 不使用代理的地址列表
 * @since 1.0.0
 */
data class ProxyConfig(
    val httpProxy: String? = null,
    val httpsProxy: String? = null,
    val noProxy: List<String> = emptyList()
) {
    companion object {
        /**
         * 从环境变量创建代理配置
         * 
         * @return 代理配置实例
         */
        fun fromEnvironment(): ProxyConfig {
            return ProxyConfig(
                httpProxy = System.getenv("HTTP_PROXY") ?: System.getenv("http_proxy"),
                httpsProxy = System.getenv("HTTPS_PROXY") ?: System.getenv("https_proxy"),
                noProxy = (System.getenv("NO_PROXY") ?: System.getenv("no_proxy"))
                    ?.split(",")?.map { it.trim() } ?: emptyList()
            )
        }
    }
    
    /**
     * 是否需要代理
     * 
     * @param url 目标URL
     * @return 是否需要使用代理
     */
    fun shouldUseProxy(url: String): Boolean {
        if (httpProxy == null && httpsProxy == null) return false
        
        val host = url.removePrefix("http://")
            .removePrefix("https://")
            .split("/").firstOrNull()
            ?.split(":")?.firstOrNull() ?: return true
        
        return noProxy.none { 
            it == host || 
            host.endsWith(".$it") ||
            it == "*" ||
            it == "."
        }
    }
}

/**
 * 日志配置
 * 
 * @property enableLogging 是否启用日志
 * @property logLevel 日志级别
 * @property logRequests 是否记录请求
 * @property logResponses 是否记录响应
 * @property maxLogLength 最大日志长度
 * @since 1.0.0
 */
data class LogConfig(
    val enableLogging: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val logRequests: Boolean = true,
    val logResponses: Boolean = false,
    val maxLogLength: Int = 1000
) {
    /**
     * 日志级别
     */
    enum class LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, NONE
    }
}

/**
 * 连接池配置
 * 
 * @property maxConnections 最大连接数
 * @property keepAliveDuration 连接保持时间
 * @property connectionIdleTimeout 空闲连接超时时间
 * @since 1.0.0
 */
data class ConnectionPoolConfig(
    val maxConnections: Int = 100,
    val keepAliveDuration: Duration = Duration.ofMinutes(5),
    val connectionIdleTimeout: Duration = Duration.ofMinutes(10)
) {
    init {
        require(maxConnections > 0) { "maxConnections must be positive" }
    }
}