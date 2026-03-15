package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationChannel
import com.dfa.core.vm.communication.CommunicationState
import com.dfa.core.vm.communication.ConnectionInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Socket通道接口
 *
 * 支持Unix Domain Socket和TCP Socket的通信通道接口
 * 继承自CommunicationChannel，提供Socket特有的功能
 */
interface SocketChannel : CommunicationChannel {

    /**
     * Socket类型
     */
    val socketType: SocketType

    /**
     * Socket地址
     *
     * 对于Unix Domain Socket，返回文件路径
     * 对于TCP Socket，返回 host:port 格式
     */
    val socketAddress: String

    /**
     * Socket选项状态流
     */
    val socketOptions: StateFlow<SocketOptions>

    /**
     * 连接状态流
     */
    override val state: StateFlow<CommunicationState>

    /**
     * 连接信息流
     */
    override val connectionInfo: StateFlow<ConnectionInfo>

    /**
     * 接收数据流
     */
    override val receiveData: Flow<ByteArray>

    /**
     * 连接Socket通道
     *
     * @param config Socket配置
     * @return 连接结果，包含连接信息
     */
    override suspend fun connect(config: ChannelConfig): Result<ConnectionInfo>

    /**
     * 断开Socket连接
     *
     * @return 断开结果
     */
    override suspend fun disconnect(): Result<Unit>

    /**
     * 发送数据
     *
     * @param data 要发送的数据字节数组
     * @return 发送结果
     */
    override suspend fun send(data: ByteArray): Result<Unit>

    /**
     * 发送数据（带超时）
     *
     * @param data 要发送的数据字节数组
     * @param timeoutMs 超时时间（毫秒）
     * @return 发送结果
     */
    override suspend fun send(data: ByteArray, timeoutMs: Long): Result<Unit>

    /**
     * 接收数据
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 接收到的数据，失败返回null
     */
    suspend fun receive(timeoutMs: Long = ChannelConfig.DEFAULT_TIMEOUT_MS): Result<ByteArray?>

    /**
     * 检查是否已连接
     *
     * @return 是否已连接
     */
    override fun isConnected(): Boolean

    /**
     * 获取当前连接信息
     *
     * @return 连接信息
     */
    override fun getConnectionInfo(): ConnectionInfo

    /**
     * 设置消息监听器
     *
     * @param listener 消息监听器回调
     */
    fun setMessageListener(listener: MessageListener?)

    /**
     * 设置Socket选项
     *
     * @param options Socket选项配置
     * @return 设置结果
     */
    suspend fun setSocketOptions(options: SocketOptions): Result<Unit>

    /**
     * 获取当前Socket选项
     *
     * @return 当前Socket选项
     */
    fun getSocketOptions(): SocketOptions

    /**
     * 设置读取超时
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 设置结果
     */
    suspend fun setReadTimeout(timeoutMs: Long): Result<Unit>

    /**
     * 设置写入超时
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 设置结果
     */
    suspend fun setWriteTimeout(timeoutMs: Long): Result<Unit>

    /**
     * 刷新发送缓冲区
     *
     * @return 刷新结果
     */
    suspend fun flush(): Result<Unit>

    /**
     * 检查Socket是否可达
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否可达
     */
    suspend fun isReachable(timeoutMs: Long = 5000): Boolean

    /**
     * 获取本地Socket地址
     *
     * @return 本地地址字符串，未连接时返回null
     */
    fun getLocalAddress(): String?

    /**
     * 获取远程Socket地址
     *
     * @return 远程地址字符串，未连接时返回null
     */
    fun getRemoteAddress(): String?

    /**
     * 释放资源
     */
    override suspend fun release()

    /**
     * 消息监听器接口
     */
    interface MessageListener {
        /**
         * 收到消息时回调
         *
         * @param data 收到的数据
         */
        fun onMessage(data: ByteArray)

        /**
         * 连接状态变化时回调
         *
         * @param state 新的连接状态
         */
        fun onStateChanged(state: CommunicationState)

        /**
         * 发生错误时回调
         *
         * @param error 错误信息
         */
        fun onError(error: Throwable)
    }
}

/**
 * Socket通道状态
 *
 * 用于描述Socket通道的详细状态信息
 */
