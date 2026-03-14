package com.dfa.core.vm.avf

import com.dfa.core.vm.AvfVmHandle
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmInfo

/**
 * AVF虚拟机适配器接口
 * 
 * 提供与Android Virtualization Framework交互的抽象接口
 */
interface AvfVmAdapter {
    
    /**
     * 检查AVF是否可用
     * 
     * @return AVF是否可用
     */
    suspend fun isAvfAvailable(): Boolean
    
    /**
     * 创建虚拟机
     * 
     * @param config 虚拟机配置
     * @return 虚拟机句柄
     */
    suspend fun createVm(config: VmConfig): Result<AvfVmHandle>
    
    /**
     * 启动虚拟机
     * 
     * @param handle 虚拟机句柄
     * @return 启动结果
     */
    suspend fun startVm(handle: AvfVmHandle): Result<VmInfo>
    
    /**
     * 停止虚拟机
     * 
     * @param handle 虚拟机句柄
     * @param force 是否强制停止
     * @return 停止结果
     */
    suspend fun stopVm(handle: AvfVmHandle, force: Boolean = false): Result<Unit>
    
    /**
     * 暂停虚拟机
     * 
     * @param handle 虚拟机句柄
     * @return 暂停结果
     */
    suspend fun pauseVm(handle: AvfVmHandle): Result<Unit>
    
    /**
     * 恢复虚拟机
     * 
     * @param handle 虚拟机句柄
     * @return 恢复结果
     */
    suspend fun resumeVm(handle: AvfVmHandle): Result<Unit>
    
    /**
     * 获取虚拟机状态
     * 
     * @param handle 虚拟机句柄
     * @return 虚拟机信息
     */
    suspend fun getVmStatus(handle: AvfVmHandle): Result<VmInfo>
    
    /**
     * 销毁虚拟机
     * 
     * @param handle 虚拟机句柄
     * @return 销毁结果
     */
    suspend fun destroyVm(handle: AvfVmHandle): Result<Unit>
    
    /**
     * 注册回调
     * 
     * @param callback 回调接口
     */
    fun registerCallback(callback: AvfVmCallback)
    
    /**
     * 注销回调
     * 
     * @param callback 回调接口
     */
    fun unregisterCallback(callback: AvfVmCallback)
    
    /**
     * 检查配置是否支持
     * 
     * @param config 虚拟机配置
     * @return 是否支持
     */
    suspend fun isConfigSupported(config: VmConfig): Boolean
    
    /**
     * 获取可用资源
     * 
     * @return 可用资源信息
     */
    suspend fun getAvailableResources(): AvfResources
}

/**
 * AVF资源信息
 */
data class AvfResources(
    val totalMemoryMb: Long,
    val availableMemoryMb: Long,
    val totalCpuCores: Int,
    val availableCpuCores: Int,
    val totalDiskSpaceGb: Long,
    val availableDiskSpaceGb: Long,
    val gpuAvailable: Boolean,
    val gpuMemoryMb: Int
) {
    val hasEnoughResources: Boolean
        get() = availableMemoryMb > 0 && availableCpuCores > 0 && availableDiskSpaceGb > 0
}