package com.dfa.core.vm.termux

import kotlinx.coroutines.flow.StateFlow

/**
 * 环境状态枚举
 *
 * 描述Termux环境中各组件的安装和运行状态
 */
enum class EnvironmentStatus {
    /**
     * 未知状态
     *
     * 尚未进行检测或检测失败
     */
    UNKNOWN,

    /**
     * 未安装
     *
     * 组件未安装在系统中
     */
    NOT_INSTALLED,

    /**
     * 已安装
     *
     * 组件已安装但可能未运行
     */
    INSTALLED,

    /**
     * 可用
     *
     * 组件已安装且正常运行
     */
    AVAILABLE,

    /**
     * 错误状态
     *
     * 检测过程中发生错误
     */
    ERROR
}

/**
 * 环境检测结果数据类
 *
 * 包含单个组件的检测结果信息
 *
 * @property status 检测状态
 * @property message 状态消息
 * @property details 详细信息映射
 */
data class EnvironmentCheckResult(
    val status: EnvironmentStatus,
    val message: String,
    val details: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * 创建未知状态结果
         */
        fun unknown(message: String = "Unknown status"): EnvironmentCheckResult {
            return EnvironmentCheckResult(
                status = EnvironmentStatus.UNKNOWN,
                message = message
            )
        }

        /**
         * 创建未安装状态结果
         */
        fun notInstalled(componentName: String): EnvironmentCheckResult {
            return EnvironmentCheckResult(
                status = EnvironmentStatus.NOT_INSTALLED,
                message = "$componentName is not installed"
            )
        }

        /**
         * 创建已安装状态结果
         */
        fun installed(
            componentName: String,
            version: String? = null
        ): EnvironmentCheckResult {
            val details = if (version != null) {
                mapOf("version" to version)
            } else {
                emptyMap()
            }
            return EnvironmentCheckResult(
                status = EnvironmentStatus.INSTALLED,
                message = "$componentName is installed",
                details = details
            )
        }

        /**
         * 创建可用状态结果
         */
        fun available(
            componentName: String,
            details: Map<String, String> = emptyMap()
        ): EnvironmentCheckResult {
            return EnvironmentCheckResult(
                status = EnvironmentStatus.AVAILABLE,
                message = "$componentName is available and running",
                details = details
            )
        }

        /**
         * 创建错误状态结果
         */
        fun error(
            message: String,
            errorDetails: Map<String, String> = emptyMap()
        ): EnvironmentCheckResult {
            return EnvironmentCheckResult(
                status = EnvironmentStatus.ERROR,
                message = message,
                details = errorDetails
            )
        }
    }

    /**
     * 检查结果是否表示组件可用
     */
    val isAvailable: Boolean
        get() = status == EnvironmentStatus.AVAILABLE || status == EnvironmentStatus.INSTALLED

    /**
     * 检查结果是否表示组件已安装
     */
    val isInstalled: Boolean
        get() = status == EnvironmentStatus.INSTALLED || status == EnvironmentStatus.AVAILABLE

    /**
     * 检查结果是否表示错误
     */
    val hasError: Boolean
        get() = status == EnvironmentStatus.ERROR
}

/**
 * 完整环境检测结果数据类
 *
 * 包含所有组件的检测结果
 *
 * @property termux Termux应用检测结果
 * @property qemu QEMU检测结果
 * @property ssh SSH服务检测结果
 * @property timestamp 检测时间戳
 */
data class FullEnvironmentCheckResult(
    val termux: EnvironmentCheckResult,
    val qemu: QemuCheckResult,
    val ssh: EnvironmentCheckResult,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 检查所有组件是否可用
     */
    val isFullyAvailable: Boolean
        get() = termux.isAvailable && qemu.isFullyAvailable && ssh.isAvailable

    /**
     * 获取所有不可用组件的消息
     */
    val unavailableMessages: List<String>
        get() {
            val messages = mutableListOf<String>()
            if (!termux.isAvailable) {
                messages.add("Termux: ${termux.message}")
            }
            if (!qemu.isFullyAvailable) {
                if (!qemu.systemX86_64.isAvailable) {
                    messages.add("QEMU x86_64: ${qemu.systemX86_64.message}")
                }
                if (!qemu.img.isAvailable) {
                    messages.add("QEMU img: ${qemu.img.message}")
                }
            }
            if (!ssh.isAvailable) {
                messages.add("SSH: ${ssh.message}")
            }
            return messages
        }
}

/**
 * QEMU检测结果数据类
 *
 * 包含QEMU各组件的检测结果
 *
 * @property systemX86_64 qemu-system-x86_64检测结果
 * @property img qemu-img检测结果
 * @property canExecute 是否可以执行QEMU命令
 */
data class QemuCheckResult(
    val systemX86_64: EnvironmentCheckResult,
    val img: EnvironmentCheckResult,
    val canExecute: Boolean = false
) {
    /**
     * 检查所有QEMU组件是否可用
     */
    val isFullyAvailable: Boolean
        get() = systemX86_64.isAvailable && img.isAvailable && canExecute
}

/**
 * Termux环境检测器接口
 *
 * 提供Termux、QEMU和SSH环境的检测功能
 */
interface TermuxEnvironmentChecker {

    /**
     * Termux应用检测结果状态流
     */
    val termuxStatus: StateFlow<EnvironmentCheckResult>

    /**
     * QEMU检测结果状态流
     */
    val qemuStatus: StateFlow<QemuCheckResult>

    /**
     * SSH服务检测结果状态流
     */
    val sshStatus: StateFlow<EnvironmentCheckResult>

    /**
     * 完整环境检测结果状态流
     */
    val fullCheckStatus: StateFlow<FullEnvironmentCheckResult?>

    /**
     * 检测中状态标志
     */
    val isChecking: StateFlow<Boolean>

    /**
     * 检测Termux应用安装状态
     *
     * 检查Termux应用是否已安装在系统中
     *
     * @return Termux应用检测结果
     */
    suspend fun checkTermuxInstallation(): EnvironmentCheckResult

    /**
     * 检测QEMU包安装状态
     *
     * 检查qemu-system-x86-64和qemu-img包是否已安装
     *
     * @return QEMU检测结果
     */
    suspend fun checkQemuInstallation(): QemuCheckResult

    /**
     * 验证QEMU命令执行能力
     *
     * 执行QEMU命令验证其是否可以正常运行
     *
     * @return 是否可以执行QEMU命令
     */
    suspend fun checkQemuExecution(): Boolean

    /**
     * 检测SSH服务运行状态
     *
     * 检查sshd服务是否正在运行
     *
     * @return SSH服务检测结果
     */
    suspend fun checkSshService(): EnvironmentCheckResult

    /**
     * 执行完整环境检测
     *
     * 依次检测Termux应用、QEMU包和SSH服务
     *
     * @return 完整环境检测结果
     */
    suspend fun performFullCheck(): FullEnvironmentCheckResult

    /**
     * 重置检测状态
     *
     * 将所有状态重置为未知
     */
    fun resetStatus()

    /**
     * 获取环境检测摘要
     *
     * 返回当前环境状态的简要描述
     *
     * @return 环境状态摘要
     */
    fun getEnvironmentSummary(): String
}