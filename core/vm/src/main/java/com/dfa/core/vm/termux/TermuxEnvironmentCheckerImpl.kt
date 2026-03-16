package com.dfa.core.vm.termux

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Termux环境检测器实现类
 *
 * 提供Termux、QEMU和SSH环境的检测功能实现
 *
 * @property context Android应用上下文
 * @property termuxBridge Termux桥接接口
 */
@Singleton
class TermuxEnvironmentCheckerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val termuxBridge: TermuxBridge
) : TermuxEnvironmentChecker {

    // ==================== 状态流 ====================

    private val _termuxStatus = MutableStateFlow<EnvironmentCheckResult>(
        EnvironmentCheckResult.unknown()
    )
    override val termuxStatus: StateFlow<EnvironmentCheckResult> = _termuxStatus.asStateFlow()

    private val _qemuStatus = MutableStateFlow<QemuCheckResult>(
        QemuCheckResult(
            systemX86_64 = EnvironmentCheckResult.unknown(),
            img = EnvironmentCheckResult.unknown(),
            canExecute = false
        )
    )
    override val qemuStatus: StateFlow<QemuCheckResult> = _qemuStatus.asStateFlow()

    private val _sshStatus = MutableStateFlow<EnvironmentCheckResult>(
        EnvironmentCheckResult.unknown()
    )
    override val sshStatus: StateFlow<EnvironmentCheckResult> = _sshStatus.asStateFlow()

    private val _fullCheckStatus = MutableStateFlow<FullEnvironmentCheckResult?>(null)
    override val fullCheckStatus: StateFlow<FullEnvironmentCheckResult?> = _fullCheckStatus.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    override val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    // ==================== Termux检测 ====================

    /**
     * 检测Termux应用安装状态
     *
     * 检查Termux应用是否已安装在系统中
     *
     * @return Termux应用检测结果
     */
    override suspend fun checkTermuxInstallation(): EnvironmentCheckResult = withContext(Dispatchers.IO) {
        try {
            // 方法1：检查包管理器中是否存在Termux应用
            val isInstalledInPm = try {
                context.packageManager.getPackageInfo(
                    TermuxConstants.TERMUX_PACKAGE_NAME,
                    PackageManager.GET_ACTIVITIES
                )
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            // 方法2：检查Termux文件目录是否存在
            val termuxFilesExist = termuxBridge.isTermuxInstalled()

            // 方法3：检查Termux环境是否可用
            val isTermuxAvailable = if (isInstalledInPm || termuxFilesExist) {
                termuxBridge.isTermuxAvailable()
            } else {
                false
            }

            val result = when {
                isTermuxAvailable -> {
                    // 获取Termux版本信息
                    val versionResult = termuxBridge.getTermuxVersion()
                    val version = versionResult.getOrNull()?.trim()?.takeIf { it != "unknown" }
                    EnvironmentCheckResult.available(
                        componentName = "Termux",
                        details = buildMap {
                            version?.let { put("version", it) }
                            put("packageInstalled", isInstalledInPm.toString())
                            put("filesExist", termuxFilesExist.toString())
                        }
                    )
                }
                isInstalledInPm || termuxFilesExist -> {
                    EnvironmentCheckResult.installed(
                        componentName = "Termux",
                        version = null
                    ).copy(
                        message = "Termux is installed but environment is not properly initialized"
                    )
                }
                else -> {
                    EnvironmentCheckResult.notInstalled("Termux")
                }
            }

            _termuxStatus.value = result
            result
        } catch (e: Exception) {
            val result = EnvironmentCheckResult.error(
                message = "Failed to check Termux installation: ${e.message}",
                errorDetails = mapOf("exception" to (e.javaClass.simpleName ?: "Unknown"))
            )
            _termuxStatus.value = result
            result
        }
    }

    // ==================== QEMU检测 ====================

    /**
     * 检测QEMU包安装状态
     *
     * 检查qemu-system-x86-64和qemu-img包是否已安装
     *
     * @return QEMU检测结果
     */
    override suspend fun checkQemuInstallation(): QemuCheckResult = withContext(Dispatchers.IO) {
        try {
            // 检测qemu-system-x86_64
            val systemX86_64Result = checkQemuPackage(
                packageName = TermuxConstants.QEMU_SYSTEM_X86_64_PACKAGE,
                commandName = TermuxConstants.QEMU_SYSTEM_X86_64_COMMAND
            )

            // 检测qemu-img
            val imgResult = checkQemuPackage(
                packageName = TermuxConstants.QEMU_IMG_PACKAGE,
                commandName = TermuxConstants.QEMU_IMG_COMMAND
            )

            // 如果两个包都已安装，验证执行能力
            val canExecute = if (systemX86_64Result.isAvailable && imgResult.isAvailable) {
                checkQemuExecution()
            } else {
                false
            }

            val result = QemuCheckResult(
                systemX86_64 = systemX86_64Result,
                img = imgResult,
                canExecute = canExecute
            )

            _qemuStatus.value = result
            result
        } catch (e: Exception) {
            val result = QemuCheckResult(
                systemX86_64 = EnvironmentCheckResult.error(
                    message = "Failed to check QEMU installation: ${e.message}"
                ),
                img = EnvironmentCheckResult.error(
                    message = "Failed to check QEMU installation: ${e.message}"
                ),
                canExecute = false
            )
            _qemuStatus.value = result
            result
        }
    }

    /**
     * 检测单个QEMU包
     *
     * @param packageName 包名
     * @param commandName 命令名
     * @return 检测结果
     */
    private suspend fun checkQemuPackage(
        packageName: String,
        commandName: String
    ): EnvironmentCheckResult {
        return try {
            // 检查包是否安装
            val isPackageInstalled = termuxBridge.isPackageInstalled(packageName)

            if (isPackageInstalled) {
                // 检查命令是否存在
                val commandResult = termuxBridge.executeCommand("which $commandName")
                val commandExists = commandResult.isSuccess &&
                        commandResult.getOrNull()?.stdout?.isNotBlank() == true

                if (commandExists) {
                    // 获取版本信息
                    val versionResult = termuxBridge.executeCommand("$commandName --version")
                    val version = versionResult.getOrNull()?.stdout?.lines()?.firstOrNull()?.trim()

                    EnvironmentCheckResult.available(
                        componentName = packageName,
                        details = buildMap {
                            version?.let { put("version", it) }
                            put("command", commandName)
                        }
                    )
                } else {
                    EnvironmentCheckResult.installed(
                        componentName = packageName,
                        version = null
                    ).copy(
                        message = "$packageName is installed but command not found"
                    )
                }
            } else {
                EnvironmentCheckResult.notInstalled(packageName)
            }
        } catch (e: Exception) {
            EnvironmentCheckResult.error(
                message = "Failed to check $packageName: ${e.message}"
            )
        }
    }

    /**
     * 验证QEMU命令执行能力
     *
     * 执行QEMU命令验证其是否可以正常运行
     *
     * @return 是否可以执行QEMU命令
     */
    override suspend fun checkQemuExecution(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 使用qemu-img info命令测试执行能力
            // 创建一个临时测试镜像
            val testImagePath = "${TermuxConstants.TERMUX_TMP}/qemu_test_$$.img"

            // 创建测试镜像
            val createResult = termuxBridge.executeCommand(
                "${TermuxConstants.QEMU_IMG_COMMAND} create -f qcow2 $testImagePath 1M"
            )

            if (createResult.isFailure) {
                return@withContext false
            }

            // 验证镜像是否创建成功
            val infoResult = termuxBridge.executeCommand(
                "${TermuxConstants.QEMU_IMG_COMMAND} info $testImagePath"
            )

            // 清理测试镜像
            termuxBridge.executeCommand("rm -f $testImagePath")

            infoResult.isSuccess && infoResult.getOrNull()?.isSuccess == true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== SSH检测 ====================

    /**
     * 检测SSH服务运行状态
     *
     * 检查sshd服务是否正在运行
     *
     * @return SSH服务检测结果
     */
    override suspend fun checkSshService(): EnvironmentCheckResult = withContext(Dispatchers.IO) {
        try {
            // 检查openssh包是否安装
            val isSshPackageInstalled = termuxBridge.isPackageInstalled(TermuxConstants.OPENSSH_PACKAGE)

            if (!isSshPackageInstalled) {
                val result = EnvironmentCheckResult.notInstalled("OpenSSH")
                _sshStatus.value = result
                return@withContext result
            }

            // 检查sshd命令是否存在
            val sshdExists = termuxBridge.executeCommand("which ${TermuxConstants.SSHD_COMMAND}")
                .getOrNull()?.stdout?.isNotBlank() == true

            if (!sshdExists) {
                val result = EnvironmentCheckResult.installed("OpenSSH").copy(
                    message = "OpenSSH is installed but sshd command not found"
                )
                _sshStatus.value = result
                return@withContext result
            }

            // 检查sshd进程是否运行
            val sshdRunning = checkSshdProcess()

            // 检查SSH端口是否监听
            val portListening = checkSshPort()

            val result = if (sshdRunning && portListening) {
                EnvironmentCheckResult.available(
                    componentName = "SSH Service",
                    details = mapOf(
                        "package" to TermuxConstants.OPENSSH_PACKAGE,
                        "port" to TermuxConstants.SSH_DEFAULT_PORT.toString(),
                        "processRunning" to "true",
                        "portListening" to "true"
                    )
                )
            } else {
                EnvironmentCheckResult.installed(
                    componentName = "OpenSSH",
                    version = null
                ).copy(
                    message = buildString {
                        append("OpenSSH is installed but ")
                        if (!sshdRunning) append("sshd is not running")
                        if (!sshdRunning && !portListening) append(" and ")
                        if (!portListening) append("port ${TermuxConstants.SSH_DEFAULT_PORT} is not listening")
                    },
                    details = mapOf(
                        "processRunning" to sshdRunning.toString(),
                        "portListening" to portListening.toString()
                    )
                )
            }

            _sshStatus.value = result
            result
        } catch (e: Exception) {
            val result = EnvironmentCheckResult.error(
                message = "Failed to check SSH service: ${e.message}"
            )
            _sshStatus.value = result
            result
        }
    }

    /**
     * 检查sshd进程是否运行
     *
     * @return sshd进程是否运行
     */
    private suspend fun checkSshdProcess(): Boolean {
        return try {
            val result = termuxBridge.executeCommand("pgrep -x ${TermuxConstants.SSHD_COMMAND}")
            result.isSuccess && result.getOrNull()?.stdout?.isNotBlank() == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查SSH端口是否监听
     *
     * @return SSH端口是否监听
     */
    private suspend fun checkSshPort(): Boolean {
        return try {
            // 使用netstat或ss检查端口
            val netstatResult = termuxBridge.executeCommand(
                "netstat -tln 2>/dev/null | grep ':${TermuxConstants.SSH_DEFAULT_PORT}' || " +
                "ss -tln 2>/dev/null | grep ':${TermuxConstants.SSH_DEFAULT_PORT}'"
            )
            netstatResult.isSuccess && netstatResult.getOrNull()?.stdout?.isNotBlank() == true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== 完整检测 ====================

    /**
     * 执行完整环境检测
     *
     * 依次检测Termux应用、QEMU包和SSH服务
     *
     * @return 完整环境检测结果
     */
    override suspend fun performFullCheck(): FullEnvironmentCheckResult = withContext(Dispatchers.IO) {
        _isChecking.value = true

        try {
            // 依次执行各项检测
            val termuxResult = checkTermuxInstallation()

            // 只有Termux可用时才继续检测其他组件
            val qemuResult = if (termuxResult.isAvailable) {
                checkQemuInstallation()
            } else {
                QemuCheckResult(
                    systemX86_64 = EnvironmentCheckResult.unknown("Termux not available"),
                    img = EnvironmentCheckResult.unknown("Termux not available"),
                    canExecute = false
                )
            }

            val sshResult = if (termuxResult.isAvailable) {
                checkSshService()
            } else {
                EnvironmentCheckResult.unknown("Termux not available")
            }

            val result = FullEnvironmentCheckResult(
                termux = termuxResult,
                qemu = qemuResult,
                ssh = sshResult
            )

            _fullCheckStatus.value = result
            result
        } finally {
            _isChecking.value = false
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 重置检测状态
     *
     * 将所有状态重置为未知
     */
    override fun resetStatus() {
        _termuxStatus.value = EnvironmentCheckResult.unknown()
        _qemuStatus.value = QemuCheckResult(
            systemX86_64 = EnvironmentCheckResult.unknown(),
            img = EnvironmentCheckResult.unknown(),
            canExecute = false
        )
        _sshStatus.value = EnvironmentCheckResult.unknown()
        _fullCheckStatus.value = null
    }

    /**
     * 获取环境检测摘要
     *
     * 返回当前环境状态的简要描述
     *
     * @return 环境状态摘要
     */
    override fun getEnvironmentSummary(): String {
        val fullResult = _fullCheckStatus.value ?: return "Environment not checked yet"

        return buildString {
            appendLine("=== Termux Environment Summary ===")
            appendLine()

            appendLine("Termux: ${fullResult.termux.status.name}")
            appendLine("  ${fullResult.termux.message}")
            if (fullResult.termux.details.isNotEmpty()) {
                fullResult.termux.details.forEach { (key, value) ->
                    appendLine("  - $key: $value")
                }
            }
            appendLine()

            appendLine("QEMU:")
            appendLine("  qemu-system-x86_64: ${fullResult.qemu.systemX86_64.status.name}")
            appendLine("    ${fullResult.qemu.systemX86_64.message}")
            appendLine("  qemu-img: ${fullResult.qemu.img.status.name}")
            appendLine("    ${fullResult.qemu.img.message}")
            appendLine("  Can execute: ${fullResult.qemu.canExecute}")
            appendLine()

            appendLine("SSH: ${fullResult.ssh.status.name}")
            appendLine("  ${fullResult.ssh.message}")
            if (fullResult.ssh.details.isNotEmpty()) {
                fullResult.ssh.details.forEach { (key, value) ->
                    appendLine("  - $key: $value")
                }
            }
            appendLine()

            appendLine("Overall: ${if (fullResult.isFullyAvailable) "READY" else "NOT READY"}")
            if (!fullResult.isFullyAvailable && fullResult.unavailableMessages.isNotEmpty()) {
                appendLine("Issues:")
                fullResult.unavailableMessages.forEach { msg ->
                    appendLine("  - $msg")
                }
            }
        }
    }
}