data class SocketChannelState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val localAddress: String? = null,
    val remoteAddress: String? = null,
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val messagesReceived: Long = 0,
    val messagesSent: Long = 0,
    val lastReceivedAt: Long? = null,
    val lastSentAt: Long? = null,
    val connectionDuration: Long = 0,
    val errorCount: Int = 0,
    val lastError: String? = null
) {
    /**
     * 平均接收速率（字节/秒）
     */
    val averageReceiveRate: Double
        get() = if (connectionDuration > 0) bytesReceived.toDouble() / (connectionDuration / 1000.0) else 0.0

    /**
     * 平均发送速率（字节/秒）
     */
    val averageSendRate: Double
        get() = if (connectionDuration > 0) bytesSent.toDouble() / (connectionDuration / 1000.0) else 0.0

    companion object {
        /**
         * 空状态
         */
        val EMPTY = SocketChannelState()
    }
}

/**
 * Socket通道工厂接口
 *
 * 用于创建不同类型的Socket通道
 */
interface SocketChannelFactory {
    /**
     * 创建Unix Domain Socket通道
     *
     * @param config Socket配置
     * @return Socket通道实例
     */
    fun createUnixChannel(config: SocketConfig): SocketChannel

    /**
     * 创建TCP Socket通道
     *
     * @param config Socket配置
     * @return Socket通道实例
     */
    fun createTcpChannel(config: SocketConfig): SocketChannel

    /**
     * 创建Socket通道
     *
     * @param type Socket类型
     * @param config Socket配置
     * @return Socket通道实例
     */
    fun createChannel(type: SocketType, config: SocketConfig): SocketChannel

    /**
     * 检查是否支持指定Socket类型
     *
     * @param type Socket类型
     * @return 是否支持
     */
    fun isSupported(type: SocketType): Boolean
}

/**
 * Socket通道事件
 *
 * 用于描述Socket通道中发生的各种事件
 */
sealed class SocketChannelEvent {
    /**
     * 连接成功事件
     */
    data class Connected(
        val channelId: String,
        val localAddress: String?,
        val remoteAddress: String?,
        val timestamp: Long = System.currentTimeMillis()
    ) : SocketChannelEvent()

    /**
     * 断开连接事件
     */
    data class Disconnected(
        val channelId: String,
        val reason: String?,
        val timestamp: Long = System.currentTimeMillis()
    ) : SocketChannelEvent()

    /**
     * 数据接收事件
     */
    data class DataReceived(
        val channelId: String,
        val data: ByteArray,
        val timestamp: Long = System.currentTimeMillis()
    ) : SocketChannelEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DataReceived) return false
            return channelId == other.channelId && data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            var result = channelId.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * 数据发送事件
     */
    data class DataSent(
        val channelId: String,
        val bytesCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : SocketChannelEvent()

    /**
     * 错误事件
     */
    data class Error(
        val channelId: String,
        val error: Throwable,
        val timestamp: Long = System.currentTimeMillis()
    ) : SocketChannelEvent()

    /**
     * 状态变化事件
     */
    data class StateChanged(
        val channelId: String,
        val oldState: CommunicationState,
        val newState: CommunicationState,
        val timestamp: Long = System.currentTimeMillis()
    ) : SocketChannelEvent()

    /**
     * 重连事件
     */
    data class Reconnecting(
        val channelId: String,
        val attempt: Int,
        val maxAttempts: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : SocketChannelEvent()
}

/**
 * Socket通道监听器接口
 *
 * 用于监听Socket通道的各种事件
 */
interface SocketChannelListener {
    /**
     * 连接成功时回调
     */
    fun onConnected(event: SocketChannelEvent.Connected) {}

    /**
     * 断开连接时回调
     */
    fun onDisconnected(event: SocketChannelEvent.Disconnected) {}

    /**
     * 收到数据时回调
     */
    fun onDataReceived(event: SocketChannelEvent.DataReceived) {}

    /**
     * 发送数据时回调
     */
    fun onDataSent(event: SocketChannelEvent.DataSent) {}

    /**
     * 发生错误时回调
     */
    fun onError(event: SocketChannelEvent.Error) {}

    /**
     * 状态变化时回调
     */
    fun onStateChanged(event: SocketChannelEvent.StateChanged) {}

    /**
     * 重连时回调
     */
    fun onReconnecting(event: SocketChannelEvent.Reconnecting) {}
}