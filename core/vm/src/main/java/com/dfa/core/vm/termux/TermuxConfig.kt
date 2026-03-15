package com.dfa.core.vm.termux

/**
 * Termux配置数据类
 *
 * 包含Termux环境的路径配置、环境变量配置和包管理器配置
 */
data class TermuxConfig(
    /**
     * Termux前缀路径
     */
    val prefixPath: String = TermuxConstants.TERMUX_PREFIX,

    /**
     * Termux主目录
     */
    val homePath: String = TermuxConstants.TERMUX_HOME,

    /**
     * Termux临时目录
     */
    val tmpPath: String = TermuxConstants.TERMUX_TMP,

    /**
     * Termux bin目录
     */
    val binPath: String = TermuxConstants.TERMUX_BIN,

    /**
     * Termux shell路径
     */
    val shellPath: String = TermuxConstants.TERMUX_SHELL,

    /**
     * 是否启用调试模式
     */
    val debugMode: Boolean = false,

    /**
     * 命令执行超时时间（毫秒）
     */
    val commandTimeoutMs: Long = TermuxConstants.DEFAULT_COMMAND_TIMEOUT_MS,

    /**
     * 长时间命令执行超时时间（毫秒）
     */
    val longCommandTimeoutMs: Long = TermuxConstants.LONG_COMMAND_TIMEOUT_MS,

    /**
     * 包安装超时时间（毫秒）
     */
    val packageInstallTimeoutMs: Long = TermuxConstants.PACKAGE_INSTALL_TIMEOUT_MS,

    /**
     * 文件操作超时时间（毫秒）
     */
    val fileOperationTimeoutMs: Long = TermuxConstants.FILE_OPERATION_TIMEOUT_MS,

    /**
     * 环境变量配置
     */
    val environmentVariables: Map<String, String> = emptyMap(),

    /**
     * 包管理器配置
     */
    val packageManagerConfig: PackageManagerConfig = PackageManagerConfig()
) {
    companion object {
        /**
         * 默认Termux配置
         */
        val DEFAULT = TermuxConfig()

        /**
         * 调试模式配置
         */
        val DEBUG = TermuxConfig(
            debugMode = true
        )

        /**
         * 从环境变量创建配置
         *
         * @param envVars 环境变量映射
         * @return TermuxConfig实例
         */
        fun fromEnvironment(envVars: Map<String, String>): TermuxConfig {
            return TermuxConfig(
                prefixPath = envVars[TermuxConstants.ENV_PREFIX] ?: TermuxConstants.TERMUX_PREFIX,
                homePath = envVars[TermuxConstants.ENV_HOME] ?: TermuxConstants.TERMUX_HOME,
                tmpPath = envVars[TermuxConstants.ENV_TMPDIR] ?: TermuxConstants.TERMUX_TMP,
                environmentVariables = envVars
            )
        }
    }

    /**
     * 获取完整的环境变量映射
     *
     * @return 环境变量映射
     */
    fun getFullEnvironment(): Map<String, String> {
        return buildMap {
            put(TermuxConstants.ENV_PREFIX, prefixPath)
            put(TermuxConstants.ENV_HOME, homePath)
            put(TermuxConstants.ENV_TMPDIR, tmpPath)
            put(TermuxConstants.ENV_PATH, "$binPath:/system/bin:/system/xbin")
            put(TermuxConstants.ENV_LD_LIBRARY_PATH, "$prefixPath/lib")
            put(TermuxConstants.ENV_TERM, "xterm-256color")
            put(TermuxConstants.ENV_LANG, "en_US.UTF-8")
            putAll(environmentVariables)
        }
    }

    /**
     * 验证配置有效性
     *
     * @return 配置是否有效
     */
    fun validate(): Boolean {
        return prefixPath.isNotBlank() &&
                homePath.isNotBlank() &&
                shellPath.isNotBlank() &&
                commandTimeoutMs > 0 &&
                longCommandTimeoutMs > 0 &&
                packageInstallTimeoutMs > 0 &&
                fileOperationTimeoutMs > 0
    }

    /**
     * 创建Builder用于构建配置
     */
    class Builder {
        private var prefixPath: String = TermuxConstants.TERMUX_PREFIX
        private var homePath: String = TermuxConstants.TERMUX_HOME
        private var tmpPath: String = TermuxConstants.TERMUX_TMP
        private var binPath: String = TermuxConstants.TERMUX_BIN
        private var shellPath: String = TermuxConstants.TERMUX_SHELL
        private var debugMode: Boolean = false
        private var commandTimeoutMs: Long = TermuxConstants.DEFAULT_COMMAND_TIMEOUT_MS
        private var longCommandTimeoutMs: Long = TermuxConstants.LONG_COMMAND_TIMEOUT_MS
        private var packageInstallTimeoutMs: Long = TermuxConstants.PACKAGE_INSTALL_TIMEOUT_MS
        private var fileOperationTimeoutMs: Long = TermuxConstants.FILE_OPERATION_TIMEOUT_MS
        private val environmentVariables: MutableMap<String, String> = mutableMapOf()
        private var packageManagerConfig: PackageManagerConfig = PackageManagerConfig()

        /**
         * 设置前缀路径
         */
        fun prefixPath(path: String) = apply {
            prefixPath = path
        }

        /**
         * 设置主目录
         */
        fun homePath(path: String) = apply {
            homePath = path
        }

        /**
         * 设置临时目录
         */
        fun tmpPath(path: String) = apply {
            tmpPath = path
        }

        /**
         * 设置bin目录
         */
        fun binPath(path: String) = apply {
            binPath = path
        }

        /**
         * 设置shell路径
         */
        fun shellPath(path: String) = apply {
            shellPath = path
        }

        /**
         * 设置调试模式
         */
        fun debugMode(enable: Boolean) = apply {
            debugMode = enable
        }

        /**
         * 设置命令超时时间
         */
        fun commandTimeout(timeoutMs: Long) = apply {
            commandTimeoutMs = timeoutMs
        }

        /**
         * 设置长时间命令超时时间
         */
        fun longCommandTimeout(timeoutMs: Long) = apply {
            longCommandTimeoutMs = timeoutMs
        }

        /**
         * 设置包安装超时时间
         */
        fun packageInstallTimeout(timeoutMs: Long) = apply {
            packageInstallTimeoutMs = timeoutMs
        }

        /**
         * 设置文件操作超时时间
         */
        fun fileOperationTimeout(timeoutMs: Long) = apply {
            fileOperationTimeoutMs = timeoutMs
        }

        /**
         * 添加环境变量
         */
        fun environmentVariable(name: String, value: String) = apply {
            environmentVariables[name] = value
        }

        /**
         * 设置多个环境变量
         */
        fun environmentVariables(vars: Map<String, String>) = apply {
            environmentVariables.putAll(vars)
        }

        /**
         * 设置包管理器配置
         */
        fun packageManagerConfig(config: PackageManagerConfig) = apply {
            packageManagerConfig = config
        }

        /**
         * 构建TermuxConfig
         */
        fun build(): TermuxConfig {
            return TermuxConfig(
                prefixPath = prefixPath,
                homePath = homePath,
                tmpPath = tmpPath,
                binPath = binPath,
                shellPath = shellPath,
                debugMode = debugMode,
                commandTimeoutMs = commandTimeoutMs,
                longCommandTimeoutMs = longCommandTimeoutMs,
                packageInstallTimeoutMs = packageInstallTimeoutMs,
                fileOperationTimeoutMs = fileOperationTimeoutMs,
                environmentVariables = environmentVariables.toMap(),
                packageManagerConfig = packageManagerConfig
            )
        }
    }
}

