package com.dfa.core.vm.communication

import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * 重连策略接口
 */
interface ReconnectionStrategy {
    /**
     * 是否应该重连
     *
     * @param attempt 当前尝试次数
     * @param error 错误信息
     * @return 是否应该重连
     */
    fun shouldReconnect(attempt: Int, error: CommunicationError): Boolean

    /**
     * 获取重连延迟时间
     *
     * @param attempt 当前尝试次数
     * @return 延迟时间（毫秒）
     */
    fun getReconnectDelay(attempt: Int): Long

    /**
     * 获取最大重连次数
     *
     * @return 最大重连次数
     */
    fun getMaxAttempts(): Int

    /**
     * 重置策略状态
     */
    fun reset()
}

/**
 * 重连策略类型
 */
enum class ReconnectionStrategyType {
    FIXED_DELAY,
    EXPONENTIAL_BACKOFF,
    LINEAR_BACKOFF,
    ADAPTIVE
}

/**
 * 重连策略配置
 */
data class ReconnectionConfig(
    val type: ReconnectionStrategyType = ReconnectionStrategyType.EXPONENTIAL_BACKOFF,
    val maxAttempts: Int = MAX_ATTEMPTS,
    val initialDelayMs: Long = INITIAL_DELAY_MS,
    val maxDelayMs: Long = MAX_DELAY_MS,
    val multiplier: Double = MULTIPLIER,
    val jitterFactor: Double = JITTER_FACTOR
) {
    companion object {
        const val MAX_ATTEMPTS = 5
        const val INITIAL_DELAY_MS = 1000L
        const val MAX_DELAY_MS = 30000L
        const val MULTIPLIER = 2.0
        const val JITTER_FACTOR = 0.1
    }
}

/**
 * 指数退避重连策略
 */
class ExponentialBackoffStrategy @Inject constructor(
    private val config: ReconnectionConfig = ReconnectionConfig()
) : ReconnectionStrategy {

    private var currentAttempt = 0

    override fun shouldReconnect(attempt: Int, error: CommunicationError): Boolean {
        return when (error) {
            is CommunicationError.PermissionError -> false
            is CommunicationError.ConfigurationError -> false
            else -> attempt < config.maxAttempts
        }
    }

    override fun getReconnectDelay(attempt: Int): Long {
        val baseDelay = config.initialDelayMs * Math.pow(config.multiplier, attempt.toDouble())
        val delay = Math.min(baseDelay.toLong(), config.maxDelayMs)
        
        // 添加抖动以避免同时重连
        val jitter = delay * config.jitterFactor * (Math.random() * 2 - 1)
        return (delay + jitter).toLong().coerceAtLeast(0)
    }

    override fun getMaxAttempts(): Int = config.maxAttempts

    override fun reset() {
        currentAttempt = 0
    }
}

/**
 * 固定延迟重连策略
 */
class FixedDelayStrategy @Inject constructor(
    private val delayMs: Long = ReconnectionConfig.INITIAL_DELAY_MS,
    private val maxAttempts: Int = ReconnectionConfig.MAX_ATTEMPTS
) : ReconnectionStrategy {

    override fun shouldReconnect(attempt: Int, error: CommunicationError): Boolean {
        return when (error) {
            is CommunicationError.PermissionError -> false
            is CommunicationError.ConfigurationError -> false
            else -> attempt < maxAttempts
        }
    }

    override fun getReconnectDelay(attempt: Int): Long = delayMs

    override fun getMaxAttempts(): Int = maxAttempts

    override fun reset() {
        // 无状态，无需重置
    }
}

/**
 * 线性退避重连策略
 */
class LinearBackoffStrategy @Inject constructor(
    private val config: ReconnectionConfig = ReconnectionConfig()
) : ReconnectionStrategy {

    override fun shouldReconnect(attempt: Int, error: CommunicationError): Boolean {
        return when (error) {
            is CommunicationError.PermissionError -> false
            is CommunicationError.ConfigurationError -> false
            else -> attempt < config.maxAttempts
        }
    }

    override fun getReconnectDelay(attempt: Int): Long {
        val delay = config.initialDelayMs + (attempt * config.initialDelayMs)
        return delay.coerceAtMost(config.maxDelayMs)
    }

    override fun getMaxAttempts(): Int = config.maxAttempts

    override fun reset() {
        // 无状态，无需重置
    }
}

/**
 * 自适应重连策略
 */
class AdaptiveReconnectionStrategy @Inject constructor(
    private val config: ReconnectionConfig = ReconnectionConfig()
) : ReconnectionStrategy {

    private var consecutiveFailures = 0
    private var lastSuccessTime = System.currentTimeMillis()

    override fun shouldReconnect(attempt: Int, error: CommunicationError): Boolean {
        return when (error) {
            is CommunicationError.PermissionError -> false
            is CommunicationError.ConfigurationError -> false
            else -> {
                consecutiveFailures++
                attempt < calculateMaxAttempts()
            }
        }
    }

    override fun getReconnectDelay(attempt: Int): Long {
        val timeSinceLastSuccess = System.currentTimeMillis() - lastSuccessTime
        
        // 根据历史成功率调整延迟
        val adaptiveFactor = when {
            consecutiveFailures > 10 -> 2.0
            consecutiveFailures > 5 -> 1.5
            else -> 1.0
        }

        val baseDelay = config.initialDelayMs * Math.pow(config.multiplier, attempt.toDouble())
        val delay = Math.min((baseDelay * adaptiveFactor).toLong(), config.maxDelayMs)
        
        return delay
    }

    override fun getMaxAttempts(): Int = calculateMaxAttempts()

    override fun reset() {
        consecutiveFailures = 0
        lastSuccessTime = System.currentTimeMillis()
    }

    fun recordSuccess() {
        consecutiveFailures = 0
        lastSuccessTime = System.currentTimeMillis()
    }

    private fun calculateMaxAttempts(): Int {
        return when {
            consecutiveFailures > 10 -> config.maxAttempts / 2
            consecutiveFailures > 5 -> config.maxAttempts - 1
            else -> config.maxAttempts
        }
    }
}

/**
 * 重连策略工厂
 */
class ReconnectionStrategyFactory @Inject constructor() {
    
    fun create(type: ReconnectionStrategyType, config: ReconnectionConfig = ReconnectionConfig()): ReconnectionStrategy {
        return when (type) {
            ReconnectionStrategyType.FIXED_DELAY -> FixedDelayStrategy(
                delayMs = config.initialDelayMs,
                maxAttempts = config.maxAttempts
            )
            ReconnectionStrategyType.EXPONENTIAL_BACKOFF -> ExponentialBackoffStrategy(config)
            ReconnectionStrategyType.LINEAR_BACKOFF -> LinearBackoffStrategy(config)
            ReconnectionStrategyType.ADAPTIVE -> AdaptiveReconnectionStrategy(config)
        }
    }
}