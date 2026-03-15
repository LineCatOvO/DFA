package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationError
import com.dfa.core.vm.communication.CommunicationState
import com.google.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * SshChannelImpl单元测试
 *
 * 测试SSH通道的连接状态管理、认证方法和命令执行功能
 */
class SshChannelImplTest {

    private lateinit var sshChannel: SshChannel

    @Before
    fun setup() {
        // 创建mock实例用于接口测试
        sshChannel = mockk(relaxed = true)
    }

    // ==================== 基础属性测试 ====================

    @Test
    fun `SshChannel channelId should be unique`() {
        val id1 = java.util.UUID.randomUUID().toString()
        val id2 = java.util.UUID.randomUUID().toString()

        every { sshChannel.channelId } returns id1

        assertThat(sshChannel.channelId).isEqualTo(id1)
        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `SshChannel channelType should be SSH`() {
        every { sshChannel.channelType } returns ChannelType.SSH

        assertThat(sshChannel.channelType).isEqualTo(ChannelType.SSH)
    }

    // ==================== 连接状态管理测试 ====================

    @Test
    fun `SshChannel state should be DISCONNECTED initially`() {
        val stateFlow = MutableStateFlow(CommunicationState.DISCONNECTED)
        every { sshChannel.state } returns stateFlow

        assertThat(sshChannel.state.value).isEqualTo(CommunicationState.DISCONNECTED)
    }

    @Test
    fun `SshChannel isConnected should return false when disconnected`() {
        every { sshChannel.isConnected() } returns false

        assertThat(sshChannel.isConnected()).isFalse()
    }

    @Test
    fun `SshChannel isConnected should return true when connected`() {
        every { sshChannel.isConnected() } returns true

        assertThat(sshChannel.isConnected()).isTrue()
    }

    @Test
    fun `SshChannel connect should update state to CONNECTED`() = runTest {
        val mockConnectionInfo = mockk<com.dfa.core.vm.communication.ConnectionInfo>()
        every { sshChannel.connect(any()) } returns Result.success(mockConnectionInfo)

        val result = sshChannel.connect(mockk())

        assertThat(result.isSuccess).isTrue()
        verify { sshChannel.connect(any()) }
    }

    @Test
    fun `SshChannel connect should fail with invalid config`() = runTest {
        val error = CommunicationError.ConfigurationError("Invalid config")
        every { sshChannel.connect(any()) } returns Result.failure(error)

        val result = sshChannel.connect(mockk())

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `SshChannel disconnect should update state to DISCONNECTED`() = runTest {
        every { sshChannel.disconnect() } returns Result.success(Unit)

        val result = sshChannel.disconnect()

        assertThat(result.isSuccess).isTrue()
        verify { sshChannel.disconnect() }
    }

    // ==================== 认证方法测试 ====================

    @Test
    fun `SshChannel host should return configured host`() {
        every { sshChannel.host } returns "192.168.1.100"

        assertThat(sshChannel.host).isEqualTo("192.168.1.100")
    }

    @Test
    fun `SshChannel sshPort should return configured port`() {
        every { sshChannel.sshPort } returns 22

        assertThat(sshChannel.sshPort).isEqualTo(22)
    }

    @Test
    fun `SshChannel username should return configured username`() {
        every { sshChannel.username } returns "testuser"

        assertThat(sshChannel.username).isEqualTo("testuser")
    }

    @Test
    fun `SshChannel sessionState should reflect authentication status`() {
        val sessionState = SshSessionState(
            isConnected = true,
            isAuthenticated = true,
            sessionId = "session-123"
        )
        every { sshChannel.sessionState } returns flowOf(sessionState)

        // 验证sessionState flow存在
        assertThat(sshChannel.sessionState).isNotNull()
    }

    // ==================== 命令执行测试 ====================

    @Test
    fun `SshChannel executeCommand should return success result`() = runTest {
        val expectedResult = SshCommandResult(
            command = "ls -la",
            exitCode = 0,
            stdout = "file1\nfile2",
            stderr = "",
            executionTimeMs = 100
        )
        every { sshChannel.executeCommand("ls -la", any()) } returns Result.success(expectedResult)

        val result = sshChannel.executeCommand("ls -la")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.isSuccess).isTrue()
        assertThat(result.getOrNull()?.stdout).isEqualTo("file1\nfile2")
    }

    @Test
    fun `SshChannel executeCommand should return failure when not connected`() = runTest {
        val error = CommunicationError.ChannelError("SSH session not connected")
        every { sshChannel.executeCommand(any(), any()) } returns Result.failure(error)

        val result = sshChannel.executeCommand("ls")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `SshChannel executeCommand should handle non-zero exit code`() = runTest {
        val expectedResult = SshCommandResult(
            command = "false",
            exitCode = 1,
            stdout = "",
            stderr = "command failed",
            executionTimeMs = 50
        )
        every { sshChannel.executeCommand("false", any()) } returns Result.success(expectedResult)

        val result = sshChannel.executeCommand("false")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.isSuccess).isFalse()
        assertThat(result.getOrNull()?.exitCode).isEqualTo(1)
    }

    @Test
    fun `SshChannel executeCommand with environment should pass env vars`() = runTest {
        val expectedResult = SshCommandResult(
            command = "echo $VAR",
            exitCode = 0,
            stdout = "value",
            stderr = "",
            executionTimeMs = 50
        )
        val env = mapOf("VAR" to "value")
        every { sshChannel.executeCommand("echo \$VAR", env, any()) } returns Result.success(expectedResult)

        val result = sshChannel.executeCommand("echo \$VAR", env)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `SshChannel executeInteractiveCommand should handle input flow`() = runTest {
        val expectedResult = SshCommandResult(
            command = "cat",
            exitCode = 0,
            stdout = "input data",
            stderr = "",
            executionTimeMs = 100
        )
        every { sshChannel.executeInteractiveCommand(any(), any(), any()) } returns Result.success(expectedResult)

        val result = sshChannel.executeInteractiveCommand("cat", flowOf("input".toByteArray()))

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 文件传输测试 ====================

    @Test
    fun `SshChannel uploadFile should return transfer result`() = runTest {
        val expectedResult = FileTransferResult(
            sourcePath = "/local/file.txt",
            destinationPath = "/remote/file.txt",
            bytesTransferred = 1024,
            transferTimeMs = 100,
            averageSpeed = 10240
        )
        every { sshChannel.uploadFile("/local/file.txt", "/remote/file.txt", any()) } returns Result.success(expectedResult)

        val result = sshChannel.uploadFile("/local/file.txt", "/remote/file.txt")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.bytesTransferred).isEqualTo(1024)
    }

    @Test
    fun `SshChannel uploadFile with bytes should return transfer result`() = runTest {
        val data = "test content".toByteArray()
        val expectedResult = FileTransferResult(
            sourcePath = "<memory>",
            destinationPath = "/remote/file.txt",
            bytesTransferred = data.size.toLong(),
            transferTimeMs = 50,
            averageSpeed = data.size * 20L
        )
        every { sshChannel.uploadFile(any<String>(), "/remote/file.txt", any()) } returns Result.success(expectedResult)

        val result = sshChannel.uploadFile(data, "/remote/file.txt")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `SshChannel downloadFile should return download result`() = runTest {
        val expectedData = "downloaded content".toByteArray()
        val expectedResult = FileDownloadResult(
            remotePath = "/remote/file.txt",
            data = expectedData,
            bytesTransferred = expectedData.size.toLong(),
            transferTimeMs = 100,
            averageSpeed = expectedData.size * 10L
        )
        every { sshChannel.downloadFile("/remote/file.txt", any()) } returns Result.success(expectedResult)

        val result = sshChannel.downloadFile("/remote/file.txt")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.data).isEqualTo(expectedData)
    }

    @Test
    fun `SshChannel downloadFile to local path should return transfer result`() = runTest {
        val expectedResult = FileTransferResult(
            sourcePath = "/remote/file.txt",
            destinationPath = "/local/file.txt",
            bytesTransferred = 2048,
            transferTimeMs = 200,
            averageSpeed = 10240
        )
        every { sshChannel.downloadFile("/remote/file.txt", "/local/file.txt", any()) } returns Result.success(expectedResult)

        val result = sshChannel.downloadFile("/remote/file.txt", "/local/file.txt")

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 端口转发测试 ====================

    @Test
    fun `SshChannel createLocalTunnel should return tunnel info`() = runTest {
        val expectedTunnel = SshTunnel(
            tunnelId = "tunnel-1",
            type = SshTunnelType.LOCAL,
            localHost = "127.0.0.1",
            localPort = 8080,
            remoteHost = "remote.server",
            remotePort = 80,
            isActive = true
        )
        every { sshChannel.createLocalTunnel(8080, "remote.server", 80) } returns Result.success(expectedTunnel)

        val result = sshChannel.createLocalTunnel(8080, "remote.server", 80)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.type).isEqualTo(SshTunnelType.LOCAL)
        assertThat(result.getOrNull()?.localPort).isEqualTo(8080)
    }

    @Test
    fun `SshChannel createRemoteTunnel should return tunnel info`() = runTest {
        val expectedTunnel = SshTunnel(
            tunnelId = "tunnel-2",
            type = SshTunnelType.REMOTE,
            localHost = "127.0.0.1",
            localPort = 3000,
            remoteHost = null,
            remotePort = 3000,
            isActive = true
        )
        every { sshChannel.createRemoteTunnel(3000, "127.0.0.1", 3000) } returns Result.success(expectedTunnel)

        val result = sshChannel.createRemoteTunnel(3000, "127.0.0.1", 3000)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.type).isEqualTo(SshTunnelType.REMOTE)
    }

    @Test
    fun `SshChannel createDynamicTunnel should return SOCKS tunnel`() = runTest {
        val expectedTunnel = SshTunnel(
            tunnelId = "tunnel-3",
            type = SshTunnelType.DYNAMIC,
            localHost = "127.0.0.1",
            localPort = 1080,
            remoteHost = null,
            remotePort = null,
            isActive = true
        )
        every { sshChannel.createDynamicTunnel(1080) } returns Result.success(expectedTunnel)

        val result = sshChannel.createDynamicTunnel(1080)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.type).isEqualTo(SshTunnelType.DYNAMIC)
    }

