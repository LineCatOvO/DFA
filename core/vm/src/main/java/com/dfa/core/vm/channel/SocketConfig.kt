package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType

/**
 * Socket类型 sealed class
 *
 * 用于区分Unix Domain Socket和TCP Socket
 */
sealed class SocketType {
    /**
     * Unix Domain Socket类型
     *
     * @property path Unix域套接字文件路径
     */
    data class Unix(val path: String) : SocketType() {
        companion object {
            // 常用Unix Domain Socket路径
            const val DEFAULT_PATH = "/tmp/dfa.sock"
        }
    }

    /**
     * TCP Socket类型
     *
     * @property host 主机地址
     * @property port 端口号
     */
    data class Tcp(val host: String, val port: Int) : SocketType() {
        companion object {
            const val DEFAULT_HOST = "127.0.0.1"
            const val DEFAULT_PORT = 8080
        }
    }

    /**
     * 获取通道类型
     *
     * @return 对应的ChannelType
     */
    fun toChannelType(): ChannelType = when (this) {
        is Unix -> ChannelType.SOCKET_UNIX
        is Tcp -> ChannelType.SOCKET_TCP
    }

    /**
     * 获取连接地址描述
     *
     * @return 连接地址字符串
     */
    fun toAddressString(): String = when (this) {
        is Unix -> "unix://$path"
        is Tcp -> "tcp://$host:$port"
    }
}

/**
 * Socket通道配置
 *
 * 用于配置Unix Domain Socket和TCP Socket连接参数
 */
class SocketConfig(
    override val type: ChannelType,
    override val port: Int = 0,
    override val path: String? = null,
    override val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    override val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    override val enableReconnect: Boolean = true,
    override val maxReconnectAttempts: Int = MAX_RECONNECT_ATTEMPTS,
    override val reconnectDelayMs: Long = RECONNECT_DELAY_MS,
    val socketType: SocketType,
    val host: String? = null,
    val connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS,
    val readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
    val writeTimeoutMs: Long = DEFAULT_WRITE_TIMEOUT_MS,
    val keepAlive: Boolean = true,
    val tcpNoDelay: Boolean = true,
    val sendBufferSize: Int = DEFAULT_SEND_BUFFER_SIZE,
    val receiveBufferSize: Int = DEFAULT_RECEIVE_BUFFER_SIZE,
    val soLinger: Int = DEFAULT_SO_LINGER,
    val trafficClass: Int = DEFAULT_TRAFFIC_CLASS
) : ChannelConfig(
    type = type,
    port = port,
    path = path,
    bufferSize = bufferSize,
    timeoutMs = timeoutMs,
    enableReconnect = enableReconnect,
    maxReconnectAttempts = maxReconnectAttempts,
    reconnectDelayMs = reconnectDelayMs
) {
    companion object {
        // 连接超时配置
        const val DEFAULT_CONNECTION_TIMEOUT_MS = 10000L // 10秒
        const val DEFAULT_READ_TIMEOUT_MS = 30000L // 30秒
        const val DEFAULT_WRITE_TIMEOUT_MS = 30000L // 30秒

        // 缓冲区大小配置
        const val DEFAULT_SEND_BUFFER_SIZE = 262144 // 256KB
        const val DEFAULT_RECEIVE_BUFFER_SIZE = 262144 // 256KB

        // Socket选项
        const val DEFAULT_SO_LINGER = -1 // 禁用SO_LINGER
        const val DEFAULT_TRAFFIC_CLASS = 0 // 默认流量类别

        // Unix Domain Socket默认配置
        fun unixDefault(
            path: String = SocketType.Unix.DEFAULT_PATH,
            bufferSize: Int = DEFAULT_BUFFER_SIZE,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS
        ): SocketConfig = SocketConfig(
            type = ChannelType.SOCKET_UNIX,
            path = path,
            socketType = SocketType.Unix(path),
            bufferSize = bufferSize,
            timeoutMs = timeoutMs
        )

        // TCP Socket默认配置
        fun tcpDefault(
            host: String = SocketType.Tcp.DEFAULT_HOST,
            port: Int = SocketType.Tcp.DEFAULT_PORT,
            bufferSize: Int = DEFAULT_BUFFER_SIZE,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS
        ): SocketConfig = SocketConfig(
            type = ChannelType.SOCKET_TCP,
            port = port,
            host = host,
            socketType = SocketType.Tcp(host, port),
            bufferSize = bufferSize,
            timeoutMs = timeoutMs
        )
    }

    /**
     * 验证配置有效性
     *
     * @return 配置是否有效
     */
    fun validateConfig(): Boolean {
        return when (socketType) {
            is SocketType.Unix -> socketType.path.isNotBlank()
            is SocketType.Tcp -> socketType.host.isNotBlank() && socketType.port > 0
        }
    }

    /**
     * 获取连接地址
     *
     * @return 连接地址字符串
     */
    fun getConnectionAddress(): String = socketType.toAddressString()

    /**
     * 创建Builder用于构建配置
     */
    class Builder {
        private var socketType: SocketType? = null
        private var bufferSize: Int = DEFAULT_BUFFER_SIZE
        private var timeoutMs: Long = DEFAULT_TIMEOUT_MS
        private var connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS
        private var readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS
        private var writeTimeoutMs: Long = DEFAULT_WRITE_TIMEOUT_MS
        private var enableReconnect: Boolean = true
        private var maxReconnectAttempts: Int = MAX_RECONNECT_ATTEMPTS
        private var reconnectDelayMs: Long = RECONNECT_DELAY_MS
        private var keepAlive: Boolean = true
        private var tcpNoDelay: Boolean = true
        private var sendBufferSize: Int = DEFAULT_SEND_BUFFER_SIZE
        private var receiveBufferSize: Int = DEFAULT_RECEIVE_BUFFER_SIZE
        private var soLinger: Int = DEFAULT_SO_LINGER
        private var trafficClass: Int = DEFAULT_TRAFFIC_CLASS

        /**
         * 设置Unix Domain Socket
         */
        fun unixSocket(path: String) = apply {
            socketType = SocketType.Unix(path)
        }

        /**
         * 设置TCP Socket
         */
        fun tcpSocket(host: String, port: Int) = apply {
            socketType = SocketType.Tcp(host, port)
        }

        /**
         * 设置缓冲区大小
         */
        fun bufferSize(size: Int) = apply {
            bufferSize = size
        }

        /**
         * 设置超时时间
         */
        fun timeout(timeout: Long) = apply {
            timeoutMs = timeout
        }

        /**
         * 设置连接超时
         */
        fun connectionTimeout(timeout: Long) = apply {
            connectionTimeoutMs = timeout
        }

        /**
         * 设置读取超时
         */
        fun readTimeout(timeout: Long) = apply {
            readTimeoutMs = timeout
        }

        /**
         * 设置写入超时
         */
        fun writeTimeout(timeout: Long) = apply {
            writeTimeoutMs = timeout
        }

        /**
         * 设置是否启用重连
         */
        fun enableReconnect(enable: Boolean) = apply {
            enableReconnect = enable
        }

        /**
         * 设置最大重连次数
         */
        fun maxReconnectAttempts(attempts: Int) = apply {
            maxReconnectAttempts = attempts
        }

        /**
         * 设置重连延迟
         */
        fun reconnectDelay(delay: Long) = apply {
            reconnectDelayMs = delay
        }

        /**
         * 设置KeepAlive选项
         */
        fun keepAlive(enable: Boolean) = apply {
            keepAlive = enable
        }

        /**
         * 设置TCP NoDelay选项
         */
        fun tcpNoDelay(enable: Boolean) = apply {
            tcpNoDelay = enable
        }

        /**
         * 设置发送缓冲区大小
         */
        fun sendBufferSize(size: Int) = apply {
            sendBufferSize = size
        }

        /**
         * 设置接收缓冲区大小
         */
        fun receiveBufferSize(size: Int) = apply {
            receiveBufferSize = size
        }

        /**
         * 设置SO_LINGER选项
         */
        fun soLinger(linger: Int) = apply {
            soLinger = linger
        }

        /**
         * 设置流量类别
         */
        fun trafficClass(tc: Int) = apply {
            trafficClass = tc
        }

        /**
         * 构建SocketConfig
         *
         * @throws IllegalStateException 如果未设置socketType
         */
        fun build(): SocketConfig {
            val type = socketType ?: throw IllegalStateException("Socket type must be set")
            return SocketConfig(
                type = type.toChannelType(),
                port = (type as? SocketType.Tcp)?.port ?: 0,
                path = (type as? SocketType.Unix)?.path,
                host = (type as? SocketType.Tcp)?.host,
                socketType = type,
                bufferSize = bufferSize,
                timeoutMs = timeoutMs,
                connectionTimeoutMs = connectionTimeoutMs,
                readTimeoutMs = readTimeoutMs,
                writeTimeoutMs = writeTimeoutMs,
                enableReconnect = enableReconnect,
                maxReconnectAttempts = maxReconnectAttempts,
                reconnectDelayMs = reconnectDelayMs,
                keepAlive = keepAlive,
                tcpNoDelay = tcpNoDelay,
                sendBufferSize = sendBufferSize,
                receiveBufferSize = receiveBufferSize,
                soLinger = soLinger,
                trafficClass = trafficClass
            )
        }
    }
}

