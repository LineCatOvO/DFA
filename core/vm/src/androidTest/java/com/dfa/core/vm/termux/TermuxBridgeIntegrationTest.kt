package com.dfa.core.vm.termux

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Termux桥接集成测试
 *
 * 测试Termux环境的可用性检查、命令执行和文件操作功能
 * 需要在已安装Termux的Android设备上运行
 *
 * 测试覆盖范围：
 * - Termux环境检查
 * - 命令执行
 * - 文件操作
 * - 包管理
 * - 环境变量管理
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class TermuxBridgeIntegrationTest {

    // Termux桥接实例
    private lateinit var termuxBridge: TermuxBridge

    // 测试用的临时目录
    private val testDirectory = "${TermuxConstants.TERMUX_TMP}/integration-test-${System.currentTimeMillis()}"

    @Before
    fun setup() = runTest {
        // 初始化Termux桥接
        // 实际实现中应该通过依赖注入获取
        // termuxBridge = TermuxBridgeImpl(TermuxConfig.DEFAULT)

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

    // ==================== Termux环境检查测试 ====================

    @Test
    fun `isTermuxInstalled should return true when Termux app is installed`() = runTest {
        // When: 检查Termux应用是否安装
        val isInstalled = termuxBridge.isTermuxInstalled()

        // Then: 应该返回true（测试设备上应安装Termux）
        assertThat(isInstalled).isTrue()
    }

    @Test
    fun `isTermuxAvailable should return true when environment is properly set up`() = runTest {
        // When: 检查Termux环境是否可用
        val isAvailable = termuxBridge.isTermuxAvailable()

        // Then: 应该返回true
        assertThat(isAvailable).isTrue()
    }

    @Test
    fun `isTermuxApiInstalled should return correct status`() = runTest {
        // When: 检查Termux:API插件是否安装
        val isApiInstalled = termuxBridge.isTermuxApiInstalled()

        // Then: 应该返回布尔值（取决于是否安装了API插件）
        // 这是一个信息性测试，不强制要求安装
        assertThat(isApiInstalled).isNotNull()
    }

    @Test
    fun `getEnvironmentInfo should return valid environment map`() = runTest {
        // When: 获取环境信息
        val result = termuxBridge.getEnvironmentInfo()

        // Then: 应该返回有效的环境信息
        assertThat(result.isSuccess).isTrue()
        val envInfo = result.getOrThrow()
        assertThat(envInfo).isNotEmpty()
        assertThat(envInfo).containsKey("PREFIX")
        assertThat(envInfo).containsKey("HOME")
    }

    @Test
    fun `getTermuxVersion should return valid version string`() = runTest {
        // When: 获取Termux版本
        val result = termuxBridge.getTermuxVersion()

        // Then: 应该返回有效的版本字符串
        assertThat(result.isSuccess).isTrue()
        val version = result.getOrNull()
        assertThat(version).isNotEmpty()
    }

    @Test
    fun `config should return valid TermuxConfig`() {
        // When: 获取配置
        val config = termuxBridge.config

        // Then: 应该返回有效的配置
        assertThat(config).isNotNull()
        assertThat(config.prefixPath).isEqualTo(TermuxConstants.TERMUX_PREFIX)
        assertThat(config.homePath).isEqualTo(TermuxConstants.TERMUX_HOME)
    }

    @Test
    fun `initializeSession should succeed when Termux is available`() = runTest {
        // When: 初始化会话
        val result = termuxBridge.initializeSession()

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `isSessionActive should return true after initialization`() = runTest {
        // Given: 已初始化的会话
        termuxBridge.initializeSession()

        // When: 检查会话是否活跃
        val isActive = termuxBridge.isSessionActive()

        // Then: 应该返回true
        assertThat(isActive).isTrue()
    }

    // ==================== 命令执行测试 ====================

    @Test
    fun `executeCommand should return success for simple command`() = runTest {
        // Given: 简单的echo命令
        val command = "echo 'Hello Termux'"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.isSuccess).isTrue()
        assertThat(execResult.stdout).contains("Hello Termux")
    }

    @Test
    fun `executeCommand should return correct exit code for failing command`() = runTest {
        // Given: 会失败的命令
        val command = "exit 1"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该返回失败结果
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.isSuccess).isFalse()
        assertThat(execResult.exitCode).isEqualTo(1)
    }

    @Test
    fun `executeCommand should capture stdout correctly`() = runTest {
        // Given: 输出到stdout的命令
        val command = "ls ${TermuxConstants.TERMUX_BIN}"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该捕获输出
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.stdout).isNotEmpty()
    }

    @Test
    fun `executeCommand should capture stderr correctly`() = runTest {
        // Given: 输出到stderr的命令
        val command = "ls /nonexistent_directory_12345 2>&1"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该捕获错误输出
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.stderr.isNotEmpty() || execResult.stdout.contains("No such file")).isTrue()
    }

    @Test
    fun `executeCommand with timeout should respect timeout setting`() = runTest {
        // Given: 短超时和长时间运行的命令
        val command = "sleep 10"
        val timeoutMs = 1000L // 1秒超时

        // When: 执行命令
        val result = termuxBridge.executeCommand(command, timeoutMs)

        // Then: 应该超时
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.isSuccess).isFalse()
    }

    @Test
    fun `executeScript should execute multi-line script`() = runTest {
        // Given: 多行脚本
        val script = """
            #!/bin/bash
            echo "Line 1"
            echo "Line 2"
            echo "Line 3"
        """.trimIndent()

        // When: 执行脚本
        val result = termuxBridge.executeScript(script, "test-script")

        // Then: 应该成功执行
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.stdout).contains("Line 1")
        assertThat(execResult.stdout).contains("Line 2")
        assertThat(execResult.stdout).contains("Line 3")
    }

    @Test
    fun `executeLongRunningCommand should use extended timeout`() = runTest {
        // Given: 需要较长时间的命令
        val command = "sleep 5 && echo 'done'"

        // When: 执行长时间命令
        val result = termuxBridge.executeLongRunningCommand(command)

        // Then: 应该成功完成
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.stdout).contains("done")
    }

    // ==================== 文件操作测试 ====================

    @Test
    fun `writeFile should create new file`() = runTest {
        // Given: 文件路径和内容
        val filePath = "$testDirectory/test-write.txt"
        val content = "Test content for write operation"

        // When: 写入文件
        val result = termuxBridge.writeFile(filePath, content)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `readFile should return file content`() = runTest {
        // Given: 已存在的文件
        val filePath = "$testDirectory/test-read.txt"
        val content = "Content to read"
        termuxBridge.writeFile(filePath, content)

        // When: 读取文件
        val result = termuxBridge.readFile(filePath)

        // Then: 应该返回正确的内容
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(content)
    }

    @Test
    fun `readFileBytes should return byte array`() = runTest {
        // Given: 已存在的二进制文件
        val filePath = "$testDirectory/test-bytes.bin"
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        termuxBridge.writeFileBytes(filePath, data)

        // When: 读取文件字节
        val result = termuxBridge.readFileBytes(filePath)

        // Then: 应该返回正确的字节数组
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(data)
    }

    @Test
    fun `writeFileBytes should write binary data`() = runTest {
        // Given: 文件路径和二进制数据
        val filePath = "$testDirectory/test-binary.bin"
        val data = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte())

        // When: 写入二进制数据
        val result = termuxBridge.writeFileBytes(filePath, data)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `writeFile with append should append to existing file`() = runTest {
        // Given: 已存在的文件
        val filePath = "$testDirectory/test-append.txt"
        termuxBridge.writeFile(filePath, "Initial content\n")

        // When: 追加内容
        val result = termuxBridge.writeFile(filePath, "Appended content", append = true)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()

        // 验证内容
        val readResult = termuxBridge.readFile(filePath)
        assertThat(readResult.getOrNull()).contains("Initial content")
        assertThat(readResult.getOrNull()).contains("Appended content")
    }

    @Test
    fun `deleteFile should remove existing file`() = runTest {
        // Given: 已存在的文件
        val filePath = "$testDirectory/test-delete.txt"
        termuxBridge.writeFile(filePath, "To be deleted")

        // When: 删除文件
        val result = termuxBridge.deleteFile(filePath)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        assertThat(termuxBridge.fileExists(filePath)).isFalse()
    }

    @Test
    fun `fileExists should return true for existing file`() = runTest {
        // Given: 已存在的文件
        val filePath = "$testDirectory/test-exists.txt"
        termuxBridge.writeFile(filePath, "Content")

        // When: 检查文件是否存在
        val exists = termuxBridge.fileExists(filePath)

        // Then: 应该返回true
        assertThat(exists).isTrue()
    }

    @Test
    fun `fileExists should return false for non-existing file`() = runTest {
        // Given: 不存在的文件
        val filePath = "$testDirectory/nonexistent-file.txt"

        // When: 检查文件是否存在
        val exists = termuxBridge.fileExists(filePath)

        // Then: 应该返回false
        assertThat(exists).isFalse()
    }

    @Test
    fun `createDirectory should create new directory`() = runTest {
        // Given: 目录路径
        val dirPath = "$testDirectory/new-dir"

        // When: 创建目录
        val result = termuxBridge.createDirectory(dirPath)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        assertThat(termuxBridge.fileExists(dirPath)).isTrue()
    }

    @Test
    fun `createDirectory with recursive should create parent directories`() = runTest {
        // Given: 嵌套目录路径
        val dirPath = "$testDirectory/parent/child/grandchild"

        // When: 递归创建目录
        val result = termuxBridge.createDirectory(dirPath, recursive = true)

        // Then: 应该成功创建所有层级
        assertThat(result.isSuccess).isTrue()
        assertThat(termuxBridge.fileExists(dirPath)).isTrue()
    }

    @Test
    fun `listDirectory should return directory contents`() = runTest {
        // Given: 包含文件的目录
        val dirPath = "$testDirectory/list-test"
        termuxBridge.createDirectory(dirPath)
        termuxBridge.writeFile("$dirPath/file1.txt", "content1")
        termuxBridge.writeFile("$dirPath/file2.txt", "content2")

        // When: 列出目录内容
        val result = termuxBridge.listDirectory(dirPath)

        // Then: 应该返回文件列表
        assertThat(result.isSuccess).isTrue()
        val files = result.getOrThrow()
        assertThat(files).hasSize(2)
        assertThat(files.map { it.name }).containsExactly("file1.txt", "file2.txt")
    }

    @Test
    fun `getFileInfo should return correct file information`() = runTest {
        // Given: 已存在的文件
        val filePath = "$testDirectory/info-test.txt"
        val content = "Test content for info"
        termuxBridge.writeFile(filePath, content)

        // When: 获取文件信息
        val result = termuxBridge.getFileInfo(filePath)

        // Then: 应该返回正确的信息
        assertThat(result.isSuccess).isTrue()
        val info = result.getOrThrow()
        assertThat(info.name).isEqualTo("info-test.txt")
        assertThat(info.isFile).isTrue()
        assertThat(info.isDirectory).isFalse()
        assertThat(info.size).isGreaterThan(0)
    }

    @Test
    fun `copyFile should create duplicate file`() = runTest {
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
    fun `moveFile should relocate file`() = runTest {
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

    // ==================== 包管理测试 ====================

    @Test
    fun `isPackageInstalled should return true for essential packages`() = runTest {
        // Given: 核心包名
        val packageName = "bash"

        // When: 检查包是否安装
        val isInstalled = termuxBridge.isPackageInstalled(packageName)

        // Then: bash应该是已安装的
        assertThat(isInstalled).isTrue()
    }

    @Test
    fun `isPackageInstalled should return false for non-existent package`() = runTest {
        // Given: 不存在的包名
        val packageName = "nonexistent_package_xyz123"

        // When: 检查包是否安装
        val isInstalled = termuxBridge.isPackageInstalled(packageName)

        // Then: 应该返回false
        assertThat(isInstalled).isFalse()
    }

    @Test
    fun `getInstalledPackages should return non-empty list`() = runTest {
        // When: 获取已安装包列表
        val result = termuxBridge.getInstalledPackages()

        // Then: 应该返回非空列表
        assertThat(result.isSuccess).isTrue()
        val packages = result.getOrThrow()
        assertThat(packages).isNotEmpty()
        assertThat(packages.any { it.name == "bash" }).isTrue()
    }

    @Test
    fun `searchPackage should find existing packages`() = runTest {
        // Given: 搜索关键词
        val query = "python"

        // When: 搜索包
        val result = termuxBridge.searchPackage(query)

        // Then: 应该找到相关包
        assertThat(result.isSuccess).isTrue()
        val packages = result.getOrThrow()
        assertThat(packages).isNotEmpty()
        assertThat(packages.any { it.name.contains("python") }).isTrue()
    }

    @Test
    fun `updatePackageList should succeed`() = runTest {
        // When: 更新包列表
        val result = termuxBridge.updatePackageList()

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `installPackage should install specified package`() = runTest {
        // Given: 要安装的包（选择一个小型包）
        val packageName = "cowsay"

        // When: 安装包
        val result = termuxBridge.installPackage(packageName)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        assertThat(termuxBridge.isPackageInstalled(packageName)).isTrue()
    }

    @Test
    fun `installPackages should install multiple packages`() = runTest {
        // Given: 要安装的包列表
        val packageNames = listOf("sl", "fortune")

        // When: 批量安装包
        val result = termuxBridge.installPackages(packageNames)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        packageNames.forEach { packageName ->
            assertThat(termuxBridge.isPackageInstalled(packageName)).isTrue()
        }
    }

    @Test
    fun `uninstallPackage should remove installed package`() = runTest {
        // Given: 已安装的包
        val packageName = "cowsay"
        termuxBridge.installPackage(packageName)

        // When: 卸载包
        val result = termuxBridge.uninstallPackage(packageName)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        assertThat(termuxBridge.isPackageInstalled(packageName)).isFalse()
    }

    // ==================== 环境变量管理测试 ====================

    @Test
    fun `getEnvVar should return existing environment variable`() = runTest {
        // Given: 已知存在的环境变量
        val varName = "HOME"

        // When: 获取环境变量
        val result = termuxBridge.getEnvVar(varName)

        // Then: 应该返回值
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNotEmpty()
    }

    @Test
    fun `getEnvVar should return null for non-existing variable`() = runTest {
        // Given: 不存在的环境变量
        val varName = "NONEXISTENT_VAR_XYZ123"

        // When: 获取环境变量
        val result = termuxBridge.getEnvVar(varName)

        // Then: 应该返回null
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `setEnvVar should set new environment variable`() = runTest {
        // Given: 环境变量名和值
        val varName = "TEST_VAR_INTEGRATION"
        val varValue = "test_value_123"

        // When: 设置环境变量
        val result = termuxBridge.setEnvVar(varName, varValue)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()

        // 验证设置成功
        val getResult = termuxBridge.getEnvVar(varName)
        assertThat(getResult.getOrNull()).isEqualTo(varValue)
    }

    @Test
    fun `unsetEnvVar should remove environment variable`() = runTest {
        // Given: 已设置的环境变量
        val varName = "TEST_VAR_TO_UNSET"
        termuxBridge.setEnvVar(varName, "temporary")

        // When: 删除环境变量
        val result = termuxBridge.unsetEnvVar(varName)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()

        // 验证已删除
        val getResult = termuxBridge.getEnvVar(varName)
        assertThat(getResult.getOrNull()).isNull()
    }

    @Test
    fun `getAllEnvVars should return all environment variables`() = runTest {
        // When: 获取所有环境变量
        val result = termuxBridge.getAllEnvVars()

        // Then: 应该返回非空的映射
        assertThat(result.isSuccess).isTrue()
        val envVars = result.getOrThrow()
        assertThat(envVars).isNotEmpty()
        assertThat(envVars).containsKey("PATH")
        assertThat(envVars).containsKey("HOME")
        assertThat(envVars).containsKey("PREFIX")
    }

    @Test
    fun `setEnvVars should set multiple environment variables`() = runTest {
        // Given: 多个环境变量
        val envVars = mapOf(
            "TEST_VAR_1" to "value1",
            "TEST_VAR_2" to "value2",
            "TEST_VAR_3" to "value3"
        )

        // When: 批量设置环境变量
        val result = termuxBridge.setEnvVars(envVars)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()

        // 验证所有变量都已设置
        envVars.forEach { (name, value) ->
            val getResult = termuxBridge.getEnvVar(name)
            assertThat(getResult.getOrNull()).isEqualTo(value)
        }
    }

    // ==================== TermuxConstants验证测试 ====================

    @Test
    fun `TermuxConstants paths should be valid`() = runTest {
        // When: 检查Termux常量路径是否存在
        val prefixExists = termuxBridge.fileExists(TermuxConstants.TERMUX_PREFIX)
        val homeExists = termuxBridge.fileExists(TermuxConstants.TERMUX_HOME)
        val binExists = termuxBridge.fileExists(TermuxConstants.TERMUX_BIN)

        // Then: 核心路径应该存在
        assertThat(prefixExists).isTrue()
        assertThat(homeExists).isTrue()
        assertThat(binExists).isTrue()
    }

    @Test
    fun `TermuxConstants shell path should be executable`() = runTest {
        // When: 检查shell是否可执行
        val result = termuxBridge.executeCommand("test -x ${TermuxConstants.TERMUX_SHELL}")

        // Then: 应该可执行
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().isSuccess).isTrue()
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `readFile should fail for non-existing file`() = runTest {
        // Given: 不存在的文件
        val filePath = "$testDirectory/nonexistent-file.txt"

        // When: 尝试读取
        val result = termuxBridge.readFile(filePath)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `listDirectory should fail for non-existing directory`() = runTest {
        // Given: 不存在的目录
        val dirPath = "$testDirectory/nonexistent-dir"

        // When: 尝试列出目录
        val result = termuxBridge.listDirectory(dirPath)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `executeCommand should handle command with special characters`() = runTest {
        // Given: 包含特殊字符的命令
        val command = "echo 'Test with \$pecial char\$ and \"quotes\"'"

        // When: 执行命令
        val result = termuxBridge.executeCommand(command)

        // Then: 应该成功处理
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().stdout).contains("Test")
    }
}