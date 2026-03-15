package com.dfa.core.vm.termux

/**
 * Termux桥接接口
 *
 * 提供与Termux环境交互的统一接口，包括环境检查、命令执行、
 * 包管理和文件操作等功能
 */
interface TermuxBridge {

    /**
     * 获取Termux配置
     *
     * @return 当前Termux配置
     */
    val config: TermuxConfig

    // ==================== 环境检查方法 ====================

    /**
     * 检查Termux环境是否可用
     *
     * 验证Termux环境是否已正确初始化，包括检查必要的目录、
     * 环境变量和可执行文件
     *
     * @return Termux环境是否可用
     */
    suspend fun isTermuxAvailable(): Boolean

    /**
     * 检查Termux应用是否已安装
     *
     * 仅检查Termux应用是否存在于系统中，不验证环境完整性
     *
     * @return Termux应用是否已安装
     */
    suspend fun isTermuxInstalled(): Boolean

    /**
     * 检查Termux:API插件是否已安装
     *
     * @return Termux:API插件是否已安装
     */
    suspend fun isTermuxApiInstalled(): Boolean

    /**
     * 获取Termux环境信息
     *
     * @return 环境信息映射
     */
    suspend fun getEnvironmentInfo(): Result<Map<String, String>>

    /**
     * 获取Termux版本信息
     *
     * @return 版本信息字符串
     */
    suspend fun getTermuxVersion(): Result<String>

    // ==================== 命令执行方法 ====================

    /**
     * 执行命令
     *
     * 在Termux环境中执行指定的shell命令
     *
     * @param command 要执行的命令
     * @return 执行结果，包含输出和退出码
     */
    suspend fun executeCommand(command: String): Result<TermuxExecutionResult>

    /**
     * 执行命令（带超时）
     *
     * 在Termux环境中执行指定的shell命令，支持自定义超时时间
     *
     * @param command 要执行的命令
     * @param timeoutMs 超时时间（毫秒）
     * @return 执行结果
     */
    suspend fun executeCommand(
        command: String,
        timeoutMs: Long
    ): Result<TermuxExecutionResult>

    /**
     * 执行脚本
     *
     * 执行多行脚本内容
     *
     * @param script 脚本内容
     * @param scriptName 脚本名称（用于日志）
     * @return 执行结果
     */
    suspend fun executeScript(
        script: String,
        scriptName: String = "script"
    ): Result<TermuxExecutionResult>

    /**
     * 执行长时间运行的命令
     *
     * 用于执行需要较长时间的命令，使用长超时配置
     *
     * @param command 要执行的命令
     * @return 执行结果
     */
    suspend fun executeLongRunningCommand(command: String): Result<TermuxExecutionResult>

    // ==================== 包管理方法 ====================

    /**
     * 安装包
     *
     * 使用Termux包管理器安装指定的软件包
     *
     * @param packageName 包名
     * @return 安装结果
     */
    suspend fun installPackage(packageName: String): Result<TermuxExecutionResult>

    /**
     * 批量安装包
     *
     * 一次性安装多个软件包
     *
     * @param packageNames 包名列表
     * @return 安装结果
     */
    suspend fun installPackages(packageNames: List<String>): Result<TermuxExecutionResult>

    /**
     * 卸载包
     *
     * 卸载指定的软件包
     *
     * @param packageName 包名
     * @return 卸载结果
     */
    suspend fun uninstallPackage(packageName: String): Result<TermuxExecutionResult>

    /**
     * 更新包列表
     *
     * 从远程仓库更新包索引
     *
     * @return 更新结果
     */
    suspend fun updatePackageList(): Result<TermuxExecutionResult>

    /**
     * 升级所有包
     *
     * 升级所有已安装的软件包到最新版本
     *
     * @return 升级结果
     */
    suspend fun upgradePackages(): Result<TermuxExecutionResult>

