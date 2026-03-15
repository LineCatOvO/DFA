package com.dfa.core.vm.qemu

/**
 * QEMU监控接口
 *
 * 提供与QEMU Monitor Protocol (QMP)交互的抽象接口
 * 用于执行QEMU监控命令、查询状态、管理设备等操作
 */
interface QemuMonitor {

    // ==================== 连接管理 ====================

    /**
     * 检查监控连接是否活跃
     *
     * @return 连接是否活跃
     */
    val isConnected: Boolean

    /**
     * 连接到QEMU监控
     *
     * @return 连接结果
     */
    suspend fun connect(): Result<Unit>

    /**
     * 断开与QEMU监控的连接
     *
     * @return 断开结果
     */
    suspend fun disconnect(): Result<Unit>

    // ==================== 命令执行 ====================

    /**
     * 执行QMP命令
     *
     * 执行任意的QEMU Monitor Protocol命令
     *
     * @param command 命令名称
     * @param arguments 命令参数
     * @return 命令执行结果（JSON格式字符串）
     */
    suspend fun executeCommand(
        command: String,
        arguments: Map<String, Any> = emptyMap()
    ): Result<String>

    /**
     * 执行HMP命令（Human Monitor Protocol）
     *
     * 执行传统的人类可读监控命令
     *
     * @param command HMP命令字符串
     * @return 命令执行结果
     */
    suspend fun executeHmpCommand(command: String): Result<String>

    // ==================== 状态查询 ====================

    /**
     * 查询虚拟机状态
     *
     * @return 虚拟机状态信息
     */
    suspend fun queryStatus(): Result<QemuStatus>

    /**
     * 查询虚拟机信息
     *
     * @return 虚拟机详细信息
     */
    suspend fun queryVmInfo(): Result<QemuVmInfo>

    /**
     * 查询CPU信息
     *
     * @return CPU信息列表
     */
    suspend fun queryCpus(): Result<List<QemuCpuInfo>>

    /**
     * 查询内存信息
     *
     * @return 内存信息
     */
    suspend fun queryMemoryInfo(): Result<QemuMemoryInfo>

    /**
     * 查询块设备信息
     *
     * @return 块设备信息列表
     */
    suspend fun queryBlockDevices(): Result<List<QemuBlockDeviceInfo>>

    /**
     * 查询网络设备信息
     *
     * @return 网络设备信息列表
     */
    suspend fun queryNetworkDevices(): Result<List<QemuNetworkDeviceInfo>>

    /**
     * 查询PCI设备信息
     *
     * @return PCI设备信息列表
     */
    suspend fun queryPciDevices(): Result<List<QemuPciDeviceInfo>>

    /**
     * 查询USB设备信息
     *
     * @return USB设备信息列表
     */
    suspend fun queryUsbDevices(): Result<List<QemuUsbDeviceInfo>>

    // ==================== 设备操作 ====================

    /**
     * 添加设备
     *
     * 热添加一个设备到虚拟机
     *
     * @param driver 设备驱动名称
     * @param id 设备ID
     * @param properties 设备属性
     * @return 添加结果
     */
    suspend fun deviceAdd(
        driver: String,
        id: String,
        properties: Map<String, Any> = emptyMap()
    ): Result<Unit>

    /**
     * 移除设备
     *
     * 热移除虚拟机中的设备
     *
     * @param id 设备ID
     * @return 移除结果
     */
    suspend fun deviceRemove(id: String): Result<Unit>

    /**
     * 查询设备列表
     *
     * @return 设备列表
     */
    suspend fun queryDevices(): Result<List<QemuDeviceInfo>>

    // ==================== 媒体操作 ====================

    /**
     * 弹出媒体
     *
     * 弹出可移动媒体设备中的媒体
     *
     * @param deviceId 设备ID
     * @param force 是否强制弹出
     * @return 弹出结果
     */
    suspend fun ejectMedia(deviceId: String, force: Boolean = false): Result<Unit>

    /**
     * 更换媒体
     *
     * 更换可移动媒体设备中的媒体
     *
     * @param deviceId 设备ID
     * @param source 新媒体路径
     * @return 更换结果
     */
    suspend fun changeMedia(deviceId: String, source: String): Result<Unit>

    // ==================== 快照操作 ====================

    /**
     * 保存虚拟机状态到快照
     *
     * @param name 快照名称
     * @return 保存结果
     */
    suspend fun saveSnapshot(name: String): Result<Unit>

    /**
     * 加载虚拟机快照
     *
     * @param name 快照名称
     * @return 加载结果
     */
    suspend fun loadSnapshot(name: String): Result<Unit>