/**
 * 包管理器配置
 *
 * 用于配置Termux包管理器的行为
 */
data class PackageManagerConfig(
    /**
     * 包管理器类型
     */
    val packageManagerType: PackageManagerType = PackageManagerType.PKG,

    /**
     * 是否自动确认安装
     */
    val autoConfirm: Boolean = true,

    /**
     * 是否在安装前更新包列表
     */
    val updateBeforeInstall: Boolean = true,

    /**
     * 是否允许降级安装
     */
    val allowDowngrade: Boolean = false,

    /**
     * 是否清理缓存
     */
    val cleanCache: Boolean = false,

    /**
     * 自定义镜像源URL
     */
    val mirrorUrl: String? = null,

    /**
     * 最大重试次数
     */
    val maxRetries: Int = 3,

    /**
     * 重试间隔（毫秒）
     */
    val retryDelayMs: Long = 1000L
) {
    companion object {
        /**
         * 默认包管理器配置
         */
        val DEFAULT = PackageManagerConfig()

        /**
         * 快速安装配置（跳过更新）
         */
        val FAST_INSTALL = PackageManagerConfig(
            updateBeforeInstall = false
        )

        /**
         * 安全安装配置（更新并清理缓存）
         */
        val SAFE_INSTALL = PackageManagerConfig(
            updateBeforeInstall = true,
            cleanCache = true
        )
    }

    /**
     * 获取包管理器命令
     *
     * @return 包管理器命令名称
     */
    fun getPackageManagerCommand(): String {
        return when (packageManagerType) {
            PackageManagerType.PKG -> TermuxConstants.PKG_COMMAND
            PackageManagerType.APT -> TermuxConstants.APT_COMMAND
        }
    }

    /**
     * 创建Builder用于构建配置
     */
    class Builder {
        private var packageManagerType: PackageManagerType = PackageManagerType.PKG
        private var autoConfirm: Boolean = true
        private var updateBeforeInstall: Boolean = true
        private var allowDowngrade: Boolean = false
        private var cleanCache: Boolean = false
        private var mirrorUrl: String? = null
        private var maxRetries: Int = 3
        private var retryDelayMs: Long = 1000L

        fun packageManagerType(type: PackageManagerType) = apply { packageManagerType = type }
        fun autoConfirm(enable: Boolean) = apply { autoConfirm = enable }
        fun updateBeforeInstall(enable: Boolean) = apply { updateBeforeInstall = enable }
        fun allowDowngrade(enable: Boolean) = apply { allowDowngrade = enable }
        fun cleanCache(enable: Boolean) = apply { cleanCache = enable }
        fun mirrorUrl(url: String) = apply { mirrorUrl = url }
        fun maxRetries(retries: Int) = apply { maxRetries = retries }
        fun retryDelay(delayMs: Long) = apply { retryDelayMs = delayMs }

        fun build(): PackageManagerConfig {
            return PackageManagerConfig(
                packageManagerType = packageManagerType,
                autoConfirm = autoConfirm,
                updateBeforeInstall = updateBeforeInstall,
                allowDowngrade = allowDowngrade,
                cleanCache = cleanCache,
                mirrorUrl = mirrorUrl,
                maxRetries = maxRetries,
                retryDelayMs = retryDelayMs
            )
        }
    }
}

