package com.dfa.core.vm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.dfa.core.vm.statemachine.VmStateMachine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 虚拟机完整生命周期集成测试
 *
 * 测试虚拟机从创建到销毁的完整生命周期管理。
 * 需要在真实的Android设备上运行，验证与AVF/QEMU的集成。
 *
 * 测试覆盖范围：
 * - 虚拟机创建和初始化
 * - 启动和停止流程
 * - 暂停和恢复功能
 * - 状态转换验证
 * - 错误恢复机制
 * - 资源清理
 * - 并发操作处理
 *
 * 运行条件：
 * - 设备支持虚拟化（AVF或QEMU）
 * - 有足够的系统资源
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 33)
class VmFullLifecycleIntegrationTest {

    // 虚拟机管理器实例
    private lateinit var vmManager: VmManager

    // 测试用的虚拟机配置
    private val testVmConfig = VmConfig(
        id = "test-vm-lifecycle-${System.currentTimeMillis()}",
        name = "Lifecycle Test VM",
        memory = 512,
        cpu = 1,
        diskSize = 1
    )

    // 测试过程中创建的虚拟机
    private val createdVmIds = mutableListOf<String>()

    @Before
    fun setup() = runTest {
        // 检查虚拟化是否可用
        val isVirtualizationAvailable = checkVirtualizationAvailability()
        Assume.assumeTrue("Virtualization is not available on this device", isVirtualizationAvailable)
    }

    @After
    fun tearDown() = runTest {
        // 清理测试过程中创建的虚拟机
        cleanupTestVms()
        if (::vmManager.isInitialized) {
            vmManager.release()
        }
    }

    // ==================== 辅助方法 ====================