    /**
     * 删除虚拟机快照
     *
     * @param name 快照名称
     * @return 删除结果
     */
    suspend fun deleteSnapshot(name: String): Result<Unit>

    /**
     * 列出所有快照
     *
     * @return 快照信息列表
     */
    suspend fun listSnapshots(): Result<List<QemuSnapshotInfo>>

    // ==================== 迁移操作 ====================

    /**
     * 开始迁移
     *
     * @param uri 目标URI
     * @param options 迁移选项
     * @return 迁移结果
     */
    suspend fun migrateStart(uri: String, options: QemuMigrateOptions = QemuMigrateOptions()): Result<Unit>

    /**
     * 查询迁移状态
     *
     * @return 迁移状态信息
     */
    suspend fun queryMigrateStatus(): Result<QemuMigrateStatus>

    /**
     * 取消迁移
     *
     * @return 取消结果
     */
    suspend fun migrateCancel(): Result<Unit>

    // ==================== 电源操作 ====================

    /**
     * 系统电源关闭
     *
     * 发送ACPI电源关闭信号
     *
     * @return 操作结果
     */
    suspend fun systemPowerdown(): Result<Unit>

    /**
     * 系统重置
     *
     * 重置虚拟机
     *
     * @return 操作结果
     */
    suspend fun systemReset(): Result<Unit>

    /**
     * 停止虚拟机
     *
     * 暂停虚拟机执行
     *
     * @return 操作结果
     */
    suspend fun stop(): Result<Unit>

    /**
     * 恢复虚拟机
     *
     * 恢复暂停的虚拟机执行
     *
     * @return 操作结果
     */
    suspend fun cont(): Result<Unit>

    // ==================== 输入操作 ====================

    /**
     * 发送按键事件
     *
     * @param keys 按键码列表
     * @return 操作结果
     */
    suspend fun sendKeyEvent(keys: List<QemuKeyEvent>): Result<Unit>

    /**
     * 发送鼠标移动事件
     *
     * @param x X坐标
     * @param y Y坐标
     * @return 操作结果
     */
    suspend fun sendMouseMoveEvent(x: Int, y: Int): Result<Unit>

    /**
     * 发送鼠标按钮事件
     *
     * @param button 按钮类型
     * @param pressed 是否按下
     * @return 操作结果
     */
    suspend fun sendMouseButtonEvent(button: QemuMouseButton, pressed: Boolean): Result<Unit>

    // ==================== 截图操作 ====================

    /**
     * 截取屏幕
     *
     * @param format 图像格式
     * @return 截图数据
     */
    suspend fun screendump(format: String = "png"): Result<ByteArray>

    // ==================== 字符设备操作 ====================

    /**
     * 查询字符设备
     *
     * @return 字符设备信息列表
     */
    suspend fun queryCharDevices(): Result<List<QemuCharDeviceInfo>>

    /**
     * 向字符设备发送数据
     *
     * @param deviceId 设备ID
     * @param data 数据
     * @return 发送结果
     */
    suspend fun sendCharData(deviceId: String, data: ByteArray): Result<Unit>

    // ==================== 事件监听 ====================

    /**
     * 注册事件监听器
     *
     * @param listener 事件监听器
     */
    fun registerEventListener(listener: QemuEventListener)

    /**
     * 注销事件监听器
     *
     * @param listener 事件监听器
     */
    fun unregisterEventListener(listener: QemuEventListener)

    /**
     * 获取事件流
     *
     * @return 事件流
     */
    fun getEventFlow(): kotlinx.coroutines.flow.Flow<QemuEvent>
}

// ==================== 数据类定义 ====================

/**
 * QEMU虚拟机状态
 *
 * @property running 是否运行中
 * @property status 状态字符串
 * @property singlestep 是否单步模式
 * @property statusDetail 状态详情
 */
data class QemuStatus(
    val running: Boolean,
    val status: String,
    val singlestep: Boolean = false,
    val statusDetail: String? = null
)

/**
 * QEMU CPU信息
 *
 * @property cpuIndex CPU索引
 * @property architecture 架构
 * @property current 是否当前CPU
 * @property halted 是否暂停
 * @property pc 程序计数器
 * @property threadId 线程ID
 * @property props CPU属性
 */
data class QemuCpuInfo(
    val cpuIndex: Int,
    val architecture: String? = null,
    val current: Boolean = false,
    val halted: Boolean = false,
    val pc: Long? = null,
    val threadId: Int? = null,
    val props: Map<String, Any> = emptyMap()
)

/**
 * QEMU内存信息
 *
 * @property baseMemory 基础内存（字节）
 * @property totalMemory 总内存（字节）
 * @property availableMemory 可用内存（字节）
 * @property memorySlots 内存插槽数
 * @property memoryDevices 内存设备列表
 */
