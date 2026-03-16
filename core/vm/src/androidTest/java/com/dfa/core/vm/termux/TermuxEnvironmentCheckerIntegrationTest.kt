package com.dfa.core.vm.termux

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TermuxEnvironmentChecker集成测试
 *
 * 测试Termux应用安装检测、QEMU包安装检测、QEMU命令执行验证、SSH服务运行检测和完整环境检测流程
 *
 * 注意：这些测试需要在真实的Android设备或模拟器上运行，且需要Termux环境
 */
@RunWith(AndroidJUnit4::class)
class TermuxEnvironmentCheckerIntegrationTest {

    private lateinit var environmentChecker: TermuxEnvironmentChecker
    private lateinit var context: Context
    private lateinit var termuxBridge: TermuxBridge

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // 手动创建实例，不使用Hilt注入
        termuxBridge = TermuxBridgeImpl(TermuxConfig.DEFAULT)
        environmentChecker = TermuxEnvironmentCheckerImpl(context, termuxBridge)
    }

    // ==================== Termux应用安装检测测试 ====================

    @Test
    fun testCheckTermuxInstallation_returnsValidResult(): Unit = runBlocking {
        val result = environmentChecker.checkTermuxInstallation()

        assertNotNull(result)
        assertNotNull(result.status)
        assertNotNull(result.message)
    }

    @Test
    fun testCheckTermuxInstallation_updatesTermuxStatus(): Unit = runBlocking {
        environmentChecker.resetStatus()

        // 初始状态应该是UNKNOWN
        assertEquals(EnvironmentStatus.UNKNOWN, environmentChecker.termuxStatus.value.status)

        // 执行检测
        environmentChecker.checkTermuxInstallation()

        // 状态应该被更新
        val status = environmentChecker.termuxStatus.value.status
        assertTrue(
            "Status should be one of the valid values",
            status in listOf(
                EnvironmentStatus.UNKNOWN,
                EnvironmentStatus.NOT_INSTALLED,
                EnvironmentStatus.INSTALLED,
                EnvironmentStatus.AVAILABLE,
                EnvironmentStatus.ERROR
            )
        )
    }

    @Test
    fun testCheckTermuxInstallation_resultHasConsistentState(): Unit = runBlocking {
        val result = environmentChecker.checkTermuxInstallation()

        // 如果状态是AVAILABLE或INSTALLED，isAvailable应该是true
        if (result.status == EnvironmentStatus.AVAILABLE || result.status == EnvironmentStatus.INSTALLED) {
            assertTrue("Result should be available when status is ${result.status}", result.isAvailable)
        }

        // 如果状态是NOT_INSTALLED，isAvailable应该是false
        if (result.status == EnvironmentStatus.NOT_INSTALLED) {
            assertFalse("Result should not be available when status is NOT_INSTALLED", result.isAvailable)
        }

        // 如果状态是ERROR，hasError应该是true
        if (result.status == EnvironmentStatus.ERROR) {
            assertTrue("Result should have error when status is ERROR", result.hasError)
        }
    }

    // ==================== QEMU包安装检测测试 ====================

    @Test
    fun testCheckQemuInstallation_returnsValidResult(): Unit = runBlocking {
        val result = environmentChecker.checkQemuInstallation()

        assertNotNull(result)
        assertNotNull(result.systemX86_64)
        assertNotNull(result.img)
    }

    @Test
    fun testCheckQemuInstallation_updatesQemuStatus(): Unit = runBlocking {
        environmentChecker.resetStatus()

        // 执行检测
        environmentChecker.checkQemuInstallation()

        // 状态应该被更新
        val qemuStatus = environmentChecker.qemuStatus.value
        assertNotNull(qemuStatus)
        assertNotNull(qemuStatus.systemX86_64)
        assertNotNull(qemuStatus.img)
    }

    @Test
    fun testCheckQemuInstallation_componentsHaveConsistentState(): Unit = runBlocking {
        val result = environmentChecker.checkQemuInstallation()

        // 检查systemX86_64组件
        assertNotNull("systemX86_64 should not be null", result.systemX86_64)
        assertNotNull("systemX86_64 status should not be null", result.systemX86_64.status)
        assertNotNull("systemX86_64 message should not be null", result.systemX86_64.message)

        // 检查img组件
        assertNotNull("img should not be null", result.img)
        assertNotNull("img status should not be null", result.img.status)
        assertNotNull("img message should not be null", result.img.message)
    }

    @Test
    fun testCheckQemuInstallation_isFullyAvailableConsistency(): Unit = runBlocking {
        val result = environmentChecker.checkQemuInstallation()

        // isFullyAvailable应该与各组件状态一致
        if (result.isFullyAvailable) {
            assertTrue("systemX86_64 should be available", result.systemX86_64.isAvailable)
            assertTrue("img should be available", result.img.isAvailable)
            assertTrue("canExecute should be true", result.canExecute)
        }
    }

    // ==================== QEMU命令执行验证测试 ====================

    @Test
    fun testCheckQemuExecution_returnsBoolean(): Unit = runBlocking {
        val result = environmentChecker.checkQemuExecution()

        // 结果应该是布尔值
        assertNotNull("Result should not be null", result)
        // Kotlin的Boolean是原始类型，不需要检查null
    }

    @Test
    fun testCheckQemuExecution_requiresQemuInstalled(): Unit = runBlocking {
        // 先检查QEMU是否安装
        val qemuResult = environmentChecker.checkQemuInstallation()

        // 如果QEMU没有安装，执行验证应该返回false
        if (!qemuResult.systemX86_64.isAvailable || !qemuResult.img.isAvailable) {
            val canExecute = environmentChecker.checkQemuExecution()
            assertFalse("QEMU execution should fail when QEMU is not installed", canExecute)
        }
    }

    // ==================== SSH服务运行检测测试 ====================

    @Test
    fun testCheckSshService_returnsValidResult(): Unit = runBlocking {
        val result = environmentChecker.checkSshService()

        assertNotNull(result)
        assertNotNull(result.status)
        assertNotNull(result.message)
    }

    @Test
    fun testCheckSshService_updatesSshStatus(): Unit = runBlocking {
        environmentChecker.resetStatus()

        // 初始状态应该是UNKNOWN
        assertEquals(EnvironmentStatus.UNKNOWN, environmentChecker.sshStatus.value.status)

        // 执行检测
        environmentChecker.checkSshService()

        // 状态应该被更新
        val status = environmentChecker.sshStatus.value.status
        assertTrue(
            "Status should be one of the valid values",
            status in listOf(
                EnvironmentStatus.UNKNOWN,
                EnvironmentStatus.NOT_INSTALLED,
                EnvironmentStatus.INSTALLED,
                EnvironmentStatus.AVAILABLE,
                EnvironmentStatus.ERROR
            )
        )
    }

    @Test
    fun testCheckSshService_resultHasConsistentState(): Unit = runBlocking {
        val result = environmentChecker.checkSshService()

        // 如果状态是AVAILABLE，应该有详细信息
        if (result.status == EnvironmentStatus.AVAILABLE) {
            assertTrue("Result should have details when available", result.details.isNotEmpty())
            assertTrue("Details should contain port info", result.details.containsKey("port"))
        }
    }

    // ==================== 完整环境检测流程测试 ====================

    @Test
    fun testPerformFullCheck_returnsValidResult(): Unit = runBlocking {
        val result = environmentChecker.performFullCheck()

        assertNotNull(result)
        assertNotNull(result.termux)
        assertNotNull(result.qemu)
        assertNotNull(result.ssh)
        assertNotNull(result.timestamp)
    }

    @Test
    fun testPerformFullCheck_updatesAllStatuses(): Unit = runBlocking {
        environmentChecker.resetStatus()

        // 执行完整检测
        val result = environmentChecker.performFullCheck()

        // 验证所有状态都被更新
        assertEquals(result.termux, environmentChecker.termuxStatus.value)
        assertEquals(result.qemu, environmentChecker.qemuStatus.value)
        assertEquals(result.ssh, environmentChecker.sshStatus.value)
        assertEquals(result, environmentChecker.fullCheckStatus.value)
    }

    @Test
    fun testPerformFullCheck_setsIsCheckingFlag(): Unit = runBlocking {
        // isChecking应该在检测过程中为true，检测完成后为false
        assertFalse("isChecking should be false before check", environmentChecker.isChecking.value)

        environmentChecker.performFullCheck()

        assertFalse("isChecking should be false after check", environmentChecker.isChecking.value)
    }

    @Test
    fun testPerformFullCheck_skipsQemuAndSshWhenTermuxNotAvailable(): Unit = runBlocking {
        val result = environmentChecker.performFullCheck()

        // 如果Termux不可用，QEMU和SSH应该是UNKNOWN状态
        if (!result.termux.isAvailable) {
            assertEquals(
                "QEMU should be UNKNOWN when Termux not available",
                EnvironmentStatus.UNKNOWN,
                result.qemu.systemX86_64.status
            )
            assertEquals(
                "SSH should be UNKNOWN when Termux not available",
                EnvironmentStatus.UNKNOWN,
                result.ssh.status
            )
        }
    }

    @Test
    fun testPerformFullCheck_isFullyAvailableConsistency(): Unit = runBlocking {
        val result = environmentChecker.performFullCheck()

        // isFullyAvailable应该与各组件状态一致
        if (result.isFullyAvailable) {
            assertTrue("Termux should be available", result.termux.isAvailable)
            assertTrue("QEMU should be fully available", result.qemu.isFullyAvailable)
            assertTrue("SSH should be available", result.ssh.isAvailable)
        }
    }

    @Test
    fun testPerformFullCheck_unavailableMessagesConsistency(): Unit = runBlocking {
        val result = environmentChecker.performFullCheck()

        // 如果不是完全可用，应该有不可用消息
        if (!result.isFullyAvailable) {
            assertTrue("Should have unavailable messages when not fully available", result.unavailableMessages.isNotEmpty())
        } else {
            assertTrue("Should have no unavailable messages when fully available", result.unavailableMessages.isEmpty())
        }
    }

    // ==================== 状态重置测试 ====================

    @Test
    fun testResetStatus_resetsAllStates(): Unit = runBlocking {
        // 先执行检测
        environmentChecker.performFullCheck()

        // 重置状态
        environmentChecker.resetStatus()

        // 验证所有状态都被重置
        assertEquals(EnvironmentStatus.UNKNOWN, environmentChecker.termuxStatus.value.status)
        assertEquals(EnvironmentStatus.UNKNOWN, environmentChecker.qemuStatus.value.systemX86_64.status)
        assertEquals(EnvironmentStatus.UNKNOWN, environmentChecker.sshStatus.value.status)
    }

    // ==================== 环境摘要测试 ====================

    @Test
    fun testGetEnvironmentSummary_returnsValidString(): Unit = runBlocking {
        // 未检测时应该返回默认消息
        environmentChecker.resetStatus()
        val summaryBefore = environmentChecker.getEnvironmentSummary()
        assertTrue("Summary should indicate not checked", summaryBefore.contains("not checked", ignoreCase = true))

        // 检测后应该返回详细摘要
        environmentChecker.performFullCheck()
        val summaryAfter = environmentChecker.getEnvironmentSummary()

        assertTrue("Summary should contain Termux", summaryAfter.contains("Termux", ignoreCase = true))
        assertTrue("Summary should contain QEMU", summaryAfter.contains("QEMU", ignoreCase = true))
        assertTrue("Summary should contain SSH", summaryAfter.contains("SSH", ignoreCase = true))
    }

    @Test
    fun testGetEnvironmentSummary_showsCorrectOverallStatus(): Unit = runBlocking {
        environmentChecker.performFullCheck()
        val result = environmentChecker.fullCheckStatus.value
        val summary = environmentChecker.getEnvironmentSummary()

        if (result != null && result.isFullyAvailable) {
            assertTrue("Summary should show READY when fully available", summary.contains("READY"))
        } else if (result != null) {
            assertTrue("Summary should show NOT READY when not fully available", summary.contains("NOT READY"))
        }
    }

    // ==================== StateFlow测试 ====================

    @Test
    fun testStateFlows_areInitialized(): Unit = runBlocking {
        assertNotNull("termuxStatus should be initialized", environmentChecker.termuxStatus)
        assertNotNull("qemuStatus should be initialized", environmentChecker.qemuStatus)
        assertNotNull("sshStatus should be initialized", environmentChecker.sshStatus)
        assertNotNull("fullCheckStatus should be initialized", environmentChecker.fullCheckStatus)
        assertNotNull("isChecking should be initialized", environmentChecker.isChecking)
    }

    @Test
    fun testStateFlows_emitCorrectValues(): Unit = runBlocking {
        environmentChecker.resetStatus()

        // 验证初始值
        assertEquals(EnvironmentStatus.UNKNOWN, environmentChecker.termuxStatus.value.status)
        assertFalse(environmentChecker.isChecking.value)

        // 执行检测
        environmentChecker.performFullCheck()

        // 验证状态已更新
        assertNotNull(environmentChecker.fullCheckStatus.value)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun testMultipleChecks_dontInterfere(): Unit = runBlocking {
        // 执行多次检测
        val result1 = environmentChecker.performFullCheck()
        val result2 = environmentChecker.performFullCheck()
        val result3 = environmentChecker.performFullCheck()

        // 所有结果应该一致（假设环境没有变化）
        assertEquals(result1.termux.status, result2.termux.status)
        assertEquals(result2.termux.status, result3.termux.status)
    }

    @Test
    fun testCheckAfterReset_worksCorrectly(): Unit = runBlocking {
        // 执行检测
        environmentChecker.performFullCheck()

        // 重置
        environmentChecker.resetStatus()

        // 再次检测
        val result = environmentChecker.performFullCheck()

        // 应该正常工作
        assertNotNull(result)
        assertNotNull(result.termux)
        assertNotNull(result.qemu)
        assertNotNull(result.ssh)
    }
}