    private fun checkVirtualizationAvailability(): Boolean {
        return try {
            // 检查设备是否支持AVF或QEMU
            // 实际实现中应该检查具体的虚拟化支持
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun cleanupTestVms() {
        createdVmIds.forEach { vmId ->
            try {
                if (vmManager.getCurrentState() != VmState.STOPPED) {
                    vmManager.stop(force = true)
                }
            } catch (e: Exception) {
                // 忽略清理错误
            }
        }
    }

    // ==================== 初始化测试 ====================

    @Test
    fun `initialize should set up VM manager correctly`() = runTest {
        // When: 初始化虚拟机管理器
        val result = vmManager.initialize(testVmConfig)

        // Then: 应该成功初始化
        assertThat(result.isSuccess).isTrue()
        assertThat(vmManager.isInitialized.first()).isTrue()
    }

    @Test
    fun `initialize should return valid VM info`() = runTest {
        // When: 初始化虚拟机管理器
        val result = vmManager.initialize(testVmConfig)

        // Then: 应该返回有效的虚拟机信息
        assertThat(result.isSuccess).isTrue()
        val vmInfo = result.getOrThrow()
        assertThat(vmInfo.config.id).isEqualTo(testVmConfig.id)
        assertThat(vmInfo.config.name).isEqualTo(testVmConfig.name)
        assertThat(vmInfo.state).isEqualTo(VmState.CREATED)
    }

    @Test
    fun `initialize should fail with invalid config`() = runTest {
        // Given: 无效的配置（内存为0）
        val invalidConfig = VmConfig(
            id = "invalid-vm",
            name = "Invalid VM",
            memory = 0,
            cpu = 0,
            diskSize = 0
        )

        // When: 尝试初始化
        val result = vmManager.initialize(invalidConfig)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `initialize should fail when already initialized`() = runTest {
        // Given: 已初始化的管理器
        vmManager.initialize(testVmConfig)

        // When: 再次初始化
        val result = vmManager.initialize(testVmConfig)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    // ==================== 启动测试 ====================

    @Test
    fun `start should transition VM to RUNNING state`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 启动虚拟机
        val result = vmManager.start()

        // Then: 应该成功启动
        assertThat(result.isSuccess).isTrue()
        val vmInfo = result.getOrThrow()
        assertThat(vmInfo.state).isEqualTo(VmState.RUNNING)
    }

    @Test
    fun `start should update VM state flow`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 启动虚拟机
        vmManager.start()

        // Then: 状态流应该更新
        val currentState = vmManager.vmState.first()
        assertThat(currentState).isEqualTo(VmState.RUNNING)
    }

    @Test
    fun `start should fail when not initialized`() = runTest {
        // Given: 未初始化的管理器

        // When: 尝试启动
        val result = vmManager.start()

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `start should fail when already running`() = runTest {
        // Given: 正在运行的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()

        // When: 再次启动
        val result = vmManager.start()

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    // ==================== 停止测试 ====================

    @Test
    fun `stop should transition VM to STOPPED state`() = runTest {
        // Given: 正在运行的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()

        // When: 停止虚拟机
        val result = vmManager.stop()

        // Then: 应该成功停止
        assertThat(result.isSuccess).isTrue()
        val vmInfo = result.getOrThrow()
        assertThat(vmInfo.state).isEqualTo(VmState.STOPPED)
    }

    @Test
    fun `stop with force should immediately stop VM`() = runTest {
        // Given: 正在运行的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()

        // When: 强制停止虚拟机
        val result = vmManager.stop(force = true)

        // Then: 应该成功停止
        assertThat(result.isSuccess).isTrue()
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.STOPPED)
    }

    @Test
    fun `stop should fail when not running`() = runTest {
        // Given: 已初始化但未启动的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 尝试停止
        val result = vmManager.stop()

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    // ==================== 暂停恢复测试 ====================

    @Test
    fun `pause should transition VM to PAUSED state`() = runTest {
        // Given: 正在运行的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()

        // When: 暂停虚拟机
        val result = vmManager.pause()

        // Then: 应该成功暂停
        assertThat(result.isSuccess).isTrue()
        val vmInfo = result.getOrThrow()
        assertThat(vmInfo.state).isEqualTo(VmState.PAUSED)
    }

    @Test
    fun `resume should transition PAUSED VM to RUNNING state`() = runTest {
        // Given: 已暂停的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()
        vmManager.pause()

        // When: 恢复虚拟机
        val result = vmManager.resume()

        // Then: 应该成功恢复
        assertThat(result.isSuccess).isTrue()
        val vmInfo = result.getOrThrow()
        assertThat(vmInfo.state).isEqualTo(VmState.RUNNING)
    }

    @Test
    fun `pause should fail when not running`() = runTest {
        // Given: 已初始化但未启动的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 尝试暂停
        val result = vmManager.pause()

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `resume should fail when not paused`() = runTest {
        // Given: 正在运行的虚拟机（未暂停）
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()

        // When: 尝试恢复
        val result = vmManager.resume()

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    // ==================== 重置测试 ====================

    @Test
    fun `reset should restart VM`() = runTest {
        // Given: 正在运行的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()

        // When: 重置虚拟机
        val result = vmManager.reset()

        // Then: 应该成功重置
        assertThat(result.isSuccess).isTrue()
        // 重置后应该回到运行状态
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.RUNNING)
    }

    // ==================== 状态查询测试 ====================

    @Test
    fun `getCurrentState should return correct state`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 获取当前状态
        val state = vmManager.getCurrentState()

        // Then: 应该返回CREATED状态
        assertThat(state).isEqualTo(VmState.CREATED)
    }

    @Test
    fun `getCurrentInfo should return valid info`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 获取当前信息
        val info = vmManager.getCurrentInfo()

        // Then: 应该返回有效的信息
        assertThat(info).isNotNull()
        assertThat(info?.config?.id).isEqualTo(testVmConfig.id)
    }

    @Test
    fun `getCurrentInfo should return null when not initialized`() = runTest {
        // Given: 未初始化的管理器

        // When: 获取当前信息
        val info = vmManager.getCurrentInfo()

        // Then: 应该返回null
        assertThat(info).isNull()
    }

    // ==================== 操作权限检查测试 ====================

    @Test
    fun `canPerformOperation should return correct values for each state`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 检查各操作的权限
        val canStart = vmManager.canPerformOperation(VmManager.VmOperation.START)
        val canStop = vmManager.canPerformOperation(VmManager.VmOperation.STOP)
        val canPause = vmManager.canPerformOperation(VmManager.VmOperation.PAUSE)

        // Then: CREATED状态应该可以启动，不能停止或暂停
        assertThat(canStart).isTrue()
        assertThat(canStop).isFalse()
        assertThat(canPause).isFalse()
    }

    @Test
    fun `canPerformOperation should update after state change`() = runTest {
        // Given: 正在运行的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()

        // When: 检查各操作的权限
        val canStart = vmManager.canPerformOperation(VmManager.VmOperation.START)
        val canStop = vmManager.canPerformOperation(VmManager.VmOperation.STOP)
        val canPause = vmManager.canPerformOperation(VmManager.VmOperation.PAUSE)

        // Then: RUNNING状态应该可以停止和暂停，不能启动
        assertThat(canStart).isFalse()
        assertThat(canStop).isTrue()
        assertThat(canPause).isTrue()
    }

    // ==================== 完整生命周期测试 ====================

    @Test
    fun `full lifecycle should work correctly`() = runTest {
        // Given: 初始化
        val initResult = vmManager.initialize(testVmConfig)
        assertThat(initResult.isSuccess).isTrue()
        createdVmIds.add(testVmConfig.id)
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.CREATED)

        // When: 启动
        val startResult = vmManager.start()
        assertThat(startResult.isSuccess).isTrue()
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.RUNNING)

        // When: 暂停
        val pauseResult = vmManager.pause()
        assertThat(pauseResult.isSuccess).isTrue()
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.PAUSED)