    /**
     * 搜索包
     *
     * 搜索匹配指定名称的软件包
     *
     * @param query 搜索关键词
     * @return 搜索结果列表
     */
    suspend fun searchPackage(query: String): Result<List<PackageInfo>>

    /**
     * 获取已安装包列表
     *
     * @return 已安装包信息列表
     */
    suspend fun getInstalledPackages(): Result<List<PackageInfo>>

    /**
     * 检查包是否已安装
     *
     * @param packageName 包名
     * @return 是否已安装
     */
    suspend fun isPackageInstalled(packageName: String): Boolean

    // ==================== 文件操作方法 ====================

    /**
     * 读取文件
     *
     * 读取Termux环境中指定路径的文件内容
     *
     * @param path 文件路径
     * @return 文件内容
     */
    suspend fun readFile(path: String): Result<String>

    /**
     * 读取文件（二进制）
     *
     * 以二进制模式读取文件内容
     *
     * @param path 文件路径
     * @return 文件字节数据
     */
    suspend fun readFileBytes(path: String): Result<ByteArray>

    /**
     * 写入文件
     *
     * 将内容写入Termux环境中指定路径的文件
     *
     * @param path 文件路径
     * @param content 文件内容
     * @param append 是否追加模式
     * @return 写入结果
     */
    suspend fun writeFile(
        path: String,
        content: String,
        append: Boolean = false
    ): Result<Unit>

    /**
     * 写入文件（二进制）
     *
     * 以二进制模式写入文件内容
     *
     * @param path 文件路径
     * @param data 文件字节数据
     * @param append 是否追加模式
     * @return 写入结果
     */
    suspend fun writeFileBytes(
        path: String,
        data: ByteArray,
        append: Boolean = false
    ): Result<Unit>

    /**
     * 删除文件
     *
     * @param path 文件路径
     * @return 删除结果
     */
    suspend fun deleteFile(path: String): Result<Unit>

    /**
     * 检查文件是否存在
     *
     * @param path 文件路径
     * @return 文件是否存在
     */
    suspend fun fileExists(path: String): Boolean

    /**
     * 创建目录
     *
     * @param path 目录路径
     * @param recursive 是否递归创建父目录
     * @return 创建结果
     */
    suspend fun createDirectory(
        path: String,
        recursive: Boolean = true
    ): Result<Unit>

    /**
     * 列出目录内容
     *
     * @param path 目录路径
     * @return 目录内容列表
     */
    suspend fun listDirectory(path: String): Result<List<FileInfo>>

    /**
     * 获取文件信息
     *
     * @param path 文件路径
     * @return 文件信息
     */
    suspend fun getFileInfo(path: String): Result<FileInfo>

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @return 复制结果
     */
    suspend fun copyFile(
        sourcePath: String,
        destPath: String
    ): Result<Unit>

    /**
     * 移动文件
     *
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @return 移动结果
     */
    suspend fun moveFile(
        sourcePath: String,
        destPath: String
    ): Result<Unit>

    // ==================== 环境变量方法 ====================

    /**
     * 获取环境变量
     *
     * 获取Termux环境中指定名称的环境变量值
     *
     * @param name 环境变量名
     * @return 环境变量值，不存在则返回null
     */
    suspend fun getEnvVar(name: String): Result<String?>

    /**
     * 设置环境变量
     *
     * 在Termux环境中设置环境变量
     * 注意：此设置仅在当前会话有效
     *
     * @param name 环境变量名
     * @param value 环境变量值
     * @return 设置结果
     */
    suspend fun setEnvVar(
        name: String,
        value: String
    ): Result<Unit>

    /**
     * 删除环境变量
     *
     * @param name 环境变量名
     * @return 删除结果
     */
    suspend fun unsetEnvVar(name: String): Result<Unit>

    /**
     * 获取所有环境变量
     *
     * @return 环境变量映射
     */
    suspend fun getAllEnvVars(): Result<Map<String, String>>

    /**
     * 批量设置环境变量
     *
     * @param envVars 环境变量映射
     * @return 设置结果
     */
    suspend fun setEnvVars(envVars: Map<String, String>): Result<Unit>

