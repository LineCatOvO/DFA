package com.dfa.core.vm.termux

import kotlinx.coroutines.flow.StateFlow

/**
 * Termux包管理器接口
 *
 * 提供Termux环境下的软件包管理功能，包括包列表查询、包信息获取、
 * 包安装/卸载/更新操作，以及仓库管理等功能。
 *
 * 所有操作均使用Result<T>封装结果，支持协程异步执行。
 */
interface TermuxPackageManager {

    /**
     * 包管理器状态
     */
    val state: StateFlow<PackageManagerState>

    /**
     * 当前操作进度
     */
    val progress: StateFlow<PackageOperationProgress?>

    // ==================== 包列表方法 ====================

    /**
     * 获取可用包列表
     *
     * 从配置的仓库获取所有可用软件包的列表
     *
     * @return 包信息列表结果
     */
    suspend fun listPackages(): Result<List<PackageInfo>>

    /**
     * 搜索包
     *
     * 根据关键词搜索匹配的软件包
     *
     * @param query 搜索关键词（支持包名和描述搜索）
     * @return 匹配的包信息列表结果
     */
    suspend fun searchPackages(query: String): Result<List<PackageInfo>>

    /**
     * 获取已安装包列表
     *
     * 获取当前Termux环境中已安装的所有软件包
     *
     * @return 已安装包信息列表结果
     */
    suspend fun listInstalledPackages(): Result<List<PackageInfo>>

    /**
     * 获取可升级包列表
     *
     * 获取有新版本可升级的软件包列表
     *
     * @return 可升级包信息列表结果
     */
    suspend fun listUpgradablePackages(): Result<List<PackageInfo>>

    // ==================== 包信息方法 ====================

    /**
     * 获取包详细信息
     *
     * 获取指定软件包的详细信息，包括版本、描述、依赖等
     *
     * @param name 包名
     * @return 包详细信息结果
     */
    suspend fun getPackageInfo(name: String): Result<PackageDetailedInfo>

    /**
     * 检查包是否已安装
     *
     * @param name 包名
     * @return 是否已安装
     */
    suspend fun isPackageInstalled(name: String): Boolean

    /**
     * 获取包依赖列表
     *
     * 获取指定软件包的所有依赖项
     *
     * @param name 包名
     * @return 依赖包名列表结果
     */
    suspend fun getPackageDependencies(name: String): Result<List<String>>

    /**
     * 获取包反向依赖列表
     *
     * 获取依赖于指定软件包的其他包
     *
     * @param name 包名
     * @return 反向依赖包名列表结果
     */
    suspend fun getPackageReverseDependencies(name: String): Result<List<String>>

    // ==================== 包操作方法 ====================

    /**
     * 安装包
     *
     * 从仓库安装指定的软件包
     *
     * @param name 包名
     * @return 安装操作结果
     */
    suspend fun install(name: String): Result<PackageOperationResult>

    /**
     * 批量安装包
     *
     * 一次性安装多个软件包
     *
     * @param names 包名列表
     * @return 安装操作结果
     */
    suspend fun installBatch(names: List<String>): Result<BatchOperationResult>

    /**
     * 卸载包
     *
     * 卸载指定的软件包
     *
     * @param name 包名
     * @param purge 是否清除配置文件
     * @return 卸载操作结果
     */
    suspend fun uninstall(name: String, purge: Boolean = false): Result<PackageOperationResult>

    /**
     * 批量卸载包
     *
     * 一次性卸载多个软件包
     *
     * @param names 包名列表
     * @param purge 是否清除配置文件
     * @return 卸载操作结果
     */
    suspend fun uninstallBatch(names: List<String>, purge: Boolean = false): Result<BatchOperationResult>

    /**
     * 更新包
     *
     * 将指定的软件包升级到最新版本
     *
     * @param name 包名
     * @return 更新操作结果
     */
    suspend fun update(name: String): Result<PackageOperationResult>

    /**
     * 更新所有包
     *
     * 升级所有已安装的软件包到最新版本
     *
     * @return 更新操作结果
     */
    suspend fun upgradeAll(): Result<PackageOperationResult>

    /**
     * 更新包索引
     *
     * 从远程仓库更新包索引信息
     *
     * @return 更新操作结果
     */
    suspend fun updatePackageIndex(): Result<PackageOperationResult>

    /**
     * 重新安装包
     *
     * 重新安装指定的软件包
     *
     * @param name 包名
     * @return 重装操作结果
     */
    suspend fun reinstall(name: String): Result<PackageOperationResult>

    // ==================== 仓库方法 ====================

    /**
     * 获取仓库列表
     *
     * 获取当前配置的所有软件源仓库
     *
     * @return 仓库信息列表结果
     */
    suspend fun listRepositories(): Result<List<RepositoryInfo>>

