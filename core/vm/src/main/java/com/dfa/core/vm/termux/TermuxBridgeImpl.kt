package com.dfa.core.vm.termux

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Termux桥接实现类
 *
 * 提供与Termux环境交互的具体实现，包括环境检查、命令执行、
 * 包管理和文件操作等功能。使用ProcessBuilder执行Termux命令，
 * 并通过Kotlin协程实现异步操作。
 *
 * @property config Termux配置
 */
class TermuxBridgeImpl(
    override val config: TermuxConfig = TermuxConfig.DEFAULT
) : TermuxBridge {

    /**
     * 会话活跃状态
     */
    private var sessionActive = false

    /**
     * 环境变量缓存
     */
    private val envVarCache = mutableMapOf<String, String>()

    // ==================== 环境检查方法 ====================

    /**
     * 检查Termux环境是否可用
     *
     * 验证Termux环境是否已正确初始化，包括检查必要的目录、
     * 环境变量和可执行文件
     *
     * @return Termux环境是否可用
     */
    override suspend fun isTermuxAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查关键目录是否存在
            val prefixDir = File(config.prefixPath)
            val homeDir = File(config.homePath)
            val binDir = File(config.binPath)

            if (!prefixDir.exists() || !homeDir.exists() || !binDir.exists()) {
                return@withContext false
            }

            // 检查shell是否存在
            val shellFile = File(config.shellPath)
            if (!shellFile.exists()) {
                return@withContext false
            }

            // 检查pkg命令是否可用
            val result = executeCommand("which ${TermuxConstants.PKG_COMMAND}")
            result.isSuccess && result.getOrNull()?.stdout?.isNotBlank() == true
        } catch (e: Exception) {
            if (config.debugMode) {
                println("Termux availability check failed: ${e.message}")
            }
            false
        }
    }

    /**
     * 检查Termux应用是否已安装
     *
     * 仅检查Termux应用是否存在于系统中，不验证环境完整性
     *
     * @return Termux应用是否已安装
     */
    override suspend fun isTermuxInstalled(): Boolean = withContext(Dispatchers.IO) {
        try {
            val termuxDir = File(TermuxConstants.TERMUX_FILES_PATH)
            termuxDir.exists() && termuxDir.isDirectory
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查Termux:API插件是否已安装
     *
     * @return Termux:API插件是否已安装
     */
    override suspend fun isTermuxApiInstalled(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查termux-api包是否安装
            val result = executeCommand("which termux-api")
            result.isSuccess && result.getOrNull()?.stdout?.isNotBlank() == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取Termux环境信息
     *
     * @return 环境信息映射
     */
    override suspend fun getEnvironmentInfo(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val envInfo = mutableMapOf<String, String>()

            // 获取基本环境变量
            envInfo["PREFIX"] = config.prefixPath
            envInfo["HOME"] = config.homePath
            envInfo["TMPDIR"] = config.tmpPath
            envInfo["PATH"] = "${config.binPath}:/system/bin:/system/xbin"

            // 获取架构信息
            val archResult = executeCommand("uname -m")
            archResult.getOrNull()?.stdout?.trim()?.let { arch ->
                envInfo["ARCH"] = arch
            }

            // 获取Termux版本
            val versionResult = getTermuxVersion()
            versionResult.getOrNull()?.let { version ->
                envInfo["TERMUX_VERSION"] = version
            }

            // 获取已安装包数量
            val pkgCountResult = executeCommand("dpkg -l | wc -l")
            pkgCountResult.getOrNull()?.stdout?.trim()?.let { count ->
                envInfo["INSTALLED_PACKAGES"] = count
            }

            Result.success(envInfo.toMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取Termux版本信息
     *
     * @return 版本信息字符串
     */
    override suspend fun getTermuxVersion(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 尝试从termux-info获取版本
            val result = executeCommand("cat ${config.prefixPath}/etc/termux.info 2>/dev/null || echo 'unknown'")
            val output = result.getOrNull()?.stdout?.trim() ?: "unknown"
            Result.success(output)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 命令执行方法 ====================

    /**
     * 执行命令
     *
     * 在Termux环境中执行指定的shell命令
     *
     * @param command 要执行的命令
     * @return 执行结果，包含输出和退出码
     */
    override suspend fun executeCommand(command: String): Result<TermuxExecutionResult> {
        return executeCommand(command, config.commandTimeoutMs)
    }

    /**
     * 执行命令（带超时）
     *
     * 在Termux环境中执行指定的shell命令，支持自定义超时时间
     *
     * @param command 要执行的命令
     * @param timeoutMs 超时时间（毫秒）
     * @return 执行结果
     */
    override suspend fun executeCommand(
        command: String,
        timeoutMs: Long
    ): Result<TermuxExecutionResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            withTimeoutOrNull(timeoutMs) {
                executeCommandInternal(command)
            } ?: run {
                val executionTime = System.currentTimeMillis() - startTime
                Result.success(TermuxExecutionResult.timeout(executionTime))
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            Result.failure(TermuxExecutionException("Command execution failed: ${e.message}", e))
        }
    }

    /**
     * 执行脚本
     *
     * 执行多行脚本内容
     *
     * @param script 脚本内容
     * @param scriptName 脚本名称（用于日志）
     * @return 执行结果
     */
    override suspend fun executeScript(
        script: String,
        scriptName: String
    ): Result<TermuxExecutionResult> = withContext(Dispatchers.IO) {
        try {
            // 创建临时脚本文件
            val scriptFile = File("${config.tmpPath}/$scriptName.sh")
            scriptFile.writeText(script)

            // 设置执行权限
            scriptFile.setExecutable(true)

            // 执行脚本
            val result = executeCommand("bash ${scriptFile.absolutePath}")

            // 清理临时文件
            scriptFile.delete()

            result
        } catch (e: Exception) {
            Result.failure(TermuxExecutionException("Script execution failed: ${e.message}", e))
        }
    }

    /**
     * 执行长时间运行的命令
     *
     * 用于执行需要较长时间的命令，使用长超时配置
     *
     * @param command 要执行的命令
     * @return 执行结果
     */
    override suspend fun executeLongRunningCommand(command: String): Result<TermuxExecutionResult> {
        return executeCommand(command, config.longCommandTimeoutMs)
    }

    /**
     * 内部命令执行方法
     *
     * @param command 要执行的命令
     * @return 执行结果
     */
    private fun executeCommandInternal(command: String): Result<TermuxExecutionResult> {
        val startTime = System.currentTimeMillis()

        return try {
            val processBuilder = ProcessBuilder(config.shellPath, "-c", command)

            // 设置环境变量
            val environment = processBuilder.environment()
            environment.putAll(config.getFullEnvironment())

            // 设置工作目录
            processBuilder.directory(File(config.homePath))

            // 重定向错误流
            processBuilder.redirectErrorStream(false)

            val process = processBuilder.start()

            // 读取输出
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()

            // 等待进程完成
            val exitCode = process.waitFor()

            val executionTime = System.currentTimeMillis() - startTime

            val result = TermuxExecutionResult(
                isSuccess = exitCode == 0,
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                executionTimeMs = executionTime
            )

            if (config.debugMode) {
                println("Command: $command")
                println("Exit code: $exitCode")
                println("Stdout: $stdout")
                println("Stderr: $stderr")
                println("Execution time: ${executionTime}ms")
            }

            Result.success(result)
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            Result.failure(TermuxExecutionException("Command execution failed: ${e.message}", e))
        }
    }

    // ==================== 包管理方法 ====================

    /**
     * 安装包
     *
     * 使用Termux包管理器安装指定的软件包
     *
     * @param packageName 包名
     * @return 安装结果
     */
    override suspend fun installPackage(packageName: String): Result<TermuxExecutionResult> {
        return withContext(Dispatchers.IO) {
            val pkgManager = config.packageManagerConfig.getPackageManagerCommand()
            val confirmFlag = if (config.packageManagerConfig.autoConfirm) "-y " else ""

            val command = if (config.packageManagerConfig.updateBeforeInstall) {
                "$pkgManager install $confirmFlag$packageName"
            } else {
                "$pkgManager install $confirmFlag$packageName"
            }

            executeCommand(command, config.packageInstallTimeoutMs)
        }
    }

    /**
     * 批量安装包
     *
     * 一次性安装多个软件包
     *
     * @param packageNames 包名列表
     * @return 安装结果
     */
    override suspend fun installPackages(packageNames: List<String>): Result<TermuxExecutionResult> {
        return withContext(Dispatchers.IO) {
            if (packageNames.isEmpty()) {
                return@withContext Result.success(
                    TermuxExecutionResult.success("No packages to install")
                )
            }

            val pkgManager = config.packageManagerConfig.getPackageManagerCommand()
            val confirmFlag = if (config.packageManagerConfig.autoConfirm) "-y " else ""
            val packages = packageNames.joinToString(" ")

            val command = "$pkgManager install $confirmFlag$packages"
            executeCommand(command, config.packageInstallTimeoutMs)
        }
    }

    /**
     * 卸载包
     *
     * 卸载指定的软件包
     *
     * @param packageName 包名
     * @return 卸载结果
     */
    override suspend fun uninstallPackage(packageName: String): Result<TermuxExecutionResult> {
        return withContext(Dispatchers.IO) {
            val pkgManager = config.packageManagerConfig.getPackageManagerCommand()
            val confirmFlag = if (config.packageManagerConfig.autoConfirm) "-y " else ""

            val command = "$pkgManager uninstall $confirmFlag$packageName"
            executeCommand(command, config.packageInstallTimeoutMs)
        }
    }

    /**
     * 更新包列表
     *
     * 从远程仓库更新包索引
     *
     * @return 更新结果
     */
    override suspend fun updatePackageList(): Result<TermuxExecutionResult> {
        return withContext(Dispatchers.IO) {
            val pkgManager = config.packageManagerConfig.getPackageManagerCommand()
            val command = "$pkgManager update"

            executeCommand(command, config.packageInstallTimeoutMs)
        }
    }

    /**
     * 升级所有包
     *
     * 升级所有已安装的软件包到最新版本
     *
     * @return 升级结果
     */
    override suspend fun upgradePackages(): Result<TermuxExecutionResult> {
        return withContext(Dispatchers.IO) {
            val pkgManager = config.packageManagerConfig.getPackageManagerCommand()
            val confirmFlag = if (config.packageManagerConfig.autoConfirm) "-y " else ""

            val command = "$pkgManager upgrade $confirmFlag"
            executeCommand(command, config.longCommandTimeoutMs)
        }
    }

    /**
     * 搜索包
     *
     * 搜索匹配指定名称的软件包
     *
     * @param query 搜索关键词
     * @return 搜索结果列表
     */
    override suspend fun searchPackage(query: String): Result<List<PackageInfo>> {
        return withContext(Dispatchers.IO) {
            val pkgManager = config.packageManagerConfig.getPackageManagerCommand()
            val command = "$pkgManager search $query"

            val result = executeCommand(command)

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    parsePackageList(executionResult.stdout)
                } else {
                    emptyList()
                }
            }
        }
    }

    /**
     * 获取已安装包列表
     *
     * @return 已安装包信息列表
     */
    override suspend fun getInstalledPackages(): Result<List<PackageInfo>> {
        return withContext(Dispatchers.IO) {
            val command = "${TermuxConstants.DPKG_COMMAND} -l"

            val result = executeCommand(command)

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    parseInstalledPackages(executionResult.stdout)
                } else {
                    emptyList()
                }
            }
        }
    }

    /**
     * 检查包是否已安装
     *
     * @param packageName 包名
     * @return 是否已安装
     */
    override suspend fun isPackageInstalled(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val command = "${TermuxConstants.DPKG_COMMAND} -s $packageName"
            val result = executeCommand(command)
            result.isSuccess && result.getOrNull()?.isSuccess == true
        }
    }

    /**
     * 解析包列表输出
     *
     * @param output 命令输出
     * @return 包信息列表
     */
    private fun parsePackageList(output: String): List<PackageInfo> {
        return output.lines()
            .filter { it.isNotBlank() }
            .dropWhile { !it.startsWith("Package:") && !it.contains("/") }
            .mapNotNull { line ->
                // 解析 pkg/apt search 输出格式
                // 格式: package-name/architecture version description
                val parts = line.split(Regex("\\s+"))
                if (parts.isNotEmpty()) {
                    val namePart = parts[0]
                    val name = if (namePart.contains("/")) {
                        namePart.substringBefore("/")
                    } else {
                        namePart
                    }

                    if (name.isNotBlank()) {
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
                } else null
            }
    }

    /**
     * 解析已安装包列表
     *
     * @param output dpkg -l 输出
     * @return 包信息列表
     */
    private fun parseInstalledPackages(output: String): List<PackageInfo> {
        return output.lines()
            .filter { it.isNotBlank() }
            .drop(5) // 跳过dpkg -l的头部信息
            .mapNotNull { line ->
                // dpkg -l 输出格式:
                // Desired=Unknown/Install/Remove/Purge/Hold
                // | Status=Not/Inst/Conf-files/Unpacked/halF-conf/Half-inst/trig-aWait/Trig-pend
                // |/ Err?=(none)/Reinst-required (Status,Err: uppercase=bad)
                // ||/ Name           Version        Architecture Description
                // ii  package-name   1.0.0          aarch64      description
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4 && parts[0].startsWith("ii")) {
                    PackageInfo(
                        name = parts[1],
                        version = parts[2],
                        architecture = parts[3],
                        description = parts.drop(4).joinToString(" "),
                        isInstalled = true
                    )
                } else null
            }
    }

    // ==================== 文件操作方法 ====================

    /**
     * 读取文件
     *
     * 读取Termux环境中指定路径的文件内容
     *
     * @param path 文件路径
     * @return 文件内容
     */
    override suspend fun readFile(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val command = "cat \"$path\""
            val result = executeCommand(command, config.fileOperationTimeoutMs)

            result.mapCatching { it.stdout }
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to read file: $path", e))
        }
    }

    /**
     * 读取文件（二进制）
     *
     * 以二进制模式读取文件内容
     *
     * @param path 文件路径
     * @return 文件字节数据
     */
    override suspend fun readFileBytes(path: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(TermuxFileException("File not found: $path"))
            }
            Result.success(file.readBytes())
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to read file bytes: $path", e))
        }
    }

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
    override suspend fun writeFile(
        path: String,
        content: String,
        append: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 确保父目录存在
            val parentDir = File(path).parentFile
            if (parentDir != null && !parentDir.exists()) {
                createDirectory(parentDir.absolutePath)
            }

            val redirectOp = if (append) ">>" else ">"
            // 使用base64编码避免特殊字符问题
            val encodedContent = java.util.Base64.getEncoder().encodeToString(content.toByteArray())
            val command = "echo \"$encodedContent\" | base64 -d $redirectOp \"$path\""

            val result = executeCommand(command, config.fileOperationTimeoutMs)
            result.map { }
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to write file: $path", e))
        }
    }

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
    override suspend fun writeFileBytes(
        path: String,
        data: ByteArray,
        append: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            // 确保父目录存在
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            if (append) {
                file.appendBytes(data)
            } else {
                file.writeBytes(data)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to write file bytes: $path", e))
        }
    }

    /**
     * 删除文件
     *
     * @param path 文件路径
     * @return 删除结果
     */
    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val command = "rm -f \"$path\""
            val result = executeCommand(command, config.fileOperationTimeoutMs)

            result.map { }
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to delete file: $path", e))
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param path 文件路径
     * @return 文件是否存在
     */
    override suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val command = "test -e \"$path\" && echo 'exists'"
            val result = executeCommand(command)
            result.isSuccess && result.getOrNull()?.stdout?.trim() == "exists"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 创建目录
     *
     * @param path 目录路径
     * @param recursive 是否递归创建父目录
     * @return 创建结果
     */
    override suspend fun createDirectory(
        path: String,
        recursive: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val recursiveFlag = if (recursive) "-p " else ""
            val command = "mkdir $recursiveFlag\"$path\""
            val result = executeCommand(command, config.fileOperationTimeoutMs)

            result.map { }
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to create directory: $path", e))
        }
    }

    /**
     * 列出目录内容
     *
     * @param path 目录路径
     * @return 目录内容列表
     */
    override suspend fun listDirectory(path: String): Result<List<FileInfo>> = withContext(Dispatchers.IO) {
        try {
            val command = "ls -la \"$path\""
            val result = executeCommand(command, config.fileOperationTimeoutMs)

            result.mapCatching { executionResult ->
                if (executionResult.isSuccess) {
                    parseDirectoryListing(executionResult.stdout, path)
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to list directory: $path", e))
        }
    }

    /**
     * 获取文件信息
     *
     * @param path 文件路径
     * @return 文件信息
     */
    override suspend fun getFileInfo(path: String): Result<FileInfo> = withContext(Dispatchers.IO) {
        try {
            val command = "ls -ld \"$path\""
            val result = executeCommand(command, config.fileOperationTimeoutMs)

            result.mapCatching { executionResult ->
                val line = executionResult.stdout.lines().firstOrNull { it.isNotBlank() }
                    ?: throw TermuxFileException("File not found: $path")

                FileInfo.parseFromLsLine(line, File(path).parent ?: "") ?: throw TermuxFileException("Failed to parse file info: $path")
            }
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to get file info: $path", e))
        }
    }

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @return 复制结果
     */
    override suspend fun copyFile(
        sourcePath: String,
        destPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val command = "cp -f \"$sourcePath\" \"$destPath\""
            val result = executeCommand(command, config.fileOperationTimeoutMs)

            result.map { }
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to copy file from $sourcePath to $destPath", e))
        }
    }

    /**
     * 移动文件
     *
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @return 移动结果
     */
    override suspend fun moveFile(
        sourcePath: String,
        destPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val command = "mv -f \"$sourcePath\" \"$destPath\""
            val result = executeCommand(command, config.fileOperationTimeoutMs)

            result.map { }
        } catch (e: Exception) {
            Result.failure(TermuxFileException("Failed to move file from $sourcePath to $destPath", e))
        }
    }

    /**
     * 解析目录列表输出
     *
     * @param output ls -la 输出
     * @param parentPath 父目录路径
     * @return 文件信息列表
     */
    private fun parseDirectoryListing(output: String, parentPath: String): List<FileInfo> {
        return output.lines()
            .filter { it.isNotBlank() }
            .drop(1) // 跳过total行
            .mapNotNull { line -> FileInfo.parseFromLsLine(line, parentPath) }
    }

    // ==================== 环境变量方法 ====================

    /**
     * 获取环境变量
     *
     * 获取Termux环境中指定名称的环境变量值
     *
     * @param name 环境变量名
     * @return 环境变量值，不存在则返回null
     */
    override suspend fun getEnvVar(name: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            // 首先检查缓存
            envVarCache[name]?.let {
                return@withContext Result.success(it)
            }

            val command = "echo \$$name"
            val result = executeCommand(command)

            result.mapCatching { executionResult ->
                val value = executionResult.stdout.trim()
                if (value.isNotEmpty()) {
                    envVarCache[name] = value
                    value
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Result.failure(TermuxEnvException("Failed to get environment variable: $name", e))
        }
    }

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
    override suspend fun setEnvVar(
        name: String,
        value: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 更新缓存
            envVarCache[name] = value

            // 写入到~/.termux/shell.bashrc或类似文件以持久化
            val envFile = "${config.homePath}/.termux/shell.bashrc"
            val command = "mkdir -p ${config.homePath}/.termux && echo 'export $name=\"$value\"' >> \"$envFile\""
            val result = executeCommand(command)

            result.map { }
        } catch (e: Exception) {
            Result.failure(TermuxEnvException("Failed to set environment variable: $name", e))
        }
    }

    /**
     * 删除环境变量
     *
     * @param name 环境变量名
     * @return 删除结果
     */
    override suspend fun unsetEnvVar(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 从缓存中移除
            envVarCache.remove(name)

            // 从配置文件中移除
            val envFile = "${config.homePath}/.termux/shell.bashrc"
            val command = "sed -i '/^export $name=/d' \"$envFile\" 2>/dev/null || true"
            val result = executeCommand(command)

            result.map { }
        } catch (e: Exception) {
            Result.failure(TermuxEnvException("Failed to unset environment variable: $name", e))
        }
    }

    /**
     * 获取所有环境变量
     *
     * @return 环境变量映射
     */
    override suspend fun getAllEnvVars(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val command = "env"
            val result = executeCommand(command)

            result.mapCatching { executionResult ->
                executionResult.stdout.lines()
                    .filter { it.contains("=") }
                    .associate {
                        val parts = it.split("=", limit = 2)
                        parts[0] to parts.getOrElse(1) { "" }
                    }
            }
        } catch (e: Exception) {
            Result.failure(TermuxEnvException("Failed to get all environment variables", e))
        }
    }

    /**
     * 批量设置环境变量
     *
     * @param envVars 环境变量映射
     * @return 设置结果
     */
    override suspend fun setEnvVars(envVars: Map<String, String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            envVars.forEach { (name, value) ->
                setEnvVar(name, value)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(TermuxEnvException("Failed to set environment variables", e))
        }
    }

    // ==================== 会话管理方法 ====================

    /**
     * 初始化Termux会话
     *
     * 准备Termux环境，包括设置环境变量、检查必要组件等
     *
     * @return 初始化结果
     */
    override suspend fun initializeSession(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 检查Termux是否可用
            if (!isTermuxAvailable()) {
                return@withContext Result.failure(TermuxSessionException("Termux environment is not available"))
            }

            // 创建必要的目录
            createDirectory(config.tmpPath)
            createDirectory("${config.homePath}/.termux")

            // 清空环境变量缓存
            envVarCache.clear()

            // 标记会话为活跃
            sessionActive = true

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(TermuxSessionException("Failed to initialize Termux session", e))
        }
    }

    /**
     * 关闭Termux会话
     *
     * 清理会话资源
     */
    override suspend fun closeSession() {
        sessionActive = false
        envVarCache.clear()
    }

    /**
     * 检查会话是否活跃
     *
     * @return 会话是否活跃
     */
    override fun isSessionActive(): Boolean = sessionActive
}

/**
 * Termux执行异常
 *
 * 表示Termux命令执行过程中的错误
 */
class TermuxExecutionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Termux文件操作异常
 *
 * 表示Termux文件操作过程中的错误
 */
class TermuxFileException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Termux环境变量异常
 *
 * 表示Termux环境变量操作过程中的错误
 */
class TermuxEnvException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Termux会话异常
 *
 * 表示Termux会话管理过程中的错误
 */
class TermuxSessionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * TermuxBridge工厂实现
 *
 * 用于创建TermuxBridge实例
 */
class TermuxBridgeFactoryImpl : TermuxBridgeFactory {

    /**
     * 创建TermuxBridge实例
     *
     * @param config Termux配置
     * @return TermuxBridge实例
     */
    override fun create(config: TermuxConfig): TermuxBridge {
        return TermuxBridgeImpl(config)
    }

    /**
     * 检查Termux环境是否可用
     *
     * @return Termux环境是否可用
     */
    override suspend fun isTermuxAvailable(): Boolean {
        val bridge = create()
        return bridge.isTermuxAvailable()
    }
}