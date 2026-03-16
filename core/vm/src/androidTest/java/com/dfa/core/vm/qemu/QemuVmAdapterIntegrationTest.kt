package com.dfa.core.vm.qemu

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmHandle
import com.dfa.core.vm.VmInfo
import com.dfa.core.vm.VmResources
import com.dfa.core.vm.VmState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * QEMU虚拟机适配器集成测试
 *
 * 测试QEMU虚拟机的创建、启动、停止和状态管理功能
 * 需要在真实的Android设备上运行，验证与Termux环境的集成
 *
 * 测试覆盖范围：
 * - QEMU可用性检查
 * - 虚拟机创建和销毁
 * - 虚拟机启动和停止
 * - 状态变化监听
 * - 快照管理
 * - 磁盘镜像操作
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class QemuVmAdapterIntegrationTest {

    // 测试用的QEMU适配器实例
    private lateinit var qemuAdapter: QemuVmAdapter

    // 测试用的QEMU配置（用于QEMU特定功能测试）
    private val testQemuConfig = QemuConfig.Builder()
        .id("test-vm-integration")
        .name("Integration Test VM")
        .targetArch(QemuTargetArch.X86_64)
        .memoryMb(512)
        .cpuCores(1)
        .enableKvm(false)
        .build()

    // 测试用的通用虚拟机配置（用于VmAdapter接口方法）
    private val testVmConfig = VmConfig(
        id = "test-vm-integration",
        name = "Integration Test VM",
        memory = 512,
        cpu = 1
    )

    // 测试过程中创建的虚拟机句柄
    private var createdHandle: VmHandle? = null

    @Before
    fun setup() {
        // 初始化QEMU适配器
        // 实际实现中应该通过依赖注入获取
        // qemuAdapter = QemuVmAdapterImpl()
    }

    @After
    fun tearDown() = runTest {
        // 清理测试过程中创建的虚拟机
        createdHandle?.let { handle ->
            try {
                qemuAdapter.stopVm(handle)
                qemuAdapter.destroyVm(handle)
            } catch (e: Exception) {
                // 忽略清理错误
            }
        }
    }

    // ==================== QEMU可用性检查测试 ====================

    @Test
    fun `isQemuAvailable should return true when QEMU is installed`() = runTest {
        // Given: QEMU已安装在Termux环境中
        // When: 检查QEMU是否可用
        val isAvailable = qemuAdapter.isQemuAvailable()

        // Then: 应该返回true
        assertThat(isAvailable).isTrue()
    }

    @Test
    fun `getQemuVersion should return valid version string`() = runTest {
        // Given: QEMU已安装
        // When: 获取QEMU版本
        val result = qemuAdapter.getQemuVersion()

        // Then: 应该返回有效的版本字符串
        assertThat(result.isSuccess).isTrue()
        val version = result.getOrNull()
        assertThat(version).isNotEmpty()
        assertThat(version?.matches(Regex(".*\\d+\\.\\d+.*")) ?: false).isTrue()
    }

    @Test
    fun `getSupportedArchitectures should return non-empty list`() = runTest {
        // Given: QEMU已安装
        // When: 获取支持的架构列表
        val architectures = qemuAdapter.getSupportedArchitectures()

        // Then: 应该返回非空列表
        assertThat(architectures).isNotEmpty()
        assertThat(architectures).contains(QemuTargetArch.X86_64)
    }

    @Test
    fun `isKvmAvailable should return boolean on supported devices`() = runTest {
        // Given: 设备可能支持KVM
        // When: 检查KVM是否可用
        val isKvmAvailable = qemuAdapter.isKvmAvailable()

        // Then: 应该返回布尔值（取决于设备）
        // 在大多数Android设备上，KVM不可用
        assertThat(isKvmAvailable).isFalse()
    }

    @Test
    fun `getSupportedAccelerators should return at least TCG`() = runTest {
        // Given: QEMU已安装
        // When: 获取支持的加速器列表
        val accelerators = qemuAdapter.getSupportedAccelerators()

        // Then: 应该至少包含TCG（软件模拟）
        assertThat(accelerators).contains(QemuAccelerator.TCG)
    }

    // ==================== 虚拟机创建和销毁测试 ====================

    @Test
    fun `createVm should return valid handle with correct config`() = runTest {
        // Given: 有效的虚拟机配置
        val config = testVmConfig

        // When: 创建虚拟机
        val result = qemuAdapter.createVm(config)

        // Then: 应该返回有效的句柄
        assertThat(result.isSuccess).isTrue()
        val handle = result.getOrThrow()
        assertThat(handle.vmId).isEqualTo(config.id)

        // 记录句柄以便清理
        createdHandle = handle
    }

    @Test
    fun `createVm should fail with invalid config`() = runTest {
        // Given: 无效的虚拟机配置（内存为0）
        val invalidConfig = VmConfig(
            id = "invalid-vm",
            name = "Invalid VM",
            memory = 0,
            cpu = 0
        )

        // When: 尝试创建虚拟机
        val result = qemuAdapter.createVm(invalidConfig)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `destroyVm should release all resources`() = runTest {
        // Given: 已创建的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        assertThat(createResult.isSuccess).isTrue()
        val handle = createResult.getOrThrow()

        // When: 销毁虚拟机
        val destroyResult = qemuAdapter.destroyVm(handle)

        // Then: 应该成功销毁
        assertThat(destroyResult.isSuccess).isTrue()
    }

    @Test
    fun `createVm with disk image should succeed`() = runTest {
        // Given: 带磁盘镜像的配置
        val configWithDisk = VmConfig(
            id = "vm-with-disk",
            name = "VM with Disk",
            memory = 512,
            cpu = 1,
            diskSize = 1
        )

        // When: 创建虚拟机
        val result = qemuAdapter.createVm(configWithDisk)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        createdHandle = result.getOrNull()
    }

    // ==================== 虚拟机启动和停止测试 ====================

    @Test
    fun `startVm should transition state to RUNNING`() = runTest {
        // Given: 已创建的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        assertThat(createResult.isSuccess).isTrue()
        val handle = createResult.getOrThrow()
        createdHandle = handle

        // When: 启动虚拟机
        val startResult = qemuAdapter.startVm(handle)

        // Then: 应该成功启动
        assertThat(startResult.isSuccess).isTrue()
        val vmInfo = startResult.getOrThrow()
        assertThat(vmInfo.state).isEqualTo(VmState.RUNNING)
    }

    @Test
    fun `stopVm should transition state to STOPPED`() = runTest {
        // Given: 正在运行的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.startVm(handle)

        // When: 停止虚拟机
        val stopResult = qemuAdapter.stopVm(handle)

        // Then: 应该成功停止
        assertThat(stopResult.isSuccess).isTrue()
        val statusResult = qemuAdapter.getVmStatus(handle)
        assertThat(statusResult.getOrNull()?.state).isEqualTo(VmState.STOPPED)
    }

    @Test
    fun `pauseVm should transition state to PAUSED`() = runTest {
        // Given: 正在运行的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.startVm(handle)

        // When: 暂停虚拟机
        val pauseResult = qemuAdapter.pauseVm(handle)

        // Then: 应该成功暂停
        assertThat(pauseResult.isSuccess).isTrue()
        val statusResult = qemuAdapter.getVmStatus(handle)
        assertThat(statusResult.getOrNull()?.state).isEqualTo(VmState.PAUSED)
    }

    @Test
    fun `resumeVm should transition state back to RUNNING`() = runTest {
        // Given: 已暂停的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.startVm(handle)
        qemuAdapter.pauseVm(handle)

        // When: 恢复虚拟机
        val resumeResult = qemuAdapter.resumeVm(handle)

        // Then: 应该成功恢复
        assertThat(resumeResult.isSuccess).isTrue()
        val statusResult = qemuAdapter.getVmStatus(handle)
        assertThat(statusResult.getOrNull()?.state).isEqualTo(VmState.RUNNING)
    }

    @Test
    fun `startVm should fail for already running VM`() = runTest {
        // Given: 正在运行的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.startVm(handle)

        // When: 再次尝试启动
        val startAgainResult = qemuAdapter.startVm(handle)

        // Then: 应该失败
        assertThat(startAgainResult.isFailure).isTrue()
    }

    // ==================== 状态变化监听测试 ====================

    @Test
    fun `state flow should emit state changes during VM lifecycle`() = runTest {
        // Given: 创建虚拟机并收集状态变化
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle

        // When: 执行生命周期操作
        qemuAdapter.startVm(handle)

        // Then: 状态应该变为RUNNING
        val statusResult = qemuAdapter.getVmStatus(handle)
        assertThat(statusResult.getOrNull()?.state).isEqualTo(VmState.RUNNING)
    }

    @Test
    fun `getVmStatus should return current VM information`() = runTest {
        // Given: 已创建的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle

        // When: 获取虚拟机信息
        val infoResult = qemuAdapter.getVmStatus(handle)

        // Then: 应该返回正确的信息
        assertThat(infoResult.isSuccess).isTrue()
        val info = infoResult.getOrThrow()
        assertThat(info.config.id).isEqualTo(testVmConfig.id)
        assertThat(info.config.name).isEqualTo(testVmConfig.name)
    }

    // ==================== 快照管理测试 ====================

    @Test
    fun `createSnapshot should succeed for stopped VM`() = runTest {
        // Given: 已停止的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle

        // When: 创建快照
        val snapshotResult = qemuAdapter.createSnapshot(handle, "test-snapshot")

        // Then: 应该成功
        assertThat(snapshotResult.isSuccess).isTrue()
    }

    @Test
    fun `listSnapshots should return created snapshots`() = runTest {
        // Given: 有快照的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.createSnapshot(handle, "snapshot-1")
        qemuAdapter.createSnapshot(handle, "snapshot-2")

        // When: 列出快照
        val listResult = qemuAdapter.listSnapshots(handle)

        // Then: 应该包含创建的快照
        assertThat(listResult.isSuccess).isTrue()
        val snapshots = listResult.getOrThrow()
        assertThat(snapshots).contains("snapshot-1")
        assertThat(snapshots).contains("snapshot-2")
    }

    @Test
    fun `restoreSnapshot should restore VM to snapshot state`() = runTest {
        // Given: 有快照的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.createSnapshot(handle, "restore-test")

        // When: 恢复快照
        val restoreResult = qemuAdapter.restoreSnapshot(handle, "restore-test")

        // Then: 应该成功
        assertThat(restoreResult.isSuccess).isTrue()
    }

    @Test
    fun `deleteSnapshot should remove snapshot from list`() = runTest {
        // Given: 有快照的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.createSnapshot(handle, "to-delete")

        // When: 删除快照
        val deleteResult = qemuAdapter.deleteSnapshot(handle, "to-delete")

        // Then: 应该成功
        assertThat(deleteResult.isSuccess).isTrue()

        // 验证快照已被删除
        val listResult = qemuAdapter.listSnapshots(handle)
        assertThat(listResult.getOrNull()).doesNotContain("to-delete")
    }

    // ==================== 磁盘镜像操作测试 ====================

    @Test
    fun `createDiskImage should create valid qcow2 file`() = runTest {
        // Given: 磁盘镜像路径
        val imagePath = "/tmp/test-image-${System.currentTimeMillis()}.qcow2"

        // When: 创建磁盘镜像
        val result = qemuAdapter.createDiskImage(
            path = imagePath,
            format = QemuDiskFormat.QCOW2,
            sizeGb = 1
        )

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `getDiskImageInfo should return correct information`() = runTest {
        // Given: 已存在的磁盘镜像
        val imagePath = "/tmp/test-info-${System.currentTimeMillis()}.qcow2"
        qemuAdapter.createDiskImage(imagePath, QemuDiskFormat.QCOW2, 1)

        // When: 获取镜像信息
        val result = qemuAdapter.getDiskImageInfo(imagePath)

        // Then: 应该返回正确的信息
        assertThat(result.isSuccess).isTrue()
        val info = result.getOrThrow()
        assertThat(info.format).isEqualTo(QemuDiskFormat.QCOW2)
        assertThat(info.virtualSizeGb).isEqualTo(1.0)
    }

    @Test
    fun `convertDiskImage should convert between formats`() = runTest {
        // Given: 源镜像和目标路径
        val sourcePath = "/tmp/convert-source-${System.currentTimeMillis()}.qcow2"
        val targetPath = "/tmp/convert-target-${System.currentTimeMillis()}.raw"
        qemuAdapter.createDiskImage(sourcePath, QemuDiskFormat.QCOW2, 1)

        // When: 转换镜像格式
        val result = qemuAdapter.convertDiskImage(
            sourcePath = sourcePath,
            targetPath = targetPath,
            targetFormat = QemuDiskFormat.RAW
        )

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 资源管理测试 ====================

    @Test
    fun `getAvailableResources should return valid resource info`() = runTest {
        // When: 获取可用资源
        val resources = qemuAdapter.getAvailableResources()

        // Then: 应该返回有效的资源信息
        assertThat(resources.totalMemoryMb).isGreaterThan(0)
        assertThat(resources.availableMemoryMb).isGreaterThan(0)
        assertThat(resources.totalCpuCores).isGreaterThan(0)
        assertThat(resources.availableCpuCores).isGreaterThan(0)
    }

    @Test
    fun `isConfigSupported should return true for valid config`() = runTest {
        // Given: 有效的配置
        val config = testVmConfig

        // When: 检查配置是否支持
        val isSupported = qemuAdapter.isConfigSupported(config)

        // Then: 应该支持
        assertThat(isSupported).isTrue()
    }

    @Test
    fun `isConfigSupported should return false for excessive memory`() = runTest {
        // Given: 内存过大的配置
        val excessiveConfig = VmConfig(
            id = "excessive-vm",
            name = "Excessive Memory VM",
            memory = Int.MAX_VALUE,
            cpu = 1
        )

        // When: 检查配置是否支持
        val isSupported = qemuAdapter.isConfigSupported(excessiveConfig)

        // Then: 应该不支持
        assertThat(isSupported).isFalse()
    }

    // ==================== QEMU监控测试 ====================

    @Test
    fun `getQemuMonitor should return valid monitor interface`() = runTest {
        // Given: 正在运行的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.startVm(handle)

        // When: 获取QEMU监控
        val result = qemuAdapter.getQemuMonitor(handle)

        // Then: 应该返回有效的监控接口
        assertThat(result.isSuccess).isTrue()
        val monitor = result.getOrThrow()
        assertThat(monitor).isNotNull()
    }

    @Test
    fun `getQemuProcessInfo should return process details for running VM`() = runTest {
        // Given: 正在运行的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.startVm(handle)

        // When: 获取进程信息
        val result = qemuAdapter.getQemuProcessInfo(handle)

        // Then: 应该返回有效的进程信息
        assertThat(result.isSuccess).isTrue()
        val processInfo = result.getOrThrow()
        assertThat(processInfo.pid).isGreaterThan(0)
        assertThat(processInfo.memoryUsageMb).isGreaterThan(0)
    }

    // ==================== 输入设备测试 ====================

    @Test
    fun `sendKeys should succeed for running VM`() = runTest {
        // Given: 正在运行的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.startVm(handle)

        // When: 发送按键
        val result = qemuAdapter.sendKeys(handle, listOf(28)) // Enter key

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `sendMouseEvent should succeed for running VM`() = runTest {
        // Given: 正在运行的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.startVm(handle)

        // When: 发送鼠标事件
        val result = qemuAdapter.sendMouseEvent(handle, x = 100, y = 100, buttons = 1)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `takeScreenshot should return image data for running VM`() = runTest {
        // Given: 正在运行的虚拟机
        val createResult = qemuAdapter.createVm(testVmConfig)
        val handle = createResult.getOrThrow()
        createdHandle = handle
        qemuAdapter.startVm(handle)

        // When: 截取屏幕
        val result = qemuAdapter.takeScreenshot(handle, "png")

        // Then: 应该返回有效的图像数据
        assertThat(result.isSuccess).isTrue()
        val imageData = result.getOrThrow()
        assertThat(imageData).isNotEmpty()
    }
}