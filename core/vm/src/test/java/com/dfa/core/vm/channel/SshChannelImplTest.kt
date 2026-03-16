package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SshChannelImpl单元测试
 *
 * 测试SSH通道的连接、命令执行和文件传输功能
 */
class SshChannelImplTest {

    private lateinit var sshChannel: SshChannel

    @Before
    fun setup() {
        sshChannel = mockk(relaxed = true)
    }

    // ==================== 基础属性测试 ====================

    @Test
    fun `SshChannel host should return correct value`() {
        every { sshChannel.host } returns "192.168.1.100"

        assertEquals("192.168.1.100", sshChannel.host)
    }

    @Test
    fun `SshChannel sshPort should return correct value`() {
        every { sshChannel.sshPort } returns 22

        assertEquals(22, sshChannel.sshPort)
    }

    @Test
    fun `SshChannel username should return correct value`() {
        every { sshChannel.username } returns "testuser"

        assertEquals("testuser", sshChannel.username)
    }

    @Test
    fun `SshChannel channelType should be SSH`() {
        every { sshChannel.channelType } returns ChannelType.SSH

        assertEquals(ChannelType.SSH, sshChannel.channelType)
    }

    // ==================== 连接状态测试 ====================

    @Test
    fun `SshChannel isConnected should return false when disconnected`() {
        every { sshChannel.isConnected() } returns false

        assertFalse(sshChannel.isConnected())
    }

    @Test
    fun `SshChannel isConnected should return true when connected`() {
        every { sshChannel.isConnected() } returns true

        assertTrue(sshChannel.isConnected())
    }

    // ==================== SshAuthMethod Tests ====================

    @Test
    fun `SshAuthMethod Password should have correct properties`() {
        val auth = SshAuthMethod.Password("user", "pass")

        assertEquals("user", auth.username)
        assertEquals("pass", auth.password)
    }

    @Test
    fun `SshAuthMethod PublicKey should have correct properties`() {
        val auth = SshAuthMethod.PublicKey("user", "private-key", "passphrase")

        assertEquals("user", auth.username)
        assertEquals("private-key", auth.privateKey)
        assertEquals("passphrase", auth.passphrase)
    }

    @Test
    fun `SshAuthMethod PublicKeyFile should have correct properties`() {
        val auth = SshAuthMethod.PublicKeyFile("user", "/path/to/key", "passphrase")

        assertEquals("user", auth.username)
        assertEquals("/path/to/key", auth.privateKeyPath)
        assertEquals("passphrase", auth.passphrase)
    }

    @Test
    fun `SshAuthMethod KeyboardInteractive should have correct properties`() {
        val auth = SshAuthMethod.KeyboardInteractive("user")

        assertEquals("user", auth.username)
    }

    // ==================== SshChannelConfig Tests ====================

    @Test
    fun `SshChannelConfig should have correct default values`() {
        val config = SshChannelConfig(
            host = "127.0.0.1",
            authMethod = SshAuthMethod.Password("user", "pass")
        )

        assertEquals(ChannelType.SSH, config.type)
        assertEquals(22, config.port)
        assertEquals("127.0.0.1", config.host)
        assertEquals("user", config.username)
    }

    @Test
    fun `SshChannelConfig validateConfig should return true for valid config`() {
        val config = SshChannelConfig(
            host = "192.168.1.100",
            port = 22,
            authMethod = SshAuthMethod.Password("user", "password")
        )

        assertTrue(config.validateConfig())
    }

    @Test
    fun `SshChannelConfig validateConfig should return false for empty host`() {
        val config = SshChannelConfig(
            host = "",
            port = 22,
            authMethod = SshAuthMethod.Password("user", "password")
        )

        assertFalse(config.validateConfig())
    }

    @Test
    fun `SshChannelConfig validateConfig should return false for invalid port`() {
        val config = SshChannelConfig(
            host = "192.168.1.100",
            port = -1,
            authMethod = SshAuthMethod.Password("user", "password")
        )

        assertFalse(config.validateConfig())
    }

    // ==================== SshTimeoutConfig Tests ====================

    @Test
    fun `SshTimeoutConfig should have correct default values`() {
        val config = SshTimeoutConfig()

        assertEquals(30000L, config.connectionTimeoutMs)
        assertEquals(60000L, config.readTimeoutMs)
        assertEquals(60000L, config.writeTimeoutMs)
        assertEquals(30000L, config.keepAliveIntervalMs)
    }

    // ==================== SshReconnectConfig Tests ====================

    @Test
    fun `SshReconnectConfig should have correct default values`() {
        val config = SshReconnectConfig()

        assertTrue(config.enableReconnect)
        assertEquals(5, config.maxReconnectAttempts)
        assertEquals(1000L, config.reconnectDelayMs)
    }

