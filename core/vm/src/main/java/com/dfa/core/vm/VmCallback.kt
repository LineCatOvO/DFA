package com.dfa.core.vm

import com.dfa.core.vm.avf.AvfVmCallbackAdapter

/**
 * 通用虚拟机回调接口
 *
 * 用于接收虚拟机状态变化、错误和进度通知
 * 支持多种虚拟化后端（AVF、QEMU等）
 */
interface VmCallback {

    /**
     * 状态变化回调
     *
     * @param vmId 虚拟机ID
     * @param oldState 旧状态
     * @param newState 新状态
     */
    fun onStateChanged(vmId: String, oldState: VmState, newState: VmState)

    /**
     * 错误回调
     *
     * @param vmId 虚拟机ID
     * @param error 错误信息
     */
    fun onError(vmId: String, error: VmError)

    /**
     * 进度回调
     *
     * @param vmId 虚拟机ID
     * @param event 进度事件
     */
    fun onProgress(vmId: String, event: VmProgressEvent) {}

    /**
     * 虚拟机启动完成回调
     *
     * @param vmId 虚拟机ID
     * @param ipAddress 分配的IP地址
     */
    fun onVmStarted(vmId: String, ipAddress: String) {}

    /**
     * 虚拟机停止完成回调
     *
     * @param vmId 虚拟机ID
     */
    fun onVmStopped(vmId: String) {}

    /**
     * 虚拟机销毁回调
     *
     * @param vmId 虚拟机ID
     */
    fun onVmDestroyed(vmId: String) {}
}

/**
 * 虚拟机进度事件 sealed class
 *
 * 用于表示虚拟机操作的进度信息
 */
sealed class VmProgressEvent {
    /**
     * 进度百分比（0-100）
     */
    abstract val percentage: Int

    /**
     * 创建进度
     */
    data class Creating(
        override val percentage: Int,
        val stage: CreateStage
    ) : VmProgressEvent() {
        enum class CreateStage {
            INITIALIZING,
            ALLOCATING_RESOURCES,
            CREATING_DISK,
            LOADING_IMAGE,
            FINALIZING
        }
    }

    /**
     * 启动进度
     */
    data class Starting(
        override val percentage: Int,
        val stage: StartStage
    ) : VmProgressEvent() {
        enum class StartStage {
            INITIALIZING,
            LOADING_KERNEL,
            INITIALIZING_HARDWARE,
            BOOTING,
            WAITING_NETWORK
        }
    }

    /**
     * 停止进度
     */
    data class Stopping(
        override val percentage: Int,
        val stage: StopStage
    ) : VmProgressEvent() {
        enum class StopStage {
            SENDING_SIGNAL,
            WAITING_PROCESS,
            CLEANING_UP,
            FORCE_KILLING
        }
    }

    /**
     * 迁移进度
     */
    data class Migrating(
        override val percentage: Int,
        val stage: MigrateStage,
        val targetHost: String
    ) : VmProgressEvent() {
        enum class MigrateStage {
            PREPARING,
            TRANSFERRING_MEMORY,
            TRANSFERRING_DISK,
            FINALIZING,
            COMPLETED
        }
    }

    /**
     * 快照进度
     */
    data class Snapshotting(
        override val percentage: Int,
        val stage: SnapshotStage,
        val snapshotName: String
    ) : VmProgressEvent() {
        enum class SnapshotStage {
            PAUSING,
            SAVING_MEMORY,
            SAVING_DISK,
            RESUMING
        }
    }

    /**
     * 恢复快照进度
     */
    data class RestoringSnapshot(
        override val percentage: Int,
        val stage: RestoreStage,
        val snapshotName: String
    ) : VmProgressEvent() {
        enum class RestoreStage {
            LOADING_MEMORY,
            LOADING_DISK,
            RESUMING
        }
    }
}

/**
 * 虚拟机回调适配器
 *
 * 提供回调接口的默认实现，方便子类选择性重写
 */
open class VmCallbackAdapter : VmCallback {
    override fun onStateChanged(vmId: String, oldState: VmState, newState: VmState) {
        // 默认空实现
    }

    override fun onError(vmId: String, error: VmError) {
        // 默认空实现
    }

    override fun onProgress(vmId: String, event: VmProgressEvent) {
        // 默认空实现
    }

    override fun onVmStarted(vmId: String, ipAddress: String) {
        // 默认空实现
    }

    override fun onVmStopped(vmId: String) {
        // 默认空实现
    }

    override fun onVmDestroyed(vmId: String) {
        // 默认空实现
    }
}

/**
 * VmCallback扩展函数：转换为AvfVmCallback（向后兼容）
 */
fun VmCallback.toAvfVmCallback(vmId: String): AvfVmCallbackAdapter = object : AvfVmCallbackAdapter() {
    override fun onStateChanged(newState: VmState) {
        this@toAvfVmCallback.onStateChanged(vmId, newState, newState)
    }

    override fun onError(error: VmError) {
        this@toAvfVmCallback.onError(vmId, error)
    }

    override fun onVmStarted(ipAddress: String) {
        this@toAvfVmCallback.onVmStarted(vmId, ipAddress)
    }

    override fun onVmStopped() {
        this@toAvfVmCallback.onVmStopped(vmId)
    }

    override fun onVmDestroyed() {
        this@toAvfVmCallback.onVmDestroyed(vmId)
    }
}