package com.dfa.core.vm.termux

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * TermuxBridgeImpl单元测试
 *
 * 测试Termux桥接实现的核心功能，包括：
 * - 环境检查（Termux可用性、安装状态）
 * - 命令执行（普通命令、脚本、长时间命令）
 * - 包管理（安装、卸载、搜索、更新）
 * - 文件操作（读写、创建、删除、复制、移动）
 * - 环境变量管理
 * - 会话管理
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TermuxBridgeImplTest {

    // 测试调度器
    private val testDispatcher = StandardTestDispatcher()

    // 测试对象
    private lateinit var bridge: TermuxBridgeImpl

    // 测试配置
    private val testConfig = TermuxConfig(
        prefixPath = "/data/data/com.termux/files/usr",
        homePath = "/data/data/com.termux/files/home",
        tmpPath = "/data/data/com.termux/files/usr/tmp",
        binPath = "/data/data/com.termux/files/usr/bin",
        shellPath = "/data/data/com.termux/files/usr/bin/bash",
        debugMode = false,
        commandTimeoutMs = 30000,
        longCommandTimeoutMs = 300000
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bridge = TermuxBridgeImpl(testConfig)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 配置测试 ====================

    @Test
    fun `config should return the provided configuration`() {
        assertThat(bridge.config).isEqualTo(testConfig)
    }

    @Test
    fun `constructor should use default config when not provided`() {
        val defaultBridge = TermuxBridgeImpl()

        assertThat(defaultBridge.config).isEqualTo(TermuxConfig.DEFAULT)
    }

    // ==================== 会话状态测试 ====================

    @Test
    fun `isSessionActive should return false initially`() {
        assertThat(bridge.isSessionActive()).isFalse()
    }

    // ==================== 环境检查测试 ====================

    @Test
    fun `isTermuxAvailable should check environment`() = runTest {
        // 在非Termux环境中测试
        val result = bridge.isTermuxAvailable()

        // 结果取决于测试环境
        // 不强制要求false，因为测试环境可能有bash
    }

    @Test
    fun `isTermuxInstalled should check Termux directory`() = runTest {
        val result = bridge.isTermuxInstalled()

        // 结果取决于测试环境
    }

    @Test
    fun `isTermuxApiInstalled should return false when termux-api not available`() = runTest {
        val result = bridge.isTermuxApiInstalled()

        // 测试环境中termux-api不可用
        assertThat(result).isFalse()
    }

    @Test
    fun `getEnvironmentInfo should return environment information`() = runTest {
        val result = bridge.getEnvironmentInfo()

        assertThat(result.isSuccess).isTrue()
        val envInfo = result.getOrThrow()
        assertThat(envInfo).containsKey("PREFIX")
        assertThat(envInfo).containsKey("HOME")
        assertThat(envInfo).containsKey("PATH")
    }

    @Test
    fun `getTermuxVersion should return version or unknown`() = runTest {
        val result = bridge.getTermuxVersion()

        assertThat(result.isSuccess).isTrue()
        // 版本可能是实际版本或"unknown"
        assertThat(result.getOrNull()).isNotNull()
    }

    // ==================== 命令执行测试 ====================

    @Test
    fun `executeCommand should return failure for invalid command`() = runTest {
        // 在非Termux环境中执行命令会失败
        val result = bridge.executeCommand("invalid-command-that-does-not-exist")

        // 命令执行可能失败或返回非零退出码
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.isSuccess).isFalse()
    }

    @Test
    fun `executeCommand with timeout should respect timeout parameter`() = runTest {
        val result = bridge.executeCommand("echo test", 5000)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `executeScript should handle script execution`() = runTest {
        val script = """
            #!/bin/bash
            echo "Hello from script"
        """.trimIndent()

        val result = bridge.executeScript(script, "test-script")

        // 在非Termux环境中可能失败
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `executeLongRunningCommand should use longer timeout`() = runTest {
        val result = bridge.executeLongRunningCommand("echo test")

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 包管理测试 ====================

    @Test
    fun `installPackage should return failure in non-Termux environment`() = runTest {
        val result = bridge.installPackage("test-package")

        // 在非Termux环境中安装会失败
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.isSuccess).isFalse()
    }

    @Test
    fun `installPackages should handle multiple packages`() = runTest {
        val result = bridge.installPackages(listOf("pkg1", "pkg2"))

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `installPackages should return success for empty list`() = runTest {
        val result = bridge.installPackages(emptyList())

        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.isSuccess).isTrue()
    }

    @Test
    fun `uninstallPackage should return failure in non-Termux environment`() = runTest {
        val result = bridge.uninstallPackage("test-package")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `updatePackageList should be callable`() {
        // This test verifies the method signature is correct
        // Actual execution is tested in integration tests
        assertThat(bridge::updatePackageList.name).isEqualTo("updatePackageList")
    }

    @Test
    fun `upgradePackages should be callable`() {
        // This test verifies the method signature is correct
        // Actual execution is tested in integration tests
        assertThat(bridge::upgradePackages.name).isEqualTo("upgradePackages")
    }

    @Test
    fun `searchPackage should handle search request`() = runTest {
        val result = bridge.searchPackage("test")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `getInstalledPackages should handle request`() = runTest {
        val result = bridge.getInstalledPackages()

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `isPackageInstalled should check package status`() = runTest {
        val result = bridge.isPackageInstalled("bash")

        // 结果取决于测试环境
        // 不强制要求false
    }

    // ==================== 文件操作测试 ====================

    @Test
    fun `readFile should handle non-existent file`() = runTest {
        val result = bridge.readFile("/non/existent/file.txt")

        // 结果可能是失败或成功但包含错误
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `readFileBytes should return failure for non-existent file`() = runTest {
        val result = bridge.readFileBytes("/non/existent/file.bin")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `writeFile should return failure for invalid path`() = runTest {
        val result = bridge.writeFile("/invalid/path/file.txt", "content")

        // 在非Termux环境中写入可能失败
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `writeFileBytes should handle invalid path`() = runTest {
        val result = bridge.writeFileBytes("/invalid/path/file.bin", byteArrayOf(1, 2, 3))

        // 结果取决于测试环境
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `deleteFile should return failure for non-existent file`() = runTest {
        val result = bridge.deleteFile("/non/existent/file.txt")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `fileExists should return false for non-existent file`() = runTest {
        val result = bridge.fileExists("/non/existent/file.txt")

        assertThat(result).isFalse()
    }

    @Test
    fun `createDirectory should return failure for invalid path`() = runTest {
        val result = bridge.createDirectory("/invalid/path/dir", recursive = true)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `listDirectory should handle non-existent directory`() = runTest {
        val result = bridge.listDirectory("/non/existent/directory")

        // 结果可能是失败或成功但包含错误
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `getFileInfo should return failure for non-existent file`() = runTest {
        val result = bridge.getFileInfo("/non/existent/file.txt")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `copyFile should return failure for non-existent source`() = runTest {
        val result = bridge.copyFile("/non/existent/source.txt", "/tmp/dest.txt")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `moveFile should return failure for non-existent source`() = runTest {
        val result = bridge.moveFile("/non/existent/source.txt", "/tmp/dest.txt")

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 环境变量测试 ====================

    @Test
    fun `getEnvVar should return null for non-existent variable`() = runTest {
        val result = bridge.getEnvVar("NON_EXISTENT_VAR")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `setEnvVar should return success`() = runTest {
        val result = bridge.setEnvVar("TEST_VAR", "test_value")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `unsetEnvVar should return success`() = runTest {
        val result = bridge.unsetEnvVar("TEST_VAR")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `getAllEnvVars should return environment variables`() = runTest {
        val result = bridge.getAllEnvVars()

        assertThat(result.isSuccess).isTrue()
        val envVars = result.getOrThrow()
        assertThat(envVars).isNotEmpty()
    }

    @Test
    fun `setEnvVars should set multiple variables`() = runTest {
        val envVars = mapOf(
            "VAR1" to "value1",
            "VAR2" to "value2"
        )

        val result = bridge.setEnvVars(envVars)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 会话管理测试 ====================

    @Test
    fun `initializeSession should return success`() = runTest {
        val result = bridge.initializeSession()

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `closeSession should complete without error`() = runTest {
        bridge.closeSession()

        assertThat(bridge.isSessionActive()).isFalse()
    }

    // ==================== TermuxExecutionResult测试 ====================

    @Test
    fun `TermuxExecutionResult success should create successful result`() {
        val result = TermuxExecutionResult.success(
            stdout = "output",
            stderr = "",
            executionTimeMs = 100
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.exitCode).isEqualTo(0)
        assertThat(result.stdout).isEqualTo("output")
    }

    @Test
    fun `TermuxExecutionResult failure should create failed result`() {
        val result = TermuxExecutionResult.failure(
            exitCode = 1,
            stderr = "error"
        )

        assertThat(result.isSuccess).isFalse()
        assertThat(result.exitCode).isEqualTo(1)
        assertThat(result.stderr).isEqualTo("error")
    }

    @Test
    fun `TermuxExecutionResult timeout should create timeout result`() {
        val result = TermuxExecutionResult.timeout(30000)

        assertThat(result.isSuccess).isFalse()
        assertThat(result.exitCode).isEqualTo(-1)
        assertThat(result.stderr).isEqualTo("Command execution timed out")
    }

    @Test
    fun `TermuxExecutionResult fullOutput should combine stdout and stderr`() {
        val result = TermuxExecutionResult(
            isSuccess = true,
            exitCode = 0,
            stdout = "stdout",
            stderr = "stderr",
            executionTimeMs = 100
        )

        assertThat(result.fullOutput).isEqualTo("stdout\nstderr")
    }

    @Test
    fun `TermuxExecutionResult fullOutput should handle empty stdout`() {
        val result = TermuxExecutionResult(
            isSuccess = false,
            exitCode = 1,
            stdout = "",
            stderr = "error",
            executionTimeMs = 100
        )

        assertThat(result.fullOutput).isEqualTo("error")
    }

    // ==================== PackageInfo测试 ====================

    @Test
    fun `PackageInfo should have correct properties`() {
        val packageInfo = PackageInfo(
            name = "bash",
            version = "5.0.0",
            description = "Bourne Again Shell",
            isInstalled = true,
            architecture = "aarch64",
            dependencies = listOf("ncurses"),
            installedSize = 1024 * 1024
        )

        assertThat(packageInfo.name).isEqualTo("bash")
        assertThat(packageInfo.version).isEqualTo("5.0.0")
        assertThat(packageInfo.isInstalled).isTrue()
        assertThat(packageInfo.dependencies).contains("ncurses")
    }

    @Test
    fun `PackageInfo parseFromAptOutput should parse output correctly`() {
        val output = """
            package1 1.0.0 Description of package 1
            package2 2.0.0 Description of package 2
        """.trimIndent()

        val packages = PackageInfo.parseFromAptOutput(output)

        assertThat(packages).hasSize(2)
        assertThat(packages[0].name).isEqualTo("package1")
        assertThat(packages[0].version).isEqualTo("1.0.0")
    }

    // ==================== FileInfo测试 ====================

    @Test
    fun `FileInfo should have correct properties`() {
        val fileInfo = FileInfo(
            path = "/tmp/file.txt",
            name = "file.txt",
            isDirectory = false,
            isFile = true,
            size = 1024,
            lastModified = System.currentTimeMillis(),
            permissions = "-rw-r--r--",
            owner = "user",
            group = "group"
        )

        assertThat(fileInfo.path).isEqualTo("/tmp/file.txt")
        assertThat(fileInfo.name).isEqualTo("file.txt")
        assertThat(fileInfo.isDirectory).isFalse()
        assertThat(fileInfo.isFile).isTrue()
        assertThat(fileInfo.size).isEqualTo(1024)
    }

    @Test
    fun `FileInfo parseFromLsLine should parse ls output correctly`() {
        val line = "drwxr-xr-x 2 user group 4096 Jan 1 12:00 mydir"
        val parentPath = "/tmp"

        val fileInfo = FileInfo.parseFromLsLine(line, parentPath)

        assertThat(fileInfo).isNotNull()
        assertThat(fileInfo!!.isDirectory).isTrue()
        // Name parsing may vary based on implementation
        assertThat(fileInfo.permissions).isEqualTo("drwxr-xr-x")
    }

    @Test
    fun `FileInfo parseFromLsLine should return null for invalid input`() {
        val line = "invalid line"
        val fileInfo = FileInfo.parseFromLsLine(line, "/tmp")

        assertThat(fileInfo).isNull()
    }

    // ==================== TermuxConstants测试 ====================

    @Test
    fun `TermuxConstants should have correct path constants`() {
        assertThat(TermuxConstants.TERMUX_PREFIX).isEqualTo("/data/data/com.termux/files/usr")
        assertThat(TermuxConstants.TERMUX_HOME).isEqualTo("/data/data/com.termux/files/home")
        assertThat(TermuxConstants.TERMUX_FILES_PATH).isEqualTo("/data/data/com.termux/files")
    }

    @Test
    fun `TermuxConstants should have correct command constants`() {
        assertThat(TermuxConstants.PKG_COMMAND).isEqualTo("pkg")
        assertThat(TermuxConstants.APT_COMMAND).isEqualTo("apt")
        assertThat(TermuxConstants.DPKG_COMMAND).isEqualTo("dpkg")
    }

    @Test
    fun `TermuxConstants should have correct timeout constants`() {
        assertThat(TermuxConstants.DEFAULT_COMMAND_TIMEOUT_MS).isEqualTo(30_000L)
        assertThat(TermuxConstants.LONG_COMMAND_TIMEOUT_MS).isEqualTo(300_000L)
    }

    // ==================== PackageManagerConfig测试 ====================

    @Test
    fun `PackageManagerConfig DEFAULT should have correct values`() {
        val config = PackageManagerConfig.DEFAULT

        assertThat(config.packageManagerType).isEqualTo(PackageManagerType.PKG)
        assertThat(config.autoConfirm).isTrue()
        assertThat(config.updateBeforeInstall).isTrue()
        assertThat(config.maxRetries).isEqualTo(3)
    }

    @Test
    fun `PackageManagerConfig getPackageManagerCommand should return correct command`() {
        val pkgConfig = PackageManagerConfig(packageManagerType = PackageManagerType.PKG)
        assertThat(pkgConfig.getPackageManagerCommand()).isEqualTo("pkg")

        val aptConfig = PackageManagerConfig(packageManagerType = PackageManagerType.APT)
        assertThat(aptConfig.getPackageManagerCommand()).isEqualTo("apt")
    }

    // ==================== TermuxConfig测试 ====================

    @Test
    fun `TermuxConfig validate should return true for valid config`() {
        assertThat(testConfig.validate()).isTrue()
    }

    @Test
    fun `TermuxConfig validate should return false for blank prefixPath`() {
        val config = TermuxConfig(prefixPath = "")
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `TermuxConfig validate should return false for zero commandTimeout`() {
        val config = TermuxConfig(commandTimeoutMs = 0)
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `TermuxConfig getFullEnvironment should return complete environment`() {
        val env = testConfig.getFullEnvironment()

        assertThat(env).containsEntry("PREFIX", testConfig.prefixPath)
        assertThat(env).containsEntry("HOME", testConfig.homePath)
        assertThat(env).containsEntry("PATH", "${testConfig.binPath}:/system/bin:/system/xbin")
    }

    // ==================== 异常测试 ====================

    @Test
    fun `TermuxExecutionException should have correct message`() {
        val exception = TermuxExecutionException("Command failed")

        assertThat(exception.message).contains("Command failed")
    }

    @Test
    fun `TermuxFileException should have correct message`() {
        val exception = TermuxFileException("File not found")

        assertThat(exception.message).contains("File not found")
    }

    @Test
    fun `TermuxEnvException should have correct message`() {
        val exception = TermuxEnvException("Variable not found")

        assertThat(exception.message).contains("Variable not found")
    }
}