data class QemuMemoryInfo(
    val baseMemory: Long,
    val totalMemory: Long,
    val availableMemory: Long? = null,
    val memorySlots: Int = 0,
    val memoryDevices: List<QemuMemoryDeviceInfo> = emptyList()
)

/**
 * QEMU内存设备信息
 *
 * @property id 设备ID
 * @property size 大小（字节）
 * @property slot 插槽号
 * @property node NUMA节点
 * @property hotplugged 是否热插拔
 * @property hotpluggable 是否可热插拔
 */
data class QemuMemoryDeviceInfo(
    val id: String? = null,
    val size: Long,
    val slot: Int? = null,
    val node: Int? = null,
    val hotplugged: Boolean = false,
    val hotpluggable: Boolean = true
)

/**
 * QEMU块设备信息
 *
 * @property device 设备名称
 * @property nodeName 节点名称
 * @property inserted 插入的媒体信息
 * @property removable 是否可移除
 * @property locked 是否锁定
 * @property trayOpen 托盘是否打开
 * @property ioStatus IO状态
 */
data class QemuBlockDeviceInfo(
    val device: String,
    val nodeName: String? = null,
    val inserted: QemuBlockDeviceInfo? = null,
    val removable: Boolean = false,
    val locked: Boolean = false,
    val trayOpen: Boolean = false,
    val ioStatus: String? = null,
    val file: String? = null,
    val format: String? = null,
    val virtualSize: Long? = null,
    val actualSize: Long? = null
)

/**
 * QEMU网络设备信息
 *
 * @property name 设备名称
 * @property type 设备类型
 * @property macAddress MAC地址
 * @property ipAddress IP地址
 * @property netdev 网络设备名称
 * @property link 是否连接
 * @property rxBytes 接收字节数
 * @property txBytes 发送字节数
 * @property rxPackets 接收包数
 * @property txPackets 发送包数
 */
data class QemuNetworkDeviceInfo(
    val name: String,
    val type: String,
    val macAddress: String? = null,
    val ipAddress: String? = null,
    val netdev: String? = null,
    val link: Boolean = true,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxPackets: Long = 0,
    val txPackets: Long = 0
)

/**
 * QEMU PCI设备信息
 *
 * @property bus 总线号
 * @property slot 插槽号
 * @property function 功能号
 * @property className 类名
 * @property vendorId 厂商ID
 * @property deviceId 设备ID
 * @property irq IRQ号
 * @property pciAddress PCI地址
 */
data class QemuPciDeviceInfo(
    val bus: Int,
    val slot: Int,
    val function: Int,
    val className: String? = null,
    val vendorId: String? = null,
    val deviceId: String? = null,
    val irq: Int? = null,
    val pciAddress: String? = null
)

/**
 * QEMU USB设备信息
 *
 * @property bus 总线号
 * @property port 端口号
 * @property deviceId 设备ID
 * @property vendorId 厂商ID
 * @property productId 产品ID
 * @property speed 速度
 * @property manufacturer 制造商
 * @property product 产品名
 */
data class QemuUsbDeviceInfo(
    val bus: Int,
    val port: String,
    val deviceId: Int,
    val vendorId: String? = null,
    val productId: String? = null,
    val speed: String? = null,
    val manufacturer: String? = null,
    val product: String? = null
)

/**
 * QEMU设备信息
 *
 * @property id 设备ID
 * @property driver 驱动名称
 * @property parentPath 父设备路径
 * @property properties 设备属性
 */
data class QemuDeviceInfo(
    val id: String,
    val driver: String,
    val parentPath: String? = null,
    val properties: Map<String, Any> = emptyMap()
)

/**
 * QEMU快照信息
 *
 * @property name 快照名称
 * @property id 快照ID
 * @property vmStateSize 虚拟机状态大小
 * @property dateSec 创建时间戳（秒）
 * @property dateNsec 创建时间戳（纳秒部分）
 * @property vmClockSec 虚拟机时钟（秒）
 * @property vmClockNsec 虚拟机时钟（纳秒部分）
 * @property icount 指令计数
 */
data class QemuSnapshotInfo(
    val name: String,
    val id: String? = null,
    val vmStateSize: Long = 0,
    val dateSec: Long = 0,
    val dateNsec: Long = 0,
    val vmClockSec: Long = 0,
    val vmClockNsec: Long = 0,
    val icount: Long? = null
) {
    /**
     * 格式化的创建时间
     */
    val formattedDate: String
        get() {
            val timestamp = dateSec * 1000 + dateNsec / 1_000_000
            return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(java.util.Date(timestamp))
        }
}

