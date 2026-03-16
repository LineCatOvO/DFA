package com.dfa.core.vm.qemu

import com.dfa.core.vm.VmCallback
import com.dfa.core.vm.VmError
import com.dfa.core.vm.VmState

/**
 * QEMU虚拟机回调接口
 *
 * 扩展VmCallback接口，添加QEMU特定的回调方法
 */
interface QemuVmCallback : VmCallback {
    /**
     * 状态变化回调
     *
     * @param newState 新状态
     */
    override fun onStateChanged(newState: VmState)

    /**
     * 错误回调
     *
     * @param error 错误信息
     */
    override fun onError(error: VmError)

    /**
     * 虚拟机启动完成回调
     *
     * @param ipAddress 虚拟机IP地址
     */
    override fun onVmStarted(ipAddress: String)

    /**
     * 虚拟机停止回调
     */
    override fun onVmStopped()

    /**
     * 虚拟机销毁回调
     */
    override fun onVmDestroyed()

    /**
     * QEMU进程启动回调
     *
     * @param pid QEMU进程ID
     */
    fun onQemuProcessStarted(pid: Int) {}

    /**
     * QEMU进程退出回调
     *
     * @param exitCode 退出码
     */
    fun onQemuProcessExited(exitCode: Int) {}

    /**
     * 快照创建完成回调
     *
     * @param name 快照名称
     * @param success 是否成功
     */
    fun onSnapshotCreated(name: String, success: Boolean) {}

    /**
     * 快照恢复完成回调
     *
     * @param name 快照名称
     * @param success 是否成功
     */
    fun onSnapshotRestored(name: String, success: Boolean) {}

    /**
     * 迁移状态变化回调
     *
     * @param status 迁移状态
     * @param progress 进度（0-100）
     */
    fun onMigrationStatusChanged(status: MigrationStatus, progress: Int) {}
}

/**
 * 迁移状态枚举
 */
enum class MigrationStatus {
    /** 未开始 */
    NONE,
    /** 准备中 */
    PREPARING,
    /** 进行中 */
    IN_PROGRESS,
    /** 已完成 */
    COMPLETED,
    /** 已取消 */
    CANCELLED,
    /** 失败 */
    FAILED
}