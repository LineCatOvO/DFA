package com.dfa.core.vm.avf

import android.content.Context
import android.os.ParcelFileDescriptor
import android.system.virtualmachine.VirtualMachine
import android.system.virtualmachine.VirtualMachineCallback
import android.system.virtualmachine.VirtualMachineConfig
import android.system.virtualmachine.VirtualMachineException
import android.system.virtualmachine.VirtualMachineManager
import android.util.Log
import com.dfa.core.vm.AvfVmHandle
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmError
import com.dfa.core.vm.VmInfo
import com.dfa.core.vm.VmState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AVF管理器封装类
 *
 * 封装Android Virtualization Framework的VirtualMachineManager，
 * 提供虚拟机创建、获取、删除等核心操作。
 */
@Singleton
class AvfManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AvfManager"
        private const val VM_STATUS_STOPPED = 0
        private const val VM_STATUS_RUNNING = 1
    }

    private val vmm: VirtualMachineManager? by lazy {
        try {
            context.getSystemService(VirtualMachineManager::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get VirtualMachineManager", e)
            null
        }
    }

    // 存储活跃的虚拟机实例
    private val activeVms = mutableMapOf<String, VirtualMachine>()

    /**
     * 检查AVF是否可用
     *
     * @return AVF是否可用
     */
    fun isAvfAvailable(): Boolean {
        return try {
            vmm != null
        } catch (e: Exception) {
            Log.e(TAG, "AVF not available", e)
            false
        }
    }

    /**
     * 创建虚拟机
     *
     * @param name 虚拟机名称
     * @param config 虚拟机配置
     * @return 创建结果
     */
    suspend fun createVm(name: String, config: VirtualMachineConfig): Result<VirtualMachine> {
        return withContext(Dispatchers.IO) {
            try {
                val vmmInstance = vmm ?: return@withContext Result.failure(
                    VmError.ResourceError("VirtualMachineManager not available")
                )

                // 检查是否已存在同名VM
                if (activeVms.containsKey(name)) {
                    return@withContext Result.failure(
                        VmError.ConfigurationError("VM with name '$name' already exists")
                    )
                }

                val vm = vmmInstance.create(name, config)
                activeVms[name] = vm

                Log.i(TAG, "VM created successfully: $name")
                Result.success(vm)
            } catch (e: VirtualMachineException) {
                Log.e(TAG, "Failed to create VM: $name", e)
                Result.failure(VmError.ConfigurationError("Failed to create VM: ${e.message}"))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error creating VM: $name", e)
                Result.failure(VmError.UnknownError("Failed to create VM: ${e.message}", e))
            }
        }
    }

    /**
     * 获取已存在的虚拟机
     *
     * @param name 虚拟机名称
     * @return 虚拟机实例，不存在则返回null
     */
    suspend fun getVm(name: String): Result<VirtualMachine?> {
        return withContext(Dispatchers.IO) {
            try {
                // 首先检查活跃列表
                activeVms[name]?.let { return@withContext Result.success(it) }

                val vmmInstance = vmm ?: return@withContext Result.failure(
                    VmError.ResourceError("VirtualMachineManager not available")
                )

                val vm = vmmInstance.get(name)
                if (vm != null) {
                    activeVms[name] = vm
                }
                Result.success(vm)
            } catch (e: VirtualMachineException) {
                Log.e(TAG, "Failed to get VM: $name", e)
                Result.failure(VmError.UnknownError("Failed to get VM: ${e.message}"))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error getting VM: $name", e)
                Result.failure(VmError.UnknownError("Failed to get VM: ${e.message}", e))
            }
        }
    }

    /**
     * 删除虚拟机
     *
     * @param name 虚拟机名称
     * @return 删除结果
     */
    suspend fun deleteVm(name: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val vmmInstance = vmm ?: return@withContext Result.failure(
                    VmError.ResourceError("VirtualMachineManager not available")
                )

                // 先停止并移除活跃实例
                activeVms[name]?.let { vm ->
                    try {
                        vm.stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error stopping VM before delete: $name", e)
                    }
                    activeVms.remove(name)
                }

                vmmInstance.delete(name)
                Log.i(TAG, "VM deleted successfully: $name")
                Result.success(Unit)
            } catch (e: VirtualMachineException) {
                Log.e(TAG, "Failed to delete VM: $name", e)
                Result.failure(VmError.UnknownError("Failed to delete VM: ${e.message}"))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error deleting VM: $name", e)
                Result.failure(VmError.UnknownError("Failed to delete VM: ${e.message}", e))
            }
        }
    }

    /**
     * 启动虚拟机
     *
     * @param vm 虚拟机实例
     * @param callback 回调接口
     * @param executor 执行器
     * @return 启动结果
     */
    fun startVm(
        vm: VirtualMachine,
        callback: VirtualMachineCallback,
        executor: java.util.concurrent.Executor
    ): Result<Unit> {
        return try {
            vm.run(callback, executor)
            Log.i(TAG, "VM started successfully")
            Result.success(Unit)
        } catch (e: VirtualMachineException) {
            Log.e(TAG, "Failed to start VM", e)
            Result.failure(VmError.ResourceError("Failed to start VM: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error starting VM", e)
            Result.failure(VmError.UnknownError("Failed to start VM: ${e.message}", e))
        }
    }

    /**
     * 停止虚拟机
     *
     * @param vm 虚拟机实例
     * @return 停止结果
     */
    fun stopVm(vm: VirtualMachine): Result<Unit> {
        return try {
            vm.stop()
            Log.i(TAG, "VM stopped successfully")
            Result.success(Unit)
        } catch (e: VirtualMachineException) {
            Log.e(TAG, "Failed to stop VM", e)
            Result.failure(VmError.ResourceError("Failed to stop VM: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error stopping VM", e)
            Result.failure(VmError.UnknownError("Failed to stop VM: ${e.message}", e))
        }
    }

    /**
     * 获取虚拟机状态
     *
     * @param vm 虚拟机实例
     * @return 虚拟机状态
     */
    fun getVmStatus(vm: VirtualMachine): VmState {
        return try {
            when (vm.status) {
                VM_STATUS_STOPPED -> VmState.STOPPED
                VM_STATUS_RUNNING -> VmState.RUNNING
                else -> VmState.STARTING
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get VM status", e)
            VmState.ERROR
        }
    }

    /**
     * 连接到虚拟机的vsock端口
     *
     * @param vm 虚拟机实例
     * @param port vsock端口
     * @return 文件描述符
     */
    fun connectVsock(vm: VirtualMachine, port: Int): Result<ParcelFileDescriptor> {
        return try {
            val fd = vm.connectVsock(port)
            Log.i(TAG, "Connected to vsock port: $port")
            Result.success(fd)
        } catch (e: VirtualMachineException) {
            Log.e(TAG, "Failed to connect vsock: $port", e)
            Result.failure(VmError.NetworkError("Failed to connect vsock: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error connecting vsock: $port", e)
            Result.failure(VmError.UnknownError("Failed to connect vsock: ${e.message}", e))
        }
    }

    /**
     * 清理所有活跃的虚拟机
     */
    fun cleanup() {
        activeVms.forEach { (name, vm) ->
            try {
                vm.stop()
                Log.i(TAG, "Stopped VM during cleanup: $name")
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping VM during cleanup: $name", e)
            }
        }
        activeVms.clear()
    }

    /**
     * 获取活跃虚拟机数量
     */
    fun getActiveVmCount(): Int = activeVms.size

    /**
     * 检查指定名称的虚拟机是否存在
     */
    fun hasVm(name: String): Boolean = activeVms.containsKey(name)
}