/**
 * 包管理器类型枚举
 */
enum class PackageManagerType {
    /**
     * pkg命令（Termux推荐）
     */
    PKG,

    /**
     * apt命令
     */
    APT
}

/**
 * Termux执行结果
 *
 * 封装命令执行的结果
 */
data class TermuxExecutionResult(
    /**
     * 是否成功
     */
    val isSuccess: Boolean,

    /**
     * 退出码
     */
    val exitCode: Int,

    /**
     * 标准输出
     */
    val stdout: String,

    /**
     * 标准错误输出
     */
    val stderr: String,

    /**
     * 执行时间（毫秒）
     */
    val executionTimeMs: Long = 0
) {
    /**
     * 获取完整输出（stdout + stderr）
     */
    val fullOutput: String
        get() = buildString {
            if (stdout.isNotEmpty()) {
                append(stdout)
            }
            if (stderr.isNotEmpty()) {
                if (stdout.isNotEmpty()) append("\n")
                append(stderr)
            }
        }

    companion object {
        /**
         * 创建成功结果
         */
        fun success(
            stdout: String,
            stderr: String = "",
            executionTimeMs: Long = 0
        ): TermuxExecutionResult {
            return TermuxExecutionResult(
                isSuccess = true,
                exitCode = 0,
                stdout = stdout,
                stderr = stderr,
                executionTimeMs = executionTimeMs
            )
        }

        /**
         * 创建失败结果
         */
        fun failure(
            exitCode: Int,
            stdout: String = "",
            stderr: String,
            executionTimeMs: Long = 0
        ): TermuxExecutionResult {
            return TermuxExecutionResult(
                isSuccess = false,
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                executionTimeMs = executionTimeMs
            )
        }

        /**
         * 创建超时结果
         */
        fun timeout(executionTimeMs: Long): TermuxExecutionResult {
            return TermuxExecutionResult(
                isSuccess = false,
                exitCode = -1,
                stdout = "",
                stderr = "Command execution timed out",
                executionTimeMs = executionTimeMs
            )
        }
    }
}