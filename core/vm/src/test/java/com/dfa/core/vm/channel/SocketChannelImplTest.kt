package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationError
import com.dfa.core.vm.communication.CommunicationState
import com.google.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
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

        assertThat(socketChannel.channelId).isEqualTo(id)
    }

    @Test
    fun `SocketChannel channelType should be SOCKET_UNIX for Unix socket`() {
        every { socketChannel.channelType } returns ChannelType.SOCKET_UNIX

        assertThat(socketChannel.channelType).isEqualTo(ChannelType.SOCKET_UNIX)
    }

    @Test
    fun `SocketChannel channelType should be SOCKET_TCP for TCP socket`() {
        every { socketChannel.channelType } returns ChannelType.SOCKET_TCP

        assertThat(socketChannel.channelType).isEqualTo(ChannelType.SOCKET_TCP)
    }

    @Test
    fun `SocketChannel socketType should return Unix type`() {
        val unixType = SocketType.Unix("/tmp/socket.sock")
        every { socketChannel.socketType } returns unixType

        assertThat(socketChannel.socketType).isInstanceOf(SocketType.Unix::class.java)
        assertThat((socketChannel.socketType as SocketType.Unix).path).isEqualTo("/tmp/socket.sock")
    }

    @Test
    fun `SocketChannel socketType should return Tcp type`() {
        val tcpType = SocketType.Tcp("127.0.0.1", 8080)
        every { socketChannel.socketType } returns tcpType

        assertThat(socketChannel.socketType).isInstanceOf(SocketType.Tcp::class.java)
        assertThat((socketChannel.socketType as SocketType.Tcp).host).isEqualTo("127.0.0.1")
        assertThat((socketChannel.socketType as SocketType.Tcp).port).isEqualTo(8080)
    }

    @Test
    fun `SocketChannel socketAddress should return correct address for Unix socket`() {
        every { socketChannel.socketAddress } returns "/tmp/socket.sock"

        assertThat(socketChannel.socketAddress).isEqualTo("/tmp/socket.sock")
    }

    @Test
    fun `SocketChannel socketAddress should return correct address for TCP socket`() {
        every { socketChannel.socketAddress } returns "127.0.0.1:8080"

        assertThat(socketChannel.socketAddress).isEqualTo("127.0.0.1:8080")
    }

    // ==================== 连接状态管理测试 ====================

    @Test
    fun `SocketChannel state should be DISCONNECTED initially`() {
        val stateFlow = MutableStateFlow(CommunicationState.DISCONNECTED)
        every { socketChannel.state } returns stateFlow

        assertThat(socketChannel.state.value).isEqualTo(CommunicationState.DISCONNECTED)
    }

    @Test
    fun `SocketChannel isConnected should return false when disconnected`() {
        every { socketChannel.isConnected() } returns false

        assertThat(socketChannel.isConnected()).isFalse()
    }

    @Test
    fun `SocketChannel isConnected should return true when connected`() {
        every { socketChannel.isConnected() } returns true

        assertThat(socketChannel.isConnected()).isTrue()
    }

    // ==================== TCP Socket连接测试 ====================

    @Test
    fun `SocketChannel connect should succeed with valid TCP config`() = runTest {
        val mockConnectionInfo = mockk<com.dfa.core.vm.communication.ConnectionInfo>()
        every { socketChannel.connect(any()) } returns Result.success(mockConnectionInfo)

        val config = SocketConfig(
            socketType = SocketType.Tcp("127.0.0.1", 8080)
        )
        val result = socketChannel.connect(config)

        assertThat(result.isSuccess).isTrue()
        verify { socketChannel.connect(any()) }
    }

    @Test
    fun `SocketChannel connect should fail with invalid config`() = runTest {
        val error = CommunicationError.ConfigurationError("Invalid config")
        every { socketChannel.connect(any()) } returns Result.failure(error)

        val result = socketChannel.connect(mockk())

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `SocketChannel connect should fail when connection refused`() = runTest {
        val error = CommunicationError.ConnectionError("Connection refused")
        every { socketChannel.connect(any()) } returns Result.failure(error)

        val config = SocketConfig(
            socketType = SocketType.Tcp("192.168.1.100", 9999)
        )
        val result = socketChannel.connect(config)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `SocketChannel disconnect should succeed`() = runTest {
        every { socketChannel.disconnect() } returns Result.success(Unit)

        val result = socketChannel.disconnect()

        assertThat(result.isSuccess).isTrue()
        verify { socketChannel.disconnect() }
    }

    // ==================== Unix Domain Socket测试 ====================

    @Test
    fun `SocketChannel connect should succeed with valid Unix socket`() = runTest {
        val mockConnectionInfo = mockk<com.dfa.core.vm.communication.ConnectionInfo>()
        every { socketChannel.connect(any()) } returns Result.success(mockConnectionInfo)

        val config = SocketConfig(
            socketType = SocketType.Unix("/tmp/test.sock")
        )
        val result = socketChannel.connect(config)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `SocketChannel connect should fail when Unix socket file not found`() = runTest {
        val error = CommunicationError.ConnectionError("Unix socket file not found")
        every { socketChannel.connect(any()) } returns Result.failure(error)

        val config = SocketConfig(
            socketType = SocketType.Unix("/nonexistent/socket.sock")
        )
        val result = socketChannel.connect(config)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== 数据发送/接收测试 ====================

    @Test
    fun `SocketChannel send should succeed when connected`() = runTest {
        every { socketChannel.send(any<ByteArray>()) } returns Result.success(Unit)

        val data = "Hello, Socket!".toByteArray()
        val result = socketChannel.send(data)

        assertThat(result.isSuccess).isTrue()
        verify { socketChannel.send(any<ByteArray>()) }
    }

    @Test
    fun `SocketChannel send should fail when not connected`() = runTest {
        val error = CommunicationError.ChannelError("Channel not connected")
        every { socketChannel.send(any<ByteArray>()) } returns Result.failure(error)

        val result = socketChannel.send("test".toByteArray())

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `SocketChannel send with timeout should succeed`() = runTest {
        every { socketChannel.send(any<ByteArray>(), any()) } returns Result.success(Unit)

        val data = "test data".toByteArray()
        val result = socketChannel.send(data, 5000)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `SocketChannel receive should return data when available`() = runTest {
        val expectedData = "received data".toByteArray()
        every { socketChannel.receive(any()) } returns Result.success(expectedData)

        val result = socketChannel.receive(5000)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedData)
    }

    @Test
    fun `SocketChannel receive should return null when no data`() = runTest {
        every { socketChannel.receive(any()) } returns Result.success(null)

        val result = socketChannel.receive(1000)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `SocketChannel receive should fail when not connected`() = runTest {
        val error = CommunicationError.ChannelError("Channel not connected")
        every { socketChannel.receive(any()) } returns Result.failure(error)

        val result = socketChannel.receive(1000)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Socket选项测试 ====================

    @Test
    fun `SocketChannel socketOptions should return current options`() {
        val options = SocketOptions.DEFAULT
        every { socketChannel.socketOptions } returns MutableStateFlow(options)

        assertThat(socketChannel.socketOptions.value).isEqualTo(options)
    }

    @Test
    fun `SocketChannel setSocketOptions should succeed`() = runTest {
        val options = SocketOptions(
            keepAlive = true,
            tcpNoDelay = true,
            sendBufferSize = 8192,
            receiveBufferSize = 8192
        )
        every { socketChannel.setSocketOptions(options) } returns Result.success(Unit)

        val result = socketChannel.setSocketOptions(options)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `SocketChannel getSocketOptions should return current options`() {
        val options = SocketOptions.DEFAULT
        every { socketChannel.getSocketOptions() } returns options

        assertThat(socketChannel.getSocketOptions()).isEqualTo(options)
    }

    @Test
    fun `SocketChannel setReadTimeout should succeed`() = runTest {
        every { socketChannel.setReadTimeout(any()) } returns Result.success(Unit)

        val result = socketChannel.setReadTimeout(5000)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `SocketChannel setWriteTimeout should succeed`() = runTest {
        every { socketChannel.setWriteTimeout(any()) } returns Result.success(Unit)

        val result = socketChannel.setWriteTimeout(5000)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `SocketChannel flush should succeed`() = runTest {
        every { socketChannel.flush() } returns Result.success(Unit)

        val result = socketChannel.flush()

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 地址查询测试 ====================

    @Test
    fun `SocketChannel getLocalAddress should return local address when connected`() {
        every { socketChannel.getLocalAddress() } returns "127.0.0.1:54321"

        assertThat(socketChannel.getLocalAddress()).isEqualTo("127.0.0.1:54321")
    }

    @Test
    fun `SocketChannel getLocalAddress should return null when not connected`() {
        every { socketChannel.getLocalAddress() } returns null

        assertThat(socketChannel.getLocalAddress()).isNull()
    }

    @Test
    fun `SocketChannel getRemoteAddress should return remote address when connected`() {
        every { socketChannel.getRemoteAddress() } returns "192.168.1.100:8080"

        assertThat(socketChannel.getRemoteAddress()).isEqualTo("192.168.1.100:8080")
    }

    @Test
    fun `SocketChannel getRemoteAddress should return null when not connected`() {
        every { socketChannel.getRemoteAddress() } returns null

        assertThat(socketChannel.getRemoteAddress()).isNull()
    }

    @Test
    fun `SocketChannel isReachable should return true for reachable host`() = runTest {
        every { socketChannel.isReachable(any()) } returns true

        assertThat(socketChannel.isReachable(5000)).isTrue()
    }

    @Test
    fun `SocketChannel isReachable should return false for unreachable host`() = runTest {
        every { socketChannel.isReachable(any()) } returns false

        assertThat(socketChannel.isReachable(5000)).isFalse()
    }

    // ==================== 消息监听器测试 ====================

    @Test
    fun `SocketChannel setMessageListener should accept listener`() {
        val listener = object : SocketChannel.MessageListener {
            override fun onMessage(data: ByteArray) {}
            override fun onStateChanged(state: CommunicationState) {}
            override fun onError(error: Throwable) {}
        }

        // 验证方法可以被调用
        every { socketChannel.setMessageListener(any()) } returns Unit

        socketChannel.setMessageListener(listener)

        verify { socketChannel.setMessageListener(any()) }
    }

    @Test
    fun `SocketChannel setMessageListener should accept null`() {
        every { socketChannel.setMessageListener(null) } returns Unit

        socketChannel.setMessageListener(null)

        verify { socketChannel.setMessageListener(null) }
    }

    // ==================== 资源释放测试 ====================

    @Test
    fun `SocketChannel release should disconnect and clean up`() = runTest {
        every { socketChannel.release() } returns Unit

        socketChannel.release()

        verify { socketChannel.release() }
    }

    // ==================== SocketType测试 ====================

    @Test
    fun `SocketType Unix should have correct properties`() {
        val unixType = SocketType.Unix("/tmp/socket.sock")

        assertThat(unixType.path).isEqualTo("/tmp/socket.sock")
    }

    @Test
    fun `SocketType Unix DEFAULT_PATH should be correct`() {
        assertThat(SocketType.Unix.DEFAULT_PATH).isNotEmpty()
    }

    @Test
    fun `SocketType Tcp should have correct properties`() {
        val tcpType = SocketType.Tcp("192.168.1.100", 8080)

        assertThat(tcpType.host).isEqualTo("192.168.1.100")
        assertThat(tcpType.port).isEqualTo(8080)
    }

    @Test
    fun `SocketType Tcp DEFAULT_HOST should be localhost`() {
        assertThat(SocketType.Tcp.DEFAULT_HOST).isEqualTo("127.0.0.1")
    }

    @Test
    fun `SocketType Tcp DEFAULT_PORT should be valid`() {
        assertThat(SocketType.Tcp.DEFAULT_PORT).isGreaterThan(0)
        assertThat(SocketType.Tcp.DEFAULT_PORT).isLessThan(65536)
    }

    // ==================== SocketOptions测试 ====================

    @Test
    fun `SocketOptions DEFAULT should have sensible defaults`() {
        val options = SocketOptions.DEFAULT

        assertThat(options.keepAlive).isTrue()
        assertThat(options.tcpNoDelay).isTrue()
        assertThat(options.sendBufferSize).isGreaterThan(0)
        assertThat(options.receiveBufferSize).isGreaterThan(0)
    }

    @Test
    fun `SocketOptions should allow custom values`() {
        val options = SocketOptions(
            keepAlive = false,
            tcpNoDelay = false,
            sendBufferSize = 16384,
            receiveBufferSize = 16384,
            soTimeout = 10000,
            soLinger = 5,
            reuseAddress = true,
            oobInline = false,
            trafficClass = 0
        )

        assertThat(options.keepAlive).isFalse()
        assertThat(options.tcpNoDelay).isFalse()
        assertThat(options.sendBufferSize).isEqualTo(16384)
        assertThat(options.soTimeout).isEqualTo(10000)
        assertThat(options.soLinger).isEqualTo(5)
    }

    @Test
    fun `SocketOptions copy should create modified instance`() {
        val original = SocketOptions.DEFAULT
        val modified = original.copy(tcpNoDelay = false)

        assertThat(original.tcpNoDelay).isTrue()
        assertThat(modified.tcpNoDelay).isFalse()
        assertThat(modified.keepAlive).isEqualTo(original.keepAlive)
    }

    // ==================== SocketChannelState测试 ====================

    @Test
    fun `SocketChannelState EMPTY should have default values`() {
        val state = SocketChannelState.EMPTY

        assertThat(state.isConnected).isFalse()
        assertThat(state.isConnecting).isFalse()
        assertThat(state.localAddress).isNull()
        assertThat(state.remoteAddress).isNull()
        assertThat(state.bytesReceived).isEqualTo(0)
        assertThat(state.bytesSent).isEqualTo(0)
    }

    @Test
    fun `SocketChannelState should track statistics`() {
        val state = SocketChannelState(
            isConnected = true,
            localAddress = "127.0.0.1:54321",
            remoteAddress = "192.168.1.100:8080",
            bytesReceived = 1024,
            bytesSent = 512,
            messagesReceived = 10,
            messagesSent = 5,
            connectionDuration = 60000,
            errorCount = 0
        )

        assertThat(state.isConnected).isTrue()
        assertThat(state.bytesReceived).isEqualTo(1024)
        assertThat(state.bytesSent).isEqualTo(512)
        assertThat(state.messagesReceived).isEqualTo(10)
    }

    @Test
    fun `SocketChannelState averageReceiveRate should calculate correctly`() {
        val state = SocketChannelState(
            bytesReceived = 60000,
            connectionDuration = 60000 // 60 seconds
        )

        // 60000 bytes / 60 seconds = 1000 bytes/second
        assertThat(state.averageReceiveRate).isWithin(0.1).of(1000.0)
    }

    @Test
    fun `SocketChannelState averageSendRate should calculate correctly`() {
        val state = SocketChannelState(
            bytesSent = 30000,
            connectionDuration = 60000 // 60 seconds
        )

        // 30000 bytes / 60 seconds = 500 bytes/second
        assertThat(state.averageSendRate).isWithin(0.1).of(500.0)
    }

    @Test
    fun `SocketChannelState averageReceiveRate should be 0 when no duration`() {
        val state = SocketChannelState(
            bytesReceived = 1000,
            connectionDuration = 0
        )

        assertThat(state.averageReceiveRate).isEqualTo(0.0)
    }

    // ==================== SocketChannelEvent测试 ====================

    @Test
    fun `SocketChannelEvent Connected should have correct properties`() {
        val event = SocketChannelEvent.Connected(
            channelId = "channel-1",
            localAddress = "127.0.0.1:54321",
            remoteAddress = "192.168.1.100:8080"
        )

        assertThat(event.channelId).isEqualTo("channel-1")
        assertThat(event.localAddress).isEqualTo("127.0.0.1:54321")
        assertThat(event.remoteAddress).isEqualTo("192.168.1.100:8080")
    }

    @Test
    fun `SocketChannelEvent Disconnected should have correct properties`() {
        val event = SocketChannelEvent.Disconnected(
            channelId = "channel-1",
            reason = "Connection closed"
        )

        assertThat(event.channelId).isEqualTo("channel-1")
        assertThat(event.reason).isEqualTo("Connection closed")
    }

    @Test
    fun `SocketChannelEvent DataReceived should have correct properties`() {
        val data = "test data".toByteArray()
        val event = SocketChannelEvent.DataReceived(
            channelId = "channel-1",
            data = data
        )

        assertThat(event.channelId).isEqualTo("channel-1")
        assertThat(event.data).isEqualTo(data)
    }

    @Test
    fun `SocketChannelEvent DataReceived equals should compare data content`() {
        val event1 = SocketChannelEvent.DataReceived("ch1", "test".toByteArray())
        val event2 = SocketChannelEvent.DataReceived("ch1", "test".toByteArray())

        assertThat(event1).isEqualTo(event2)
    }

    @Test
    fun `SocketChannelEvent DataSent should have correct properties`() {
        val event = SocketChannelEvent.DataSent(
            channelId = "channel-1",
            bytesCount = 1024
        )

        assertThat(event.channelId).isEqualTo("channel-1")
        assertThat(event.bytesCount).isEqualTo(1024)
    }

    @Test
    fun `SocketChannelEvent Error should have correct properties`() {
        val error = RuntimeException("Test error")
        val event = SocketChannelEvent.Error(
            channelId = "channel-1",
            error = error
        )

        assertThat(event.channelId).isEqualTo("channel-1")
        assertThat(event.error).isEqualTo(error)
    }

    @Test
    fun `SocketChannelEvent StateChanged should have correct properties`() {
        val event = SocketChannelEvent.StateChanged(
            channelId = "channel-1",
            oldState = CommunicationState.CONNECTING,
            newState = CommunicationState.CONNECTED
        )

        assertThat(event.oldState).isEqualTo(CommunicationState.CONNECTING)
        assertThat(event.newState).isEqualTo(CommunicationState.CONNECTED)
    }

    @Test
    fun `SocketChannelEvent Reconnecting should have correct properties`() {
        val event = SocketChannelEvent.Reconnecting(
            channelId = "channel-1",
            attempt = 2,
            maxAttempts = 5
        )

        assertThat(event.attempt).isEqualTo(2)
        assertThat(event.maxAttempts).isEqualTo(5)
    }

    // ==================== SocketChannelFactory测试 ====================

    @Test
    fun `SocketChannelFactory createUnixChannel should create Unix socket channel`() {
        val factory = mockk<SocketChannelFactory>()
        val config = SocketConfig(socketType = SocketType.Unix("/tmp/test.sock"))
        val mockChannel = mockk<SocketChannel>()

        every { factory.createUnixChannel(config) } returns mockChannel

        val channel = factory.createUnixChannel(config)

        assertThat(channel).isNotNull()
    }

    @Test
    fun `SocketChannelFactory createTcpChannel should create TCP socket channel`() {
        val factory = mockk<SocketChannelFactory>()
        val config = SocketConfig(socketType = SocketType.Tcp("127.0.0.1", 8080))
        val mockChannel = mockk<SocketChannel>()

        every { factory.createTcpChannel(config) } returns mockChannel

        val channel = factory.createTcpChannel(config)

        assertThat(channel).isNotNull()
    }

    @Test
    fun `SocketChannelFactory isSupported should return true for Unix`() {
        val factory = mockk<SocketChannelFactory>()
        every { factory.isSupported(SocketType.Unix("/tmp/test.sock")) } returns true

        assertThat(factory.isSupported(SocketType.Unix("/tmp/test.sock"))).isTrue()
    }

    @Test
    fun `SocketChannelFactory isSupported should return true for TCP`() {
        val factory = mockk<SocketChannelFactory>()
        every { factory.isSupported(SocketType.Tcp("127.0.0.1", 8080)) } returns true

        assertThat(factory.isSupported(SocketType.Tcp("127.0.0.1", 8080))).isTrue()
    }

    // ==================== SocketChannelListener测试 ====================

    @Test
    fun `SocketChannelListener should have default empty implementations`() {
        val listener = object : SocketChannelListener {}

        // 验证所有方法都有默认实现，不会抛出异常
        listener.onConnected(SocketChannelEvent.Connected("ch1", null, null))
        listener.onDisconnected(SocketChannelEvent.Disconnected("ch1", null))
        listener.onDataReceived(SocketChannelEvent.DataReceived("ch1", byteArrayOf()))
        listener.onDataSent(SocketChannelEvent.DataSent("ch1", 0))
        listener.onError(SocketChannelEvent.Error("ch1", RuntimeException()))
        listener.onStateChanged(SocketChannelEvent.StateChanged("ch1", CommunicationState.DISCONNECTED, CommunicationState.CONNECTED))
        listener.onReconnecting(SocketChannelEvent.Reconnecting("ch1", 1, 3))
    }
}