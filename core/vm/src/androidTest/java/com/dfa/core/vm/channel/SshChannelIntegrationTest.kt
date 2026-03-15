package com.dfa.core.vm.channel

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationState
import com.google.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SSH通道集成测试
 *
 * 测试SSH连接建立、命令执行和文件传输功能
 * 需要在有SSH服务器的环境中运行
 *
 * 测试覆盖范围：
 * - SSH连接建立
 * - 命令执行
 * - 文件传输
 * - 端口转发
 * - Shell会话
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class SshChannelIntegrationTest {

    // SSH通道实例
    private lateinit var sshChannel: SshChannel

    // 测试用的SSH配置
    private val testHost = "127.0.0.1"
    private val testPort = 22
    private val testUsername = "testuser"
    private val testPassword = "testpassword"

    // 测试用的远程目录
    private val remoteTestDir = "/tmp/ssh-integration-test-${System.currentTimeMillis()}"

    @Before
    fun setup() = runTest {
        // 初始化SSH通道
        // 实际实现中应该通过依赖注入获取
        // sshChannel = SshChannelImpl(SshConfig(...))

        // 创建远程测试目录
        if (sshChannel.isConnected()) {
            sshChannel.executeCommand("mkdir -p $remoteTestDir")
        }
    }

    @After
    fun tearDown() = runTest {
        // 清理远程测试目录
        try {
            if (sshChannel.isConnected()) {
                sshChannel.executeCommand("rm -rf $remoteTestDir")
                sshChannel.disconnect()
            }
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }

    // ==================== SSH连接建立测试 ====================

    @Test
    fun `connect should establish SSH connection`() = runTest {
        // Given: 有效的SSH配置
        val config = ChannelConfig(
            type = ChannelType.SSH,
            host = testHost,
            port = testPort,
            username = testUsername,
            password = testPassword
        )

        // When: 连接SSH
        val result = sshChannel.connect(config)

        // Then: 应该成功连接
        assertThat(result.isSuccess).isTrue()
        assertThat(sshChannel.isConnected()).isTrue()
    }

    @Test
    fun `connect should update state to CONNECTED`() = runTest {
        // Given: 有效的SSH配置
        val config = ChannelConfig(
            type = ChannelType.SSH,
            host = testHost,
            port = testPort,
            username = testUsername,
            password = testPassword
        )

        // When: 连接SSH
        sshChannel.connect(config)

        // Then: 状态应该变为CONNECTED
        val state = sshChannel.state.first()
        assertThat(state).isEqualTo(CommunicationState.CONNECTED)
    }

    @Test
    fun `connect should fail with invalid credentials`() = runTest {
        // Given: 无效的凭据
        val config = ChannelConfig(
            type = ChannelType.SSH,
            host = testHost,
            port = testPort,
            username = "invalid_user",
            password = "invalid_password"
        )

        // When: 尝试连接
        val result = sshChannel.connect(config)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `connect should fail with unreachable host`() = runTest {
        // Given: 不可达的主机
        val config = ChannelConfig(
            type = ChannelType.SSH,
            host = "192.168.255.255", // 不可达地址
            port = testPort,
            username = testUsername,
            password = testPassword,
            connectionTimeoutMs = 5000
        )

        // When: 尝试连接
        val result = sshChannel.connect(config)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `disconnect should close SSH connection`() = runTest {
        // Given: 已建立的SSH连接
        establishConnection()

        // When: 断开连接
        val result = sshChannel.disconnect()

        // Then: 应该成功断开
        assertThat(result.isSuccess).isTrue()
        assertThat(sshChannel.isConnected()).isFalse()
    }

    @Test
    fun `disconnect should update state to DISCONNECTED`() = runTest {
        // Given: 已建立的SSH连接
        establishConnection()

        // When: 断开连接
        sshChannel.disconnect()

        // Then: 状态应该变为DISCONNECTED
        val state = sshChannel.state.first()
        assertThat(state).isEqualTo(CommunicationState.DISCONNECTED)
    }

    @Test
    fun `isServerReachable should return true for available server`() = runTest {
        // When: 检查服务器是否可达
        val isReachable = sshChannel.isServerReachable()

        // Then: 应该返回true
        assertThat(isReachable).isTrue()
    }

    @Test
    fun `isServerReachable should return false for unavailable server`() = runTest {
        // Given: 不可达的服务器
        // 需要创建一个指向不可达服务器的通道
        // 这里假设sshChannel已配置为不可达服务器

        // When: 检查服务器是否可达
        val isReachable = sshChannel.isServerReachable(timeoutMs = 1000)

        // Then: 应该返回false
        assertThat(isReachable).isFalse()
    }

    @Test
    fun `getServerFingerprint should return valid fingerprint`() = runTest {
        // Given: 已建立的连接
        establishConnection()

        // When: 获取服务器指纹
        val result = sshChannel.getServerFingerprint()

        // Then: 应该返回有效的指纹
        assertThat(result.isSuccess).isTrue()
        val fingerprint = result.getOrThrow()
        assertThat(fingerprint.host).isEqualTo(testHost)
        assertThat(fingerprint.port).isEqualTo(testPort)
        assertThat(fingerprint.fingerprint).isNotEmpty()
    }

    @Test
    fun `getSshConnectionInfo should return connection details`() = runTest {
        // Given: 已建立的连接
        establishConnection()

        // When: 获取连接信息
        val info = sshChannel.getSshConnectionInfo()

        // Then: 应该返回正确的信息
        assertThat(info.host).isEqualTo(testHost)
        assertThat(info.port).isEqualTo(testPort)
        assertThat(info.username).isEqualTo(testUsername)
    }

    @Test
    fun `sessionState should reflect connection status`() = runTest {
        // Given: 已建立的连接
        establishConnection()

        // When: 获取会话状态
        val sessionState = sshChannel.sessionState.first()

        // Then: 应该反映已连接状态
        assertThat(sessionState.isConnected).isTrue()
        assertThat(sessionState.isAuthenticated).isTrue()
    }

    // ==================== 命令执行测试 ====================

    @Test
    fun `executeCommand should return success for simple command`() = runTest {
        // Given: 已建立的连接
        establishConnection()

        // When: 执行简单命令
        val result = sshChannel.executeCommand("echo 'Hello SSH'")

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        val cmdResult = result.getOrThrow()
        assertThat(cmdResult.isSuccess).isTrue()
        assertThat(cmdResult.stdout).contains("Hello SSH")
    }

    @Test
    fun `executeCommand should return correct exit code`() = runTest {
        // Given: 已建立的连接
        establishConnection()

        // When: 执行会失败的命令
        val result = sshChannel.executeCommand("exit 42")

        // Then: 应该返回正确的退出码
        assertThat(result.isSuccess).isTrue()
        val cmdResult = result.getOrThrow()
        assertThat(cmdResult.exitCode).isEqualTo(42)
    }

    @Test
    fun `executeCommand should capture stdout correctly`() = runTest {
        // Given: 已建立的连接
        establishConnection()

        // When: 执行输出到stdout的命令
        val result = sshChannel.executeCommand("ls /")

        // Then: 应该捕获输出
        assertThat(result.isSuccess).isTrue()
        val cmdResult = result.getOrThrow()
        assertThat(cmdResult.stdout).isNotEmpty()
        assertThat(cmdResult.stdout).contains("bin")
        assertThat(cmdResult.stdout).contains("etc")
    }

    @Test
    fun `executeCommand should capture stderr correctly`() = runTest {
        // Given: 已建立的连接
        establishConnection()

        // When: 执行输出到stderr的命令
        val result = sshChannel.executeCommand("ls /nonexistent_directory_xyz 2>&1")

        // Then: 应该捕获错误输出
        assertThat(result.isSuccess).isTrue()
        val cmdResult = result.getOrThrow()
        assertThat(cmdResult.stderr.isNotEmpty() || cmdResult.stdout.contains("No such file")).isTrue()
    }

    @Test
    fun `executeCommand with environment should pass env vars`() = runTest {
        // Given: 已建立的连接和环境变量
        establishConnection()
        val env = mapOf("TEST_VAR" to "test_value")

        // When: 执行使用环境变量的命令
        val result = sshChannel.executeCommand("echo \$TEST_VAR", env)

        // Then: 应该输出环境变量的值
        assertThat(result.isSuccess).isTrue()
        val cmdResult = result.getOrThrow()
        assertThat(cmdResult.stdout.trim()).isEqualTo("test_value")
    }

    @Test
    fun `executeCommand should respect timeout`() = runTest {
        // Given: 已建立的连接和短超时
        establishConnection()
        val timeoutMs = 1000L

        // When: 执行长时间运行的命令
        val result = sshChannel.executeCommand("sleep 10", timeoutMs)

        // Then: 应该超时或返回错误
        assertThat(result.isFailure || result.getOrNull()?.isSuccess == false).isTrue()
    }

    @Test
    fun `executeInteractiveCommand should handle input`() = runTest {
        // Given: 已建立的连接
        establishConnection()
        val input = kotlinx.coroutines.flow.flowOf("test input\n".toByteArray())

        // When: 执行交互式命令
        val result = sshChannel.executeInteractiveCommand("cat", input)

        // Then: 应该处理输入
        assertThat(result.isSuccess).isTrue()
        val cmdResult = result.getOrThrow()
        assertThat(cmdResult.stdout).contains("test input")
    }

    @Test
    fun `executeCommand should handle complex commands`() = runTest {
        // Given: 已建立的连接
        establishConnection()

        // When: 执行复杂命令（管道、重定向）
        val result = sshChannel.executeCommand("echo 'line1\nline2\nline3' | grep line2")

        // Then: 应该正确处理
        assertThat(result.isSuccess).isTrue()
        val cmdResult = result.getOrThrow()
        assertThat(cmdResult.stdout).contains("line2")
        assertThat(cmdResult.stdout).doesNotContain("line1")
    }

    @Test
    fun `executeCommand should fail when not connected`() = runTest {
        // Given: 未连接的通道

        // When: 尝试执行命令
        val result = sshChannel.executeCommand("echo test")

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    // ==================== 文件传输测试 ====================

    @Test
    fun `uploadFile should transfer local file to remote`() = runTest {
        // Given: 已建立的连接和本地文件
        establishConnection()
        val localPath = "/tmp/test-upload-${System.currentTimeMillis()}.txt"
        val remotePath = "$remoteTestDir/uploaded.txt"
        val content = "Test upload content"
        java.io.File(localPath).writeText(content)

        // When: 上传文件
        val result = sshChannel.uploadFile(localPath, remotePath)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        val transferResult = result.getOrThrow()
        assertThat(transferResult.bytesTransferred).isGreaterThan(0)

        // 清理本地文件
        java.io.File(localPath).delete()
    }

    @Test
    fun `uploadFile with bytes should transfer data to remote`() = runTest {
        // Given: 已建立的连接和数据
        establishConnection()
        val data = "Direct byte upload content".toByteArray()
        val remotePath = "$remoteTestDir/bytes-upload.txt"

        // When: 上传字节数据
        val result = sshChannel.uploadFile(data, remotePath)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().bytesTransferred).isEqualTo(data.size.toLong())
    }

    @Test
    fun `uploadFile should report progress`() = runTest {
        // Given: 已建立的连接
        establishConnection()
        val data = ByteArray(1024 * 10) { it.toByte() } // 10KB
        val remotePath = "$remoteTestDir/progress-test.bin"
        var progressCalled = false

        // When: 上传文件并监听进度
        val result = sshChannel.uploadFile(data, remotePath) { transferred, total ->
            progressCalled = true
            assertThat(transferred).isAtMost(total)
        }

        // Then: 应该调用进度回调
        assertThat(result.isSuccess).isTrue()
        assertThat(progressCalled).isTrue()
    }

    @Test
    fun `downloadFile should transfer remote file to local`() = runTest {
        // Given: 已建立的连接和远程文件
        establishConnection()
        val remotePath = "$remoteTestDir/download-test.txt"
        val localPath = "/tmp/test-download-${System.currentTimeMillis()}.txt"
        val content = "Download test content"
        sshChannel.executeCommand("echo '$content' > $remotePath")

        // When: 下载文件
        val result = sshChannel.downloadFile(remotePath, localPath)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        assertThat(java.io.File(localPath).exists()).isTrue()

        // 清理本地文件
        java.io.File(localPath).delete()
    }

    @Test
    fun `downloadFile to bytes should return file data`() = runTest {
        // Given: 已建立的连接和远程文件
        establishConnection()
        val remotePath = "$remoteTestDir/bytes-download.txt"
        val content = "Bytes download content"
        sshChannel.executeCommand("echo -n '$content' > $remotePath")

        // When: 下载文件到字节数组
        val result = sshChannel.downloadFile(remotePath)

        // Then: 应该返回正确的数据
        assertThat(result.isSuccess).isTrue()
        val downloadResult = result.getOrThrow()
        assertThat(String(downloadResult.data)).isEqualTo(content)
    }

    @Test
    fun `downloadFile should report progress`() = runTest {
        // Given: 已建立的连接和远程文件
        establishConnection()
        val remotePath = "$remoteTestDir/progress-download.bin"
        sshChannel.executeCommand("dd if=/dev/zero of=$remotePath bs=1024 count=10")
        var progressCalled = false

        // When: 下载文件并监听进度
        val result = sshChannel.downloadFile(remotePath) { transferred, total ->
            progressCalled = true
        }

        // Then: 应该调用进度回调
        assertThat(result.isSuccess).isTrue()
        assertThat(progressCalled).isTrue()
    }

    @Test
    fun `downloadFile should fail for non-existing file`() = runTest {
        // Given: 已建立的连接和不存在的文件
        establishConnection()
        val remotePath = "$remoteTestDir/nonexistent-file.txt"

        // When: 尝试下载
        val result = sshChannel.downloadFile(remotePath)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    // ==================== 端口转发测试 ====================

    @Test
    fun `createLocalTunnel should create local port forward`() = runTest {
        // Given: 已建立的连接
        establishConnection()
        val localPort = 18080
        val remoteHost = "127.0.0.1"
        val remotePort = 80

        // When: 创建本地端口转发
        val result = sshChannel.createLocalTunnel(localPort, remoteHost, remotePort)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        val tunnel = result.getOrThrow()
        assertThat(tunnel.type).isEqualTo(SshTunnelType.LOCAL)
        assertThat(tunnel.localPort).isEqualTo(localPort)
        assertThat(tunnel.remotePort).isEqualTo(remotePort)

        // 清理隧道
        sshChannel.closeTunnel(tunnel.tunnelId)
    }

    @Test
    fun `createRemoteTunnel should create remote port forward`() = runTest {
        // Given: 已建立的连接
        establishConnection()
        val remotePort = 13000
        val localHost = "127.0.0.1"
        val localPort = 3000

        // When: 创建远程端口转发
        val result = sshChannel.createRemoteTunnel(remotePort, localHost, localPort)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        val tunnel = result.getOrThrow()
        assertThat(tunnel.type).isEqualTo(SshTunnelType.REMOTE)

        // 清理隧道
        sshChannel.closeTunnel(tunnel.tunnelId)
    }

    @Test
    fun `createDynamicTunnel should create SOCKS proxy`() = runTest {
        // Given: 已建立的连接
        establishConnection()
        val localPort = 11080

        // When: 创建动态端口转发
        val result = sshChannel.createDynamicTunnel(localPort)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        val tunnel = result.getOrThrow()
        assertThat(tunnel.type).isEqualTo(SshTunnelType.DYNAMIC)
        assertThat(tunnel.localPort).isEqualTo(localPort)

        // 清理隧道
        sshChannel.closeTunnel(tunnel.tunnelId)
    }

    @Test
    fun `closeTunnel should close existing tunnel`() = runTest {
        // Given: 已创建的隧道
        establishConnection()
        val tunnelResult = sshChannel.createLocalTunnel(18081, "127.0.0.1", 80)
        val tunnelId = tunnelResult.getOrThrow().tunnelId

        // When: 关闭隧道
        val result = sshChannel.closeTunnel(tunnelId)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()

        // 验证隧道已关闭
        val activeTunnels = sshChannel.getActiveTunnels()
        assertThat(activeTunnels.none { it.tunnelId == tunnelId }).isTrue()
    }

    @Test
    fun `getActiveTunnels should return all active tunnels`() = runTest {
        // Given: 已建立的连接和多个隧道
        establishConnection()
        sshChannel.createLocalTunnel(18082, "127.0.0.1", 80)
        sshChannel.createDynamicTunnel(11082)

        // When: 获取活动隧道
        val tunnels = sshChannel.getActiveTunnels()

        // Then: 应该返回所有隧道
        assertThat(tunnels).hasSize(2)
        assertThat(tunnels.all { it.isActive }).isTrue()

        // 清理
        tunnels.forEach { sshChannel.closeTunnel(it.tunnelId) }
    }

    // ==================== Shell会话测试 ====================

    @Test
    fun `createShell should create interactive shell session`() = runTest {
        // Given: 已建立的连接
        establishConnection()

        // When: 创建Shell会话
        val result = sshChannel.createShell("xterm-256color", 80, 24)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        val shell = result.getOrThrow()
        assertThat(shell.isOpen).isTrue()
        assertThat(shell.terminalType).isEqualTo("xterm-256color")

        // 清理
        shell.close()
    }

    @Test
    fun `SshShell sendInput should send data to shell`() = runTest {
        // Given: 已创建的Shell会话
        establishConnection()
        val shellResult = sshChannel.createShell()
        val shell = shellResult.getOrThrow()

        // When: 发送输入
        val result = shell.sendInput("echo test\n")

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()

        // 清理
        shell.close()
    }

    @Test
    fun `SshShell resize should change terminal size`() = runTest {
        // Given: 已创建的Shell会话
        establishConnection()
        val shellResult = sshChannel.createShell()
        val shell = shellResult.getOrThrow()

        // When: 调整终端大小
        val result = shell.resize(120, 40)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()

        // 清理
        shell.close()
    }

    @Test
    fun `SshShell output should receive shell output`() = runTest {
        // Given: 已创建的Shell会话
        establishConnection()
        val shellResult = sshChannel.createShell()
        val shell = shellResult.getOrThrow()

        // When: 发送命令并接收输出
        shell.sendInput("echo test_output\n")
        val output = shell.output.first()

        // Then: 应该收到输出
        assertThat(output).isNotEmpty()
        assertThat(String(output)).contains("test_output")

        // 清理
        shell.close()
    }

    // ==================== 连接恢复测试 ====================

    @Test
    fun `reconnect should restore connection after disconnect`() = runTest {
        // Given: 已断开的连接
        establishConnection()
        sshChannel.disconnect()

        // When: 重新连接
        val config = ChannelConfig(
            type = ChannelType.SSH,
            host = testHost,
            port = testPort,
            username = testUsername,
            password = testPassword
        )
        val result = sshChannel.connect(config)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
        assertThat(sshChannel.isConnected()).isTrue()
    }

    @Test
    fun `channelId should be unique`() {
        // When: 获取通道ID
        val channelId = sshChannel.channelId

        // Then: 应该是唯一的
        assertThat(channelId).isNotEmpty()
    }

    @Test
    fun `channelType should be SSH`() {
        // When: 获取通道类型
        val channelType = sshChannel.channelType

        // Then: 应该是SSH类型
        assertThat(channelType).isEqualTo(ChannelType.SSH)
    }

    // ==================== 辅助方法 ====================

    /**
     * 建立SSH连接
     */
    private suspend fun establishConnection() {
        val config = ChannelConfig(
            type = ChannelType.SSH,
            host = testHost,
            port = testPort,
            username = testUsername,
            password = testPassword
        )
        sshChannel.connect(config)
    }
}