package com.dfa.core.vm.avf

import android.system.virtualmachine.VirtualMachine
import android.system.virtualmachine.VirtualMachineCallback
import android.util.Log
import com.dfa.core.vm.VmError
import com.dfa.core.vm.VmState
import java.util.concurrent.CopyOnWriteArrayList

/**
 * AVF虚拟机回调实现
 *
 * 实现Android Virtualization Framework的VirtualMachineCallback接口，
 * 并将事件转换为内部的AvfVmCallback接口调用。
 */
class AvfVmCallbackImpl(
    private val internalCallback: AvfVmCallback
) : VirtualMachineCallback {

    companion object {
        private const val TAG = "AvfVmCallbackImpl"

        // AVF错误码常量
        private const val ERROR_INVALID_CONFIG = 1
        private const val ERROR_PAYLOAD_INVALID = 2
        private const val ERROR_INSUFFICIENT_MEMORY = 3
        private const val ERROR_INSUFFICIENT_RESOURCES = 4
        private const val ERROR_NETWORK = 5
        private const val ERROR_PERMISSION_DENIED = 6
        private const val ERROR_TIMEOUT = 7
    }

    // 额外的回调监听器
    private val additionalCallbacks = CopyOnWriteArrayList<AvfVmCallback>()

    /**
     * 添加额外的回调监听器
     */
    fun addCallback(callback: AvfVmCallback) {
        additionalCallbacks.add(callback)
    }

    /**
     * 移除回调监听器
     */
    fun removeCallback(callback: AvfVmCallback) {
        additionalCallbacks.remove(callback)
    }

    /**
     * Payload启动回调
     *
     * 当虚拟机中的Payload开始执行时调用
     */
    override fun onPayloadStarted(vm: VirtualMachine) {
        Log.i(TAG, "Payload started for VM: ${vm.name}")

        // 通知状态变化
        notifyStateChanged(VmState.RUNNING)

        // 通知内部回调
        internalCallback.onStateChanged(VmState.RUNNING)

        // 通知额外回调
        additionalCallbacks.forEach { callback ->
            try {
                callback.onStateChanged(VmState.RUNNING)
            } catch (e: Exception) {
                Log.e(TAG, "Error in additional callback", e)
            }
        }
    }

    /**
     * Payload就绪回调
     *
     * 当虚拟机中的Payload准备就绪可以接收请求时调用
     */
    override fun onPayloadReady(vm: VirtualMachine) {
        Log.i(TAG, "Payload ready for VM: ${vm.name}")

        // Payload就绪意味着VM完全启动
        // 可以在这里通知VM已准备好接收连接
        notifyStateChanged(VmState.RUNNING)

        // 通知内部回调
        internalCallback.onVmStarted("vm-ready")

        // 通知额外回调
        additionalCallbacks.forEach { callback ->
            try {
                callback.onVmStarted("vm-ready")
            } catch (e: Exception) {
                Log.e(TAG, "Error in additional callback", e)
            }
        }
    }

    /**
     * Payload结束回调
     *
     * 当虚拟机中的Payload执行结束时调用
     *
     * @param vm 虚拟机实例
     * @param exitCode 退出码
     */
    override fun onPayloadFinished(vm: VirtualMachine, exitCode: Int) {
        Log.i(TAG, "Payload finished for VM: ${vm.name}, exitCode: $exitCode")

        // 根据退出码判断状态
        val newState = if (exitCode == 0) {
            VmState.STOPPED
        } else {
            VmState.ERROR
        }

        // 通知状态变化
        notifyStateChanged(newState)

        // 通知内部回调
        internalCallback.onStateChanged(newState)
        internalCallback.onVmStopped()

        // 通知额外回调
        additionalCallbacks.forEach { callback ->
            try {
                callback.onStateChanged(newState)
                callback.onVmStopped()
            } catch (e: Exception) {
                Log.e(TAG, "Error in additional callback", e)
            }
        }
    }

    /**
     * 错误回调
     *
     * 当虚拟机发生错误时调用
     *
     * @param vm 虚拟机实例
     * @param errorCode 错误码
     * @param message 错误消息
     */
    override fun onError(vm: VirtualMachine, errorCode: Int, message: String) {
        Log.e(TAG, "VM error for ${vm.name}: code=$errorCode, message=$message")

        // 通知状态变化
        notifyStateChanged(VmState.ERROR)

        // 转换错误码为内部错误类型
        val error = convertError(errorCode, message)

        // 通知内部回调
        internalCallback.onStateChanged(VmState.ERROR)
        internalCallback.onError(error)

        // 通知额外回调
        additionalCallbacks.forEach { callback ->
            try {
                callback.onStateChanged(VmState.ERROR)
                callback.onError(error)
            } catch (e: Exception) {
                Log.e(TAG, "Error in additional callback", e)
            }
        }
    }

    /**
     * 通知状态变化
     */
    private fun notifyStateChanged(newState: VmState) {
        Log.d(TAG, "VM state changed to: $newState")
    }

    /**
     * 转换AVF错误码为内部错误类型
     */
    private fun convertError(errorCode: Int, message: String): VmError {
        return when (errorCode) {
            // 配置相关错误
            ERROR_INVALID_CONFIG -> VmError.ConfigurationError(message)
            ERROR_PAYLOAD_INVALID -> VmError.ConfigurationError("Invalid payload: $message")

            // 资源相关错误
            ERROR_INSUFFICIENT_MEMORY -> VmError.ResourceError("Insufficient memory: $message")
            ERROR_INSUFFICIENT_RESOURCES -> VmError.ResourceError("Insufficient resources: $message")

            // 网络相关错误
            ERROR_NETWORK -> VmError.NetworkError(message)

            // 权限相关错误
            ERROR_PERMISSION_DENIED -> VmError.PermissionError(message)

            // 超时错误
            ERROR_TIMEOUT -> VmError.TimeoutError(message)

            // 未知错误
            else -> VmError.UnknownError("AVF error $errorCode: $message")
        }
    }
}