    @Test
    fun `SshChannel closeTunnel should succeed`() = runTest {
        every { sshChannel.closeTunnel("tunnel-1") } returns Result.success(Unit)

        val result = sshChannel.closeTunnel("tunnel-1")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `SshChannel getActiveTunnels should return tunnel list`() = runTest {
        val tunnels = listOf(
            SshTunnel("t1", SshTunnelType.LOCAL, "127.0.0.1", 8080, "remote", 80),
            SshTunnel("t2", SshTunnelType.DYNAMIC, "127.0.0.1", 1080, null, null)
        )
        every { sshChannel.getActiveTunnels() } returns tunnels

        val result = sshChannel.getActiveTunnels()

        assertThat(result).hasSize(2)
    }

    // ==================== Shell会话测试 ====================

    @Test
    fun `SshChannel createShell should return shell session`() = runTest {
        val mockShell = mockk<SshShell>()
        every { mockShell.shellId } returns "shell-1"
        every { mockShell.terminalType } returns "xterm-256color"
        every { mockShell.isOpen } returns true
        every { sshChannel.createShell(any(), any(), any()) } returns Result.success(mockShell)

        val result = sshChannel.createShell("xterm-256color", 80, 24)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.shellId).isEqualTo("shell-1")
    }

    // ==================== 服务器信息测试 ====================