/**
 * Socket选项配置
 *
 * 用于运行时修改Socket选项
 */
data class SocketOptions(
    val keepAlive: Boolean = true,
    val tcpNoDelay: Boolean = true,
    val sendBufferSize: Int = SocketConfig.DEFAULT_SEND_BUFFER_SIZE,
    val receiveBufferSize: Int = SocketConfig.DEFAULT_RECEIVE_BUFFER_SIZE,
    val soTimeout: Int = SocketConfig.DEFAULT_READ_TIMEOUT_MS.toInt(),
    val soLinger: Int = SocketConfig.DEFAULT_SO_LINGER,
    val trafficClass: Int = SocketConfig.DEFAULT_TRAFFIC_CLASS,
    val reuseAddress: Boolean = true,
    val oobInline: Boolean = false
) {
    companion object {
        /**
         * 默认Socket选项
         */
        val DEFAULT = SocketOptions()

        /**
         * 高性能Socket选项
         */
        val HIGH_PERFORMANCE = SocketOptions(
            keepAlive = true,
            tcpNoDelay = true,
            sendBufferSize = 524288, // 512KB
            receiveBufferSize = 524288, // 512KB
            soTimeout = 60000, // 60秒
            reuseAddress = true
        )

        /**
         * 低延迟Socket选项
         */
        val LOW_LATENCY = SocketOptions(
            keepAlive = true,
            tcpNoDelay = true,
            sendBufferSize = 65536, // 64KB
            receiveBufferSize = 65536, // 64KB
            soTimeout = 5000, // 5秒
            reuseAddress = true
        )
    }
}