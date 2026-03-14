package com.dfa.core.vm

import kotlinx.coroutines.flow.StateFlow

/**
 * 虚拟机管理器接口
 * 
 * 提供虚拟机生命周期管理的核心接口
 */
interface VmManager {
    
    /**
     * 当前管理的虚拟机状态流
     */
    val vmState: StateFlow<VmState>
    
    /**
     * 当前虚拟机信息流
     */
    val vmInfo: StateFlow<VmInfo?>
    
    /**
     * 是否已初始化
     */
    val isInitialized: StateFlow<Boolean>
    
    /**
     * 初始化虚拟机管理器
     * 
     * @param config 虚拟机配置
     * @return 初始化结果
     */
    suspend fun initialize(config: VmConfig): Result<VmInfo>
    
    /**
     * 启动虚拟机
     * 
     * @return 启动结果
     */
    suspend fun start(): Result<VmInfo>
    
    /**
     * 停止虚拟机
     * 
     * @param force 是否强制停止
     * @return 停止结果
     */
    suspend fun stop(force: Boolean = false): Result<VmInfo>
    
    /**
     * 暂停虚拟机
     * 
     * @return 暂停结果
     */
    suspend fun pause(): Result<VmInfo>
    
    /**
     * 恢复虚拟机
     * 
     * @return 恢复结果
     */
    suspend fun resume(): Result<VmInfo>
    
    /**
     * 重置虚拟机
     * 
     * @return 重置结果
     */
    suspend fun reset(): Result<VmInfo>
    
    /**
     * 获取当前虚拟机状态
     * 
     * @return 当前状态
     */
    fun getCurrentState(): VmState
    
    /**
     * 获取当前虚拟机信息
     * 
     * @return 当前信息，如果未初始化则返回null
     */
    fun getCurrentInfo(): VmInfo?
    
    /**
     * 检查是否可以执行指定操作
     * 
     * @param operation 操作类型
     * @return 是否可以执行
     */
    fun canPerformOperation(operation: VmOperation): Boolean
    
    /**
     * 释放资源
     */
    suspend fun release()
    
    /**
     * 虚拟机操作类型
     */
    enum class VmOperation {
        START, STOP, PAUSE, RESUME, RESET, MIGRATE
    }
}

/**
 * 虚拟机管理器工厂接口
 */
interface VmManagerFactory {
    /**
     * 创建虚拟机管理器实例
     * 
     * @param config 虚拟机配置
     * @return 虚拟机管理器实例
     */
    fun create(config: VmConfig): VmManager
    
    /**
     * 检查是否支持指定配置
     * 
     * @param config 虚拟机配置
     * @return 是否支持
     */
    fun isSupported(config: VmConfig): Boolean
}