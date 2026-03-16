package com.dfa.core.vm.termux

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * TermuxBridge单元测试
 *
 * 测试TermuxBridge接口、TermuxConfig配置和TermuxConstants常量
 */
class TermuxBridgeTest {

    // ==================== TermuxConfig测试 ====================

    @Test
    fun `TermuxConfig DEFAULT should have correct values`() {
        val config = TermuxConfig.DEFAULT

        assertThat(config.prefixPath).isEqualTo(TermuxConstants.TERMUX_PREFIX)
        assertThat(config.homePath).isEqualTo(TermuxConstants.TERMUX_HOME)
        assertThat(config.tmpPath).isEqualTo(TermuxConstants.TERMUX_TMP)
        assertThat(config.binPath).isEqualTo(TermuxConstants.TERMUX_BIN)
        assertThat(config.shellPath).isEqualTo(TermuxConstants.TERMUX_SHELL)
        assertThat(config.debugMode).isFalse()
        assertThat(config.commandTimeoutMs).isEqualTo(TermuxConstants.DEFAULT_COMMAND_TIMEOUT_MS)
    }

    @Test
    fun `TermuxConfig DEBUG should have debugMode enabled`() {
        val config = TermuxConfig.DEBUG

        assertThat(config.debugMode).isTrue()
    }

    @Test
    fun `TermuxConfig Builder should create custom config`() {
        val config = TermuxConfig.Builder()
            .prefixPath("/custom/prefix")
            .homePath("/custom/home")
            .tmpPath("/custom/tmp")
            .binPath("/custom/bin")
            .shellPath("/custom/shell")
            .debugMode(true)
            .commandTimeout(60_000)
            .longCommandTimeout(600_000)
            .packageInstallTimeout(180_000)
            .fileOperationTimeout(120_000)
            .environmentVariable("CUSTOM_VAR", "value")
            .build()

        assertThat(config.prefixPath).isEqualTo("/custom/prefix")
        assertThat(config.homePath).isEqualTo("/custom/home")
        assertThat(config.tmpPath).isEqualTo("/custom/tmp")
        assertThat(config.binPath).isEqualTo("/custom/bin")
        assertThat(config.shellPath).isEqualTo("/custom/shell")
        assertThat(config.debugMode).isTrue()
        assertThat(config.commandTimeoutMs).isEqualTo(60_000)
        assertThat(config.longCommandTimeoutMs).isEqualTo(600_000)
        assertThat(config.packageInstallTimeoutMs).isEqualTo(180_000)
        assertThat(config.fileOperationTimeoutMs).isEqualTo(120_000)
        assertThat(config.environmentVariables).containsEntry("CUSTOM_VAR", "value")
    }

    @Test
    fun `TermuxConfig fromEnvironment should use env vars when present`() {
        val envVars = mapOf(
            TermuxConstants.ENV_PREFIX to "/env/prefix",
            TermuxConstants.ENV_HOME to "/env/home",
            TermuxConstants.ENV_TMPDIR to "/env/tmp"
        )

        val config = TermuxConfig.fromEnvironment(envVars)

        assertThat(config.prefixPath).isEqualTo("/env/prefix")
        assertThat(config.homePath).isEqualTo("/env/home")
        assertThat(config.tmpPath).isEqualTo("/env/tmp")
    }

    @Test
    fun `TermuxConfig fromEnvironment should use defaults when env vars missing`() {
        val config = TermuxConfig.fromEnvironment(emptyMap())

        assertThat(config.prefixPath).isEqualTo(TermuxConstants.TERMUX_PREFIX)
        assertThat(config.homePath).isEqualTo(TermuxConstants.TERMUX_HOME)
    }

    @Test
    fun `TermuxConfig getFullEnvironment should return complete environment`() {
        val config = TermuxConfig.Builder()
            .environmentVariable("CUSTOM_VAR", "custom_value")
            .build()

        val env = config.getFullEnvironment()

        assertThat(env).containsEntry(TermuxConstants.ENV_PREFIX, config.prefixPath)
        assertThat(env).containsEntry(TermuxConstants.ENV_HOME, config.homePath)
        assertThat(env).containsEntry(TermuxConstants.ENV_TMPDIR, config.tmpPath)
        assertThat(env).containsEntry(TermuxConstants.ENV_PATH, "${config.binPath}:/system/bin:/system/xbin")
        assertThat(env).containsEntry(TermuxConstants.ENV_LD_LIBRARY_PATH, "${config.prefixPath}/lib")
        assertThat(env).containsEntry(TermuxConstants.ENV_TERM, "xterm-256color")
        assertThat(env).containsEntry(TermuxConstants.ENV_LANG, "en_US.UTF-8")
        assertThat(env).containsEntry("CUSTOM_VAR", "custom_value")
    }

