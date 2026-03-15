package com.dfa.core.vm.avf

import android.content.Context
import android.system.virtualmachine.VirtualMachineConfig
import android.util.Log
import com.dfa.core.vm.BuildConfig
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmError
import com.dfa.core.vm.VmResources
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VM配置构建器
 *
 * 将内部的VmConfig转换为AVF的VirtualMachineConfig
 */
@Singleton
class VmConfigBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VmConfigBuilder"

        // 默认配置
        private const val DEFAULT_MEMORY_MB = 2048
        private const val DEFAULT_CPU_COUNT = 2
        private const val DEFAULT_DISK_SIZE_GB = 10

        // 资源限制
        private const val MAX_MEMORY_MB = 8192
        private const val MAX_CPU_COUNT = 8
        private const val MIN_MEMORY_MB = 512
        private const val MIN_CPU_COUNT = 1
    }

    /**
     * 将VmConfig转换为VirtualMachineConfig
     *
     * @param vmConfig 内部VM配置
     * @return AVF配置结果
     */
    fun build(vmConfig: VmConfig): Result<VirtualMachineConfig> {
        return try {
            // 验证配置
            val validationResult = validateConfig(vmConfig)
            if (validationResult.isFailure) {
                return validationResult
            }

            val builder = VirtualMachineConfig.Builder(context)

            // 基本配置
            configureBasicSettings(builder, vmConfig)

            // 资源配置
            configureResources(builder, vmConfig.resources)

            // Payload配置（如果有bootImage）
            configurePayload(builder, vmConfig)

            // 网络配置
            configureNetwork(builder, vmConfig)

            // 调试配置
            configureDebug(builder, vmConfig)

            val config = builder.build()
            Log.i(TAG, "VM config built successfully for: ${vmConfig.name}")
            Result.success(config)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build VM config", e)
            Result.failure(VmError.ConfigurationError("Failed to build config: ${e.message}"))
        }
    }

    /**
     * 构建Protected VM配置
     *
     * @param vmConfig 内部VM配置
     * @return AVF配置结果
     */
    fun buildProtectedVm(vmConfig: VmConfig): Result<VirtualMachineConfig> {
        return try {
            val validationResult = validateConfig(vmConfig)
            if (validationResult.isFailure) {
                return validationResult
            }

            val builder = VirtualMachineConfig.Builder(context)

            // 设置为Protected VM
            builder.setProtectedVm(true)

            // 基本配置
            configureBasicSettings(builder, vmConfig)

            // 资源配置
            configureResources(builder, vmConfig.resources)

            // Payload配置
            configurePayload(builder, vmConfig)

            // 网络配置
            configureNetwork(builder, vmConfig)

            val config = builder.build()
            Log.i(TAG, "Protected VM config built successfully for: ${vmConfig.name}")
            Result.success(config)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build protected VM config", e)
            Result.failure(VmError.ConfigurationError("Failed to build protected VM config: ${e.message}"))
        }
    }

    /**
     * 验证VM配置
     */
    private fun validateConfig(vmConfig: VmConfig): Result<Unit> {
        // 验证名称
        if (vmConfig.name.isBlank()) {
            return Result.failure(VmError.ConfigurationError("VM name cannot be empty"))
        }

        // 验证ID
        if (vmConfig.id.isBlank()) {
            return Result.failure(VmError.ConfigurationError("VM ID cannot be empty"))
        }

        // 验证资源配置
        val resources = vmConfig.resources
        if (!resources.validate()) {
            return Result.failure(VmError.ConfigurationError("Invalid resource configuration"))
        }

        // 验证内存范围
        if (resources.memoryMb < MIN_MEMORY_MB || resources.memoryMb > MAX_MEMORY_MB) {
            return Result.failure(
                VmError.ConfigurationError(
                    "Memory must be between $MIN_MEMORY_MB MB and $MAX_MEMORY_MB MB"
                )
            )
        }

        // 验证CPU范围
        if (resources.cpuCores < MIN_CPU_COUNT || resources.cpuCores > MAX_CPU_COUNT) {
            return Result.failure(
                VmError.ConfigurationError(
                    "CPU cores must be between $MIN_CPU_COUNT and $MAX_CPU_COUNT"
                )
            )
        }

        return Result.success(Unit)
    }

    /**
     * 配置基本设置
     */
    private fun configureBasicSettings(
        builder: VirtualMachineConfig.Builder,
        vmConfig: VmConfig
    ) {
        // 设置APK路径（使用当前应用的APK）
        builder.setApkPath(context.packageCodePath)
    }

    /**
     * 配置资源
     */
    private fun configureResources(
        builder: VirtualMachineConfig.Builder,
        resources: VmResources
    ) {
        // 设置内存（转换为字节）
        val memoryBytes = resources.memoryMb.toLong() * 1024 * 1024
        builder.setMemoryBytes(memoryBytes)

        // 设置CPU核心数
        builder.setCpuCount(resources.cpuCores)
    }

    /**
     * 配置Payload
     */
    private fun configurePayload(
        builder: VirtualMachineConfig.Builder,
        vmConfig: VmConfig
    ) {
        // 设置Payload二进制名称
        // 如果指定了bootImage，使用它作为payload
        vmConfig.bootImage?.let { bootImage ->
            // 提取文件名作为payload名称
            val payloadName = bootImage.substringAfterLast("/")
                .substringBeforeLast(".")
            builder.setPayloadBinaryName(payloadName)
        } ?: run {
            // 使用默认payload名称
            builder.setPayloadBinaryName("vm_payload")
        }

        // 设置内核参数
        vmConfig.kernelArgs?.let { args ->
            // 内核参数通过payload配置传递
        }
    }

    /**
     * 配置网络
     */
    private fun configureNetwork(
        builder: VirtualMachineConfig.Builder,
        vmConfig: VmConfig
    ) {
        // 默认启用网络
        builder.setNetworkEnabled(true)
    }

    /**
     * 配置调试选项
     */
    private fun configureDebug(
        builder: VirtualMachineConfig.Builder,
        vmConfig: VmConfig
    ) {
        // 在调试构建中启用完整调试
        if (BuildConfig.DEBUG) {
            builder.setDebugLevel(VirtualMachineConfig.DEBUG_LEVEL_FULL)
        }
    }

    /**
     * 获取默认配置
     */
    fun getDefaultConfig(name: String, id: String): VmConfig {
        return VmConfig(
            id = id,
            name = name,
            memory = DEFAULT_MEMORY_MB,
            cpu = DEFAULT_CPU_COUNT,
            diskSize = DEFAULT_DISK_SIZE_GB,
            resources = VmResources(
                memoryMb = DEFAULT_MEMORY_MB,
                cpuCores = DEFAULT_CPU_COUNT,
                diskSizeGb = DEFAULT_DISK_SIZE_GB
            )
        )
    }

    /**
     * 检查配置是否支持
     */
    fun isConfigSupported(vmConfig: VmConfig): Boolean {
        return try {
            val validationResult = validateConfig(vmConfig)
            validationResult.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Config validation failed", e)
            false
        }
    }
}