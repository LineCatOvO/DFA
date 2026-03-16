package com.dfa.core.vm.repository

import com.dfa.core.vm.AvfVmHandle
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmHandle
import com.dfa.core.vm.VmInfo
import com.dfa.core.vm.VmState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 虚拟机数据仓库实现
 * 
 * 使用内存存储，支持并发访问
 */
@Singleton
class VmRepositoryImpl @Inject constructor() : VmRepository {
    
    private val mutex = Mutex()
    
    // 内存存储
    private val vmInfoMap = mutableMapOf<String, VmInfo>()
    private val vmHandleMap = mutableMapOf<String, VmHandle>()
    private val vmConfigMap = mutableMapOf<String, VmConfig>()
    
    // 状态流
    private val vmStateFlows = mutableMapOf<String, MutableStateFlow<VmState>>()
    private val vmInfoFlows = mutableMapOf<String, MutableStateFlow<VmInfo?>>()
    
    override suspend fun saveVmInfo(vmInfo: VmInfo) {
        mutex.withLock {
            vmInfoMap[vmInfo.config.id] = vmInfo
            getOrCreateStateFlow(vmInfo.config.id).value = vmInfo.state
            getOrCreateInfoFlow(vmInfo.config.id).value = vmInfo
        }
    }
    
    override suspend fun getVmInfo(vmId: String): VmInfo? {
        return mutex.withLock {
            vmInfoMap[vmId]
        }
    }
    
    override suspend fun getAllVmInfo(): List<VmInfo> {
        return mutex.withLock {
            vmInfoMap.values.toList()
        }
    }
    
    override suspend fun deleteVmInfo(vmId: String) {
        mutex.withLock {
            vmInfoMap.remove(vmId)
            vmHandleMap.remove(vmId)
            vmConfigMap.remove(vmId)
            vmStateFlows.remove(vmId)
            vmInfoFlows.remove(vmId)
        }
    }
    
    override suspend fun updateVmState(vmId: String, state: VmState) {
        mutex.withLock {
            vmInfoMap[vmId]?.let { info ->
                val updatedInfo = info.copy(state = state)
                vmInfoMap[vmId] = updatedInfo
                getOrCreateStateFlow(vmId).value = state
                getOrCreateInfoFlow(vmId).value = updatedInfo
            }
        }
    }
    
    override suspend fun saveVmHandle(vmId: String, handle: VmHandle) {
        mutex.withLock {
            vmHandleMap[vmId] = handle
        }
    }
    
    override suspend fun getVmHandle(vmId: String): VmHandle? {
        return mutex.withLock {
            vmHandleMap[vmId]
        }
    }
    
    override suspend fun deleteVmHandle(vmId: String) {
        mutex.withLock {
            vmHandleMap.remove(vmId)
        }
    }
    
    override fun observeVmState(vmId: String): Flow<VmState> {
        return getOrCreateStateFlow(vmId).asStateFlow()
    }
    
    override fun observeVmInfo(vmId: String): Flow<VmInfo> {
        return getOrCreateInfoFlow(vmId).map { it ?: VmInfo(
            config = VmConfig(id = vmId, name = "Unknown"),
            state = VmState.ERROR
        ) }
    }
    
    override suspend fun saveVmConfig(config: VmConfig) {
        mutex.withLock {
            vmConfigMap[config.id] = config
        }
    }
    
    override suspend fun getVmConfig(vmId: String): VmConfig? {
        return mutex.withLock {
            vmConfigMap[vmId]
        }
    }
    
    override suspend fun clearAll() {
        mutex.withLock {
            vmInfoMap.clear()
            vmHandleMap.clear()
            vmConfigMap.clear()
            vmStateFlows.clear()
            vmInfoFlows.clear()
        }
    }
    
    private fun getOrCreateStateFlow(vmId: String): MutableStateFlow<VmState> {
        return vmStateFlows.getOrPut(vmId) { 
            MutableStateFlow(vmInfoMap[vmId]?.state ?: VmState.CREATED)
        }
    }
    
    private fun getOrCreateInfoFlow(vmId: String): MutableStateFlow<VmInfo?> {
        return vmInfoFlows.getOrPut(vmId) { 
            MutableStateFlow(vmInfoMap[vmId])
        }
    }
}