    @Test
    fun `TermuxConfig validate should return true for valid config`() {
        val config = TermuxConfig.DEFAULT
        assertThat(config.validate()).isTrue()
    }

    @Test
    fun `TermuxConfig validate should return false for blank prefixPath`() {
        val config = TermuxConfig(prefixPath = "")
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `TermuxConfig validate should return false for blank homePath`() {
        val config = TermuxConfig(homePath = "")
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `TermuxConfig validate should return false for blank shellPath`() {
        val config = TermuxConfig(shellPath = "")
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `TermuxConfig validate should return false for zero commandTimeout`() {
        val config = TermuxConfig(commandTimeoutMs = 0)
        assertThat(config.validate()).isFalse()
    }

    // ==================== PackageManagerConfig测试 ====================

    @Test
    fun `PackageManagerConfig DEFAULT should have correct values`() {
        val config = PackageManagerConfig.DEFAULT

        assertThat(config.packageManagerType).isEqualTo(PackageManagerType.PKG)
        assertThat(config.autoConfirm).isTrue()
        assertThat(config.updateBeforeInstall).isTrue()
        assertThat(config.allowDowngrade).isFalse()
        assertThat(config.cleanCache).isFalse()
        assertThat(config.maxRetries).isEqualTo(3)
        assertThat(config.retryDelayMs).isEqualTo(1000L)
    }

    @Test
    fun `PackageManagerConfig FAST_INSTALL should skip update`() {
        val config = PackageManagerConfig.FAST_INSTALL

        assertThat(config.updateBeforeInstall).isFalse()
    }

    @Test
    fun `PackageManagerConfig SAFE_INSTALL should update and clean cache`() {
        val config = PackageManagerConfig.SAFE_INSTALL

        assertThat(config.updateBeforeInstall).isTrue()
        assertThat(config.cleanCache).isTrue()
    }

    @Test
    fun `PackageManagerConfig Builder should create custom config`() {
        val config = PackageManagerConfig.Builder()
            .packageManagerType(PackageManagerType.APT)
            .autoConfirm(false)
            .updateBeforeInstall(false)
            .allowDowngrade(true)
            .cleanCache(true)
            .mirrorUrl("https://mirror.example.com")
            .maxRetries(5)
            .retryDelay(2000)
            .build()

        assertThat(config.packageManagerType).isEqualTo(PackageManagerType.APT)
        assertThat(config.autoConfirm).isFalse()
        assertThat(config.updateBeforeInstall).isFalse()
        assertThat(config.allowDowngrade).isTrue()
        assertThat(config.cleanCache).isTrue()
        assertThat(config.mirrorUrl).isEqualTo("https://mirror.example.com")
        assertThat(config.maxRetries).isEqualTo(5)
        assertThat(config.retryDelayMs).isEqualTo(2000L)
    }

    @Test
    fun `PackageManagerConfig getPackageManagerCommand should return correct command`() {
        val pkgConfig = PackageManagerConfig(packageManagerType = PackageManagerType.PKG)
        assertThat(pkgConfig.getPackageManagerCommand()).isEqualTo(TermuxConstants.PKG_COMMAND)

        val aptConfig = PackageManagerConfig(packageManagerType = PackageManagerType.APT)
        assertThat(aptConfig.getPackageManagerCommand()).isEqualTo(TermuxConstants.APT_COMMAND)
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
        assertThat(result.stderr).isEmpty()
        assertThat(result.executionTimeMs).isEqualTo(100)
    }

    @Test
    fun `TermuxExecutionResult failure should create failed result`() {
        val result = TermuxExecutionResult.failure(
            exitCode = 1,
            stdout = "",
            stderr = "error message",
            executionTimeMs = 50
        )

        assertThat(result.isSuccess).isFalse()
        assertThat(result.exitCode).isEqualTo(1)
        assertThat(result.stderr).isEqualTo("error message")
    }

    @Test
    fun `TermuxExecutionResult timeout should create timeout result`() {
        val result = TermuxExecutionResult.timeout(executionTimeMs = 30000)

        assertThat(result.isSuccess).isFalse()
        assertThat(result.exitCode).isEqualTo(-1)
        assertThat(result.stderr).isEqualTo("Command execution timed out")
    }

    @Test
    fun `TermuxExecutionResult fullOutput should combine stdout and stderr`() {
        val result = TermuxExecutionResult(
            isSuccess = true,
            exitCode = 0,
            stdout = "stdout content",
            stderr = "stderr content",
            executionTimeMs = 100
        )

        assertThat(result.fullOutput).isEqualTo("stdout content\nstderr content")
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
            name = "test-package",
            version = "1.0.0",
            description = "Test package",
            isInstalled = true,
            architecture = "aarch64",
            dependencies = listOf("dep1", "dep2"),
            installedSize = 1024 * 1024,
            downloadSize = 512 * 1024
        )

        assertThat(packageInfo.name).isEqualTo("test-package")
        assertThat(packageInfo.version).isEqualTo("1.0.0")
        assertThat(packageInfo.description).isEqualTo("Test package")
        assertThat(packageInfo.isInstalled).isTrue()
        assertThat(packageInfo.architecture).isEqualTo("aarch64")
        assertThat(packageInfo.dependencies).containsExactly("dep1", "dep2")
        assertThat(packageInfo.installedSize).isEqualTo(1024 * 1024)
    }

    @Test
    fun `PackageInfo parseFromAptOutput should parse simple output`() {
        val output = """
            package1 1.0.0 Description of package 1
            package2 2.0.0 Description of package 2
        """.trimIndent()

        val packages = PackageInfo.parseFromAptOutput(output)

        assertThat(packages).hasSize(2)
        assertThat(packages[0].name).isEqualTo("package1")
        assertThat(packages[0].version).isEqualTo("1.0.0")
        assertThat(packages[1].name).isEqualTo("package2")
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
            lastModified = 1234567890L,
            permissions = "-rw-r--r--",
            owner = "user",
            group = "group"
        )

        assertThat(fileInfo.path).isEqualTo("/tmp/file.txt")
        assertThat(fileInfo.name).isEqualTo("file.txt")
        assertThat(fileInfo.isDirectory).isFalse()
        assertThat(fileInfo.isFile).isTrue()
        assertThat(fileInfo.size).isEqualTo(1024)
        assertThat(fileInfo.permissions).isEqualTo("-rw-r--r--")
    }

    @Test
    fun `FileInfo parseFromLsLine should parse ls -l output`() {
        val line = "drwxr-xr-x 2 user group 4096 Jan 1 12:00 mydir"
        val parentPath = "/tmp"

        val fileInfo = FileInfo.parseFromLsLine(line, parentPath)

        assertThat(fileInfo).isNotNull()
        assertThat(fileInfo!!.isDirectory).isTrue()
        assertThat(fileInfo.name).isEqualTo("mydir")
        assertThat(fileInfo.owner).isEqualTo("user")
        assertThat(fileInfo.group).isEqualTo("group")
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
        assertThat(TermuxConstants.TERMUX_TMP).isEqualTo("/data/data/com.termux/files/usr/tmp")
        assertThat(TermuxConstants.TERMUX_BIN).isEqualTo("/data/data/com.termux/files/usr/bin")
        assertThat(TermuxConstants.TERMUX_LIB).isEqualTo("/data/data/com.termux/files/usr/lib")
        assertThat(TermuxConstants.TERMUX_ETC).isEqualTo("/data/data/com.termux/files/usr/etc")
        assertThat(TermuxConstants.TERMUX_VAR).isEqualTo("/data/data/com.termux/files/usr/var")
    }

    @Test
    fun `TermuxConstants should have correct package name constants`() {
        assertThat(TermuxConstants.TERMUX_PACKAGE_NAME).isEqualTo("com.termux")
        assertThat(TermuxConstants.TERMUX_API_PACKAGE_NAME).isEqualTo("com.termux.api")
        assertThat(TermuxConstants.TERMUX_BOOT_PACKAGE_NAME).isEqualTo("com.termux.boot")
        assertThat(TermuxConstants.TERMUX_FLOAT_PACKAGE_NAME).isEqualTo("com.termux.window")
        assertThat(TermuxConstants.TERMUX_STYLING_PACKAGE_NAME).isEqualTo("com.termux.styling")
        assertThat(TermuxConstants.TERMUX_TASKER_PACKAGE_NAME).isEqualTo("com.termux.tasker")
        assertThat(TermuxConstants.TERMUX_WIDGET_PACKAGE_NAME).isEqualTo("com.termux.widget")
    }

    @Test
    fun `TermuxConstants should have correct command constants`() {
        assertThat(TermuxConstants.PKG_COMMAND).isEqualTo("pkg")
        assertThat(TermuxConstants.APT_COMMAND).isEqualTo("apt")
        assertThat(TermuxConstants.DPKG_COMMAND).isEqualTo("dpkg")
        assertThat(TermuxConstants.TERMUX_SHELL).isEqualTo("/data/data/com.termux/files/usr/bin/bash")
        assertThat(TermuxConstants.TERMUX_ZSH).isEqualTo("/data/data/com.termux/files/usr/bin/zsh")
    }

    @Test
    fun `TermuxConstants should have correct environment variable names`() {
        assertThat(TermuxConstants.ENV_PREFIX).isEqualTo("PREFIX")
        assertThat(TermuxConstants.ENV_HOME).isEqualTo("HOME")
        assertThat(TermuxConstants.ENV_PATH).isEqualTo("PATH")
        assertThat(TermuxConstants.ENV_LD_LIBRARY_PATH).isEqualTo("LD_LIBRARY_PATH")
        assertThat(TermuxConstants.ENV_TMPDIR).isEqualTo("TMPDIR")
        assertThat(TermuxConstants.ENV_TERM).isEqualTo("TERM")
        assertThat(TermuxConstants.ENV_LANG).isEqualTo("LANG")
    }

    @Test
    fun `TermuxConstants should have correct timeout constants`() {
        assertThat(TermuxConstants.DEFAULT_COMMAND_TIMEOUT_MS).isEqualTo(30_000L)
        assertThat(TermuxConstants.LONG_COMMAND_TIMEOUT_MS).isEqualTo(300_000L)
        assertThat(TermuxConstants.PACKAGE_INSTALL_TIMEOUT_MS).isEqualTo(120_000L)
        assertThat(TermuxConstants.FILE_OPERATION_TIMEOUT_MS).isEqualTo(60_000L)
    }

    @Test
    fun `TermuxConstants SUPPORTED_ARCHITECTURES should contain expected architectures`() {
        assertThat(TermuxConstants.SUPPORTED_ARCHITECTURES).containsExactly(
            "aarch64",
            "arm",
            "x86_64",
            "i686"
        )
    }

    @Test
    fun `TermuxConstants ESSENTIAL_PACKAGES should contain expected packages`() {
        assertThat(TermuxConstants.ESSENTIAL_PACKAGES).containsAtLeast(
            "bash",
            "coreutils",
            "grep",
            "sed",
            "curl",
            "wget"
        )
    }

    @Test
    fun `TermuxConstants API_PACKAGES should contain termux-api`() {
        assertThat(TermuxConstants.API_PACKAGES).contains("termux-api")
    }

    @Test
    fun `TermuxConstants DEVELOPMENT_PACKAGES should contain expected packages`() {
        assertThat(TermuxConstants.DEVELOPMENT_PACKAGES).containsAtLeast(
            "clang",
            "make",
            "git",
            "python",
            "nodejs"
        )
    }

    // ==================== PackageManagerType测试 ====================

    @Test
    fun `PackageManagerType should have PKG and APT values`() {
        assertThat(PackageManagerType.values()).asList().containsExactly(
            PackageManagerType.PKG,
            PackageManagerType.APT
        )
    }

    // ==================== TermuxBridge接口测试（Mock） ====================

    @Test
    fun `TermuxBridge isTermuxAvailable should return boolean`() = runTest {
        val mockBridge = mockk<TermuxBridge>()
        coEvery { mockBridge.isTermuxAvailable() } returns true

        val result = mockBridge.isTermuxAvailable()

        assertThat(result).isTrue()
    }

    @Test
    fun `TermuxBridge isTermuxInstalled should return boolean`() = runTest {
        val mockBridge = mockk<TermuxBridge>()
        coEvery { mockBridge.isTermuxInstalled() } returns true

        val result = mockBridge.isTermuxInstalled()

        assertThat(result).isTrue()
    }

    @Test
    fun `TermuxBridge isTermuxApiInstalled should return boolean`() = runTest {
        val mockBridge = mockk<TermuxBridge>()
        coEvery { mockBridge.isTermuxApiInstalled() } returns false

        val result = mockBridge.isTermuxApiInstalled()

        assertThat(result).isFalse()
    }

    @Test
    fun `TermuxBridge config should return TermuxConfig`() {
        val mockBridge = mockk<TermuxBridge>()
        val config = TermuxConfig.DEFAULT
        every { mockBridge.config } returns config

        assertThat(mockBridge.config).isEqualTo(config)
    }

    @Test
    fun `TermuxBridge isSessionActive should return boolean`() {
        val mockBridge = mockk<TermuxBridge>()
        every { mockBridge.isSessionActive() } returns true

        assertThat(mockBridge.isSessionActive()).isTrue()
    }
}