    /**
     * 添加仓库
     *
     * 添加新的软件源仓库
     *
     * @param url 仓库URL
     * @param name 仓库名称（可选）
     * @return 添加操作结果
     */
    suspend fun addRepository(url: String, name: String? = null): Result<Unit>

    /**
     * 移除仓库
     *
     * 移除指定的软件源仓库
     *
     * @param urlOrName 仓库URL或名称
     * @return 移除操作结果
     */
    suspend fun removeRepository(urlOrName: String): Result<Unit>

    /**
     * 启用仓库
     *
     * 启用指定的软件源仓库
     *
     * @param urlOrName 仓库URL或名称
     * @return 启用操作结果
     */
    suspend fun enableRepository(urlOrName: String): Result<Unit>

    /**
     * 禁用仓库
     *
     * 禁用指定的软件源仓库
     *
     * @param urlOrName 仓库URL或名称
     * @return 禁用操作结果
     */
    suspend fun disableRepository(urlOrName: String): Result<Unit>

    /**
     * 更换镜像源
     *
     * 更换Termux软件源镜像
     *
     * @param mirror 镜像名称或URL
     * @return 更换操作结果
     */
    suspend fun changeMirror(mirror: String): Result<Unit>

    // ==================== 缓存管理方法 ====================

    /**
     * 清理包缓存
     *
     * 清理下载的包文件缓存
     *
     * @return 清理操作结果
     */
    suspend fun cleanCache(): Result<Unit>

    /**
     * 获取缓存大小
     *
     * 获取包缓存占用的磁盘空间
     *
     * @return 缓存大小（字节）
     */
    suspend fun getCacheSize(): Result<Long>

    /**
     * 自动清理
     *
     * 自动清理不需要的包和缓存
     *
     * @return 清理操作结果
     */
    suspend fun autoremove(): Result<Unit>

    // ==================== 修复方法 ====================

    /**
     * 修复依赖
     *
     * 修复损坏的包依赖关系
     *
     * @return 修复操作结果
     */
    suspend fun fixBrokenDependencies(): Result<Unit>

    /**
     * 检查包完整性
     *
     * 检查已安装包的完整性
     *
     * @return 检查结果
     */
    suspend fun verifyPackages(): Result<List<PackageVerificationResult>>
}

/**
 * 包管理器状态
 *
 * 表示包管理器的当前状态
 */
sealed class PackageManagerState {
    /**
     * 空闲状态
     */
    data object Idle : PackageManagerState()

    /**
     * 正在加载包列表
     */
    data object LoadingPackages : PackageManagerState()

    /**
     * 正在执行操作
     */
    data class Executing(val operation: String) : PackageManagerState()

    /**
     * 操作完成
     */
    data class Completed(val result: PackageOperationResult) : PackageManagerState()

    /**
     * 错误状态
     */
    data class Error(val message: String, val exception: Throwable? = null) : PackageManagerState()
}

/**
 * 包操作进度
 *
 * 表示当前操作的进度信息
 */
data class PackageOperationProgress(
    /**
     * 当前操作描述
     */
    val operation: String,

    /**
     * 当前处理的包名
     */
    val currentPackage: String? = null,

    /**
     * 总包数
     */
    val totalPackages: Int = 0,

    /**
     * 已处理包数
     */
    val processedPackages: Int = 0,

    /**
     * 进度百分比（0-100）
     */
    val percentage: Int = 0,

    /**
     * 下载进度（字节）
     */
    val downloadProgress: Long = 0,

    /**
     * 下载总大小（字节）
     */
    val downloadTotal: Long = 0,

    /**
     * 当前阶段
     */
    val phase: OperationPhase = OperationPhase.PREPARING
) {
    /**
     * 操作阶段
     */
    enum class OperationPhase {
        PREPARING,
        DOWNLOADING,
        INSTALLING,
        CONFIGURING,
        COMPLETED,
        FAILED
    }
}

/**
 * 包操作结果
 *
 * 表示单个包操作的结果
 */
