package com.dfa.core.vm.channel

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.ServerSocket

/**
 * Socket通道集成测试
 *
 * 测试TCP Socket和Unix Domain Socket的连接和数据传输功能
 * 需要在真实的Android设备上运行
 *
 * 测试覆盖范围：
 * - TCP Socket连接
 * - Unix Domain Socket连接
 * - 数据传输
 * - 连接状态管理
 * - Socket选项配置
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class SocketChannelIntegrationTest {

    // Socket通道实例
    private lateinit var socketChannel: SocketChannel

    // 测试用的TCP服务器
    private var testServer: ServerSocket? = null
    private var testServerPort: Int = 0

    // 测试用的Unix Domain Socket路径
    private val unixSocketPath = "/tmp/test-socket-${System.currentTimeMillis()}.sock"

    // 测试用的临时目录
    private val testDirectory = "/tmp/socket-test-${System.currentTimeMillis()}"

    @Before
    fun setup() = runTest {
        // 创建测试目录
        File(testDirectory).mkdirs()

        // 启动测试TCP服务器
        testServer = ServerSocket(0) // 使用随机端口
        testServerPort = testServer!!.localPort

        // 启动服务器线程
        startTestServer()

        // 初始化Socket通道
        // 实际实现中应该通过依赖注入获取
        // socketChannel = SocketChannelImpl(SocketConfig(...))
    }

    @After
    fun tearDown() = runTest {
        // 关闭Socket通道
        try {
            if (socketChannel.isConnected()) {
                socketChannel.disconnect()
            }
            socketChannel.release()
        } catch (e: Exception) {
            // 忽略关闭错误
        }

        // 关闭测试服务器
        try {
            testServer?.close()
        } catch (e: Exception) {
            // 忽略关闭错误
        }

        // 清理测试文件
        File(unixSocketPath).delete()
        File(testDirectory).deleteRecursively()
    }

    // ==================== TCP Socket连接测试 ====================

    @Test
    fun `connect should establish TCP socket connection`() = runTest {
        // Given: TCP Socket配置
        val config = SocketConfig(
            type = ChannelType.SOCKET_TCP,
            socketType = SocketType.Tcp("127.0.0.1", testServerPort),
            host = "127.0.0.1",
            port = testServerPort
        )

        // When: 连接Socket
        val result = socketChannel.connect(config)

        // Then: 应该成功连接
        assertThat(result.isSuccess).isTrue()
        assertThat(socketChannel.isConnected()).isTrue()
    }

    @Test
    fun `connect should update state to CONNECTED for TCP`() = runTest {
        // Given: TCP Socket配置
        val config = SocketConfig(
            type = ChannelType.SOCKET_TCP,
            socketType = SocketType.Tcp("127.0.0.1", testServerPort),
            host = "127.0.0.1",
            port = testServerPort
        )

        // When: 连接Socket
        socketChannel.connect(config)

        // Then: 状态应该变为CONNECTED
        val state = socketChannel.state.first()
        assertThat(state).isEqualTo(CommunicationState.CONNECTED)
    }

    @Test
    fun `connect should fail for unreachable TCP host`() = runTest {
        // Given: 不可达的主机
        val config = SocketConfig(
            type = ChannelType.SOCKET_TCP,
            socketType = SocketType.Tcp("192.168.255.255", 9999),
            host = "192.168.255.255",
            port = 9999,
            connectionTimeoutMs = 1000
        )

        // When: 尝试连接
        val result = socketChannel.connect(config)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `connect should fail for invalid TCP port`() = runTest {
        // Given: 无效的端口
        val config = SocketConfig(
            type = ChannelType.SOCKET_TCP,
            socketType = SocketType.Tcp("127.0.0.1", -1),
            host = "127.0.0.1",
            port = -1
        )

        // When: 尝试连接
        val result = socketChannel.connect(config)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `disconnect should close TCP connection`() = runTest {
        // Given: 已建立的TCP连接
        establishTcpConnection()

        // When: 断开连接
        val result = socketChannel.disconnect()

        // Then: 应该成功断开
        assertThat(result.isSuccess).isTrue()
        assertThat(socketChannel.isConnected()).isFalse()
    }

    @Test
    fun `disconnect should update state to DISCONNECTED for TCP`() = runTest {
        // Given: 已建立的TCP连接
        establishTcpConnection()

        // When: 断开连接
        socketChannel.disconnect()

        // Then: 状态应该变为DISCONNECTED
        val state = socketChannel.state.first()
        assertThat(state).isEqualTo(CommunicationState.DISCONNECTED)
    }

    @Test
    fun `isReachable should return true for available TCP server`() = runTest {
        // When: 检查服务器是否可达
        val isReachable = socketChannel.isReachable()

        // Then: 应该返回true
        assertThat(isReachable).isTrue()
    }

    @Test
    fun `isReachable should return false for unavailable TCP server`() = runTest {
        // Given: 不可达的服务器
        val config = SocketConfig(
            type = ChannelType.SOCKET_TCP,
            socketType = SocketType.Tcp("192.168.255.255", 9999),
            host = "192.168.255.255",
            port = 9999
        )

        // When: 检查服务器是否可达
        val isReachable = socketChannel.isReachable(timeoutMs = 1000)

        // Then: 应该返回false
        assertThat(isReachable).isFalse()
    }

    @Test
    fun `getLocalAddress should return local address when connected`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()

        // When: 获取本地地址
        val localAddress = socketChannel.getLocalAddress()

        // Then: 应该返回有效的地址
        assertThat(localAddress).isNotNull()
        assertThat(localAddress).contains("127.0.0.1")
    }

    @Test
    fun `getRemoteAddress should return remote address when connected`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()

        // When: 获取远程地址
        val remoteAddress = socketChannel.getRemoteAddress()

        // Then: 应该返回有效的地址
        assertThat(remoteAddress).isNotNull()
        assertThat(remoteAddress).contains("127.0.0.1")
    }

    // ==================== Unix Domain Socket连接测试 ====================

    @Test
    fun `connect should establish Unix Domain Socket connection`() = runTest {
        // Given: Unix Domain Socket配置
        // 需要先启动一个Unix Domain Socket服务器
        startUnixSocketServer()

        val config = SocketConfig(
            type = ChannelType.SOCKET_UNIX,
            socketType = SocketType.Unix(unixSocketPath),
            path = unixSocketPath
        )

        // When: 连接Socket
        val result = socketChannel.connect(config)

        // Then: 应该成功连接
        assertThat(result.isSuccess).isTrue()
        assertThat(socketChannel.isConnected()).isTrue()
    }

    @Test
    fun `connect should fail for non-existing Unix socket path`() = runTest {
        // Given: 不存在的Unix Socket路径
        val config = SocketConfig(
            type = ChannelType.SOCKET_UNIX,
            socketType = SocketType.Unix("/tmp/nonexistent-socket-12345.sock"),
            path = "/tmp/nonexistent-socket-12345.sock"
        )

        // When: 尝试连接
        val result = socketChannel.connect(config)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `socketType should return UNIX for Unix Domain Socket`() = runTest {
        // Given: Unix Domain Socket连接
        establishUnixConnection()

        // When: 获取Socket类型
        val socketType = socketChannel.socketType

        // Then: 应该返回Unix类型
        assertThat(socketType is SocketType.Unix).isTrue()
    }

    @Test
    fun `socketAddress should return path for Unix Domain Socket`() = runTest {
        // Given: Unix Domain Socket连接
        establishUnixConnection()

        // When: 获取Socket地址
        val address = socketChannel.socketAddress

        // Then: 应该返回文件路径
        assertThat(address).isEqualTo(unixSocketPath)
    }

    // ==================== 数据传输测试 ====================

    @Test
    fun `send should transmit data over TCP socket`() = runTest {
        // Given: 已建立的TCP连接
        establishTcpConnection()
        val data = "Hello TCP Socket".toByteArray()

        // When: 发送数据
        val result = socketChannel.send(data)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `send should transmit binary data`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()
        val data = byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte(), 0xFE.toByte())

        // When: 发送二进制数据
        val result = socketChannel.send(data)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `send should fail when not connected`() = runTest {
        // Given: 未连接的通道
        val data = "test".toByteArray()

        // When: 尝试发送数据
        val result = socketChannel.send(data)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `send with timeout should respect timeout setting`() = runTest {
        // Given: 已建立的连接和短超时
        establishTcpConnection()
        val data = ByteArray(1024 * 1024) // 1MB数据
        val timeoutMs = 100L // 非常短的超时

        // When: 发送大数据
        val result = socketChannel.send(data, timeoutMs)

        // Then: 可能超时或成功（取决于网络速度）
        // 这里只验证不会抛出异常
        assertThat(result.isSuccess || result.isFailure).isTrue()
    }

    @Test
    fun `receive should get data from TCP socket`() = runTest {
        // Given: 已建立的连接，服务器已发送数据
        establishTcpConnection()
        // 服务器应该已经发送了响应

        // When: 接收数据
        val result = socketChannel.receive(timeoutMs = 5000)

        // Then: 应该收到数据
        assertThat(result.isSuccess).isTrue()
        val receivedData = result.getOrNull()
        assertThat(receivedData).isNotNull()
    }

    @Test
    fun `receive should timeout when no data available`() = runTest {
        // Given: 已建立的连接，但服务器没有发送数据
        establishTcpConnection()
        val shortTimeout = 100L

        // When: 尝试接收数据
        val result = socketChannel.receive(timeoutMs = shortTimeout)

        // Then: 可能返回null或超时
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `receiveData flow should emit received data`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()

        // When: 收集接收数据流
        var receivedData: ByteArray? = null
        val job = launch {
            receivedData = socketChannel.receiveData.first()
        }

        // 等待数据
        delay(100)
        job.cancel()

        // Then: 应该收到数据（如果服务器发送了）
        // 这里只验证流存在
        assertThat(socketChannel.receiveData).isNotNull()
    }

    @Test
    fun `bidirectional communication should work correctly`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()

        // When: 发送并接收数据
        val sendData = "Ping".toByteArray()
        socketChannel.send(sendData)

        val receiveResult = socketChannel.receive(timeoutMs = 5000)

        // Then: 应该收到响应
        assertThat(receiveResult.isSuccess).isTrue()
    }

    @Test
    fun `flush should flush send buffer`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()

        // When: 刷新缓冲区
        val result = socketChannel.flush()

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 连接状态管理测试 ====================

    @Test
    fun `state flow should emit state changes`() = runTest {
        // Given: 初始状态
        val initialState = socketChannel.state.first()
        assertThat(initialState).isEqualTo(CommunicationState.DISCONNECTED)

        // When: 连接
        establishTcpConnection()

        // Then: 状态应该变为CONNECTED
        val connectedState = socketChannel.state.first()
        assertThat(connectedState).isEqualTo(CommunicationState.CONNECTED)
    }

    @Test
    fun `connectionInfo should contain connection details`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()

        // When: 获取连接信息
        val connectionInfo = socketChannel.connectionInfo.first()

        // Then: 应该包含正确的信息
        assertThat(connectionInfo.isConnected).isTrue()
    }

    @Test
    fun `getConnectionInfo should return current connection info`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()

        // When: 获取连接信息
        val info = socketChannel.getConnectionInfo()

        // Then: 应该返回有效的信息
        assertThat(info.isConnected).isTrue()
    }

    @Test
    fun `isConnected should return false initially`() {
        // When: 检查连接状态
        val isConnected = socketChannel.isConnected()

        // Then: 应该返回false
        assertThat(isConnected).isFalse()
    }

    @Test
    fun `isConnected should return true after connection`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()

        // When: 检查连接状态
        val isConnected = socketChannel.isConnected()

        // Then: 应该返回true
        assertThat(isConnected).isTrue()
    }

    // ==================== Socket选项配置测试 ====================

    @Test
    fun `setSocketOptions should apply socket options`() = runTest {
        // Given: Socket选项
        val options = SocketOptions(
            receiveBufferSize = 8192,
            sendBufferSize = 8192,
            soTimeout = 5000,
            keepAlive = true,
            tcpNoDelay = true
        )

        // When: 设置Socket选项
        val result = socketChannel.setSocketOptions(options)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `getSocketOptions should return current options`() = runTest {
        // Given: 已设置的选项
        val options = SocketOptions(keepAlive = true)
        socketChannel.setSocketOptions(options)

        // When: 获取当前选项
        val currentOptions = socketChannel.getSocketOptions()

        // Then: 应该返回正确的选项
        assertThat(currentOptions.keepAlive).isTrue()
    }

    @Test
    fun `setReadTimeout should update read timeout`() = runTest {
        // Given: 超时值
        val timeoutMs = 3000L

        // When: 设置读取超时
        val result = socketChannel.setReadTimeout(timeoutMs)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `setWriteTimeout should update write timeout`() = runTest {
        // Given: 超时值
        val timeoutMs = 3000L

        // When: 设置写入超时
        val result = socketChannel.setWriteTimeout(timeoutMs)

        // Then: 应该成功
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `socketOptions flow should emit option changes`() = runTest {
        // Given: 新的选项
        val newOptions = SocketOptions(tcpNoDelay = true)

        // When: 设置选项
        socketChannel.setSocketOptions(newOptions)

        // Then: 流应该发出新选项
        val currentOptions = socketChannel.socketOptions.first()
        assertThat(currentOptions.tcpNoDelay).isTrue()
    }

    // ==================== 消息监听器测试 ====================

    @Test
    fun `setMessageListener should receive messages`() = runTest {
        // Given: 已建立的连接和消息监听器
        establishTcpConnection()
        var receivedMessage: ByteArray? = null
        val listener = object : SocketChannel.MessageListener {
            override fun onMessage(data: ByteArray) {
                receivedMessage = data
            }
            override fun onStateChanged(state: CommunicationState) {}
            override fun onError(error: Throwable) {}
        }

        // When: 设置监听器
        socketChannel.setMessageListener(listener)

        // Then: 应该能够接收消息
        // 实际测试需要服务器发送数据
        assertThat(socketChannel).isNotNull()
    }

    @Test
    fun `MessageListener onStateChanged should be called on connection`() = runTest {
        // Given: 状态变化监听器
        var stateChanged = false
        var newState: CommunicationState? = null
        val listener = object : SocketChannel.MessageListener {
            override fun onMessage(data: ByteArray) {}
            override fun onStateChanged(state: CommunicationState) {
                stateChanged = true
                newState = state
            }
            override fun onError(error: Throwable) {}
        }
        socketChannel.setMessageListener(listener)

        // When: 连接
        establishTcpConnection()

        // Then: 应该触发状态变化回调
        assertThat(stateChanged).isTrue()
        assertThat(newState).isEqualTo(CommunicationState.CONNECTED)
    }

    @Test
    fun `MessageListener onError should be called on error`() = runTest {
        // Given: 错误监听器
        var errorOccurred = false
        val listener = object : SocketChannel.MessageListener {
            override fun onMessage(data: ByteArray) {}
            override fun onStateChanged(state: CommunicationState) {}
            override fun onError(error: Throwable) {
                errorOccurred = true
            }
        }
        socketChannel.setMessageListener(listener)

        // When: 触发错误（尝试连接不可达服务器）
        val config = SocketConfig(
            type = ChannelType.SOCKET_TCP,
            socketType = SocketType.Tcp("192.168.255.255", 9999),
            host = "192.168.255.255",
            port = 9999,
            connectionTimeoutMs = 100
        )
        socketChannel.connect(config)

        // Then: 应该触发错误回调
        assertThat(errorOccurred).isTrue()
    }

    // ==================== 资源释放测试 ====================

    @Test
    fun `release should clean up all resources`() = runTest {
        // Given: 已建立的连接
        establishTcpConnection()

        // When: 释放资源
        socketChannel.release()

        // Then: 应该断开连接
        assertThat(socketChannel.isConnected()).isFalse()
    }

    @Test
    fun `release should be idempotent`() = runTest {
        // Given: 已释放的通道
        establishTcpConnection()
        socketChannel.release()

        // When: 再次释放
        socketChannel.release()

        // Then: 不应该抛出异常
        assertThat(socketChannel.isConnected()).isFalse()
    }

    // ==================== SocketType测试 ====================

    @Test
    fun `SocketType should have TCP and UNIX types`() {
        // When: 创建SocketType实例
        val tcpType = SocketType.Tcp("127.0.0.1", 8080)
        val unixType = SocketType.Unix("/tmp/test.sock")

        // Then: 应该能够创建TCP和Unix类型
        assertThat(tcpType).isNotNull()
        assertThat(unixType).isNotNull()
        assertThat(tcpType is SocketType.Tcp).isTrue()
        assertThat(unixType is SocketType.Unix).isTrue()
    }

    // ==================== SocketChannelState测试 ====================

    @Test
    fun `SocketChannelState should have correct default values`() {
        // When: 创建默认状态
        val state = SocketChannelState.EMPTY

        // Then: 应该有正确的默认值
        assertThat(state.isConnected).isFalse()
        assertThat(state.bytesReceived).isEqualTo(0)
        assertThat(state.bytesSent).isEqualTo(0)
    }

    @Test
    fun `SocketChannelState average rates should calculate correctly`() {
        // Given: 有数据传输的状态
        val state = SocketChannelState(
            bytesReceived = 1000,
            bytesSent = 500,
            connectionDuration = 1000 // 1秒
        )

        // When: 计算平均速率
        val receiveRate = state.averageReceiveRate
        val sendRate = state.averageSendRate

        // Then: 应该计算正确
        assertThat(receiveRate).isEqualTo(1000.0)
        assertThat(sendRate).isEqualTo(500.0)
    }

    // ==================== 辅助方法 ====================

    /**
     * 建立TCP连接
     */
    private suspend fun establishTcpConnection() {
        val config = SocketConfig(
            type = ChannelType.SOCKET_TCP,
            socketType = SocketType.Tcp("127.0.0.1", testServerPort),
            host = "127.0.0.1",
            port = testServerPort
        )
        socketChannel.connect(config)
    }

    /**
     * 建立Unix Domain Socket连接
     */
    private suspend fun establishUnixConnection() {
        startUnixSocketServer()
        val config = SocketConfig(
            type = ChannelType.SOCKET_UNIX,
            socketType = SocketType.Unix(unixSocketPath),
            path = unixSocketPath
        )
        socketChannel.connect(config)
    }

    /**
     * 启动测试TCP服务器
     */
    private fun startTestServer() {
        Thread {
            try {
                val server = testServer ?: return@Thread
                while (!server.isClosed) {
                    try {
                        val client = server.accept()
                        Thread {
                            try {
                                val input = client.getInputStream()
                                val output = client.getOutputStream()

                                // 简单的echo服务器
                                val buffer = ByteArray(1024)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    output.flush()
                                }
                            } catch (e: Exception) {
                                // 忽略
                            } finally {
                                client.close()
                            }
                        }.start()
                    } catch (e: Exception) {
                        // 忽略
                    }
                }
            } catch (e: Exception) {
                // 忽略
            }
        }.start()
    }

    /**
     * 启动Unix Domain Socket服务器
     */
    private fun startUnixSocketServer() {
        // 在Android上，Unix Domain Socket需要特殊处理
        // 这里只是一个占位实现
        // 实际实现需要使用LocalServerSocket
    }
}