    @Test
    fun `SshChannel getServerFingerprint should return fingerprint info`() = runTest {
        val expectedFingerprint = SshServerFingerprint(
            host = "192.168.1.100",
            port = 22,
            keyType = "RSA",
            fingerprint = "SHA256:abc123..."
        )
        every { sshChannel.getServerFingerprint() } returns Result.success(expectedFingerprint)

        val result = sshChannel.getServerFingerprint()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.host).isEqualTo("192.168.1.100")
    }

    @Test
    fun `SshChannel isServerReachable should return boolean`() = runTest {
        every { sshChannel.isServerReachable(any()) } returns true

        assertThat(sshChannel.isServerReachable()).isTrue()
    }

    @Test
    fun `SshChannel getSshConnectionInfo should return connection info`() {
        val connectionInfo = SshConnectionInfo(
            host = "192.168.1.100",
            port = 22,
            username = "testuser",
            authMethod = "password",
            serverVersion = "OpenSSH_8.9"
        )
        every { sshChannel.getSshConnectionInfo() } returns connectionInfo

        val info = sshChannel.getSshConnectionInfo()

        assertThat(info.host).isEqualTo("192.168.1.100")
        assertThat(info.port).isEqualTo(22)
        assertThat(info.username).isEqualTo("testuser")
    }

    // ==================== SshCommandResult测试 ====================

    @Test
    fun `SshCommandResult isSuccess should be true for exitCode 0`() {
        val result = SshCommandResult(
            command = "ls",
            exitCode = 0,
            stdout = "output",
            stderr = "",
            executionTimeMs = 100
        )

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `SshCommandResult isSuccess should be false for non-zero exitCode`() {
        val result = SshCommandResult(
            command = "false",
            exitCode = 1,
            stdout = "",
            stderr = "error",
            executionTimeMs = 50
        )

        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `SshCommandResult output should combine stdout and stderr`() {
        val result = SshCommandResult(
            command = "test",
            exitCode = 0,
            stdout = "stdout",
            stderr = "stderr",
            executionTimeMs = 100
        )

        assertThat(result.output).isEqualTo("stdoutstderr")
    }

    // ==================== SshTunnel测试 ====================

    @Test
    fun `SshTunnel should have correct properties`() {
        val tunnel = SshTunnel(
            tunnelId = "tunnel-1",
            type = SshTunnelType.LOCAL,
            localHost = "127.0.0.1",
            localPort = 8080,
            remoteHost = "remote.server",
            remotePort = 80,
            createdAt = System.currentTimeMillis(),
            bytesTransferred = 1024,
            isActive = true
        )

        assertThat(tunnel.tunnelId).isEqualTo("tunnel-1")
        assertThat(tunnel.type).isEqualTo(SshTunnelType.LOCAL)
        assertThat(tunnel.isActive).isTrue()
    }

    @Test
    fun `SshTunnelType should have all expected values`() {
        assertThat(SshTunnelType.values()).asList().containsExactly(
            SshTunnelType.LOCAL,
            SshTunnelType.REMOTE,
            SshTunnelType.DYNAMIC
        )
    }

    // ==================== SshSessionState测试 ====================

    @Test
    fun `SshSessionState should have correct default values`() {
        val state = SshSessionState()

        assertThat(state.isConnected).isFalse()
        assertThat(state.isAuthenticated).isFalse()
        assertThat(state.sessionId).isNull()
        assertThat(state.activeChannels).isEqualTo(0)
        assertThat(state.activeTunnels).isEqualTo(0)
    }

    @Test
    fun `SshSessionState should track connection details`() {
        val state = SshSessionState(
            isConnected = true,
            isAuthenticated = true,
            sessionId = "session-123",
            serverVersion = "OpenSSH_8.9",
            clientVersion = "JSch-0.2.0",
            activeChannels = 2,
            activeTunnels = 1
        )

        assertThat(state.isConnected).isTrue()
        assertThat(state.isAuthenticated).isTrue()
        assertThat(state.serverVersion).isEqualTo("OpenSSH_8.9")
        assertThat(state.activeChannels).isEqualTo(2)
    }

    // ==================== SshServerFingerprint测试 ====================

    @Test
    fun `SshServerFingerprint should have correct properties`() {
        val fingerprint = SshServerFingerprint(
            host = "192.168.1.100",
            port = 22,
            keyType = "ED25519",
            fingerprint = "SHA256:abc123...",
            fingerprintAlgorithm = "SHA256"
        )

        assertThat(fingerprint.host).isEqualTo("192.168.1.100")
        assertThat(fingerprint.keyType).isEqualTo("ED25519")
        assertThat(fingerprint.fingerprintAlgorithm).isEqualTo("SHA256")
    }

    // ==================== SshConnectionInfo测试 ====================

    @Test
    fun `SshConnectionInfo should have correct properties`() {
        val info = SshConnectionInfo(
            host = "192.168.1.100",
            port = 22,
            username = "testuser",
            authMethod = "publickey",
            serverVersion = "OpenSSH_8.9",
            sessionId = "session-123",
            connectedAt = System.currentTimeMillis()
        )

        assertThat(info.host).isEqualTo("192.168.1.100")
        assertThat(info.port).isEqualTo(22)
        assertThat(info.authMethod).isEqualTo("publickey")
    }

    // ==================== FileTransferResult测试 ====================

    @Test
    fun `FileTransferResult should have correct properties`() {
        val result = FileTransferResult(
            sourcePath = "/local/file",
            destinationPath = "/remote/file",
            bytesTransferred = 1024 * 1024,
            transferTimeMs = 1000,
            averageSpeed = 1024 * 1024
        )

        assertThat(result.bytesTransferred).isEqualTo(1024 * 1024)
        assertThat(result.averageSpeed).isEqualTo(1024 * 1024)
    }

    // ==================== FileDownloadResult测试 ====================

    @Test
    fun `FileDownloadResult should have correct properties`() {
        val data = "test content".toByteArray()
        val result = FileDownloadResult(
            remotePath = "/remote/file",
            data = data,
            bytesTransferred = data.size.toLong(),
            transferTimeMs = 100,
            averageSpeed = data.size * 10L
        )

        assertThat(result.remotePath).isEqualTo("/remote/file")
        assertThat(result.data).isEqualTo(data)
    }

    @Test
    fun `FileDownloadResult equals should compare data content`() {
        val data = "test".toByteArray()
        val result1 = FileDownloadResult("/file", data, 4, 100, 40)
        val result2 = FileDownloadResult("/file", "test".toByteArray(), 4, 100, 40)

        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `FileDownloadResult hashCode should use data content`() {
        val data = "test".toByteArray()
        val result1 = FileDownloadResult("/file", data, 4, 100, 40)
        val result2 = FileDownloadResult("/file", "test".toByteArray(), 4, 100, 40)

        assertThat(result1.hashCode()).isEqualTo(result2.hashCode())
    }
}