data class PackageOperationResult(
    /**
     * 操作是否成功
     */
    val isSuccess: Boolean,

    /**
     * 操作类型
     */
    val operationType: OperationType,

    /**
     * 操作的包名
     */
    val packageName: String,

    /**
     * 操作消息
     */
    val message: String = "",

    /**
     * 错误信息
     */
    val error: String? = null,

    /**
     * 执行时间（毫秒）
     */
    val executionTimeMs: Long = 0,

    /**
     * 命令输出
     */
    val output: String = ""
) {
    /**
     * 操作类型
     */
    enum class OperationType {
        INSTALL,
        UNINSTALL,
        UPDATE,
        UPGRADE_ALL,
        UPDATE_INDEX,
        REINSTALL,
        CLEAN_CACHE,
        AUTOREMOVE,
        FIX_DEPENDENCIES
    }

    companion object {
        /**
         * 创建成功结果
         */
        fun success(
            operationType: OperationType,
            packageName: String,
            message: String = "",
            executionTimeMs: Long = 0,
            output: String = ""
        ): PackageOperationResult {
            return PackageOperationResult(
                isSuccess = true,
                operationType = operationType,
                packageName = packageName,
                message = message,
                executionTimeMs = executionTimeMs,
                output = output
            )
        }

        /**
         * 创建失败结果
         */
        fun failure(
            operationType: OperationType,
            packageName: String,
            error: String,
            executionTimeMs: Long = 0,
            output: String = ""
        ): PackageOperationResult {
            return PackageOperationResult(
                isSuccess = false,
                operationType = operationType,
                packageName = packageName,
                error = error,
                executionTimeMs = executionTimeMs,
                output = output
            )
        }
    }
}

/**
 * 批量操作结果
 *
 * 表示批量包操作的结果
 */
data class BatchOperationResult(
    /**
     * 操作是否全部成功
     */
    val isSuccess: Boolean,

    /**
     * 操作类型
     */
    val operationType: PackageOperationResult.OperationType,

    /**
     * 成功的包列表
     */
    val succeeded: List<String>,

    /**
     * 失败的包列表
     */
    val failed: List<FailedPackage>,

    /**
     * 总执行时间（毫秒）
     */
    val executionTimeMs: Long = 0
) {
    /**
     * 失败包信息
     */
    data class FailedPackage(
        val name: String,
        val error: String
    )

    /**
     * 获取成功率
     */
    val successRate: Float
        get() {
            val total = succeeded.size + failed.size
            return if (total > 0) succeeded.size.toFloat() / total else 0f
        }
}

/**
 * 包详细信息
 *
 * 包含软件包的完整信息
 */
data class PackageDetailedInfo(
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
     * 推荐包列表
     */
    val recommends: List<String> = emptyList(),

    /**
     * 建议包列表
     */
    val suggests: List<String> = emptyList(),

    /**
     * 反向依赖列表
     */
    val reverseDependencies: List<String> = emptyList(),

    /**
     * 安装大小（字节）
     */
    val installedSize: Long = 0,

    /**
     * 下载大小（字节）
     */
    val downloadSize: Long = 0,

    /**
     * 维护者
     */
    val maintainer: String = "",

    /**
     * 主页
     */
    val homepage: String = "",

    /**
     * 许可证
     */
    val license: String = "",

    /**
     * 所属仓库
     */
    val repository: String = "",

    /**
     * 安装状态
     */
    val installStatus: InstallStatus = InstallStatus.NOT_INSTALLED,

    /**
     * 安装路径
     */
    val installPath: String = ""
) {
    /**
     * 安装状态
     */
    enum class InstallStatus {
        NOT_INSTALLED,
        INSTALLED,
        UPGRADABLE,
        BROKEN,
        HALF_INSTALLED
    }

    /**
     * 格式化安装大小
     */
    fun formatInstalledSize(): String {
        return formatSize(installedSize)
    }

    /**
     * 格式化下载大小
     */
    fun formatDownloadSize(): String {
        return formatSize(downloadSize)
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}

/**
 * 仓库信息
 *
 * 表示软件源仓库的信息
 */
data class RepositoryInfo(
    /**
     * 仓库名称
     */
    val name: String,

    /**
     * 仓库URL
     */
    val url: String,

    /**
     * 是否启用
     */
    val isEnabled: Boolean = true,

    /**
     * 仓库类型
     */
    val type: RepositoryType = RepositoryType.MAIN,

    /**
     * 架构
     */
    val architecture: String = "",

    /**
     * 组件列表
     */
    val components: List<String> = emptyList(),

    /**
     * 包数量
     */
    val packageCount: Int = 0
) {
    /**
     * 仓库类型
     */
    enum class RepositoryType {
        MAIN,
        X11,
        ROOT,
        UNSTABLE,
        CUSTOM
    }
}

/**
 * 包验证结果
 *
 * 表示包完整性验证的结果
 */
data class PackageVerificationResult(
    /**
     * 包名
     */
    val name: String,

    /**
     * 是否通过验证
     */
    val isValid: Boolean,

    /**
     * 问题列表
     */
    val issues: List<String> = emptyList(),

    /**
     * 缺失文件列表
     */
    val missingFiles: List<String> = emptyList(),

    /**
     * 修改文件列表
     */
    val modifiedFiles: List<String> = emptyList()
)