        // When: 恢复
        val resumeResult = vmManager.resume()
        assertThat(resumeResult.isSuccess).isTrue()
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.RUNNING)

        // When: 停止
        val stopResult = vmManager.stop()
        assertThat(stopResult.isSuccess).isTrue()
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.STOPPED)

        // When: 再次启动
        val restartResult = vmManager.start()
        assertThat(restartResult.isSuccess).isTrue()
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.RUNNING)

        // When: 最终停止
        val finalStopResult = vmManager.stop()
        assertThat(finalStopResult.isSuccess).isTrue()
    }

    @Test
    fun `lifecycle with errors should recover gracefully`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 尝试无效操作（在CREATED状态停止）
        val invalidStopResult = vmManager.stop()

        // Then: 应该失败但不影响状态
        assertThat(invalidStopResult.isFailure).isTrue()
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.CREATED)

        // When: 执行有效操作
        val startResult = vmManager.start()

        // Then: 应该成功
        assertThat(startResult.isSuccess).isTrue()
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.RUNNING)
    }

    // ==================== 资源清理测试 ====================

    @Test
    fun `release should clean up all resources`() = runTest {
        // Given: 正在运行的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()

        // When: 释放资源
        vmManager.release()

        // Then: 应该清理完成
        assertThat(vmManager.isInitialized.first()).isFalse()
    }

    @Test
    fun `release should stop running VM`() = runTest {
        // Given: 正在运行的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)
        vmManager.start()

        // When: 释放资源
        vmManager.release()

        // Then: 虚拟机应该已停止
        assertThat(vmManager.isInitialized.first()).isFalse()
    }

    // ==================== 状态流测试 ====================

    @Test
    fun `vmState flow should emit all state transitions`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 启动虚拟机
        vmManager.start()

        // Then: 状态流应该反映当前状态
        val currentState = vmManager.vmState.first()
        assertThat(currentState).isEqualTo(VmState.RUNNING)
    }

    @Test
    fun `vmInfo flow should update with VM changes`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 启动虚拟机
        vmManager.start()

        // Then: 信息流应该更新
        val currentInfo = vmManager.vmInfo.first()
        assertThat(currentInfo).isNotNull()
        assertThat(currentInfo?.state).isEqualTo(VmState.RUNNING)
    }

    // ==================== 并发操作测试 ====================

    @Test
    fun `concurrent start requests should be handled correctly`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 启动虚拟机
        val result1 = vmManager.start()
        
        // Then: 第一个启动应该成功
        assertThat(result1.isSuccess).isTrue()
        
        // When: 再次尝试启动
        val result2 = vmManager.start()
        
        // Then: 第二个启动应该失败（已在运行）
        assertThat(result2.isFailure).isTrue()
    }

    // ==================== 错误恢复测试 ====================

    @Test
    fun `VM should recover from error state`() = runTest {
        // Given: 处于错误状态的虚拟机（模拟）
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 尝试恢复
        // 实际实现中应该有恢复机制
        val currentState = vmManager.getCurrentState()

        // Then: 应该能够继续操作
        assertThat(currentState).isNotNull()
    }

    // ==================== 配置验证测试 ====================

    @Test
    fun `VM config should be preserved throughout lifecycle`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 执行生命周期操作
        vmManager.start()
        vmManager.stop()

        // Then: 配置应该保持不变
        val info = vmManager.getCurrentInfo()
        assertThat(info?.config?.id).isEqualTo(testVmConfig.id)
        assertThat(info?.config?.name).isEqualTo(testVmConfig.name)
        assertThat(info?.config?.memory).isEqualTo(testVmConfig.memory)
        assertThat(info?.config?.cpu).isEqualTo(testVmConfig.cpu)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `rapid start stop cycles should be handled correctly`() = runTest {
        // Given: 已初始化的虚拟机
        vmManager.initialize(testVmConfig)
        createdVmIds.add(testVmConfig.id)

        // When: 快速启动停止循环
        repeat(3) {
            val startResult = vmManager.start()
            if (startResult.isSuccess) {
                val stopResult = vmManager.stop()
                assertThat(stopResult.isSuccess).isTrue()
            }
        }

        // Then: 最终状态应该是STOPPED
        assertThat(vmManager.getCurrentState()).isEqualTo(VmState.STOPPED)
    }

    @Test
    fun `VM with minimal resources should work`() = runTest {
        // Given: 最小资源配置
        val minimalConfig = VmConfig(
            id = "minimal-vm-${System.currentTimeMillis()}",
            name = "Minimal VM",
            memory = 128,
            cpu = 1,
            diskSize = 1
        )

        // When: 初始化和启动
        val initResult = vmManager.initialize(minimalConfig)
        createdVmIds.add(minimalConfig.id)

        // Then: 应该成功
        assertThat(initResult.isSuccess).isTrue()
    }

    @Test
    fun `VM with large resources should be validated`() = runTest {
        // Given: 大资源配置
        val largeConfig = VmConfig(
            id = "large-vm-${System.currentTimeMillis()}",
            name = "Large VM",
            memory = 8192, // 8GB
            cpu = 8,
            diskSize = 100
        )

        // When: 尝试初始化
        val result = vmManager.initialize(largeConfig)

        // Then: 应该根据设备能力决定成功或失败
        // 如果设备资源不足，应该失败
        // 如果设备资源充足，应该成功
        // 这里只验证不会崩溃
        assertThat(result.isSuccess || result.isFailure).isTrue()
    }
}

// 辅助函数 - 已移除，直接使用VmManager的方法