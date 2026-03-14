package com.dfa.core.vm.communication

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 通信管理器接口
 *
 * 管理Android宿主与虚拟机之间的双向通信
 */
interface CommunicationManager {

    /**
     * 通信状态流
     */
    val state: StateFlow<CommunicationState>

    /**
     * 活跃连接信息流
     */
    val activeConnections: StateFlow<List<ConnectionInfo>>

    /**
     * 是否已初始化
     */
    val isInitialized: StateFlow<Boolean>

    /**
     * 初始化通信管理器
     *
     * @param configs 通道配置列表
     * @return 初始化结果
     */
    suspend fun initialize(configs: List<ChannelConfig>): Result<List<ConnectionInfo>>

    /**
     * 添加通信通道
     *
     * @param config 通道配置
     * @return 添加结果
     */
    suspend fun addChannel(config: ChannelConfig): Result<ConnectionInfo>

    /**
     * 移除通信通道
     *
     * @param channelId 通道ID
     * @return 移除结果
     */
    suspend fun removeChannel(channelId: String): Result<Unit>

    /**
     * 获取通道
     *
     * @param channelId 通道ID
     * @return 通信通道，如果不存在则返回null
     */
    fun getChannel(channelId: String): CommunicationChannel?

    /**
     * 获取所有活跃通道
     *
     * @return 通道列表
     */
    fun getActiveChannels(): List<CommunicationChannel>

    /**
     * 发送消息
     *
     * @param channelId 通道ID
     * @param message 消息内容
     * @return 发送结果
     */
    suspend fun send(channelId: String, message: ByteArray): Result<Unit>

    /**
     * 发送消息到所有通道
     *
     * @param message 消息内容
     * @return 发送结果列表
     */
    suspend fun broadcast(message: ByteArray): Map<String, Result<Unit>>

    /**
     * 接收消息流
     *
     * @param channelId 通道ID，如果为null则接收所有通道的消息
     * @return 消息流
     */
    fun receive(channelId: String? = null): Flow<Pair<String, ByteArray>>

    /**
     * 获取连接信息
     *
     * @param channelId 通道ID
     * @return 连接信息，如果不存在则返回null
     */
    fun getConnectionInfo(channelId: String): ConnectionInfo?

    /**
     * 重连指定通道
     *
     * @param channelId 通道ID
     * @return 重连结果
     */
    suspend fun reconnect(channelId: String): Result<ConnectionInfo>

    /**
     * 关闭所有连接
     */
    suspend fun closeAll()

    /**
     * 释放资源
     */
    suspend fun release()
}

/**
 * 通信管理器工厂接口
 */
interface CommunicationManagerFactory {
    /**
     * 创建通信管理器实例
     *
     * @return 通信管理器实例
     */
    fun create(): CommunicationManager
}