package com.dfa.core.vm.termux

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Termux包管理器实现类
 *
 * 提供Termux环境下的软件包管理功能的具体实现。使用TermuxBridge执行pkg/apt命令，
 * 并解析命令输出获取包信息。所有操作均通过协程实现异步执行。
 *
 * @property bridge Termux桥接实例
 * @property config Termux配置
 */
class TermuxPackageManagerImpl(
    private val bridge: TermuxBridge,
    private val config: TermuxConfig = TermuxConfig.DEFAULT
) : TermuxPackageManager {

    /**
     * 包管理器配置
     */
    private val pkgConfig = config.packageManagerConfig

    /**
     * 包管理器命令
     */
    private val pkgCommand: String
        get() = pkgConfig.getPackageManagerCommand()

    // ==================== 状态管理 ====================

    /**
     * 包管理器状态
     */
    private val _state = MutableStateFlow<PackageManagerState>(PackageManagerState.Idle)
    override val state: StateFlow<PackageManagerState> = _state.asStateFlow()

    /**
     * 当前操作进度
     */
    private val _progress = MutableStateFlow<PackageOperationProgress?>(null)
    override val progress: StateFlow<PackageOperationProgress?> = _progress.asStateFlow()

    // ==================== 包列表方法 ====================

    /**
     * 获取可用包列表
     *
     * 从配置的仓库获取所有可用软件包的列表
     *
     * @return 包信息列表结果
     */
    override suspend fun listPackages(): Result<List<PackageInfo>> = withContext(Dispatchers.IO) {
        _state.value = PackageManagerState.LoadingPackages

        try {
            // 先更新包索引以获取最新列表
            val updateResult = updatePackageIndex()
            if (updateResult.isFailure) {
                // 即使更新失败也尝试获取列表
            }

            // 使用apt-cache获取包列表
            val command = "apt-cache pkgnames | sort"
            val result = bridge.executeCommand(command)

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    val packages = executionResult.stdout.lines()
                        .filter { it.isNotBlank() }
                        .map { PackageInfo(name = it, version = "") }

                    _state.value = PackageManagerState.Idle
                    packages
                } else {
                    _state.value = PackageManagerState.Error("Failed to list packages")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            _state.value = PackageManagerState.Error("Failed to list packages: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 搜索包
     *
     * 根据关键词搜索匹配的软件包
     *
     * @param query 搜索关键词（支持包名和描述搜索）
     * @return 匹配的包信息列表结果
     */
    override suspend fun searchPackages(query: String): Result<List<PackageInfo>> = withContext(Dispatchers.IO) {
        _state.value = PackageManagerState.LoadingPackages

        try {
            val command = "$pkgCommand search $query"
            val result = bridge.executeCommand(command)

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    val packages = parseSearchResults(executionResult.stdout)
                    _state.value = PackageManagerState.Idle
                    packages
                } else {
                    _state.value = PackageManagerState.Error("Search failed: ${executionResult.stderr}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            _state.value = PackageManagerState.Error("Search failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取已安装包列表
     *
     * 获取当前Termux环境中已安装的所有软件包
     *
     * @return 已安装包信息列表结果
     */
    override suspend fun listInstalledPackages(): Result<List<PackageInfo>> = withContext(Dispatchers.IO) {
        _state.value = PackageManagerState.LoadingPackages

        try {
            val result = bridge.getInstalledPackages()

            result.onSuccess {
                _state.value = PackageManagerState.Idle
            }.onFailure {
                _state.value = PackageManagerState.Error("Failed to list installed packages: ${it.message}", it)
            }

            result
        } catch (e: Exception) {
            _state.value = PackageManagerState.Error("Failed to list installed packages: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取可升级包列表
     *
     * 获取有新版本可升级的软件包列表
     *
     * @return 可升级包信息列表结果
     */
    override suspend fun listUpgradablePackages(): Result<List<PackageInfo>> = withContext(Dispatchers.IO) {
        _state.value = PackageManagerState.LoadingPackages

        try {
            // 先更新包索引
            bridge.updatePackageList()

            val command = "apt list --upgradable 2>/dev/null"
            val result = bridge.executeCommand(command)

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    val packages = parseUpgradableList(executionResult.stdout)
                    _state.value = PackageManagerState.Idle
                    packages
                } else {
                    _state.value = PackageManagerState.Idle
                    emptyList()
                }
            }
        } catch (e: Exception) {
            _state.value = PackageManagerState.Error("Failed to list upgradable packages: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== 包信息方法 ====================

    /**
     * 获取包详细信息
     *
     * 获取指定软件包的详细信息，包括版本、描述、依赖等
     *
     * @param name 包名
     * @return 包详细信息结果
     */
    override suspend fun getPackageInfo(name: String): Result<PackageDetailedInfo> = withContext(Dispatchers.IO) {
        try {
            val command = "apt-cache show $name 2>/dev/null"
            val result = bridge.executeCommand(command)

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    parseDetailedPackageInfo(executionResult.stdout, name)
                } else {
                    throw PackageNotFoundException("Package not found: $name")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查包是否已安装
     *
     * @param name 包名
     * @return 是否已安装
     */
    override suspend fun isPackageInstalled(name: String): Boolean {
        return bridge.isPackageInstalled(name)
    }

    /**
     * 获取包依赖列表
     *
     * 获取指定软件包的所有依赖项
     *
     * @param name 包名
     * @return 依赖包名列表结果
     */
    override suspend fun getPackageDependencies(name: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val command = "apt-cache depends $name 2>/dev/null | grep Depends | awk '{print \$2}'"
            val result = bridge.executeCommand(command)

            result.mapCatching { executionResult ->
                executionResult.stdout.lines()
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取包反向依赖列表
     *
     * 获取依赖于指定软件包的其他包
     *
     * @param name 包名
     * @return 反向依赖包名列表结果
     */
    override suspend fun getPackageReverseDependencies(name: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val command = "apt-cache rdepends $name 2>/dev/null | tail -n +3"
            val result = bridge.executeCommand(command)

            result.mapCatching { executionResult ->
                executionResult.stdout.lines()
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 包操作方法 ====================

    /**
     * 安装包
     *
     * 从仓库安装指定的软件包
     *
     * @param name 包名
     * @return 安装操作结果
     */
    override suspend fun install(name: String): Result<PackageOperationResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _state.value = PackageManagerState.Executing("Installing $name")
        _progress.value = PackageOperationProgress(
            operation = "Installing $name",
            currentPackage = name,
            phase = PackageOperationProgress.OperationPhase.PREPARING
        )

        try {
            // 检查是否已安装
            if (isPackageInstalled(name)) {
                val result = PackageOperationResult.success(
                    operationType = PackageOperationResult.OperationType.INSTALL,
                    packageName = name,
                    message = "Package already installed",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
                _state.value = PackageManagerState.Completed(result)
                _progress.value = null
                return@withContext Result.success(result)
            }

            // 更新包索引（如果配置要求）
            if (pkgConfig.updateBeforeInstall) {
                _progress.value = _progress.value?.copy(phase = PackageOperationProgress.OperationPhase.PREPARING)
                bridge.updatePackageList()
            }

            // 执行安装
            _progress.value = _progress.value?.copy(phase = PackageOperationProgress.OperationPhase.DOWNLOADING)
            val confirmFlag = if (pkgConfig.autoConfirm) "-y " else ""
            val command = "$pkgCommand install $confirmFlag$name"
            val result = bridge.executeCommand(command, config.packageInstallTimeoutMs)

            val executionTime = System.currentTimeMillis() - startTime

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    _progress.value = _progress.value?.copy(phase = PackageOperationProgress.OperationPhase.COMPLETED)
                    val opResult = PackageOperationResult.success(
                        operationType = PackageOperationResult.OperationType.INSTALL,
                        packageName = name,
                        message = "Package installed successfully",
                        executionTimeMs = executionTime,
                        output = executionResult.stdout
                    )
                    _state.value = PackageManagerState.Completed(opResult)
                    _progress.value = null
                    opResult
                } else {
                    _progress.value = _progress.value?.copy(phase = PackageOperationProgress.OperationPhase.FAILED)
                    val opResult = PackageOperationResult.failure(
                        operationType = PackageOperationResult.OperationType.INSTALL,
                        packageName = name,
                        error = executionResult.stderr,
                        executionTimeMs = executionTime,
                        output = executionResult.fullOutput
                    )
                    _state.value = PackageManagerState.Error("Installation failed: ${executionResult.stderr}")
                    _progress.value = null
                    opResult
                }
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            _progress.value = _progress.value?.copy(phase = PackageOperationProgress.OperationPhase.FAILED)
            _state.value = PackageManagerState.Error("Installation failed: ${e.message}", e)
            _progress.value = null

            Result.success(
                PackageOperationResult.failure(
                    operationType = PackageOperationResult.OperationType.INSTALL,
                    packageName = name,
                    error = e.message ?: "Unknown error",
                    executionTimeMs = executionTime
                )
            )
        }
    }

    /**
     * 批量安装包
     *
     * 一次性安装多个软件包
     *
     * @param names 包名列表
     * @return 安装操作结果
     */
    override suspend fun installBatch(names: List<String>): Result<BatchOperationResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _state.value = PackageManagerState.Executing("Installing ${names.size} packages")

        val succeeded = mutableListOf<String>()
        val failed = mutableListOf<BatchOperationResult.FailedPackage>()

        // 更新包索引
        if (pkgConfig.updateBeforeInstall) {
            bridge.updatePackageList()
        }

        // 批量安装
        val confirmFlag = if (pkgConfig.autoConfirm) "-y " else ""
        val packages = names.joinToString(" ")
        val command = "$pkgCommand install $confirmFlag$packages"
        val result = bridge.executeCommand(command, config.packageInstallTimeoutMs)

        val executionTime = System.currentTimeMillis() - startTime

        result.mapCatching { executionResult ->
            if (executionResult.isSuccess) {
                // 解析成功安装的包
                names.forEach { name ->
                    if (isPackageInstalled(name)) {
                        succeeded.add(name)
                    } else {
                        failed.add(BatchOperationResult.FailedPackage(name, "Installation failed"))
                    }
                }
            } else {
                // 全部标记为失败
                names.forEach { name ->
                    failed.add(BatchOperationResult.FailedPackage(name, executionResult.stderr))
                }
            }

            _state.value = PackageManagerState.Idle
            BatchOperationResult(
                isSuccess = failed.isEmpty(),
                operationType = PackageOperationResult.OperationType.INSTALL,
                succeeded = succeeded,
                failed = failed,
                executionTimeMs = executionTime
            )
        }
    }

    /**
     * 卸载包
     *
     * 卸载指定的软件包
     *
     * @param name 包名
     * @param purge 是否清除配置文件
     * @return 卸载操作结果
     */
    override suspend fun uninstall(name: String, purge: Boolean): Result<PackageOperationResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _state.value = PackageManagerState.Executing("Uninstalling $name")

        try {
            // 检查是否已安装
            if (!isPackageInstalled(name)) {
                val result = PackageOperationResult.success(
                    operationType = PackageOperationResult.OperationType.UNINSTALL,
                    packageName = name,
                    message = "Package not installed",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
                _state.value = PackageManagerState.Completed(result)
                return@withContext Result.success(result)
            }

            val confirmFlag = if (pkgConfig.autoConfirm) "-y " else ""
            val purgeFlag = if (purge) "--purge " else ""
            val command = "$pkgCommand uninstall $confirmFlag$purgeFlag$name"
            val result = bridge.executeCommand(command, config.packageInstallTimeoutMs)

            val executionTime = System.currentTimeMillis() - startTime

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    val opResult = PackageOperationResult.success(
                        operationType = PackageOperationResult.OperationType.UNINSTALL,
                        packageName = name,
                        message = "Package uninstalled successfully",
                        executionTimeMs = executionTime,
                        output = executionResult.stdout
                    )
                    _state.value = PackageManagerState.Completed(opResult)
                    opResult
                } else {
                    val opResult = PackageOperationResult.failure(
                        operationType = PackageOperationResult.OperationType.UNINSTALL,
                        packageName = name,
                        error = executionResult.stderr,
                        executionTimeMs = executionTime,
                        output = executionResult.fullOutput
                    )
                    _state.value = PackageManagerState.Error("Uninstall failed: ${executionResult.stderr}")
                    opResult
                }
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            _state.value = PackageManagerState.Error("Uninstall failed: ${e.message}", e)

            Result.success(
                PackageOperationResult.failure(
                    operationType = PackageOperationResult.OperationType.UNINSTALL,
                    packageName = name,
                    error = e.message ?: "Unknown error",
                    executionTimeMs = executionTime
                )
            )
        }
    }

    /**
     * 批量卸载包
     *
     * 一次性卸载多个软件包
     *
     * @param names 包名列表
     * @param purge 是否清除配置文件
     * @return 卸载操作结果
     */
    override suspend fun uninstallBatch(names: List<String>, purge: Boolean): Result<BatchOperationResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _state.value = PackageManagerState.Executing("Uninstalling ${names.size} packages")

        val succeeded = mutableListOf<String>()
        val failed = mutableListOf<BatchOperationResult.FailedPackage>()

        val confirmFlag = if (pkgConfig.autoConfirm) "-y " else ""
        val purgeFlag = if (purge) "--purge " else ""
        val packages = names.joinToString(" ")
        val command = "$pkgCommand uninstall $confirmFlag$purgeFlag$packages"
        val result = bridge.executeCommand(command, config.packageInstallTimeoutMs)

        val executionTime = System.currentTimeMillis() - startTime

        result.mapCatching { executionResult ->
            if (executionResult.isSuccess) {
                names.forEach { name ->
                    if (!isPackageInstalled(name)) {
                        succeeded.add(name)
                    } else {
                        failed.add(BatchOperationResult.FailedPackage(name, "Uninstall failed"))
                    }
                }
            } else {
                names.forEach { name ->
                    failed.add(BatchOperationResult.FailedPackage(name, executionResult.stderr))
                }
            }

            _state.value = PackageManagerState.Idle
            BatchOperationResult(
                isSuccess = failed.isEmpty(),
                operationType = PackageOperationResult.OperationType.UNINSTALL,
                succeeded = succeeded,
                failed = failed,
                executionTimeMs = executionTime
            )
        }
    }

    /**
     * 更新包
     *
     * 将指定的软件包升级到最新版本
     *
     * @param name 包名
     * @return 更新操作结果
     */
    override suspend fun update(name: String): Result<PackageOperationResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _state.value = PackageManagerState.Executing("Updating $name")

        try {
            val confirmFlag = if (pkgConfig.autoConfirm) "-y " else ""
            val command = "$pkgCommand upgrade $confirmFlag$name"
            val result = bridge.executeCommand(command, config.packageInstallTimeoutMs)

            val executionTime = System.currentTimeMillis() - startTime

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    val opResult = PackageOperationResult.success(
                        operationType = PackageOperationResult.OperationType.UPDATE,
                        packageName = name,
                        message = "Package updated successfully",
                        executionTimeMs = executionTime,
                        output = executionResult.stdout
                    )
                    _state.value = PackageManagerState.Completed(opResult)
                    opResult
                } else {
                    val opResult = PackageOperationResult.failure(
                        operationType = PackageOperationResult.OperationType.UPDATE,
                        packageName = name,
                        error = executionResult.stderr,
                        executionTimeMs = executionTime,
                        output = executionResult.fullOutput
                    )
                    _state.value = PackageManagerState.Error("Update failed: ${executionResult.stderr}")
                    opResult
                }
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            _state.value = PackageManagerState.Error("Update failed: ${e.message}", e)

            Result.success(
                PackageOperationResult.failure(
                    operationType = PackageOperationResult.OperationType.UPDATE,
                    packageName = name,
                    error = e.message ?: "Unknown error",
                    executionTimeMs = executionTime
                )
            )
        }
    }

    /**
     * 更新所有包
     *
     * 升级所有已安装的软件包到最新版本
     *
     * @return 更新操作结果
     */
    override suspend fun upgradeAll(): Result<PackageOperationResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _state.value = PackageManagerState.Executing("Upgrading all packages")
        _progress.value = PackageOperationProgress(
            operation = "Upgrading all packages",
            phase = PackageOperationProgress.OperationPhase.PREPARING
        )

        try {
            // 先更新包索引
            _progress.value = _progress.value?.copy(phase = PackageOperationProgress.OperationPhase.PREPARING)
            bridge.updatePackageList()

            // 执行升级
            _progress.value = _progress.value?.copy(phase = PackageOperationProgress.OperationPhase.INSTALLING)
            val confirmFlag = if (pkgConfig.autoConfirm) "-y " else ""
            val command = "$pkgCommand upgrade $confirmFlag"
            val result = bridge.executeCommand(command, config.longCommandTimeoutMs)

            val executionTime = System.currentTimeMillis() - startTime

            result.mapCatching { executionResult ->
                _progress.value = _progress.value?.copy(phase = PackageOperationProgress.OperationPhase.COMPLETED)
                if (executionResult.isSuccess) {
                    val opResult = PackageOperationResult.success(
                        operationType = PackageOperationResult.OperationType.UPGRADE_ALL,
                        packageName = "all",
                        message = "All packages upgraded successfully",
                        executionTimeMs = executionTime,
                        output = executionResult.stdout
                    )
                    _state.value = PackageManagerState.Completed(opResult)
                    _progress.value = null
                    opResult
                } else {
                    val opResult = PackageOperationResult.failure(
                        operationType = PackageOperationResult.OperationType.UPGRADE_ALL,
                        packageName = "all",
                        error = executionResult.stderr,
                        executionTimeMs = executionTime,
                        output = executionResult.fullOutput
                    )
                    _state.value = PackageManagerState.Error("Upgrade failed: ${executionResult.stderr}")
                    _progress.value = null
                    opResult
                }
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            _progress.value = _progress.value?.copy(phase = PackageOperationProgress.OperationPhase.FAILED)
            _state.value = PackageManagerState.Error("Upgrade failed: ${e.message}", e)
            _progress.value = null

            Result.success(
                PackageOperationResult.failure(
                    operationType = PackageOperationResult.OperationType.UPGRADE_ALL,
                    packageName = "all",
                    error = e.message ?: "Unknown error",
                    executionTimeMs = executionTime
                )
            )
        }
    }

    /**
     * 更新包索引
     *
     * 从远程仓库更新包索引信息
     *
     * @return 更新操作结果
     */
    override suspend fun updatePackageIndex(): Result<PackageOperationResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _state.value = PackageManagerState.Executing("Updating package index")

        try {
            val command = "$pkgCommand update"
            val result = bridge.executeCommand(command, config.packageInstallTimeoutMs)

            val executionTime = System.currentTimeMillis() - startTime

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    val opResult = PackageOperationResult.success(
                        operationType = PackageOperationResult.OperationType.UPDATE_INDEX,
                        packageName = "index",
                        message = "Package index updated successfully",
                        executionTimeMs = executionTime,
                        output = executionResult.stdout
                    )
                    _state.value = PackageManagerState.Completed(opResult)
                    opResult
                } else {
                    val opResult = PackageOperationResult.failure(
                        operationType = PackageOperationResult.OperationType.UPDATE_INDEX,
                        packageName = "index",
                        error = executionResult.stderr,
                        executionTimeMs = executionTime,
                        output = executionResult.fullOutput
                    )
                    _state.value = PackageManagerState.Error("Update index failed: ${executionResult.stderr}")
                    opResult
                }
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            _state.value = PackageManagerState.Error("Update index failed: ${e.message}", e)

            Result.success(
                PackageOperationResult.failure(
                    operationType = PackageOperationResult.OperationType.UPDATE_INDEX,
                    packageName = "index",
                    error = e.message ?: "Unknown error",
                    executionTimeMs = executionTime
                )
            )
        }
    }

    /**
     * 重新安装包
     *
     * 重新安装指定的软件包
     *
     * @param name 包名
     * @return 重装操作结果
     */
    override suspend fun reinstall(name: String): Result<PackageOperationResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _state.value = PackageManagerState.Executing("Reinstalling $name")

        try {
            val confirmFlag = if (pkgConfig.autoConfirm) "-y " else ""
            val command = "$pkgCommand reinstall $confirmFlag$name"
            val result = bridge.executeCommand(command, config.packageInstallTimeoutMs)

            val executionTime = System.currentTimeMillis() - startTime

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    val opResult = PackageOperationResult.success(
                        operationType = PackageOperationResult.OperationType.REINSTALL,
                        packageName = name,
                        message = "Package reinstalled successfully",
                        executionTimeMs = executionTime,
                        output = executionResult.stdout
                    )
                    _state.value = PackageManagerState.Completed(opResult)
                    opResult
                } else {
                    val opResult = PackageOperationResult.failure(
                        operationType = PackageOperationResult.OperationType.REINSTALL,
                        packageName = name,
                        error = executionResult.stderr,
                        executionTimeMs = executionTime,
                        output = executionResult.fullOutput
                    )
                    _state.value = PackageManagerState.Error("Reinstall failed: ${executionResult.stderr}")
                    opResult
                }
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            _state.value = PackageManagerState.Error("Reinstall failed: ${e.message}", e)

            Result.success(
                PackageOperationResult.failure(
                    operationType = PackageOperationResult.OperationType.REINSTALL,
                    packageName = name,
                    error = e.message ?: "Unknown error",
                    executionTimeMs = executionTime
                )
            )
        }
    }

    // ==================== 仓库方法 ====================

    /**
     * 获取仓库列表
     *
     * 获取当前配置的所有软件源仓库
     *
     * @return 仓库信息列表结果
     */
    override suspend fun listRepositories(): Result<List<RepositoryInfo>> = withContext(Dispatchers.IO) {
        try {
            val sourcesListPath = "${config.prefixPath}/etc/apt/sources.list"
            val result = bridge.readFile(sourcesListPath)

            result.mapCatching { content ->
                parseSourcesList(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 添加仓库
     *
     * 添加新的软件源仓库
     *
     * @param url 仓库URL
     * @param name 仓库名称（可选）
     * @return 添加操作结果
     */
    override suspend fun addRepository(url: String, name: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sourcesListPath = "${config.prefixPath}/etc/apt/sources.list"
            val entry = if (name != null) {
                "# $name\n deb $url main"
            } else {
                "deb $url main"
            }

            val command = "echo '$entry' >> $sourcesListPath"
            val result = bridge.executeCommand(command)

            // 更新包索引
            if (result.isSuccess) {
                bridge.updatePackageList()
            }

            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 移除仓库
     *
     * 移除指定的软件源仓库
     *
     * @param urlOrName 仓库URL或名称
     * @return 移除操作结果
     */
    override suspend fun removeRepository(urlOrName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sourcesListPath = "${config.prefixPath}/etc/apt/sources.list"
            val command = "sed -i '\\|$urlOrName|d' $sourcesListPath"
            val result = bridge.executeCommand(command)

            // 更新包索引
            if (result.isSuccess) {
                bridge.updatePackageList()
            }

            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 启用仓库
     *
     * 启用指定的软件源仓库
     *
     * @param urlOrName 仓库URL或名称
     * @return 启用操作结果
     */
    override suspend fun enableRepository(urlOrName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sourcesListPath = "${config.prefixPath}/etc/apt/sources.list"
            // 移除注释行
            val command = "sed -i 's|^# *\\(.*$urlOrName.*\\)|\\1|' $sourcesListPath"
            val result = bridge.executeCommand(command)

            if (result.isSuccess) {
                bridge.updatePackageList()
            }

            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 禁用仓库
     *
     * 禁用指定的软件源仓库
     *
     * @param urlOrName 仓库URL或名称
     * @return 禁用操作结果
     */
    override suspend fun disableRepository(urlOrName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sourcesListPath = "${config.prefixPath}/etc/apt/sources.list"
            // 添加注释
            val command = "sed -i 's|^\\(.*$urlOrName.*\\)|# \\1|' $sourcesListPath"
            val result = bridge.executeCommand(command)

            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更换镜像源
     *
     * 更换Termux软件源镜像
     *
     * @param mirror 镜像名称或URL
     * @return 更换操作结果
     */
    override suspend fun changeMirror(mirror: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 使用termux-change-repo命令
            val command = "termux-change-repo $mirror"
            val result = bridge.executeCommand(command)

            if (result.isSuccess) {
                bridge.updatePackageList()
            }

            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 缓存管理方法 ====================

    /**
     * 清理包缓存
     *
     * 清理下载的包文件缓存
     *
     * @return 清理操作结果
     */
    override suspend fun cleanCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val command = "$pkgCommand clean"
            val result = bridge.executeCommand(command)

            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取缓存大小
     *
     * 获取包缓存占用的磁盘空间
     *
     * @return 缓存大小（字节）
     */
    override suspend fun getCacheSize(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val cachePath = "${config.prefixPath}/var/cache/apt/archives"
            val command = "du -sb $cachePath 2>/dev/null | awk '{print \$1}'"
            val result = bridge.executeCommand(command)

            result.mapCatching { executionResult ->
                executionResult.stdout.trim().toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 自动清理
     *
     * 自动清理不需要的包和缓存
     *
     * @return 清理操作结果
     */
    override suspend fun autoremove(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val confirmFlag = if (pkgConfig.autoConfirm) "-y " else ""
            val command = "$pkgCommand autoremove $confirmFlag"
            val result = bridge.executeCommand(command)

            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 修复方法 ====================

    /**
     * 修复依赖
     *
     * 修复损坏的包依赖关系
     *
     * @return 修复操作结果
     */
    override suspend fun fixBrokenDependencies(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val confirmFlag = if (pkgConfig.autoConfirm) "-y " else ""
            val command = "$pkgCommand install -f $confirmFlag"
            val result = bridge.executeCommand(command, config.packageInstallTimeoutMs)

            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查包完整性
     *
     * 检查已安装包的完整性
     *
     * @return 检查结果
     */
    override suspend fun verifyPackages(): Result<List<PackageVerificationResult>> = withContext(Dispatchers.IO) {
        try {
            val command = "dpkg --audit"
            val result = bridge.executeCommand(command)

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess && executionResult.stdout.isBlank()) {
                    // 没有问题
                    emptyList()
                } else {
                    // 解析问题
                    parseVerificationResults(executionResult.stdout)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 解析方法 ====================

    /**
     * 解析搜索结果
     */
    private fun parseSearchResults(output: String): List<PackageInfo> {
        return output.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                // 格式: package-name/architecture version description
                val parts = line.split(Regex("\\s+"))
                if (parts.isNotEmpty()) {
                    val namePart = parts[0]
                    val name = if (namePart.contains("/")) {
                        namePart.substringBefore("/")
                    } else {
                        namePart
                    }

                    PackageInfo(
                        name = name,
                        version = parts.getOrElse(1) { "" },
                        description = parts.drop(2).joinToString(" "),
                        architecture = if (namePart.contains("/")) {
                            namePart.substringAfter("/")
                        } else {
                            ""
                        }
                    )
                } else null
            }
    }

    /**
     * 解析可升级包列表
     */
    private fun parseUpgradableList(output: String): List<PackageInfo> {
        return output.lines()
            .filter { it.isNotBlank() && it.contains("/") }
            .mapNotNull { line ->
                // 格式: package-name/stable 1.0.0 aarch64 [upgradable from: 0.9.0]
                val parts = line.split(Regex("\\s+"))
                if (parts.isNotEmpty()) {
                    val namePart = parts[0]
                    val name = namePart.substringBefore("/")

                    PackageInfo(
                        name = name,
                        version = parts.getOrElse(1) { "" },
                        architecture = parts.getOrElse(2) { "" },
                        isInstalled = true
                    )
                } else null
            }
    }

    /**
     * 解析详细包信息
     */
    private suspend fun parseDetailedPackageInfo(output: String, packageName: String): PackageDetailedInfo {
        var version = ""
        var description = ""
        var architecture = ""
        var installedSize = 0L
        var maintainer = ""
        var homepage = ""
        var dependencies = mutableListOf<String>()
        var recommends = mutableListOf<String>()
        var suggests = mutableListOf<String>()
        var repository = ""

        output.lines().forEach { line ->
            when {
                line.startsWith("Package:") -> {}
                line.startsWith("Version:") -> version = line.substringAfter("Version:").trim()
                line.startsWith("Description:") -> description = line.substringAfter("Description:").trim()
                line.startsWith("Architecture:") -> architecture = line.substringAfter("Architecture:").trim()
                line.startsWith("Installed-Size:") -> installedSize = line.substringAfter("Installed-Size:").trim().toLongOrNull() ?: 0L
                line.startsWith("Maintainer:") -> maintainer = line.substringAfter("Maintainer:").trim()
                line.startsWith("Homepage:") -> homepage = line.substringAfter("Homepage:").trim()
                line.startsWith("Depends:") -> {
                    dependencies = line.substringAfter("Depends:").trim()
                        .split(",")
                        .map { it.trim().split(Regex("\\s+")).first() }
                        .toMutableList()
                }
                line.startsWith("Recommends:") -> {
                    recommends = line.substringAfter("Recommends:").trim()
                        .split(",")
                        .map { it.trim().split(Regex("\\s+")).first() }
                        .toMutableList()
                }
                line.startsWith("Suggests:") -> {
                    suggests = line.substringAfter("Suggests:").trim()
                        .split(",")
                        .map { it.trim().split(Regex("\\s+")).first() }
                        .toMutableList()
                }
                line.startsWith("Filename:") -> repository = line.substringAfter("Filename:").trim()
            }
        }

        return PackageDetailedInfo(
            name = packageName,
            version = version,
            description = description,
            isInstalled = isPackageInstalled(packageName),
            architecture = architecture,
            dependencies = dependencies,
            recommends = recommends,
            suggests = suggests,
            installedSize = installedSize * 1024, // 转换为字节
            maintainer = maintainer,
            homepage = homepage,
            repository = repository
        )
    }

    /**
     * 解析sources.list
     */
    private fun parseSourcesList(content: String): List<RepositoryInfo> {
        return content.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                // 格式: deb [arch=...] url distribution component1 component2 ...
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 3 && parts[0] == "deb") {
                    val urlIndex = parts.indexOfFirst { it.startsWith("http") || it.startsWith("https") }
                    if (urlIndex >= 0) {
                        RepositoryInfo(
                            name = parts.getOrNull(urlIndex + 1) ?: "",
                            url = parts[urlIndex],
                            isEnabled = true,
                            components = parts.drop(urlIndex + 2)
                        )
                    } else null
                } else null
            }
    }

    /**
     * 解析验证结果
     */
    private fun parseVerificationResults(output: String): List<PackageVerificationResult> {
        val results = mutableListOf<PackageVerificationResult>()

        output.lines()
            .filter { it.isNotBlank() }
            .forEach { line ->
                // 解析dpkg --audit输出
                if (line.contains("not installed")) {
                    val name = line.substringBefore(" ").trim()
                    results.add(
                        PackageVerificationResult(
                            name = name,
                            isValid = false,
                            issues = listOf("Package not fully installed")
                        )
                    )
                } else if (line.contains("half-configured")) {
                    val name = line.substringBefore(" ").trim()
                    results.add(
                        PackageVerificationResult(
                            name = name,
                            isValid = false,
                            issues = listOf("Package is half-configured")
                        )
                    )
                }
            }

        return results
    }
}

/**
 * 包未找到异常
 */
class PackageNotFoundException(message: String) : Exception(message)

/**
 * 包操作异常
 */
class PackageOperationException(
    message: String,
    val packageName: String,
    val operationType: PackageOperationResult.OperationType
) : Exception(message)

/**
 * TermuxPackageManager工厂
 */
object TermuxPackageManagerFactory {
    /**
     * 创建TermuxPackageManager实例
     *
     * @param bridge Termux桥接实例
     * @param config Termux配置
     * @return TermuxPackageManager实例
     */
    fun create(bridge: TermuxBridge, config: TermuxConfig = TermuxConfig.DEFAULT): TermuxPackageManager {
        return TermuxPackageManagerImpl(bridge, config)
    }
}