/**
 * QEMU迁移选项
 *
 * @property live 是否实时迁移
 * @property compress 是否压缩
 * @property compressLevel 压缩级别
 * @property bandwidth 带宽限制（字节/秒）
 * @property downtime 停机时间限制（毫秒）
 * @property autoConverge 是否自动收敛
 * @property incremental 是否增量迁移
 */
data class QemuMigrateOptions(
    val live: Boolean = true,
    val compress: Boolean = false,
    val compressLevel: Int = 1,
    val bandwidth: Long? = null,
    val downtime: Int? = null,
    val autoConverge: Boolean = false,
    val incremental: Boolean = false
)

/**
 * QEMU迁移状态
 *
 * @property status 状态
 * @property total 总数据量
 * @property remaining 剩余数据量
 * @property transferred 已传输数据量
 * @property bandwidth 当前带宽
 * @property downtime 停机时间
 * @property setupTime 设置时间
 * @property expectedDowntime 预期停机时间
 */
data class QemuMigrateStatus(
    val status: String,
    val total: Long = 0,
    val remaining: Long = 0,
    val transferred: Long = 0,
    val bandwidth: Long = 0,
    val downtime: Long = 0,
    val setupTime: Long = 0,
    val expectedDowntime: Long = 0
) {
    /**
     * 迁移进度百分比
     */
    val progress: Double
        get() = if (total > 0) (transferred.toDouble() / total) * 100 else 0.0

    /**
     * 是否完成
     */
    val isCompleted: Boolean
        get() = status == "completed"

    /**
     * 是否失败
     */
    val isFailed: Boolean
        get() = status == "failed"

    /**
     * 是否正在进行
     */
    val isInProgress: Boolean
        get() = status in listOf("setup", "active", "pre-switchover", "device")
}

/**
 * QEMU字符设备信息
 *
 * @property label 设备标签
 * @property filename 文件名/路径
 * @property frontendOpen 前端是否打开
 */
data class QemuCharDeviceInfo(
    val label: String,
    val filename: String,
    val frontendOpen: Boolean = false
)

/**
 * QEMU按键事件
 *
 * @property keyCode 按键码
 * @property pressed 是否按下
 */
data class QemuKeyEvent(
    val keyCode: Int,
    val pressed: Boolean = true
)

/**
 * QEMU鼠标按钮枚举
 */
enum class QemuMouseButton {
    /** 左键 */
    LEFT,
    /** 右键 */
    RIGHT,
    /** 中键 */
    MIDDLE,
    /** 滚轮向上 */
    WHEEL_UP,
    /** 滚轮向下 */
    WHEEL_DOWN
}

/**
 * QEMU事件类型枚举
 */
enum class QemuEventType {
    /** 虚拟机状态改变 */
    VM_STATE_CHANGED,
    /** 设备添加 */
    DEVICE_ADDED,
    /** 设备移除 */
    DEVICE_REMOVED,
    /** 块设备插入 */
    BLOCK_INSERTED,
    /** 块设备弹出 */
    BLOCK_EJECTED,
    /** 迁移状态改变 */
    MIGRATION_STATUS_CHANGED,
    /** 电源状态改变 */
    POWER_STATE_CHANGED,
    /** RTC时间改变 */
    RTC_CHANGE,
    /** 看门狗触发 */
    WATCHDOG,
    /** 重启请求 */
    RESET_REQUESTED,
    /** 关机请求 */
    SHUTDOWN_REQUESTED,
    /** 其他事件 */
    OTHER
}

/**
 * QEMU事件
 *
 * @property type 事件类型
 * @property timestamp 时间戳
 * @property data 事件数据
 */
data class QemuEvent(
    val type: QemuEventType,
    val timestamp: Long,
    val data: Map<String, Any>
)

/**
 * QEMU事件监听器接口
 */
interface QemuEventListener {
    /**
     * 处理QEMU事件
     *
     * @param event QEMU事件
     */
    fun onEvent(event: QemuEvent)
}

/**
 * QMP异常类
 *
 * @property errorClass 错误类
 * @property description 错误描述
 * @property location 错误位置
 */
class QmpException(
    val errorClass: String,
    val description: String,
    val location: String? = null
) : Exception("QMP Error: $errorClass - $description") {

    /**
     * 是否为命令未找到错误
     */
    val isCommandNotFound: Boolean
        get() = errorClass == "CommandNotFound"

    /**
     * 是否为设备未找到错误
     */
    val isDeviceNotFound: Boolean
        get() = errorClass == "DeviceNotFound"

    /**
     * 是否为无效参数错误
     */
    val isInvalidParameter: Boolean
        get() = errorClass == "InvalidParameter"

    /**
     * 是否为通用错误
     */
    val isGenericError: Boolean
        get() = errorClass == "GenericError"
}