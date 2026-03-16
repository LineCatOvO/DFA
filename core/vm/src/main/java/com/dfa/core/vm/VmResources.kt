package com.dfa.core.vm

/**
 * 通用虚拟机资源信息
 *
 * 用于表示虚拟化后端的可用资源
 * 支持多种虚拟化后端（AVF、QEMU等）
 *
 * @property backendType 后端类型
 * @property totalMemoryMb 总内存（MB）
 * @property availableMemoryMb 可用内存（MB）
 * @property totalCpuCores 总CPU核心数
 * @property availableCpuCores 可用CPU核心数
 * @property totalDiskSpaceGb 总磁盘空间（GB）
 * @property availableDiskSpaceGb 可用磁盘空间（GB）
 * @property gpuAvailable GPU是否可用
 * @property gpuMemoryMb GPU内存（MB）
 * @property networkAvailable 网络是否可用
 * @property supportedFeatures 支持的特性列表
 * @property maxVms 最大虚拟机数量
 * @property currentVms 当前虚拟机数量
 */
data class VmResources(
    val backendType: VmBackendType = VmBackendType.AVF,
    val totalMemoryMb: Long = 0,
    val availableMemoryMb: Long = 0,
    val totalCpuCores: Int = 0,
    val availableCpuCores: Int = 0,
    val totalDiskSpaceGb: Long = 0,
    val availableDiskSpaceGb: Long = 0,
    val gpuAvailable: Boolean = false,
    val gpuMemoryMb: Int = 0,
    val networkAvailable: Boolean = true,
    val supportedFeatures: Set<VmFeature> = emptySet(),
    val maxVms: Int = 1,
    val currentVms: Int = 0
) {
    /**
     * 检查是否有足够的资源
     */
    val hasEnoughResources: Boolean
        get() = availableMemoryMb > 0 &&
                availableCpuCores > 0 &&
                availableDiskSpaceGb > 0

    /**
     * 检查是否可以创建新虚拟机
     */
    val canCreateVm: Boolean
        get() = hasEnoughResources && currentVms < maxVms

    /**
     * 内存使用率（百分比）
     */
    val memoryUsagePercent: Int
        get() = if (totalMemoryMb > 0) {
            ((totalMemoryMb - availableMemoryMb) * 100 / totalMemoryMb).toInt()
        } else 0

    /**
     * CPU使用率（百分比）
     */
    val cpuUsagePercent: Int
        get() = if (totalCpuCores > 0) {
            ((totalCpuCores - availableCpuCores) * 100 / totalCpuCores)
        } else 0

    /**
     * 磁盘使用率（百分比）
     */
    val diskUsagePercent: Int
        get() = if (totalDiskSpaceGb > 0) {
            ((totalDiskSpaceGb - availableDiskSpaceGb) * 100 / totalDiskSpaceGb).toInt()
        } else 0

    /**
     * 检查是否支持指定特性
     */
    fun supportsFeature(feature: VmFeature): Boolean = feature in supportedFeatures

    /**
     * 检查是否满足配置要求
     */
    fun meetsRequirements(config: VmConfig): Boolean {
        return availableMemoryMb >= config.memory &&
                availableCpuCores >= config.cpu &&
                availableDiskSpaceGb >= config.diskSize &&
                (!config.enableGpu || gpuAvailable)
    }

    /**
     * 计算分配资源后的剩余资源
     */
    fun afterAllocation(config: VmConfig): VmResources = copy(
        availableMemoryMb = (availableMemoryMb - config.memory).coerceAtLeast(0),
        availableCpuCores = (availableCpuCores - config.cpu).coerceAtLeast(0),
        availableDiskSpaceGb = (availableDiskSpaceGb - config.diskSize).coerceAtLeast(0),
        currentVms = currentVms + 1
    )

    /**
     * 计算释放资源后的剩余资源
     */
    fun afterRelease(config: VmConfig): VmResources = copy(
        availableMemoryMb = (availableMemoryMb + config.memory).coerceAtMost(totalMemoryMb),
        availableCpuCores = (availableCpuCores + config.cpu).coerceAtMost(totalCpuCores),
        availableDiskSpaceGb = (availableDiskSpaceGb + config.diskSize).coerceAtMost(totalDiskSpaceGb),
        currentVms = (currentVms - 1).coerceAtLeast(0)
    )
}