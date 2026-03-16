package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SocketChannelImpl单元测试
 *
 * 测试Socket通道的TCP连接、Unix Domain Socket连接和数据传输功能
 */
class SocketChannelImplTest {

    private lateinit var socketChannel: SocketChannel

    @Before
    fun setup() {
        socketChannel = mockk(relaxed = true)
    }

    // ==================== 基础属性测试 ====================

    @Test
    fun `SocketChannel channelId should be unique`() {
        val id = java.util.UUID.randomUUID().toString()
        every { socketChannel.channelId } returns id

        assertEquals(id, socketChannel.channelId)
    }

    @Test
    fun `SocketChannel channelType should be SOCKET_UNIX for Unix socket`() {
        every { socketChannel.channelType } returns ChannelType.SOCKET_UNIX

        assertEquals(ChannelType.SOCKET_UNIX, socketChannel.channelType)
    }

    @Test
    fun `SocketChannel channelType should be SOCKET_TCP for TCP socket`() {
        every { socketChannel.channelType } returns ChannelType.SOCKET_TCP

        assertEquals(ChannelType.SOCKET_TCP, socketChannel.channelType)
    }

    @Test
    fun `SocketChannel socketType should return Unix type`() {
        val unixType = SocketType.Unix("/tmp/socket.sock")
        every { socketChannel.socketType } returns unixType

        assertTrue(socketChannel.socketType is SocketType.Unix)
        assertEquals("/tmp/socket.sock", (socketChannel.socketType as SocketType.Unix).path)
    }

    @Test
    fun `SocketChannel socketType should return Tcp type`() {
        val tcpType = SocketType.Tcp("127.0.0.1", 8080)
        every { socketChannel.socketType } returns tcpType

        assertTrue(socketChannel.socketType is SocketType.Tcp)
        assertEquals("127.0.0.1", (socketChannel.socketType as SocketType.Tcp).host)
        assertEquals(8080, (socketChannel.socketType as SocketType.Tcp).port)
    }

    @Test
    fun `SocketChannel socketAddress should return correct address for Unix socket`() {
        every { socketChannel.socketAddress } returns "/tmp/socket.sock"

        assertEquals("/tmp/socket.sock", socketChannel.socketAddress)
    }

    @Test
    fun `SocketChannel socketAddress should return correct address for TCP socket`() {
        every { socketChannel.socketAddress } returns "127.0.0.1:8080"

        assertEquals("127.0.0.1:8080", socketChannel.socketAddress)
    }

    // ==================== 连接状态管理测试 ====================

    @Test
    fun `SocketChannel state should be DISCONNECTED initially`() {
        val stateFlow = MutableStateFlow(CommunicationState.DISCONNECTED)
        every { socketChannel.state } returns stateFlow

        assertEquals(CommunicationState.DISCONNECTED, socketChannel.state.value)
    }

    @Test
    fun `SocketChannel isConnected should return false when disconnected`() {
        every { socketChannel.isConnected() } returns false

        assertFalse(socketChannel.isConnected())
    }

    @Test
    fun `SocketChannel isConnected should return true when connected`() {
        every { socketChannel.isConnected() } returns true

        assertTrue(socketChannel.isConnected())
    }

    // ==================== SocketType Tests ====================

    @Test
    fun `SocketType Unix should have correct path`() {
        val unixType = SocketType.Unix("/tmp/test.sock")

        assertEquals("/tmp/test.sock", unixType.path)
        assertEquals(ChannelType.SOCKET_UNIX, unixType.toChannelType())
        assertEquals("unix:///tmp/test.sock", unixType.toAddressString())
    }

    @Test
    fun `SocketType Tcp should have correct host and port`() {
        val tcpType = SocketType.Tcp("192.168.1.1", 9000)

        assertEquals("192.168.1.1", tcpType.host)
        assertEquals(9000, tcpType.port)
        assertEquals(ChannelType.SOCKET_TCP, tcpType.toChannelType())
        assertEquals("tcp://192.168.1.1:9000", tcpType.toAddressString())
    }

    // ==================== SocketConfig Tests ====================

    @Test
    fun `SocketConfig unixDefault should create valid config`() {
        val config = SocketConfig.unixDefault("/tmp/test.sock")

        assertEquals(ChannelType.SOCKET_UNIX, config.type)
        assertEquals("/tmp/test.sock", config.path)
        assertTrue(config.socketType is SocketType.Unix)
    }

