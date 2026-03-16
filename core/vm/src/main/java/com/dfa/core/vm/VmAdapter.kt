package com.dfa.core.vm

/**
 * 通用虚拟机适配器接口
 *
 * 抽象虚拟机生命周期操作，支持多种虚拟化后端（AVF、QEMU、Docker等）
 * 所有虚拟化后端实现都应实现此接口
 */
interface VmAdapter {

    /**
     * 获取后端类型
     *
     * @return 虚拟机后端类型
     */
    val backendType: VmBackendType

    /**
     * 检查后端是否可用
     *
     * @return 后端是否可用
     */
    suspend fun isAvailable(): Boolean

    /**
     * 创建虚拟机
     *
     * @param config 虚拟机配置
     * @return 虚拟机句柄
     */
    suspend fun createVm(config: VmConfig): Result<VmHandle>

    /**
     * 启动虚拟机
     *
     * @param handle 虚拟机句柄
     * @return 启动结果，包含虚拟机信息
     */
    suspend fun startVm(handle: VmHandle): Result<VmInfo>

    /**
     * 停止虚拟机
     *
     * @param handle 虚拟机句柄
     * @param force 是否强制停止
     * @return 停止结果
     */
    suspend fun stopVm(handle: VmHandle, force: Boolean = false): Result<Unit>

    /**
     * 暂停虚拟机
     *
     * @param handle 虚拟机句柄
     * @return 暂停结果
     */
    suspend fun pauseVm(handle: VmHandle): Result<Unit>

    /**
     * 恢复虚拟机
     *
     * @param handle 虚拟机句柄
     * @return 恢复结果
     */
    suspend fun resumeVm(handle: VmHandle): Result<Unit>

    /**
     * 获取虚拟机状态
     *
     * @param handle 虚拟机句柄
     * @return 虚拟机信息
     */
    suspend fun getVmStatus(handle: VmHandle): Result<VmInfo>

    /**
     * 销毁虚拟机
     *
     * @param handle 虚拟机句柄
     * @return 销毁结果
     */
    suspend fun destroyVm(handle: VmHandle): Result<Unit>

    /**
     * 注册回调
     *
     * @param callback 回调接口
     */
    fun registerCallback(callback: VmCallback)

    /**
     * 注销回调
     *
     * @param callback 回调接口
     */
    fun unregisterCallback(callback: VmCallback)

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
    suspend fun getAvailableResources(): VmResources

    /**
     * 获取支持的特性列表
     *
     * @return 支持的特性集合
     */
    fun getSupportedFeatures(): Set<VmFeature>

    /**
     * 检查是否支持指定特性
     *
     * @param feature 特性
     * @return 是否支持
     */
    fun supportsFeature(feature: VmFeature): Boolean = feature in getSupportedFeatures()
}

/**
 * VmAdapter扩展函数：创建并启动虚拟机
 *
 * @param config 虚拟机配置
 * @return 启动结果，包含虚拟机信息
 */
suspend fun VmAdapter.createAndStartVm(config: VmConfig): Result<VmInfo> {
    val handle = createVm(config).getOrElse { error ->
        return Result.failure(error)
    }
    return startVm(handle)
}

/**
 * VmAdapter扩展函数：安全停止并销毁虚拟机
 *
 * @param handle 虚拟机句柄
 * @param force 是否强制停止
 * @return 销毁结果
 */
suspend fun VmAdapter.stopAndDestroyVm(handle: VmHandle, force: Boolean = false): Result<Unit> {
    return stopVm(handle, force).getOrElse {
        // 停止失败，尝试强制停止
        if (!force) {
            stopVm(handle, force = true)
        } else {
            return Result.failure(it)
        }
    }.let {
        destroyVm(handle)
    }
}

/**
 * VmAdapter工厂接口
 *
 * 用于创建不同后端的VmAdapter实例
 */
interface VmAdapterFactory {
    /**
     * 获取支持的后端类型
     */
    val supportedBackendType: VmBackendType

    /**
     * 创建适配器实例
     *
     * @return VmAdapter实例
     */
    fun create(): VmAdapter

    /**
     * 检查后端是否可用
     *
     * @return 后端是否可用
     */
    suspend fun isBackendAvailable(): Boolean
}

/**
 * VmAdapter类型别名：用于向后兼容AvfVmAdapter
 */
typealias AvfVmAdapterCompat = VmAdapter