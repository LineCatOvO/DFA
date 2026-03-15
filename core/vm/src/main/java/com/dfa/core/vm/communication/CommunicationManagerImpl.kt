package com.dfa.core.vm.communication

import com.dfa.core.vm.channel.VirtIOChannelImpl
import com.dfa.core.vm.channel.VsockChannelImpl
import com.dfa.core.vm.protocol.MessageCodec
import com.dfa.core.vm.protocol.MessageCodecImpl
import com.dfa.core.vm.protocol.MessageWrapper
import com.dfa.core.vm.protocol.Request
import com.dfa.core.vm.protocol.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通信管理器实现
 *
 * 管理Android宿主与虚拟机之间的双向通信
 */
@Singleton
class CommunicationManagerImpl @Inject constructor(
    private val messageCodec: MessageCodec
) : CommunicationManager {

    private val channels = ConcurrentHashMap<String, CommunicationChannel>()
    private val channelConfigs = ConcurrentHashMap<String, ChannelConfig>()

    private val _state = MutableStateFlow(CommunicationState.DISCONNECTED)
    private val _activeConnections = MutableStateFlow<List<ConnectionInfo>>(emptyList())
    private val _isInitialized = MutableStateFlow(false)

    private val _messageFlow = MutableSharedFlow<Pair<String, ByteArray>>(replay = 0, extraBufferCapacity = 128)

    private val scope = CoroutineScope(Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var monitorJob: Job? = null

    override val state: StateFlow<CommunicationState> = _state.asStateFlow()
    override val activeConnections: StateFlow<List<ConnectionInfo>> = _activeConnections.asStateFlow()
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    override suspend fun initialize(configs: List<ChannelConfig>): Result<List<ConnectionInfo>> {
        if (configs.isEmpty()) {
            return Result.failure(CommunicationError.ConfigurationError("No channel configurations provided"))
        }

        val results = mutableListOf<ConnectionInfo>()
        val errors = mutableListOf<CommunicationError>()

        for (config in configs) {
            val result = addChannel(config)
            result.fold(
                onSuccess = { results.add(it) },
                onFailure = { errors.add(it as CommunicationError) }
            )
        }

        return if (results.isNotEmpty()) {
            _isInitialized.value = true
            updateState()
            startHeartbeat()
            startMonitor()
            Result.success(results)
        } else {
            Result.failure(errors.firstOrNull() ?: CommunicationError.UnknownError("Failed to initialize channels"))
        }
    }

    override suspend fun addChannel(config: ChannelConfig): Result<ConnectionInfo> {
        if (!config.validate()) {
            return Result.failure(CommunicationError.ConfigurationError("Invalid channel configuration"))
        }

        val channel = createChannel(config)
        val result = channel.connect(config)

        return result.fold(
            onSuccess = { info ->
                channels[info.channelId] = channel
                channelConfigs[info.channelId] = config
                updateActiveConnections()
                startChannelReceive(info.channelId, channel)
                Result.success(info)
            },
            onFailure = { Result.failure(it as? CommunicationError ?: CommunicationError.ConnectionError(it.message ?: "Unknown error")) }
        )
    }

    override suspend fun removeChannel(channelId: String): Result<Unit> {
        val channel = channels.remove(channelId) ?: return Result.failure(
            CommunicationError.ChannelError("Channel not found: $channelId")
        )

        channelConfigs.remove(channelId)
        val result = channel.disconnect()
        updateActiveConnections()
        updateState()

        return result
    }

    override fun getChannel(channelId: String): CommunicationChannel? {
        return channels[channelId]
    }

    override fun getActiveChannels(): List<CommunicationChannel> {
        return channels.values.toList()
    }

    override suspend fun send(channelId: String, message: ByteArray): Result<Unit> {
        val channel = channels[channelId] ?: return Result.failure(
            CommunicationError.ChannelError("Channel not found: $channelId")
        )

        if (!channel.isConnected()) {
            return Result.failure(CommunicationError.ChannelError("Channel not connected: $channelId"))
        }

        return channel.send(message)
    }

    override suspend fun broadcast(message: ByteArray): Map<String, Result<Unit>> {
        return channels.mapValues { (_, channel) ->
            if (channel.isConnected()) {
                channel.send(message)
            } else {
                Result.failure(CommunicationError.ChannelError("Channel not connected"))
            }
        }
    }

    override fun receive(channelId: String?): Flow<Pair<String, ByteArray>> {
        return if (channelId != null) {
            _messageFlow.asSharedFlow().filter { it.first == channelId }
        } else {
            _messageFlow.asSharedFlow()
        }
    }

    override fun getConnectionInfo(channelId: String): ConnectionInfo? {
        return channels[channelId]?.getConnectionInfo()
    }

    override suspend fun reconnect(channelId: String): Result<ConnectionInfo> {
        val channel = channels[channelId] ?: return Result.failure(
            CommunicationError.ChannelError("Channel not found: $channelId")
        )

        val config = channelConfigs[channelId] ?: return Result.failure(
            CommunicationError.ConfigurationError("Configuration not found for channel: $channelId")
        )

        channel.disconnect()
        return channel.connect(config)
    }

    override suspend fun closeAll() {
        channels.values.forEach { channel ->
            try {
                channel.disconnect()
            } catch (e: Exception) {
                // 忽略断开连接时的错误
            }
        }
        channels.clear()
        channelConfigs.clear()
        updateActiveConnections()
        updateState()
    }

    override suspend fun release() {
        heartbeatJob?.cancel()
        monitorJob?.cancel()
        closeAll()
        _isInitialized.value = false
    }

    private fun createChannel(config: ChannelConfig): CommunicationChannel {
        return when (config.type) {
            ChannelType.VIRTIO_SERIAL -> VirtIOChannelImpl()
            ChannelType.VSOCK -> VsockChannelImpl()
            ChannelType.SHARED_MEMORY -> {
                // 共享内存通道暂未实现，使用VirtIO作为后备
                VirtIOChannelImpl()
            }
        }
    }

    private fun startChannelReceive(channelId: String, channel: CommunicationChannel) {
        scope.launch {
            channel.receiveData.collect { data ->
                _messageFlow.emit(channelId to data)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL)
                channels.values.forEach { channel ->
                    if (channel.isConnected()) {
                        // 发送心跳消息
                        // 实际实现需要根据协议发送心跳
                    }
                }
            }
        }
    }

    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                delay(MONITOR_INTERVAL)
                updateActiveConnections()
                updateState()
            }
        }
    }

    private fun updateActiveConnections() {
        _activeConnections.value = channels.values.map { it.getConnectionInfo() }
    }

    private fun updateState() {
        val connectedCount = channels.values.count { it.isConnected() }
        _state.value = when {
            channels.isEmpty() -> CommunicationState.DISCONNECTED
            connectedCount == channels.size -> CommunicationState.CONNECTED
            connectedCount == 0 -> CommunicationState.ERROR
            else -> CommunicationState.RECONNECTING
        }
    }

    companion object {
        private const val HEARTBEAT_INTERVAL = 30000L // 30 seconds
        private const val MONITOR_INTERVAL = 5000L // 5 seconds
    }
}