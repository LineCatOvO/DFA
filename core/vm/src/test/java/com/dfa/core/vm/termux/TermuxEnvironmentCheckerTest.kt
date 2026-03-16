package com.dfa.core.vm.termux

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TermuxEnvironmentChecker单元测试
 *
 * 测试EnvironmentStatus枚举、EnvironmentCheckResult数据类和基本检测逻辑
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TermuxEnvironmentCheckerTest {

    private lateinit var mockContext: android.content.Context
    private lateinit var mockPackageManager: android.content.pm.PackageManager
    private lateinit var mockTermuxBridge: TermuxBridge

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        mockTermuxBridge = mockk(relaxed = true)

        every { mockContext.packageManager } returns mockPackageManager
    }

    // ==================== EnvironmentStatus枚举测试 ====================

    @Test
    fun `EnvironmentStatus should have all expected values`() {
        val expectedValues = listOf(
            EnvironmentStatus.UNKNOWN,
            EnvironmentStatus.NOT_INSTALLED,
            EnvironmentStatus.INSTALLED,
            EnvironmentStatus.AVAILABLE,
            EnvironmentStatus.ERROR
        )

        assertEquals(expectedValues.size, EnvironmentStatus.entries.size)
        expectedValues.forEach { status ->
            assertTrue("Expected status $status to exist", EnvironmentStatus.entries.contains(status))
        }
    }

    @Test
    fun `EnvironmentStatus UNKNOWN should be first`() {
        assertEquals(0, EnvironmentStatus.UNKNOWN.ordinal)
    }

    @Test
    fun `EnvironmentStatus ERROR should be last`() {
        assertEquals(4, EnvironmentStatus.ERROR.ordinal)
    }

    // ==================== EnvironmentCheckResult测试 ====================

    @Test
    fun `EnvironmentCheckResult unknown should create correct result`() {
        val result = EnvironmentCheckResult.unknown()

        assertEquals(EnvironmentStatus.UNKNOWN, result.status)
        assertEquals("Unknown status", result.message)
        assertTrue(result.details.isEmpty())
    }

    @Test
    fun `EnvironmentCheckResult unknown with custom message should use that message`() {
        val result = EnvironmentCheckResult.unknown("Custom unknown message")

        assertEquals(EnvironmentStatus.UNKNOWN, result.status)
        assertEquals("Custom unknown message", result.message)
    }

    @Test
    fun `EnvironmentCheckResult notInstalled should create correct result`() {
        val result = EnvironmentCheckResult.notInstalled("TestComponent")

        assertEquals(EnvironmentStatus.NOT_INSTALLED, result.status)
        assertEquals("TestComponent is not installed", result.message)
        assertTrue(result.details.isEmpty())
    }

    @Test
    fun `EnvironmentCheckResult installed should create correct result`() {
        val result = EnvironmentCheckResult.installed("TestComponent", "1.0.0")

        assertEquals(EnvironmentStatus.INSTALLED, result.status)
        assertEquals("TestComponent is installed", result.message)
        assertEquals("1.0.0", result.details["version"])
    }

    @Test
    fun `EnvironmentCheckResult installed without version should have empty details`() {
        val result = EnvironmentCheckResult.installed("TestComponent", null)

        assertEquals(EnvironmentStatus.INSTALLED, result.status)
        assertTrue(result.details.isEmpty())
    }

    @Test
    fun `EnvironmentCheckResult available should create correct result`() {
        val details = mapOf("key1" to "value1", "key2" to "value2")
        val result = EnvironmentCheckResult.available("TestComponent", details)

        assertEquals(EnvironmentStatus.AVAILABLE, result.status)
        assertEquals("TestComponent is available and running", result.message)
        assertEquals(details, result.details)
    }

    @Test
    fun `EnvironmentCheckResult error should create correct result`() {
        val errorDetails = mapOf("error" to "NullPointerException")
        val result = EnvironmentCheckResult.error("Something went wrong", errorDetails)

        assertEquals(EnvironmentStatus.ERROR, result.status)
        assertEquals("Something went wrong", result.message)
        assertEquals(errorDetails, result.details)
    }

    // ==================== EnvironmentCheckResult属性测试 ====================

    @Test
    fun `isAvailable should return true for AVAILABLE status`() {
        val result = EnvironmentCheckResult.available("Test")
        assertTrue(result.isAvailable)
    }

    @Test
    fun `isAvailable should return true for INSTALLED status`() {
        val result = EnvironmentCheckResult.installed("Test")
        assertTrue(result.isAvailable)
    }

    @Test
    fun `isAvailable should return false for NOT_INSTALLED status`() {
        val result = EnvironmentCheckResult.notInstalled("Test")
        assertFalse(result.isAvailable)
    }

    @Test
    fun `isAvailable should return false for UNKNOWN status`() {
        val result = EnvironmentCheckResult.unknown()
        assertFalse(result.isAvailable)
    }

    @Test
    fun `isAvailable should return false for ERROR status`() {
        val result = EnvironmentCheckResult.error("Error")
        assertFalse(result.isAvailable)
    }

    @Test
    fun `isInstalled should return true for INSTALLED status`() {
        val result = EnvironmentCheckResult.installed("Test")
        assertTrue(result.isInstalled)
    }

    @Test
    fun `isInstalled should return true for AVAILABLE status`() {
        val result = EnvironmentCheckResult.available("Test")
        assertTrue(result.isInstalled)
    }

    @Test
    fun `isInstalled should return false for NOT_INSTALLED status`() {
        val result = EnvironmentCheckResult.notInstalled("Test")
        assertFalse(result.isInstalled)
    }

    @Test
    fun `hasError should return true for ERROR status`() {
        val result = EnvironmentCheckResult.error("Error")
        assertTrue(result.hasError)
    }

    @Test
    fun `hasError should return false for other statuses`() {
        assertFalse(EnvironmentCheckResult.unknown().hasError)
        assertFalse(EnvironmentCheckResult.notInstalled("Test").hasError)
        assertFalse(EnvironmentCheckResult.installed("Test").hasError)
        assertFalse(EnvironmentCheckResult.available("Test").hasError)
    }

    // ==================== QemuCheckResult测试 ====================

    @Test
    fun `QemuCheckResult isFullyAvailable should return true when all components available`() {
        val result = QemuCheckResult(
            systemX86_64 = EnvironmentCheckResult.available("qemu-system-x86_64"),
            img = EnvironmentCheckResult.available("qemu-img"),
            canExecute = true
        )

        assertTrue(result.isFullyAvailable)
    }

    @Test
    fun `QemuCheckResult isFullyAvailable should return false when systemX86_64 not available`() {
        val result = QemuCheckResult(
            systemX86_64 = EnvironmentCheckResult.notInstalled("qemu-system-x86_64"),
            img = EnvironmentCheckResult.available("qemu-img"),
            canExecute = true
        )

        assertFalse(result.isFullyAvailable)
    }

    @Test
    fun `QemuCheckResult isFullyAvailable should return false when img not available`() {
        val result = QemuCheckResult(
            systemX86_64 = EnvironmentCheckResult.available("qemu-system-x86_64"),
            img = EnvironmentCheckResult.notInstalled("qemu-img"),
            canExecute = true
        )

        assertFalse(result.isFullyAvailable)
    }

    @Test
    fun `QemuCheckResult isFullyAvailable should return false when canExecute is false`() {
        val result = QemuCheckResult(
            systemX86_64 = EnvironmentCheckResult.available("qemu-system-x86_64"),
            img = EnvironmentCheckResult.available("qemu-img"),
            canExecute = false
        )

        assertFalse(result.isFullyAvailable)
    }

    // ==================== FullEnvironmentCheckResult测试 ====================

    @Test
    fun `FullEnvironmentCheckResult isFullyAvailable should return true when all available`() {
        val result = FullEnvironmentCheckResult(
            termux = EnvironmentCheckResult.available("Termux"),
            qemu = QemuCheckResult(
                systemX86_64 = EnvironmentCheckResult.available("qemu-system-x86_64"),
                img = EnvironmentCheckResult.available("qemu-img"),
                canExecute = true
            ),
            ssh = EnvironmentCheckResult.available("SSH")
        )

        assertTrue(result.isFullyAvailable)
    }

    @Test
    fun `FullEnvironmentCheckResult isFullyAvailable should return false when termux not available`() {
        val result = FullEnvironmentCheckResult(
            termux = EnvironmentCheckResult.notInstalled("Termux"),
            qemu = QemuCheckResult(
                systemX86_64 = EnvironmentCheckResult.available("qemu-system-x86_64"),
                img = EnvironmentCheckResult.available("qemu-img"),
                canExecute = true
            ),
            ssh = EnvironmentCheckResult.available("SSH")
        )

        assertFalse(result.isFullyAvailable)
    }

    @Test
    fun `FullEnvironmentCheckResult isFullyAvailable should return false when ssh not available`() {
        val result = FullEnvironmentCheckResult(
            termux = EnvironmentCheckResult.available("Termux"),
            qemu = QemuCheckResult(
                systemX86_64 = EnvironmentCheckResult.available("qemu-system-x86_64"),
                img = EnvironmentCheckResult.available("qemu-img"),
                canExecute = true
            ),
            ssh = EnvironmentCheckResult.notInstalled("SSH")
        )

        assertFalse(result.isFullyAvailable)
    }

    @Test
    fun `FullEnvironmentCheckResult unavailableMessages should list all unavailable components`() {
        val result = FullEnvironmentCheckResult(
            termux = EnvironmentCheckResult.notInstalled("Termux"),
            qemu = QemuCheckResult(
                systemX86_64 = EnvironmentCheckResult.notInstalled("qemu-system-x86_64"),
                img = EnvironmentCheckResult.notInstalled("qemu-img"),
                canExecute = false
            ),
            ssh = EnvironmentCheckResult.notInstalled("SSH")
        )

        val messages = result.unavailableMessages

        assertEquals(3, messages.size)
        assertTrue(messages.any { it.contains("Termux") })
        assertTrue(messages.any { it.contains("QEMU") })
        assertTrue(messages.any { it.contains("SSH") })
    }

    @Test
    fun `FullEnvironmentCheckResult unavailableMessages should be empty when all available`() {
        val result = FullEnvironmentCheckResult(
            termux = EnvironmentCheckResult.available("Termux"),
            qemu = QemuCheckResult(
                systemX86_64 = EnvironmentCheckResult.available("qemu-system-x86_64"),
                img = EnvironmentCheckResult.available("qemu-img"),
                canExecute = true
            ),
            ssh = EnvironmentCheckResult.available("SSH")
        )

        assertTrue(result.unavailableMessages.isEmpty())
    }

    @Test
    fun `FullEnvironmentCheckResult should have timestamp`() {
        val beforeTime = System.currentTimeMillis()
        val result = FullEnvironmentCheckResult(
            termux = EnvironmentCheckResult.available("Termux"),
            qemu = QemuCheckResult(
                systemX86_64 = EnvironmentCheckResult.available("qemu-system-x86_64"),
                img = EnvironmentCheckResult.available("qemu-img"),
                canExecute = true
            ),
            ssh = EnvironmentCheckResult.available("SSH")
        )
        val afterTime = System.currentTimeMillis()

        assertTrue(result.timestamp >= beforeTime)
        assertTrue(result.timestamp <= afterTime)
    }

    // ==================== TermuxConstants测试 ====================

    @Test
    fun `TermuxConstants should have correct QEMU package names`() {
        assertEquals("qemu-system-x86-64", TermuxConstants.QEMU_SYSTEM_X86_64_PACKAGE)
        assertEquals("qemu-system-i386", TermuxConstants.QEMU_SYSTEM_I386_PACKAGE)
        assertEquals("qemu-system-arm", TermuxConstants.QEMU_SYSTEM_ARM_PACKAGE)
        assertEquals("qemu-system-aarch64", TermuxConstants.QEMU_SYSTEM_AARCH64_PACKAGE)
        assertEquals("qemu-img", TermuxConstants.QEMU_IMG_PACKAGE)
    }

    @Test
    fun `TermuxConstants should have correct QEMU commands`() {
        assertEquals("qemu-system-x86_64", TermuxConstants.QEMU_SYSTEM_X86_64_COMMAND)
        assertEquals("qemu-system-i386", TermuxConstants.QEMU_SYSTEM_I386_COMMAND)
        assertEquals("qemu-system-arm", TermuxConstants.QEMU_SYSTEM_ARM_COMMAND)
        assertEquals("qemu-system-aarch64", TermuxConstants.QEMU_SYSTEM_AARCH64_COMMAND)
        assertEquals("qemu-img", TermuxConstants.QEMU_IMG_COMMAND)
    }

    @Test
    fun `TermuxConstants should have correct SSH constants`() {
        assertEquals("openssh", TermuxConstants.OPENSSH_PACKAGE)
        assertEquals("sshd", TermuxConstants.SSHD_COMMAND)
        assertEquals("ssh", TermuxConstants.SSH_COMMAND)
        assertEquals("scp", TermuxConstants.SCP_COMMAND)
        assertEquals("ssh-keygen", TermuxConstants.SSH_KEYGEN_COMMAND)
        assertEquals(8022, TermuxConstants.SSH_DEFAULT_PORT)
    }

    @Test
    fun `TermuxConstants should have correct timeout values`() {
        assertEquals(30_000L, TermuxConstants.DEFAULT_COMMAND_TIMEOUT_MS)
        assertEquals(300_000L, TermuxConstants.LONG_COMMAND_TIMEOUT_MS)
        assertEquals(120_000L, TermuxConstants.PACKAGE_INSTALL_TIMEOUT_MS)
        assertEquals(60_000L, TermuxConstants.FILE_OPERATION_TIMEOUT_MS)
        assertEquals(10_000L, TermuxConstants.ENVIRONMENT_CHECK_TIMEOUT_MS)
        assertEquals(30_000L, TermuxConstants.QEMU_VALIDATION_TIMEOUT_MS)
        assertEquals(5_000L, TermuxConstants.SSH_CHECK_TIMEOUT_MS)
    }

    @Test
    fun `TermuxConstants should have correct SSH paths`() {
        assertTrue(TermuxConstants.SSH_CONFIG_PATH.contains("sshd_config"))
        assertTrue(TermuxConstants.SSH_HOST_KEYS_DIR.contains("ssh"))
        assertTrue(TermuxConstants.SSH_USER_KEYS_DIR.contains(".ssh"))
    }

    // ==================== 数据类复制测试 ====================

    @Test
    fun `EnvironmentCheckResult copy should work correctly`() {
        val original = EnvironmentCheckResult.available("Test", mapOf("key" to "value"))
        val copied = original.copy(message = "Modified message")

        assertEquals(EnvironmentStatus.AVAILABLE, copied.status)
        assertEquals("Modified message", copied.message)
        assertEquals(mapOf("key" to "value"), copied.details)
    }

    @Test
    fun `QemuCheckResult copy should work correctly`() {
        val original = QemuCheckResult(
            systemX86_64 = EnvironmentCheckResult.available("qemu"),
            img = EnvironmentCheckResult.available("qemu-img"),
            canExecute = true
        )
        val copied = original.copy(canExecute = false)

        assertTrue(copied.systemX86_64.isAvailable)
        assertTrue(copied.img.isAvailable)
        assertFalse(copied.canExecute)
    }

    @Test
    fun `FullEnvironmentCheckResult copy should work correctly`() {
        val original = FullEnvironmentCheckResult(
            termux = EnvironmentCheckResult.available("Termux"),
            qemu = QemuCheckResult(
                systemX86_64 = EnvironmentCheckResult.available("qemu"),
                img = EnvironmentCheckResult.available("qemu-img"),
                canExecute = true
            ),
            ssh = EnvironmentCheckResult.available("SSH")
        )
        val copied = original.copy(
            termux = EnvironmentCheckResult.notInstalled("Termux")
        )

        assertFalse(copied.termux.isAvailable)
        assertTrue(copied.qemu.isFullyAvailable)
        assertTrue(copied.ssh.isAvailable)
    }
}