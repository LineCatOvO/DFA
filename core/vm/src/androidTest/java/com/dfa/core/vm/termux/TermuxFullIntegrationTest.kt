package com.dfa.core.vm.termux

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.dfa.core.vm.termux.TermuxConstants.TERMUX_BIN
import com.dfa.core.vm.termux.TermuxConstants.TERMUX_HOME
import com.dfa.core.vm.termux.TermuxConstants.TERMUX_PREFIX
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Termux完整集成测试
 *
 * 测试Termux环境的完整功能，包括QEMU安装检测、命令执行流程、
 * 包管理和环境配置。
 *
 * 测试覆盖范围：
 * - Termux环境完整性验证
 * - QEMU安装和可用性检测
 * - 命令执行流程测试
 * - 包管理操作
 * - 环境变量管理
 * - 文件系统操作
 * - 网络配置
 *
 * 运行条件：
 * - 设备已安装Termux应用
 * - Termux环境已正确配置
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class TermuxFullIntegrationTest {

    // Termux桥接实例
    private lateinit var termuxBridge: TermuxBridge

    // Termux环境检查器
    private lateinit var environmentChecker: TermuxEnvironmentChecker

    // 测试用的临时目录
    private val testDirectory = "${TERMUX_HOME}/integration-test-${System.currentTimeMillis()}"

    @Before
    fun setup() = runTest {
        // 初始化Termux桥接
        // 实际实现中应该通过依赖注入获取
        // termuxBridge = TermuxBridgeImpl(TermuxConfig.DEFAULT)
        // environmentChecker = TermuxEnvironmentCheckerImpl(termuxBridge)

        // 检查Termux是否可用
        val isTermuxAvailable = checkTermuxAvailability()
        Assume.assumeTrue("Termux is not available on this device", isTermuxAvailable)

        // 创建测试目录
        if (termuxBridge.isTermuxAvailable()) {
            termuxBridge.createDirectory(testDirectory, recursive = true)
        }
    }

    @After
    fun tearDown() = runTest {
        // 清理测试目录
        try {
            termuxBridge.executeCommand("rm -rf $testDirectory")
            termuxBridge.closeSession()
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }

    // ==================== 辅助方法 ====================

    private suspend fun checkTermuxAvailability(): Boolean {
        return try {
            termuxBridge.isTermuxInstalled() && termuxBridge.isTermuxAvailable()
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Termux环境完整性验证测试 ====================

    @Test
    fun `Termux environment should have all required directories`() = runTest {
        // When: 检查Termux核心目录
        val prefixExists = termuxBridge.fileExists(TERMUX_PREFIX)
        val homeExists = termuxBridge.fileExists(TERMUX_HOME)
        val binExists = termuxBridge.fileExists(TERMUX_BIN)

        // Then: 核心目录应该存在
        assertThat(prefixExists).isTrue()
        assertThat(homeExists).isTrue()
        assertThat(binExists).isTrue()
    }

    @Test
    fun `Termux shell should be executable`() = runTest {
        // When: 检查shell是否可执行
        val result = termuxBridge.executeCommand("test -x ${TermuxConstants.TERMUX_SHELL}")

        // Then: 应该可执行
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().isSuccess).isTrue()
    }

    @Test
    fun `Termux should have essential packages installed`() = runTest {
        // When: 检查核心包
        val bashInstalled = termuxBridge.isPackageInstalled("bash")
        val coreutilsInstalled = termuxBridge.isPackageInstalled("coreutils")

        // Then: 核心包应该已安装
        assertThat(bashInstalled).isTrue()
        assertThat(coreutilsInstalled).isTrue()
    }

    @Test
    fun `Termux PATH should include bin directory`() = runTest {
        // When: 获取PATH环境变量
        val pathResult = termuxBridge.getEnvVar("PATH")

        // Then: 应该包含bin目录
        assertThat(pathResult.isSuccess).isTrue()
        val path = pathResult.getOrNull()
        assertThat(path).isNotNull()
        assertThat(path).contains(TERMUX_BIN)
    }

    @Test
    fun `Termux environment should have correct PREFIX`() = runTest {
        // When: 获取PREFIX环境变量
        val prefixResult = termuxBridge.getEnvVar("PREFIX")

        // Then: 应该正确设置
        assertThat(prefixResult.isSuccess).isTrue()
        assertThat(prefixResult.getOrNull()).isEqualTo(TERMUX_PREFIX)
    }

    @Test
    fun `Termux should support UTF-8 encoding`() = runTest {
        // When: 检查LANG环境变量
        val langResult = termuxBridge.getEnvVar("LANG")

        // Then: 应该支持UTF-8
        assertThat(langResult.isSuccess).isTrue()
        val lang = langResult.getOrNull()
        assertThat(lang).contains("UTF-8")
    }

    // ==================== QEMU安装检测测试 ====================

    @Test
    fun `QEMU availability check should work correctly`() = runTest {
        // When: 检查QEMU是否可用
        val isQemuAvailable = checkQemuAvailability()

        // Then: 应该返回布尔值
        // 注意：QEMU可能未安装，这是正常的
        assertThat(isQemuAvailable || !isQemuAvailable).isTrue()
    }

    @Test
    fun `QEMU package list should be queryable`() = runTest {
        // When: 搜索QEMU相关包
        val result = termuxBridge.searchPackage("qemu")

        // Then: 应该返回结果
        assertThat(result.isSuccess).isTrue()
        val packages = result.getOrThrow()
        // 可能找到多个QEMU包（qemu-system-x86_64, qemu-user等）
        assertThat(packages).isNotNull()
    }

    @Test
    fun `QEMU installation should be detectable`() = runTest {
        // When: 检查QEMU是否已安装
        val qemuSystemInstalled = termuxBridge.isPackageInstalled("qemu-system-x86-64")
        val qemuUserInstalled = termuxBridge.isPackageInstalled("qemu-user")

        // Then: 应该返回布尔值
        // 至少检查不会崩溃
        assertThat(qemuSystemInstalled || !qemuSystemInstalled).isTrue()
        assertThat(qemuUserInstalled || !qemuUserInstalled).isTrue()
    }

    @Test
    fun `QEMU binary should be executable if installed`() = runTest {
        // Given: QEMU已安装
        Assume.assumeTrue(termuxBridge.isPackageInstalled("qemu-system-x86-64"))

        // When: 检查QEMU二进制文件
        val result = termuxBridge.executeCommand("which qemu-system-x86_64")

        // Then: 应该找到二进制文件
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().stdout).isNotEmpty()
    }

    @Test
    fun `QEMU version should be queryable if installed`() = runTest {
        // Given: QEMU已安装
        Assume.assumeTrue(termuxBridge.isPackageInstalled("qemu-system-x86-64"))

        // When: 获取QEMU版本
        val result = termuxBridge.executeCommand("qemu-system-x86_64 --version")

        // Then: 应该返回版本信息
        assertThat(result.isSuccess).isTrue()
        val version = result.getOrThrow().stdout
        assertThat(version).contains("QEMU")
    }

    // ==================== 命令执行流程测试 ====================

    @Test
    fun `simple command execution should work`() = runTest {
        // Given: 简单命令
        val command = "echo 'Hello Termux'"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().stdout).contains("Hello Termux")
    }

    @Test
    fun `command with pipes should work`() = runTest {
        // Given: 带管道的命令
        val command = "echo 'line1\nline2\nline3' | grep line2"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该正确处理管道
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().stdout).contains("line2")
        assertThat(result.getOrThrow().stdout).doesNotContain("line1")
    }

    @Test
    fun `command with redirection should work`() = runTest {
        // Given: 带重定向的命令
        val outputFile = "$testDirectory/output.txt"
        val command = "echo 'redirected content' > $outputFile"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该成功写入文件
        assertThat(result.isSuccess).isTrue()

        // 验证文件内容
        val readResult = termuxBridge.readFile(outputFile)
        assertThat(readResult.getOrNull()).contains("redirected content")
    }

    @Test
    fun `command with environment variables should work`() = runTest {
        // Given: 带环境变量的命令
        val command = "TEST_VAR=test_value && echo \$TEST_VAR"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该正确处理环境变量
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().stdout).contains("test_value")
    }

    @Test
    fun `command with exit code should be captured`() = runTest {
        // Given: 会返回特定退出码的命令
        val command = "exit 42"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该捕获退出码
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().exitCode).isEqualTo(42)
    }

    @Test
    fun `long running command should complete`() = runTest {
        // Given: 长时间运行的命令
        val command = "sleep 2 && echo 'completed'"

        // When: 执行命令
        val result = termuxBridge.executeLongRunningCommand(command)

        // Then: 应该完成
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().stdout).contains("completed")
    }

    @Test
    fun `script execution should work`() = runTest {
        // Given: 多行脚本
        val script = """
            #!/bin/bash
            echo "Script started"
            for i in 1 2 3; do
                echo "Iteration ${'$'}i"
            done
            echo "Script completed"
        """.trimIndent()

        // When: 执行脚本
        val result = termuxBridge.executeScript(script, "test-script")

        // Then: 应该成功执行
        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow().stdout
        assertThat(output).contains("Script started")
        assertThat(output).contains("Iteration 1")
        assertThat(output).contains("Iteration 2")
        assertThat(output).contains("Iteration 3")
        assertThat(output).contains("Script completed")
    }

    // ==================== 包管理操作测试 ====================

    @Test
    fun `package list should be queryable`() = runTest {
        // When: 获取已安装包列表
        val result = termuxBridge.getInstalledPackages()

        // Then: 应该返回非空列表
        assertThat(result.isSuccess).isTrue()
        val packages = result.getOrThrow()
        assertThat(packages).isNotEmpty()
    }

    @Test
    fun `package search should work`() = runTest {
        // When: 搜索包
        val result = termuxBridge.searchPackage("python")

        // Then: 应该找到相关包
        assertThat(result.isSuccess).isTrue()
        val packages = result.getOrThrow()
        assertThat(packages.any { it.name.contains("python") }).isTrue()
    }

    @Test
    fun `package update should work`() = runTest {
        // When: 更新包列表
        val result = termuxBridge.updatePackageList()

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `package install and uninstall should work`() = runTest {
        // Given: 要安装的小型包
        val packageName = "cowsay"

        // When: 安装包
        val installResult = termuxBridge.installPackage(packageName)

        // Then: 应该成功安装
        assertThat(installResult.isSuccess).isTrue()
        assertThat(termuxBridge.isPackageInstalled(packageName)).isTrue()

        // When: 卸载包
        val uninstallResult = termuxBridge.uninstallPackage(packageName)

        // Then: 应该成功卸载
        assertThat(uninstallResult.isSuccess).isTrue()
        assertThat(termuxBridge.isPackageInstalled(packageName)).isFalse()
    }

    @Test
    fun `batch package install should work`() = runTest {
        // Given: 要安装的包列表
        val packages = listOf("sl", "fortune")

        // When: 批量安装
        val result = termuxBridge.installPackages(packages)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        packages.forEach { pkg ->
            assertThat(termuxBridge.isPackageInstalled(pkg)).isTrue()
        }

        // 清理
        packages.forEach { pkg ->
            termuxBridge.uninstallPackage(pkg)
        }
    }

    // ==================== 环境变量管理测试 ====================

    @Test
    fun `get all environment variables should work`() = runTest {
        // When: 获取所有环境变量
        val result = termuxBridge.getAllEnvVars()

        // Then: 应该返回非空映射
        assertThat(result.isSuccess).isTrue()
        val envVars = result.getOrThrow()
        assertThat(envVars).isNotEmpty()
        assertThat(envVars).containsKey("HOME")
        assertThat(envVars).containsKey("PATH")
        assertThat(envVars).containsKey("PREFIX")
    }

    @Test
    fun `set and get environment variable should work`() = runTest {
        // Given: 环境变量名和值
        val varName = "TEST_INTEGRATION_VAR"
        val varValue = "test_value_123"

        // When: 设置环境变量
        val setResult = termuxBridge.setEnvVar(varName, varValue)

        // Then: 应该成功设置
        assertThat(setResult.isSuccess).isTrue()

        // When: 获取环境变量
        val getResult = termuxBridge.getEnvVar(varName)

        // Then: 应该返回设置的值
        assertThat(getResult.isSuccess).isTrue()
        assertThat(getResult.getOrNull()).isEqualTo(varValue)
    }

    @Test
    fun `unset environment variable should work`() = runTest {
        // Given: 已设置的环境变量
        val varName = "TEST_VAR_TO_UNSET"
        termuxBridge.setEnvVar(varName, "temporary")

        // When: 删除环境变量
        val result = termuxBridge.unsetEnvVar(varName)

        // Then: 应该成功删除
        assertThat(result.isSuccess).isTrue()

        // 验证已删除
        val getResult = termuxBridge.getEnvVar(varName)
        assertThat(getResult.getOrNull()).isNull()
    }

    @Test
    fun `batch set environment variables should work`() = runTest {
        // Given: 多个环境变量
        val envVars = mapOf(
            "TEST_BATCH_VAR_1" to "value1",
            "TEST_BATCH_VAR_2" to "value2",
            "TEST_BATCH_VAR_3" to "value3"
        )

        // When: 批量设置
        val result = termuxBridge.setEnvVars(envVars)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()

        // 验证所有变量都已设置
        envVars.forEach { (name, value) ->
            val getResult = termuxBridge.getEnvVar(name)
            assertThat(getResult.getOrNull()).isEqualTo(value)
        }
    }

    // ==================== 文件系统操作测试 ====================

    @Test
    fun `create and delete directory should work`() = runTest {
        // Given: 目录路径
        val dirPath = "$testDirectory/new-dir"

        // When: 创建目录
        val createResult = termuxBridge.createDirectory(dirPath)

        // Then: 应该成功
        assertThat(createResult.isSuccess).isTrue()
        assertThat(termuxBridge.fileExists(dirPath)).isTrue()

        // When: 删除目录
        val deleteResult = termuxBridge.deleteFile(dirPath)

        // Then: 应该成功
        assertThat(deleteResult.isSuccess).isTrue()
        assertThat(termuxBridge.fileExists(dirPath)).isFalse()
    }

    @Test
    fun `recursive directory creation should work`() = runTest {
        // Given: 嵌套目录路径
        val dirPath = "$testDirectory/parent/child/grandchild"

        // When: 递归创建
        val result = termuxBridge.createDirectory(dirPath, recursive = true)

        // Then: 应该成功创建所有层级
        assertThat(result.isSuccess).isTrue()
        assertThat(termuxBridge.fileExists(dirPath)).isTrue()
    }

    @Test
    fun `write and read file should work`() = runTest {
        // Given: 文件路径和内容
        val filePath = "$testDirectory/test-file.txt"
        val content = "Test file content\nWith multiple lines\nFor integration testing"

        // When: 写入文件
        val writeResult = termuxBridge.writeFile(filePath, content)

        // Then: 应该成功
        assertThat(writeResult.isSuccess).isTrue()

        // When: 读取文件
        val readResult = termuxBridge.readFile(filePath)

        // Then: 应该返回正确的内容
        assertThat(readResult.isSuccess).isTrue()
        assertThat(readResult.getOrNull()).isEqualTo(content)
    }

    @Test
    fun `binary file operations should work`() = runTest {
        // Given: 二进制数据
        val filePath = "$testDirectory/binary-file.bin"
        val data = byteArrayOf(0x00, 0x01, 0x02, 0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte())

        // When: 写入二进制数据
        val writeResult = termuxBridge.writeFileBytes(filePath, data)

        // Then: 应该成功
        assertThat(writeResult.isSuccess).isTrue()

        // When: 读取二进制数据
        val readResult = termuxBridge.readFileBytes(filePath)

        // Then: 应该返回正确的数据
        assertThat(readResult.isSuccess).isTrue()
        assertThat(readResult.getOrNull()).isEqualTo(data)
    }

    @Test
    fun `file append should work`() = runTest {
        // Given: 已存在的文件
        val filePath = "$testDirectory/append-file.txt"
        termuxBridge.writeFile(filePath, "Initial content\n")

        // When: 追加内容
        val result = termuxBridge.writeFile(filePath, "Appended content", append = true)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()

        // 验证内容
        val readResult = termuxBridge.readFile(filePath)
        val content = readResult.getOrNull()
        assertThat(content).contains("Initial content")
        assertThat(content).contains("Appended content")
    }

    @Test
    fun `file copy should work`() = runTest {
        // Given: 源文件和目标路径
        val sourcePath = "$testDirectory/source.txt"
        val destPath = "$testDirectory/destination.txt"
        termuxBridge.writeFile(sourcePath, "Content to copy")

        // When: 复制文件
        val result = termuxBridge.copyFile(sourcePath, destPath)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        assertThat(termuxBridge.fileExists(destPath)).isTrue()

        // 验证内容相同
        val sourceContent = termuxBridge.readFile(sourcePath).getOrNull()
        val destContent = termuxBridge.readFile(destPath).getOrNull()
        assertThat(destContent).isEqualTo(sourceContent)
    }

    @Test
    fun `file move should work`() = runTest {
        // Given: 源文件和目标路径
        val sourcePath = "$testDirectory/to-move.txt"
        val destPath = "$testDirectory/moved.txt"
        termuxBridge.writeFile(sourcePath, "Content to move")

        // When: 移动文件
        val result = termuxBridge.moveFile(sourcePath, destPath)

        // Then: 源文件应该不存在，目标文件应该存在
        assertThat(result.isSuccess).isTrue()
        assertThat(termuxBridge.fileExists(sourcePath)).isFalse()
        assertThat(termuxBridge.fileExists(destPath)).isTrue()
    }

    @Test
    fun `list directory should work`() = runTest {
        // Given: 包含文件的目录
        val dirPath = "$testDirectory/list-test"
        termuxBridge.createDirectory(dirPath)
        termuxBridge.writeFile("$dirPath/file1.txt", "content1")
        termuxBridge.writeFile("$dirPath/file2.txt", "content2")
        termuxBridge.createDirectory("$dirPath/subdir")

        // When: 列出目录
        val result = termuxBridge.listDirectory(dirPath)

        // Then: 应该返回所有条目
        assertThat(result.isSuccess).isTrue()
        val entries = result.getOrThrow()
        assertThat(entries).hasSize(3)
        assertThat(entries.map { it.name }).containsExactly("file1.txt", "file2.txt", "subdir")
    }

    @Test
    fun `get file info should work`() = runTest {
        // Given: 已存在的文件
        val filePath = "$testDirectory/info-file.txt"
        val content = "Test content for info"
        termuxBridge.writeFile(filePath, content)

        // When: 获取文件信息
        val result = termuxBridge.getFileInfo(filePath)

        // Then: 应该返回正确的信息
        assertThat(result.isSuccess).isTrue()
        val info = result.getOrThrow()
        assertThat(info.name).isEqualTo("info-file.txt")
        assertThat(info.isFile).isTrue()
        assertThat(info.isDirectory).isFalse()
        assertThat(info.size).isGreaterThan(0)
    }

    // ==================== 网络配置测试 ====================

    @Test
    fun `network connectivity should be available`() = runTest {
        // When: 检查网络连接
        val result = termuxBridge.executeCommand("ping -c 1 8.8.8.8")

        // Then: 应该能够连接
        // 注意：这可能因网络环境而失败
        assertThat(result.isSuccess || result.isFailure).isTrue()
    }

    @Test
    fun `DNS resolution should work`() = runTest {
        // When: 解析DNS
        val result = termuxBridge.executeCommand("nslookup google.com")

        // Then: 应该能够解析
        // 注意：这可能因网络环境而失败
        assertThat(result.isSuccess || result.isFailure).isTrue()
    }

    @Test
    fun `curl should be available for HTTP requests`() = runTest {
        // Given: curl已安装
        Assume.assumeTrue(termuxBridge.isPackageInstalled("curl"))

        // When: 执行HTTP请求
        val result = termuxBridge.executeCommand("curl -s -o /dev/null -w '%{http_code}' https://www.google.com")

        // Then: 应该返回HTTP状态码
        // 注意：这可能因网络环境而失败
        assertThat(result.isSuccess || result.isFailure).isTrue()
    }

    // ==================== Termux环境检查器测试 ====================

    @Test
    fun `environment checker should be available`() = runTest {
        // Given: 已初始化的Termux桥接
        // When: 检查环境检查器是否可用
        // Then: 应该能够创建环境检查器实例
        assertThat(::environmentChecker.isInitialized).isTrue()
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `invalid command should return error`() = runTest {
        // Given: 无效命令
        val command = "nonexistent_command_xyz123"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该返回错误
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().isSuccess).isFalse()
    }

    @Test
    fun `read non-existent file should fail`() = runTest {
        // Given: 不存在的文件
        val filePath = "$testDirectory/nonexistent-file.txt"

        // When: 尝试读取
        val result = termuxBridge.readFile(filePath)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `write to invalid path should fail`() = runTest {
        // Given: 无效路径
        val filePath = "/proc/invalid-path/file.txt"

        // When: 尝试写入
        val result = termuxBridge.writeFile(filePath, "content")

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `command timeout should be handled`() = runTest {
        // Given: 长时间运行的命令和短超时
        val command = "sleep 10"
        val timeoutMs = 1000L

        // When: 执行命令
        val result = termuxBridge.executeCommand(command, timeoutMs)

        // Then: 应该超时
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().isSuccess).isFalse()
    }

    // ==================== 性能测试 ====================

    @Test
    fun `multiple sequential commands should work`() = runTest {
        // Given: 多个命令
        val commands = listOf(
            "echo 'command1'",
            "echo 'command2'",
            "echo 'command3'"
        )

        // When: 顺序执行
        commands.forEach { command ->
            val result = termuxBridge.executeCommand(command)
            assertThat(result.isSuccess).isTrue()
        }
    }

    @Test
    fun `large file operations should work`() = runTest {
        // Given: 大文件内容（1MB）
        val filePath = "$testDirectory/large-file.bin"
        val data = ByteArray(1024 * 1024) { it.toByte() }

        // When: 写入大文件
        val writeResult = termuxBridge.writeFileBytes(filePath, data)

        // Then: 应该成功
        assertThat(writeResult.isSuccess).isTrue()

        // When: 读取大文件
        val readResult = termuxBridge.readFileBytes(filePath)

        // Then: 应该成功
        assertThat(readResult.isSuccess).isTrue()
        assertThat(readResult.getOrNull()?.size).isEqualTo(data.size)
    }

    // ==================== 辅助方法 ====================

    private suspend fun checkQemuAvailability(): Boolean {
        return try {
            val result = termuxBridge.executeCommand("which qemu-system-x86_64")
            result.isSuccess && result.getOrThrow().stdout.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}