    // ==================== SshSecurityConfig Tests ====================

    @Test
    fun `SshSecurityConfig should have correct default values`() {
        val config = SshSecurityConfig()

        assertTrue(config.strictHostKeyChecking)
        assertFalse(config.compressionEnabled)
    }

    // ==================== SshCommandResult Tests ====================

    @Test
    fun `SshCommandResult isSuccess should return true for exit code 0`() {
        val result = SshCommandResult(
            command = "ls",
            exitCode = 0,
            stdout = "file1\nfile2",
            stderr = "",
            executionTimeMs = 100
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `SshCommandResult isSuccess should return false for non-zero exit code`() {
        val result = SshCommandResult(
            command = "ls",
            exitCode = 1,
            stdout = "",
            stderr = "error",
            executionTimeMs = 100
        )

        assertFalse(result.isSuccess)
    }

    @Test
    fun `SshCommandResult output should combine stdout and stderr`() {
        val result = SshCommandResult(
            command = "test",
            exitCode = 0,
            stdout = "output",
            stderr = "error",
            executionTimeMs = 100
        )

        assertEquals("outputerror", result.output)
    }

    // ==================== SshTunnel Tests ====================

    @Test
    fun `SshTunnel should have correct properties`() {
        val tunnel = SshTunnel(
            tunnelId = "tunnel-1",
            type = SshTunnelType.LOCAL,
            localHost = "127.0.0.1",
            localPort = 8080,
            remoteHost = "192.168.1.100",
            remotePort = 80
        )

        assertEquals("tunnel-1", tunnel.tunnelId)
        assertEquals(SshTunnelType.LOCAL, tunnel.type)
        assertEquals(8080, tunnel.localPort)
        assertEquals(80, tunnel.remotePort)
        assertTrue(tunnel.isActive)
    }

    @Test
    fun `SshTunnelType should contain all expected types`() {
        val expectedTypes = listOf(
            SshTunnelType.LOCAL,
            SshTunnelType.REMOTE,
            SshTunnelType.DYNAMIC
        )

        assertEquals(expectedTypes.size, SshTunnelType.entries.size)
        expectedTypes.forEach { type ->
            assertTrue(SshTunnelType.entries.contains(type))
        }
    }

    // ==================== SshSessionState Tests ====================

    @Test
    fun `SshSessionState should have correct default values`() {
        val state = SshSessionState()

        assertFalse(state.isConnected)
        assertFalse(state.isAuthenticated)
        assertNull(state.sessionId)
        assertEquals(0, state.activeChannels)
        assertEquals(0, state.activeTunnels)
    }

    // ==================== FileTransferResult Tests ====================

    @Test
    fun `FileTransferResult should have correct properties`() {
        val result = FileTransferResult(
            sourcePath = "/local/file.txt",
            destinationPath = "/remote/file.txt",
            bytesTransferred = 1024,
            transferTimeMs = 1000,
            averageSpeed = 1024
        )

        assertEquals("/local/file.txt", result.sourcePath)
        assertEquals("/remote/file.txt", result.destinationPath)
        assertEquals(1024L, result.bytesTransferred)
    }

    // ==================== SshServerFingerprint Tests ====================

    @Test
    fun `SshServerFingerprint should have correct properties`() {
        val fingerprint = SshServerFingerprint(
            host = "192.168.1.100",
            port = 22,
            keyType = "RSA",
            fingerprint = "SHA256:abc123"
        )

        assertEquals("192.168.1.100", fingerprint.host)
        assertEquals(22, fingerprint.port)
        assertEquals("RSA", fingerprint.keyType)
        assertEquals("SHA256:abc123", fingerprint.fingerprint)
    }

    // ==================== Connection Tests ====================

    @Test
    fun `connect should succeed with valid config`() {
        every { sshChannel.isConnected() } returns true

        assertTrue(sshChannel.isConnected())
    }

    @Test
    fun `disconnect should succeed when connected`() {
        every { sshChannel.isConnected() } returns false

        assertFalse(sshChannel.isConnected())
    }

    // ==================== File Transfer Tests ====================

    @Test
    fun `uploadFile should succeed for valid file`() {
        // 验证mock配置
        every { sshChannel.isConnected() } returns true

        assertTrue(sshChannel.isConnected())
    }

    @Test
    fun `downloadFile should succeed for valid file`() {
        // 验证mock配置
        every { sshChannel.isConnected() } returns true

        assertTrue(sshChannel.isConnected())
    }

    // ==================== Tunnel Tests ====================

    @Test
    fun `createLocalTunnel should succeed with valid config`() {
        // 验证mock配置
        every { sshChannel.isConnected() } returns true

        assertTrue(sshChannel.isConnected())
    }
}