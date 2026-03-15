package com.dfa.core.vm.avf

import android.system.virtualmachine.VirtualMachine
import android.util.Log
import com.dfa.core.vm.AvfVmHandle
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmError
import com.dfa.core.vm.VmInfo
import com.dfa.core.vm.VmState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AVF虚拟机适配器实现
 *
 * 提供与Android Virtualization Framework交互的具体实现。
 * 通过AvfManager和VmConfigBuilder实现真实的AVF API调用。
 */
@Singleton
class AvfVmAdapterImpl @Inject constructor(
    private val avfManager: AvfManager,
    private val vmConfigBuilder: VmConfigBuilder
) : AvfVmAdapter {

    companion object {
        private const val TAG = "AvfVmAdapterImpl"
    }

    // 回调列表
    private val callbacks = CopyOnWriteArrayList<AvfVmCallback>()

    // 活跃的虚拟机映射
    private val activeVms = ConcurrentHashMap<String, VirtualMachine>()

    // 虚拟机配置映射
    private val vmConfigs = ConcurrentHashMap<String, VmConfig>()

    // 虚拟机状态映射
    private val vmStates = ConcurrentHashMap<String, VmState>()

    // AVF回调映射
    private val avfCallbacks = ConcurrentHashMap<String, AvfVmCallbackImpl>()

    // 主执行器
    private val mainExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    override suspend fun isAvfAvailable(): Boolean {
        return withContext(Dispatchers.Default) {
            avfManager.isAvfAvailable()
        }
    }

    override suspend fun createVm(config: VmConfig): Result<AvfVmHandle> {
        return withContext(Dispatchers.IO) {
            try {
                // 检查AVF可用性
                if (!avfManager.isAvfAvailable()) {
                    return@withContext Result.failure(
                        VmError.ResourceError("AVF is not available on this device")
                    )
                }

                // 验证配置
                if (!config.resources.validate()) {
                    return@withContext Result.failure(
                        VmError.ConfigurationError("Invalid VM configuration")
                    )
                }

                // 使用VmConfigBuilder构建AVF配置
                val avfConfigResult = vmConfigBuilder.build(config)
                if (avfConfigResult.isFailure) {
                    return@withContext Result.failure(
                        avfConfigResult.exceptionOrNull() as? VmError
                            ?: VmError.ConfigurationError("Failed to build AVF config")
                    )
                }

                val avfConfig = avfConfigResult.getOrThrow()

                // 创建AVF虚拟机
                val createResult = avfManager.createVm(config.id, avfConfig)
                if (createResult.isFailure) {
                    return@withContext Result.failure(
                        createResult.exceptionOrNull() as? VmError
                            ?: VmError.UnknownError("Failed to create AVF VM")
                    )
                }

                val vm = createResult.getOrThrow()

                // 存储虚拟机实例和配置
                activeVms[config.id] = vm
                vmConfigs[config.id] = config
                vmStates[config.id] = VmState.CREATED

                // 创建句柄
                val handle = AvfVmHandle(
                    vmId = config.id,
                    processId = null, // AVF不直接暴露进程ID
                    socketPath = null, // 将在启动后设置
                    createdAt = System.currentTimeMillis()
                )

                Log.i(TAG, "VM created successfully: ${config.id}")
                notifyStateChanged(config.id, VmState.CREATED)

                Result.success(handle)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create VM: ${config.id}", e)
                Result.failure(VmError.UnknownError("Failed to create VM: ${e.message}", e))
            }
        }
    }

    override suspend fun startVm(handle: AvfVmHandle): Result<VmInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val vm = activeVms[vmId]
                val config = vmConfigs[vmId]

                if (vm == null || config == null) {
                    return@withContext Result.failure(
                        VmError.ConfigurationError("VM not found: $vmId")
                    )
                }

                // 更新状态
                vmStates[vmId] = VmState.STARTING
                notifyStateChanged(vmId, VmState.STARTING)

                // 创建AVF回调
                val avfCallback = AvfVmCallbackImpl(object : AvfVmCallback {
                    override fun onStateChanged(newState: VmState) {
                        vmStates[vmId] = newState
                        notifyStateChanged(vmId, newState)
                    }

                    override fun onError(error: VmError) {
                        vmStates[vmId] = VmState.ERROR
                        notifyStateChanged(vmId, VmState.ERROR)
                        notifyError(vmId, error)
                    }

                    override fun onVmStarted(ipAddress: String) {
                        callbacks.forEach { it.onVmStarted(ipAddress) }
                    }

                    override fun onVmStopped() {
                        callbacks.forEach { it.onVmStopped() }
                    }
                })

                avfCallbacks[vmId] = avfCallback

                // 启动虚拟机
                val startResult = avfManager.startVm(vm, avfCallback, mainExecutor)
                if (startResult.isFailure) {
                    vmStates[vmId] = VmState.ERROR
                    notifyStateChanged(vmId, VmState.ERROR)
                    return@withContext Result.failure(
                        startResult.exceptionOrNull() as? VmError
                            ?: VmError.ResourceError("Failed to start VM")
                    )
                }

                // 等待状态更新（由回调处理）
                vmStates[vmId] = VmState.RUNNING

                val vmInfo = VmInfo(
                    config = config,
                    state = VmState.RUNNING,
                    ipAddress = null, // AVF不直接提供IP
                    uptime = 0,
                    handle = handle
                )

                Log.i(TAG, "VM started successfully: $vmId")
                Result.success(vmInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VM: ${handle.vmId}", e)
                vmStates[handle.vmId] = VmState.ERROR
                notifyStateChanged(handle.vmId, VmState.ERROR)
                Result.failure(VmError.UnknownError("Failed to start VM: ${e.message}", e))
            }
        }
    }

    override suspend fun stopVm(handle: AvfVmHandle, force: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val vm = activeVms[vmId]

                if (vm == null) {
                    return@withContext Result.failure(
                        VmError.ConfigurationError("VM not found: $vmId")
                    )
                }

                // 更新状态
                vmStates[vmId] = VmState.STOPPING
                notifyStateChanged(vmId, VmState.STOPPING)

                // 停止虚拟机
                val stopResult = avfManager.stopVm(vm)
                if (stopResult.isFailure) {
                    vmStates[vmId] = VmState.ERROR
                    notifyStateChanged(vmId, VmState.ERROR)
                    return@withContext stopResult
                }

                // 更新状态
                vmStates[vmId] = VmState.STOPPED
                notifyStateChanged(vmId, VmState.STOPPED)

                // 清理回调
                avfCallbacks.remove(vmId)

                Log.i(TAG, "VM stopped successfully: $vmId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop VM: ${handle.vmId}", e)
                vmStates[handle.vmId] = VmState.ERROR
                notifyStateChanged(handle.vmId, VmState.ERROR)
                Result.failure(VmError.UnknownError("Failed to stop VM: ${e.message}", e))
            }
        }
    }

    override suspend fun pauseVm(handle: AvfVmHandle): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val currentState = vmStates[vmId]

                if (currentState != VmState.RUNNING) {
                    return@withContext Result.failure(
                        VmError.ResourceError("Cannot pause VM in state: $currentState")
                    )
                }

                // AVF目前不支持暂停操作，返回错误
                Result.failure(VmError.ResourceError("Pause operation not supported by AVF"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause VM: ${handle.vmId}", e)
                Result.failure(VmError.UnknownError("Failed to pause VM: ${e.message}", e))
            }
        }
    }

    override suspend fun resumeVm(handle: AvfVmHandle): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val currentState = vmStates[vmId]

                if (currentState != VmState.PAUSED) {
                    return@withContext Result.failure(
                        VmError.ResourceError("Cannot resume VM in state: $currentState")
                    )
                }

                // AVF目前不支持恢复操作，返回错误
                Result.failure(VmError.ResourceError("Resume operation not supported by AVF"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume VM: ${handle.vmId}", e)
                Result.failure(VmError.UnknownError("Failed to resume VM: ${e.message}", e))
            }
        }
    }

    override suspend fun getVmStatus(handle: AvfVmHandle): Result<VmInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val vm = activeVms[vmId]
                val config = vmConfigs[vmId]

                if (vm == null || config == null) {
                    return@withContext Result.failure(
                        VmError.ConfigurationError("VM not found: $vmId")
                    )
                }

                // 获取AVF状态
                val avfState = avfManager.getVmStatus(vm)
                vmStates[vmId] = avfState

                val vmInfo = VmInfo(
                    config = config,
                    state = avfState,
                    handle = handle
                )

                Result.success(vmInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get VM status: ${handle.vmId}", e)
                Result.failure(VmError.UnknownError("Failed to get VM status: ${e.message}", e))
            }
        }
    }

    override suspend fun destroyVm(handle: AvfVmHandle): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmId = handle.vmId
                val currentState = vmStates[vmId]

                // 如果正在运行，先停止
                if (currentState == VmState.RUNNING) {
                    stopVm(handle, force = true)
                }

                // 删除AVF虚拟机
                val deleteResult = avfManager.deleteVm(vmId)
                if (deleteResult.isFailure) {
                    return@withContext deleteResult
                }

                // 清理本地状态
                activeVms.remove(vmId)
                vmConfigs.remove(vmId)
                vmStates.remove(vmId)
                avfCallbacks.remove(vmId)

                Log.i(TAG, "VM destroyed successfully: $vmId")
                notifyVmDestroyed(vmId)

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to destroy VM: ${handle.vmId}", e)
                Result.failure(VmError.UnknownError("Failed to destroy VM: ${e.message}", e))
            }
        }
    }

    override fun registerCallback(callback: AvfVmCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    override fun unregisterCallback(callback: AvfVmCallback) {
        callbacks.remove(callback)
    }

    override suspend fun isConfigSupported(config: VmConfig): Boolean {
        return withContext(Dispatchers.Default) {
            // 使用VmConfigBuilder验证配置
            vmConfigBuilder.isConfigSupported(config) &&
                    config.memory <= 8192 && // 最大8GB内存
                    config.cpu <= 8 // 最大8核CPU
        }
    }

    override suspend fun getAvailableResources(): AvfResources {
        return withContext(Dispatchers.Default) {
            // 获取系统可用资源
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / (1024 * 1024) // MB
            val availableProcessors = runtime.availableProcessors()

            // 估算可用资源（保守估计）
            AvfResources(
                totalMemoryMb = maxMemory,
                availableMemoryMb = (maxMemory * 0.75).toLong(),
                totalCpuCores = availableProcessors,
                availableCpuCores = (availableProcessors * 0.75).toInt().coerceAtLeast(1),
                totalDiskSpaceGb = 256, // 估算值
                availableDiskSpaceGb = 200, // 估算值
                gpuAvailable = false, // AVF通常不支持GPU直通
                gpuMemoryMb = 0
            )
        }
    }

    /**
     * 通知状态变化
     */
    private fun notifyStateChanged(vmId: String, newState: VmState) {
        Log.d(TAG, "VM $vmId state changed to: $newState")
        callbacks.forEach { callback ->
            try {
                callback.onStateChanged(newState)
            } catch (e: Exception) {
                Log.e(TAG, "Error in callback", e)
            }
        }
    }

    /**
     * 通知错误
     */
    private fun notifyError(vmId: String, error: VmError) {
        Log.e(TAG, "VM $vmId error: $error")
        callbacks.forEach { callback ->
            try {
                callback.onError(error)
            } catch (e: Exception) {
                Log.e(TAG, "Error in callback", e)
            }
        }
    }

    /**
     * 通知虚拟机销毁
     */
    private fun notifyVmDestroyed(vmId: String) {
        callbacks.forEach { callback ->
            try {
                callback.onVmDestroyed()
            } catch (e: Exception) {
                Log.e(TAG, "Error in callback", e)
            }
        }
    }

    /**
     * 清理所有资源
     */
    fun cleanup() {
        activeVms.forEach { (vmId, vm) ->
            try {
                avfManager.stopVm(vm)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping VM during cleanup: $vmId", e)
            }
        }
        activeVms.clear()
        vmConfigs.clear()
        vmStates.clear()
        avfCallbacks.clear()
        callbacks.clear()
    }
}