    // ==================== 会话管理方法 ====================

    /**
     * 初始化Termux会话
     *
     * 准备Termux环境，包括设置环境变量、检查必要组件等
     *
     * @return 初始化结果
     */
    suspend fun initializeSession(): Result<Unit>

    /**
     * 关闭Termux会话
     *
     * 清理会话资源
     */
    suspend fun closeSession()

    /**
     * 检查会话是否活跃
     *
     * @return 会话是否活跃
     */
    fun isSessionActive(): Boolean
}

/**
 * 包信息数据类
 *
 * 描述Termux软件包的基本信息
 */
data class PackageInfo(
    /**
     * 包名
     */
    val name: String,

    /**
     * 版本号
     */
    val version: String,

    /**
     * 描述
     */
    val description: String = "",

    /**
     * 是否已安装
     */
    val isInstalled: Boolean = false,

    /**
     * 架构
     */
    val architecture: String = "",

    /**
     * 依赖列表
     */
    val dependencies: List<String> = emptyList(),

    /**
     * 安装大小（字节）
     */
    val installedSize: Long = 0,

    /**
     * 下载大小（字节）
     */
    val downloadSize: Long = 0
) {
    companion object {
        /**
         * 从apt输出解析包信息
         *
         * @param output apt命令输出
         * @return 包信息列表
         */
        fun parseFromAptOutput(output: String): List<PackageInfo> {
            // 简单解析实现，实际实现可能需要更复杂的解析逻辑
            return output.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        PackageInfo(
                            name = parts[0],
                            version = parts.getOrElse(1) { "" },
                            description = parts.drop(2).joinToString(" ")
                        )
                    } else null
                }
        }
    }
}

/**
 * 文件信息数据类
 *
 * 描述Termux环境中文件或目录的基本信息
 */
data class FileInfo(
    /**
     * 文件路径
     */
    val path: String,

    /**
     * 文件名
     */
    val name: String,

    /**
     * 是否为目录
     */
    val isDirectory: Boolean,

    /**
     * 是否为文件
     */
    val isFile: Boolean,

    /**
     * 文件大小（字节）
     */
    val size: Long = 0,

    /**
     * 最后修改时间（Unix时间戳）
     */
    val lastModified: Long = 0,

    /**
     * 权限字符串
     */
    val permissions: String = "",

    /**
     * 所有者
     */
    val owner: String = "",

    /**
     * 所属组
     */
    val group: String = ""
) {
    companion object {
        /**
         * 从ls -l输出解析文件信息
         *
         * @param line ls -l输出行
         * @param parentPath 父目录路径
         * @return 文件信息
         */
        fun parseFromLsLine(line: String, parentPath: String): FileInfo? {
            // 解析 ls -l 输出格式
            // drwxr-xr-x 2 user group 4096 Jan 1 12:00 dirname
            val parts = line.split(Regex("\\s+"))
            if (parts.size < 8) return null

            val permissions = parts[0]
            val isDirectory = permissions.startsWith("d")
            val owner = parts[2]
            val group = parts[3]
            val size = parts[4].toLongOrNull() ?: 0
            val name = parts.drop(7).joinToString(" ")

            return FileInfo(
                path = "$parentPath/$name",
                name = name,
                isDirectory = isDirectory,
                isFile = !isDirectory,
                size = size,
                permissions = permissions,
                owner = owner,
                group = group
            )
        }
    }
}

/**
 * TermuxBridge工厂接口
 *
 * 用于创建TermuxBridge实例
 */
interface TermuxBridgeFactory {
    /**
     * 创建TermuxBridge实例
     *
     * @param config Termux配置
     * @return TermuxBridge实例
     */
    fun create(config: TermuxConfig = TermuxConfig.DEFAULT): TermuxBridge

    /**
     * 检查Termux环境是否可用
     *
     * @return Termux环境是否可用
     */
    suspend fun isTermuxAvailable(): Boolean
}