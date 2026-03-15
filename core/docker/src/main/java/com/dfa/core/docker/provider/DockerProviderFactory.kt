package com.dfa.core.docker.provider

/**
 * Docker Provider工厂接口
 *
 * 用于创建不同类型的DockerProvider实例。
 * 每种Provider类型应有对应的工厂实现。
 *
 * 设计模式：Factory Method Pattern
 *
 * @since 1.0.0
 */
interface DockerProviderFactory {

    /**
     * 获取支持的Provider类型
     *
     * @return 此工厂支持的Provider类型
     */
    val supportedProviderType: DockerProviderType

    /**
     * 创建Provider实例
     *
     * 根据配置创建对应的Provider实例。
     * 创建的Provider处于CREATED状态，需要调用initialize()进行初始化。
     *
     * @param config Provider配置
     * @return 创建结果，包含Provider实例
     * @throws ProviderConfigException 如果配置无效
     */
    suspend fun create(config: DockerProviderConfig): Result<DockerProvider>

    /**
     * 检查Provider是否可用
     *
     * 检查当前环境是否支持创建此类型的Provider。
     * 例如：检查必要的依赖、权限、环境变量等。
     *
     * @return 如果Provider可用返回true
     */
    suspend fun isProviderAvailable(): Boolean

    /**
     * 获取Provider优先级
     *
     * 当多个Provider都可用时，优先级高的会被优先选择。
     * 数值越大优先级越高。
     *
     * @return 优先级数值
     */
    fun getPriority(): Int = 0

    /**
     * 验证配置
     *
     * 验证配置是否有效。
     *
     * @param config Provider配置
     * @return 验证结果，包含验证错误列表（空列表表示验证通过）
     */
    fun validateConfig(config: DockerProviderConfig): List<String> {
        val errors = mutableListOf<String>()
        
        if (config.providerId.isEmpty()) {
            errors.add("Provider ID cannot be empty")
        }
        
        if (config.connectionTimeout <= 0) {
            errors.add("Connection timeout must be positive")
        }
        
        if (config.requestTimeout <= 0) {
            errors.add("Request timeout must be positive")
        }
        
        return errors
    }

    /**
     * 获取Provider描述
     *
     * @return Provider类型的描述信息
     */
    fun getDescription(): String = supportedProviderType.description
}

/**
 * QEMU Docker Provider工厂
 *
 * 用于创建QEMU类型的DockerProvider实例。
 *
 * @since 1.0.0
 */
interface QemuDockerProviderFactory : DockerProviderFactory {
    override val supportedProviderType: DockerProviderType
        get() = DockerProviderType.QEMU

    /**
     * 检查QEMU是否已安装
     *
     * @return 如果QEMU已安装返回true
     */
    suspend fun isQemuInstalled(): Boolean

    /**
     * 获取QEMU版本
     *
     * @return QEMU版本字符串
     */
    suspend fun getQemuVersion(): String?
}

/**
 * AVF Docker Provider工厂
 *
 * 用于创建AVF类型的DockerProvider实例。
 *
 * @since 1.0.0
 */
interface AvfDockerProviderFactory : DockerProviderFactory {
    override val supportedProviderType: DockerProviderType
        get() = DockerProviderType.AVF

    /**
     * 检查AVF是否可用
     *
     * @return 如果AVF可用返回true
     */
    suspend fun isAvfAvailable(): Boolean

    /**
     * 获取macOS版本
     *
     * @return macOS版本字符串
     */
    fun getMacOsVersion(): String?
}

/**
 * 本地Docker Provider工厂
 *
 * 用于创建本地Docker类型的DockerProvider实例。
 *
 * @since 1.0.0
 */
interface LocalDockerProviderFactory : DockerProviderFactory {
    override val supportedProviderType: DockerProviderType
        get() = DockerProviderType.LOCAL

    /**
     * 检查Docker是否已安装
     *
     * @return 如果Docker已安装返回true
     */
    suspend fun isDockerInstalled(): Boolean

    /**
     * 获取Docker版本
     *
     * @return Docker版本字符串
     */
    suspend fun getDockerVersion(): String?

    /**
     * 检查Docker守护进程是否运行
     *
     * @return 如果Docker守护进程正在运行返回true
     */
    suspend fun isDockerDaemonRunning(): Boolean
}

/**
 * Docker Provider工厂注册表
 *
 * 管理所有可用的Provider工厂。
 *
 * @since 1.0.0
 */
object DockerProviderFactoryRegistry {
    private val factories = mutableMapOf<DockerProviderType, DockerProviderFactory>()

    /**
     * 注册工厂
     *
     * @param factory 要注册的工厂
     */
    fun register(factory: DockerProviderFactory) {
        factories[factory.supportedProviderType] = factory
    }

    /**
     * 注销工厂
     *
     * @param providerType 要注销的工厂类型
     */
    fun unregister(providerType: DockerProviderType) {
        factories.remove(providerType)
    }

    /**
     * 获取工厂
     *
     * @param providerType Provider类型
     * @return 对应的工厂，如果不存在则返回null
     */
    fun getFactory(providerType: DockerProviderType): DockerProviderFactory? {
        return factories[providerType]
    }

    /**
     * 获取所有已注册的工厂
     *
     * @return 工厂列表
     */
    fun getAllFactories(): List<DockerProviderFactory> {
        return factories.values.toList()
    }

    /**
     * 获取所有可用的工厂
     *
     * @return 可用的工厂列表
     */
    suspend fun getAvailableFactories(): List<DockerProviderFactory> {
        return factories.values.filter { it.isProviderAvailable() }
    }

    /**
     * 根据优先级获取最佳可用工厂
     *
     * @return 优先级最高的可用工厂，如果没有可用工厂则返回null
     */
    suspend fun getBestAvailableFactory(): DockerProviderFactory? {
        return getAvailableFactories().maxByOrNull { it.getPriority() }
    }

    /**
     * 清空所有注册的工厂
     */
    fun clear() {
        factories.clear()
    }
}