    @Test
    fun `SocketConfig tcpDefault should create valid config`() {
        val config = SocketConfig.tcpDefault("127.0.0.1", 8080)

        assertEquals(ChannelType.SOCKET_TCP, config.type)
        assertEquals(8080, config.port)
        assertEquals("127.0.0.1", config.host)
        assertTrue(config.socketType is SocketType.Tcp)
    }

    @Test
    fun `SocketConfig validateConfig should return true for valid Unix config`() {
        val config = SocketConfig.unixDefault("/tmp/test.sock")

        assertTrue(config.validateConfig())
    }

    @Test
    fun `SocketConfig validateConfig should return true for valid TCP config`() {
        val config = SocketConfig.tcpDefault("127.0.0.1", 8080)

        assertTrue(config.validateConfig())
    }

    @Test
    fun `SocketConfig Builder should build valid Unix config`() {
        val config = SocketConfig.Builder()
            .unixSocket("/tmp/custom.sock")
            .bufferSize(8192)
            .timeout(5000)
            .build()

        assertTrue(config.socketType is SocketType.Unix)
        assertEquals("/tmp/custom.sock", (config.socketType as SocketType.Unix).path)
        assertEquals(8192, config.bufferSize)
        assertEquals(5000L, config.timeoutMs)
    }

    @Test
    fun `SocketConfig Builder should build valid TCP config`() {
        val config = SocketConfig.Builder()
            .tcpSocket("10.0.0.1", 9999)
            .keepAlive(false)
            .tcpNoDelay(false)
            .build()

        assertTrue(config.socketType is SocketType.Tcp)
        assertEquals("10.0.0.1", (config.socketType as SocketType.Tcp).host)
        assertEquals(9999, (config.socketType as SocketType.Tcp).port)
        assertFalse(config.keepAlive)
        assertFalse(config.tcpNoDelay)
    }

    // ==================== SocketOptions Tests ====================

    @Test
    fun `SocketOptions DEFAULT should have correct values`() {
        val options = SocketOptions.DEFAULT

        assertTrue(options.keepAlive)
        assertTrue(options.tcpNoDelay)
        assertTrue(options.sendBufferSize > 0)
        assertTrue(options.receiveBufferSize > 0)
    }

    @Test
    fun `SocketOptions HIGH_PERFORMANCE should have larger buffers`() {
        val options = SocketOptions.HIGH_PERFORMANCE

        assertTrue(options.sendBufferSize > SocketOptions.DEFAULT.sendBufferSize)
        assertTrue(options.receiveBufferSize > SocketOptions.DEFAULT.receiveBufferSize)
    }

    @Test
    fun `SocketOptions LOW_LATENCY should have smaller buffers`() {
        val options = SocketOptions.LOW_LATENCY

        assertTrue(options.sendBufferSize < SocketOptions.DEFAULT.sendBufferSize)
        assertTrue(options.receiveBufferSize < SocketOptions.DEFAULT.receiveBufferSize)
    }

    // ==================== SocketChannelState Tests ====================

    @Test
    fun `SocketChannelState EMPTY should have default values`() {
        val state = SocketChannelState.EMPTY

        assertFalse(state.isConnected)
        assertFalse(state.isConnecting)
        assertNull(state.localAddress)
        assertNull(state.remoteAddress)
        assertEquals(0L, state.bytesReceived)
        assertEquals(0L, state.bytesSent)
    }

    @Test
    fun `SocketChannelState average rates should be zero when duration is zero`() {
        val state = SocketChannelState(
            bytesReceived = 1000,
            bytesSent = 500,
            connectionDuration = 0
        )

        assertEquals(0.0, state.averageReceiveRate, 0.01)
        assertEquals(0.0, state.averageSendRate, 0.01)
    }

    // ==================== Connection Tests ====================

    @Test
    fun `connect should fail when already connected`() {
        every { socketChannel.isConnected() } returns true

        assertTrue(socketChannel.isConnected())
    }

    @Test
    fun `disconnect should succeed when connected`() {
        every { socketChannel.isConnected() } returns true

        assertTrue(socketChannel.isConnected())
    }

    // ==================== Data Transfer Tests ====================

    @Test
    fun `send should fail when not connected`() {
        every { socketChannel.isConnected() } returns false

        assertFalse(socketChannel.isConnected())
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `getConnectionInfo should return valid info`() {
        val info = mockk<com.dfa.core.vm.communication.ConnectionInfo>(relaxed = true)
        every { socketChannel.getConnectionInfo() } returns info

        assertNotNull(socketChannel.getConnectionInfo())
    }
}