package com.dfa.core.vm

/**
 * 虚拟机回调接口
 *
 * 用于接收虚拟机状态变化和事件通知
 */
interface VmCallback {
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
     * @param ipAddress 虚拟机IP地址
     */
    fun onVmStarted(ipAddress: String)

    /**
     * 虚拟机停止回调
     */
    fun onVmStopped()

    /**
     * 虚拟机销毁回调
     */
    fun onVmDestroyed()
}

/**
 * 虚拟机特性枚举
 *
 * 定义虚拟机后端支持的各种特性
 */
enum class VmFeature {
    /** 快照支持 */
    SNAPSHOTS,
    /** 实时迁移 */
    LIVE_MIGRATION,
    /** CPU热插拔 */
    CPU_HOTPLUG,
    /** 内存热插拔 */
    MEMORY_HOTPLUG,
    /** 设备热插拔 */
    DEVICE_HOTPLUG,
    /** 硬件加速 */
    HARDWARE_ACCELERATION,
    /** VNC显示 */
    VNC_DISPLAY,
    /** SPICE显示 */
    SPICE_DISPLAY,
    /** 监控接口 */
    MONITOR_INTERFACE,
    /** 串口控制台 */
    SERIAL_CONSOLE,
    /** USB透传 */
    USB_PASSTHROUGH,
    /** PCI透传 */
    PCI_PASSTHROUGH,
    /** GPU透传 */
    GPU_PASSTHROUGH,
    /** 网络桥接 */
    NETWORK_BRIDGE,
    /** 端口转发 */
    PORT_FORWARDING,
    /** 共享文件夹 */
    SHARED_FOLDERS,
    /** 剪贴板共享 */
    CLIPBOARD_SHARING,
    /** 拖放支持 */
    DRAG_AND_DROP,
    /** Docker支持 */
    DOCKER_SUPPORT,
    /** SSH访问 */
    SSH_ACCESS
}