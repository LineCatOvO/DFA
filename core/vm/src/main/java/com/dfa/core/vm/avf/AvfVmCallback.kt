package com.dfa.core.vm.avf

import com.dfa.core.vm.VmError
import com.dfa.core.vm.VmState

/**
 * AVF虚拟机回调接口
 * 
 * 用于接收虚拟机状态变化和事件通知
 */
interface AvfVmCallback {
    
    /**
     * 状态变化回调
     * 
     * @param newState 新状态
     */
    fun onStateChanged(newState: VmState)
    
    /**
     * 错误回调
     * 
     * @param error 错误信息
     */
    fun onError(error: VmError)
    
    /**
     * 虚拟机启动完成回调
     * 
     * @param ipAddress 分配的IP地址
     */
    fun onVmStarted(ipAddress: String) {}
    
    /**
     * 虚拟机停止完成回调
     */
    fun onVmStopped() {}
    
    /**
     * 虚拟机销毁回调
     */
    fun onVmDestroyed() {}
}

/**
 * AVF虚拟机回调适配器
 * 
 * 提供回调接口的默认实现，方便子类选择性重写
 */
open class AvfVmCallbackAdapter : AvfVmCallback {
    override fun onStateChanged(newState: VmState) {
        // 默认空实现
    }
    
    override fun onError(error: VmError) {
        // 默认空实现
    }
    
    override fun onVmStarted(ipAddress: String) {
        // 默认空实现
    }
    
    override fun onVmStopped() {
        // 默认空实现
    }
    
    override fun onVmDestroyed() {
        // 默认空实现
    }
}