package com.dfa.core.vm.qemu

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * QemuMonitorImpl单元测试
 *
 * 测试QEMU监控器的核心功能，包括：
 * - 连接管理（连接、断开）
 * - 命令执行（QMP、HMP）
 * - 状态查询（CPU、内存、设备）
 * - 设备操作（添加、移除）
 * - 快照操作
 * - 事件监听
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QemuMonitorImplTest {

    // 测试对象
    private lateinit var monitor: QemuMonitorImpl

    // 测试配置
    private val testSocketPath = "/tmp/qemu-monitor.sock"
    private val testTcpPath = "tcp://127.0.0.1:4444"

    @Before
    fun setup() {
        monitor = QemuMonitorImpl(
            socketPath = testSocketPath,
            connectionTimeoutMs = 5000,
            readTimeoutMs = 10000
        )
    }

    @After
    fun tearDown() {
        // 清理资源
    }

    // ==================== 连接管理测试 ====================

    @Test
    fun `isConnected should return false initially`() {
        assertThat(monitor.isConnected).isFalse()
    }

    @Test
    fun `connect should fail when socket does not exist`() = runTest {
        val monitorWithInvalidPath = QemuMonitorImpl(
            socketPath = "/non/existent/path.sock"
        )

        val result = monitorWithInvalidPath.connect()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `disconnect should succeed when not connected`() = runTest {
        val result = monitor.disconnect()

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 命令执行测试 ====================

    @Test
    fun `executeCommand should fail when not connected`() = runTest {
        val result = monitor.executeCommand("query-status")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(java.io.IOException::class.java)
    }

    @Test
    fun `executeHmpCommand should fail when not connected`() = runTest {
        val result = monitor.executeHmpCommand("info status")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 状态查询测试 ====================

    @Test
    fun `queryStatus should fail when not connected`() = runTest {
        val result = monitor.queryStatus()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `queryVmInfo should fail when not connected`() = runTest {
        val result = monitor.queryVmInfo()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `queryCpus should fail when not connected`() = runTest {
        val result = monitor.queryCpus()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `queryMemoryInfo should fail when not connected`() = runTest {
        val result = monitor.queryMemoryInfo()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `queryBlockDevices should fail when not connected`() = runTest {
        val result = monitor.queryBlockDevices()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `queryNetworkDevices should fail when not connected`() = runTest {
        val result = monitor.queryNetworkDevices()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `queryPciDevices should fail when not connected`() = runTest {
        val result = monitor.queryPciDevices()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `queryUsbDevices should fail when not connected`() = runTest {
        val result = monitor.queryUsbDevices()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 设备操作测试 ====================

    @Test
    fun `deviceAdd should fail when not connected`() = runTest {
        val result = monitor.deviceAdd("virtio-net-pci", "net0")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `deviceRemove should fail when not connected`() = runTest {
        val result = monitor.deviceRemove("net0")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `queryDevices should fail when not connected`() = runTest {
        val result = monitor.queryDevices()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 媒体操作测试 ====================

    @Test
    fun `ejectMedia should fail when not connected`() = runTest {
        val result = monitor.ejectMedia("drive0")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `changeMedia should fail when not connected`() = runTest {
        val result = monitor.changeMedia("drive0", "/path/to/image.iso")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 快照操作测试 ====================

    @Test
    fun `saveSnapshot should fail when not connected`() = runTest {
        val result = monitor.saveSnapshot("snapshot1")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `loadSnapshot should fail when not connected`() = runTest {
        val result = monitor.loadSnapshot("snapshot1")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `deleteSnapshot should fail when not connected`() = runTest {
        val result = monitor.deleteSnapshot("snapshot1")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `listSnapshots should fail when not connected`() = runTest {
        val result = monitor.listSnapshots()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 迁移操作测试 ====================

    @Test
    fun `migrateStart should fail when not connected`() = runTest {
        val result = monitor.migrateStart("tcp://target:4444")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `queryMigrateStatus should fail when not connected`() = runTest {
        val result = monitor.queryMigrateStatus()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `migrateCancel should fail when not connected`() = runTest {
        val result = monitor.migrateCancel()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 电源操作测试 ====================

    @Test
    fun `systemPowerdown should fail when not connected`() = runTest {
        val result = monitor.systemPowerdown()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `systemReset should fail when not connected`() = runTest {
        val result = monitor.systemReset()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `stop should fail when not connected`() = runTest {
        val result = monitor.stop()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `cont should fail when not connected`() = runTest {
        val result = monitor.cont()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 输入操作测试 ====================

    @Test
    fun `sendKeyEvent should fail when not connected`() = runTest {
        val result = monitor.sendKeyEvent(listOf(QemuKeyEvent(1, true)))

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `sendMouseMoveEvent should fail when not connected`() = runTest {
        val result = monitor.sendMouseMoveEvent(100, 200)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `sendMouseButtonEvent should fail when not connected`() = runTest {
        val result = monitor.sendMouseButtonEvent(QemuMouseButton.LEFT, true)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 截图操作测试 ====================

    @Test
    fun `screendump should fail when not connected`() = runTest {
        val result = monitor.screendump()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 字符设备操作测试 ====================

    @Test
    fun `queryCharDevices should fail when not connected`() = runTest {
        val result = monitor.queryCharDevices()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `sendCharData should fail when not connected`() = runTest {
        val result = monitor.sendCharData("serial0", byteArrayOf(1, 2, 3))

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 事件监听测试 ====================

    @Test
    fun `registerEventListener should add listener`() {
        val listener = createMockListener()

        monitor.registerEventListener(listener)

        // 验证不抛异常
    }

    @Test
    fun `unregisterEventListener should remove listener`() {
        val listener = createMockListener()
        monitor.registerEventListener(listener)

        monitor.unregisterEventListener(listener)

        // 验证不抛异常
    }

    @Test
    fun `unregisterEventListener should not throw for unregistered listener`() {
        val listener = createMockListener()

        monitor.unregisterEventListener(listener)

        // 验证不抛异常
    }

    @Test
    fun `getEventFlow should return flow`() = runTest {
        val flow = monitor.getEventFlow()

        assertThat(flow).isNotNull()
    }

    // ==================== 数据类测试 ====================

    @Test
    fun `QemuStatus should have correct properties`() {
        val status = QemuStatus(
            running = true,
            status = "running",
            singlestep = false,
            statusDetail = "normal"
        )

        assertThat(status.running).isTrue()
        assertThat(status.status).isEqualTo("running")
        assertThat(status.singlestep).isFalse()
        assertThat(status.statusDetail).isEqualTo("normal")
    }

    @Test
    fun `QemuCpuInfo should have correct properties`() {
        val cpuInfo = QemuCpuInfo(
            cpuIndex = 0,
            architecture = "x86_64",
            current = true,
            halted = false,
            threadId = 12345
        )

        assertThat(cpuInfo.cpuIndex).isEqualTo(0)
        assertThat(cpuInfo.architecture).isEqualTo("x86_64")
        assertThat(cpuInfo.current).isTrue()
        assertThat(cpuInfo.halted).isFalse()
        assertThat(cpuInfo.threadId).isEqualTo(12345)
    }

    @Test
    fun `QemuMemoryInfo should have correct properties`() {
        val memoryInfo = QemuMemoryInfo(
            baseMemory = 2048L * 1024 * 1024,
            totalMemory = 4096L * 1024 * 1024,
            memorySlots = 2
        )

        assertThat(memoryInfo.baseMemory).isEqualTo(2048L * 1024 * 1024)
        assertThat(memoryInfo.totalMemory).isEqualTo(4096L * 1024 * 1024)
        assertThat(memoryInfo.memorySlots).isEqualTo(2)
    }

    @Test
    fun `QemuBlockDeviceInfo should have correct properties`() {
        val blockInfo = QemuBlockDeviceInfo(
            device = "drive0",
            nodeName = "node0",
            removable = false,
            locked = false,
            trayOpen = false,
            file = "/path/to/disk.qcow2",
            format = "qcow2",
            virtualSize = 10L * 1024 * 1024 * 1024,
            actualSize = 5L * 1024 * 1024 * 1024
        )

        assertThat(blockInfo.device).isEqualTo("drive0")
        assertThat(blockInfo.file).isEqualTo("/path/to/disk.qcow2")
        assertThat(blockInfo.format).isEqualTo("qcow2")
    }

    @Test
    fun `QemuNetworkDeviceInfo should have correct properties`() {
        val netInfo = QemuNetworkDeviceInfo(
            name = "net0",
            type = "virtio-net-pci",
            macAddress = "52:54:00:12:34:56",
            link = true
        )

        assertThat(netInfo.name).isEqualTo("net0")
        assertThat(netInfo.type).isEqualTo("virtio-net-pci")
        assertThat(netInfo.macAddress).isEqualTo("52:54:00:12:34:56")
        assertThat(netInfo.link).isTrue()
    }

    @Test
    fun `QemuPciDeviceInfo should have correct properties`() {
        val pciInfo = QemuPciDeviceInfo(
            bus = 0,
            slot = 1,
            function = 0,
            className = "Ethernet controller",
            vendorId = "8086",
            deviceId = "100e"
        )

        assertThat(pciInfo.bus).isEqualTo(0)
        assertThat(pciInfo.slot).isEqualTo(1)
        assertThat(pciInfo.function).isEqualTo(0)
        assertThat(pciInfo.vendorId).isEqualTo("8086")
    }

    @Test
    fun `QemuUsbDeviceInfo should have correct properties`() {
        val usbInfo = QemuUsbDeviceInfo(
            bus = 1,
            port = "1",
            deviceId = 2,
            vendorId = "1234",
            productId = "5678",
            speed = "480"
        )

        assertThat(usbInfo.bus).isEqualTo(1)
        assertThat(usbInfo.vendorId).isEqualTo("1234")
        assertThat(usbInfo.productId).isEqualTo("5678")
    }

    @Test
    fun `QemuDeviceInfo should have correct properties`() {
        val deviceInfo = QemuDeviceInfo(
            id = "device0",
            driver = "virtio-net-pci",
            parentPath = "/machine/peripheral"
        )

        assertThat(deviceInfo.id).isEqualTo("device0")
        assertThat(deviceInfo.driver).isEqualTo("virtio-net-pci")
    }

    @Test
    fun `QemuSnapshotInfo should have correct properties`() {
        val snapshot = QemuSnapshotInfo(
            name = "snapshot1",
            id = "1",
            vmStateSize = 1024 * 1024,
            dateSec = System.currentTimeMillis() / 1000
        )

        assertThat(snapshot.name).isEqualTo("snapshot1")
        assertThat(snapshot.id).isEqualTo("1")
        assertThat(snapshot.formattedDate).isNotEmpty()
    }

    @Test
    fun `QemuMigrateOptions should have correct defaults`() {
        val options = QemuMigrateOptions()

        assertThat(options.live).isTrue()
        assertThat(options.compress).isFalse()
        assertThat(options.autoConverge).isFalse()
    }

    @Test
    fun `QemuMigrateStatus should calculate progress correctly`() {
        val status = QemuMigrateStatus(
            status = "active",
            total = 1000,
            transferred = 500
        )

        assertThat(status.progress).isWithin(0.1).of(50.0)
        assertThat(status.isCompleted).isFalse()
        assertThat(status.isInProgress).isTrue()
    }

    @Test
    fun `QemuMigrateStatus isCompleted should return true when completed`() {
        val status = QemuMigrateStatus(status = "completed")

        assertThat(status.isCompleted).isTrue()
        assertThat(status.isInProgress).isFalse()
    }

    @Test
    fun `QemuMigrateStatus isFailed should return true when failed`() {
        val status = QemuMigrateStatus(status = "failed")

        assertThat(status.isFailed).isTrue()
    }

    @Test
    fun `QemuCharDeviceInfo should have correct properties`() {
        val charInfo = QemuCharDeviceInfo(
            label = "serial0",
            filename = "stdio",
            frontendOpen = true
        )

        assertThat(charInfo.label).isEqualTo("serial0")
        assertThat(charInfo.frontendOpen).isTrue()
    }

    @Test
    fun `QemuKeyEvent should have correct properties`() {
        val event = QemuKeyEvent(keyCode = 1, pressed = true)

        assertThat(event.keyCode).isEqualTo(1)
        assertThat(event.pressed).isTrue()
    }

    // ==================== 枚举测试 ====================

    @Test
    fun `QemuMouseButton should have all expected values`() {
        assertThat(QemuMouseButton.values()).asList().containsExactly(
            QemuMouseButton.LEFT,
            QemuMouseButton.RIGHT,
            QemuMouseButton.MIDDLE,
            QemuMouseButton.WHEEL_UP,
            QemuMouseButton.WHEEL_DOWN
        )
    }

    @Test
    fun `QemuEventType should have all expected values`() {
        assertThat(QemuEventType.values()).asList().containsAtLeast(
            QemuEventType.VM_STATE_CHANGED,
            QemuEventType.DEVICE_ADDED,
            QemuEventType.DEVICE_REMOVED,
            QemuEventType.SHUTDOWN_REQUESTED
        )
    }

    // ==================== QmpException测试 ====================

    @Test
    fun `QmpException should have correct properties`() {
        val exception = QmpException(
            errorClass = "CommandNotFound",
            description = "Command not found",
            location = "test"
        )

        assertThat(exception.errorClass).isEqualTo("CommandNotFound")
        assertThat(exception.description).isEqualTo("Command not found")
        assertThat(exception.location).isEqualTo("test")
        assertThat(exception.isCommandNotFound).isTrue()
    }

    @Test
    fun `QmpException isDeviceNotFound should return true for DeviceNotFound`() {
        val exception = QmpException("DeviceNotFound", "Device not found")

        assertThat(exception.isDeviceNotFound).isTrue()
    }

    @Test
    fun `QmpException isInvalidParameter should return true for InvalidParameter`() {
        val exception = QmpException("InvalidParameter", "Invalid parameter")

        assertThat(exception.isInvalidParameter).isTrue()
    }

    @Test
    fun `QmpException isGenericError should return true for GenericError`() {
        val exception = QmpException("GenericError", "Generic error")

        assertThat(exception.isGenericError).isTrue()
    }

    // ==================== QemuEvent测试 ====================

    @Test
    fun `QemuEvent should have correct properties`() {
        val event = QemuEvent(
            type = QemuEventType.VM_STATE_CHANGED,
            timestamp = System.currentTimeMillis() / 1000,
            data = mapOf("state" to "running")
        )

        assertThat(event.type).isEqualTo(QemuEventType.VM_STATE_CHANGED)
        assertThat(event.data).containsEntry("state", "running")
    }

    // ==================== 辅助方法 ====================

    private fun createMockListener(): QemuEventListener {
        return mockk<QemuEventListener>(relaxed = true) {
            every { onEvent(any()) } just Runs
        }
    }
}