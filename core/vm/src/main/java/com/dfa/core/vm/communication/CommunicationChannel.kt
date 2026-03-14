package com.dfa.core.vm.communication

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 通信通道接口
 *
 * 定义虚拟机与宿主之间通信的基本接口
 */
interface CommunicationChannel {

    /**
     * 通道ID
     */
    val channelId: String

    /**
     * 通道类型
     */
    val channelType: ChannelType

    /**
     * 连接状态流
     */
    val state: StateFlow<CommunicationState>

    /**
     * 连接信息流
     */
    val connectionInfo: StateFlow<ConnectionInfo>

    /**
     * 接收数据流
     */
    val receiveData: Flow<ByteArray>

    /**
     * 连接通道
     *
     * @param config 通道配置
     * @return 连接结果
     */
    suspend fun connect(config: ChannelConfig): Result<ConnectionInfo>

    /**
     * 断开连接
     *
     * @return 断开结果
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * 发送数据
     *
     * @param data 要发送的数据
     * @return 发送结果
     */
    suspend fun send(data: ByteArray): Result<Unit>

    /**
     * 发送数据（带超时）
     *
     * @param data 要发送的数据
     * @param timeoutMs 超时时间（毫秒）
     * @return 发送结果
     */
    suspend fun send(data: ByteArray, timeoutMs: Long): Result<Unit>

    /**
     * 检查是否已连接
     *
     * @return 是否已连接
     */
    fun isConnected(): Boolean

    /**
     * 获取当前连接信息
     *
     * @return 连接信息
     */
    fun getConnectionInfo(): ConnectionInfo

    /**
     * 释放资源
     */
    suspend fun release()
}

/**
 * 通信通道工厂接口
 */
interface CommunicationChannelFactory {
    /**
     * 创建通信通道
     *
     * @param type 通道类型
     * @param config 通道配置
     * @return 通信通道实例
     */
    fun create(type: ChannelType, config: ChannelConfig): CommunicationChannel

    /**
     * 检查是否支持指定通道类型
     *
     * @param type 通道类型
     * @return 是否支持
     */
    fun isSupported(type: ChannelType): Boolean
}