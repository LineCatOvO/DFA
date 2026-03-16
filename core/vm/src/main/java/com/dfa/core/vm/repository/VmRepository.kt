package com.dfa.core.vm.repository

import com.dfa.core.vm.AvfVmHandle
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmHandle
import com.dfa.core.vm.VmInfo
import com.dfa.core.vm.VmState
import kotlinx.coroutines.flow.Flow

/**
 * 虚拟机数据仓库接口
 * 
 * 负责虚拟机状态的持久化和检索
 */
interface VmRepository {
    
    /**
     * 保存虚拟机信息
     * 
     * @param vmInfo 虚拟机信息
     */
    suspend fun saveVmInfo(vmInfo: VmInfo)
    
    /**
     * 获取虚拟机信息
     * 
     * @param vmId 虚拟机ID
     * @return 虚拟机信息，不存在则返回null
     */
    suspend fun getVmInfo(vmId: String): VmInfo?
    
    /**
     * 获取所有虚拟机信息
     * 
     * @return 虚拟机信息列表
     */
    suspend fun getAllVmInfo(): List<VmInfo>
    
    /**
     * 删除虚拟机信息
     * 
     * @param vmId 虚拟机ID
     */
    suspend fun deleteVmInfo(vmId: String)
    
    /**
     * 更新虚拟机状态
     * 
     * @param vmId 虚拟机ID
     * @param state 新状态
     */
    suspend fun updateVmState(vmId: String, state: VmState)
    
    /**
     * 保存虚拟机句柄
     * 
     * @param vmId 虚拟机ID
     * @param handle 虚拟机句柄
     */
    suspend fun saveVmHandle(vmId: String, handle: VmHandle)
    
    /**
     * 获取虚拟机句柄
     * 
     * @param vmId 虚拟机ID
     * @return 虚拟机句柄，不存在则返回null
     */
    suspend fun getVmHandle(vmId: String): VmHandle?
    
    /**
     * 删除虚拟机句柄
     * 
     * @param vmId 虚拟机ID
     */
    suspend fun deleteVmHandle(vmId: String)
    
    /**
     * 观察虚拟机状态变化
     * 
     * @param vmId 虚拟机ID
     * @return 状态变化流
     */
    fun observeVmState(vmId: String): Flow<VmState>
    
    /**
     * 观察虚拟机信息变化
     * 
     * @param vmId 虚拟机ID
     * @return 信息变化流
     */
    fun observeVmInfo(vmId: String): Flow<VmInfo>
    
    /**
     * 保存虚拟机配置
     * 
     * @param config 虚拟机配置
     */
    suspend fun saveVmConfig(config: VmConfig)
    
    /**
     * 获取虚拟机配置
     * 
     * @param vmId 虚拟机ID
     * @return 虚拟机配置，不存在则返回null
     */
    suspend fun getVmConfig(vmId: String): VmConfig?
    
    /**
     * 清除所有数据
     */
    suspend fun clearAll()
}