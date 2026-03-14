package com.dfa.core.vm.communication

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通信错误处理器接口
 */
interface CommunicationErrorHandler {
    /**
     * 处理错误
     *
     * @param channelId 通道ID
     * @param error 错误信息
     * @return 处理结果
     */
    suspend fun handleError(channelId: String, error: CommunicationError): ErrorHandlingResult

    /**
     * 获取错误统计
     *
     * @return 错误统计信息
     */
    fun getErrorStatistics(): ErrorStatistics

    /**
     * 清除错误历史
     */
    fun clearErrorHistory()

    /**
     * 设置错误回调
     *
     * @param callback 错误回调函数
     */
    fun setErrorCallback(callback: (String, CommunicationError) -> Unit)
}

/**
 * 错误处理结果
 */
sealed class ErrorHandlingResult {
    data class Retry(val delayMs: Long) : ErrorHandlingResult()
    data object Abort : ErrorHandlingResult()
    data object Ignore : ErrorHandlingResult()
    data class Escalate(val reason: String) : ErrorHandlingResult()
}

/**
 * 错误统计信息
 */
data class ErrorStatistics(
    val totalErrors: Int = 0,
    val connectionErrors: Int = 0,
    val channelErrors: Int = 0,
    val protocolErrors: Int = 0,
    val timeoutErrors: Int = 0,
    val permissionErrors: Int = 0,
    val unknownErrors: Int = 0,
    val lastErrorTime: Long? = null,
    val lastErrorMessage: String? = null,
    val errorsByChannel: Map<String, Int> = emptyMap()
)

/**
 * 错误记录
 */
data class ErrorRecord(
    val channelId: String,
    val error: CommunicationError,
    val timestamp: Long = System.currentTimeMillis(),
    val handled: Boolean = false,
    val handlingResult: ErrorHandlingResult? = null
)

/**
 * 通信错误处理器实现
 */
@Singleton
class CommunicationErrorHandlerImpl @Inject constructor(
    private val reconnectionStrategyFactory: ReconnectionStrategyFactory
) : CommunicationErrorHandler {

    private val errorHistory = mutableListOf<ErrorRecord>()
    private val errorCountsByChannel = mutableMapOf<String, Int>()
    private var errorCallback: ((String, CommunicationError) -> Unit)? = null

    private val _errorStatistics = MutableStateFlow(ErrorStatistics())
    val errorStatistics: StateFlow<ErrorStatistics> = _errorStatistics.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var cleanupJob: Job? = null

    init {
        startCleanupJob()
    }

    override suspend fun handleError(channelId: String, error: CommunicationError): ErrorHandlingResult {
        // 记录错误
        val record = ErrorRecord(channelId = channelId, error = error)
        synchronized(errorHistory) {
            errorHistory.add(record)
            errorCountsByChannel[channelId] = (errorCountsByChannel[channelId] ?: 0) + 1
        }

        // 更新统计
        updateStatistics()

        // 调用回调
        errorCallback?.invoke(channelId, error)

        // 根据错误类型决定处理方式
        return when (error) {
            is CommunicationError.PermissionError -> {
                ErrorHandlingResult.Abort
            }
            is CommunicationError.ConfigurationError -> {
                ErrorHandlingResult.Escalate("Configuration error: ${error.message}")
            }
            is CommunicationError.TimeoutError -> {
                val attemptCount = getRecentErrorCount(channelId, TIMEOUT_WINDOW_MS)
                if (attemptCount >= MAX_TIMEOUT_RETRIES) {
                    ErrorHandlingResult.Abort
                } else {
                    ErrorHandlingResult.Retry(calculateRetryDelay(attemptCount))
                }
            }
            is CommunicationError.ConnectionError -> {
                val attemptCount = getRecentErrorCount(channelId, CONNECTION_WINDOW_MS)
                if (attemptCount >= MAX_CONNECTION_RETRIES) {
                    ErrorHandlingResult.Escalate("Max connection retries exceeded")
                } else {
                    ErrorHandlingResult.Retry(calculateRetryDelay(attemptCount))
                }
            }
            is CommunicationError.ChannelError -> {
                val attemptCount = getRecentErrorCount(channelId, CHANNEL_WINDOW_MS)
                if (attemptCount >= MAX_CHANNEL_RETRIES) {
                    ErrorHandlingResult.Abort
                } else {
                    ErrorHandlingResult.Retry(calculateRetryDelay(attemptCount))
                }
            }
            is CommunicationError.ProtocolError -> {
                ErrorHandlingResult.Escalate("Protocol error: ${error.message}")
            }
            is CommunicationError.UnknownError -> {
                val attemptCount = getRecentErrorCount(channelId, UNKNOWN_WINDOW_MS)
                if (attemptCount >= MAX_UNKNOWN_RETRIES) {
                    ErrorHandlingResult.Abort
                } else {
                    ErrorHandlingResult.Retry(calculateRetryDelay(attemptCount))
                }
            }
        }
    }

    override fun getErrorStatistics(): ErrorStatistics = _errorStatistics.value

    override fun clearErrorHistory() {
        synchronized(errorHistory) {
            errorHistory.clear()
            errorCountsByChannel.clear()
        }
        updateStatistics()
    }

    override fun setErrorCallback(callback: (String, CommunicationError) -> Unit) {
        errorCallback = callback
    }

    private fun getRecentErrorCount(channelId: String, windowMs: Long): Int {
        val cutoffTime = System.currentTimeMillis() - windowMs
        synchronized(errorHistory) {
            return errorHistory.count { 
                it.channelId == channelId && it.timestamp > cutoffTime 
            }
        }
    }

    private fun calculateRetryDelay(attemptCount: Int): Long {
        val strategy = reconnectionStrategyFactory.create(ReconnectionStrategyType.EXPONENTIAL_BACKOFF)
        return strategy.getReconnectDelay(attemptCount)
    }

    private fun updateStatistics() {
        synchronized(errorHistory) {
            val stats = ErrorStatistics(
                totalErrors = errorHistory.size,
                connectionErrors = errorHistory.count { it.error is CommunicationError.ConnectionError },
                channelErrors = errorHistory.count { it.error is CommunicationError.ChannelError },
                protocolErrors = errorHistory.count { it.error is CommunicationError.ProtocolError },
                timeoutErrors = errorHistory.count { it.error is CommunicationError.TimeoutError },
                permissionErrors = errorHistory.count { it.error is CommunicationError.PermissionError },
                unknownErrors = errorHistory.count { it.error is CommunicationError.UnknownError },
                lastErrorTime = errorHistory.lastOrNull()?.timestamp,
                lastErrorMessage = errorHistory.lastOrNull()?.error?.message,
                errorsByChannel = errorCountsByChannel.toMap()
            )
            _errorStatistics.value = stats
        }
    }

    private fun startCleanupJob() {
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(CLEANUP_INTERVAL)
                cleanupOldErrors()
            }
        }
    }

    private fun cleanupOldErrors() {
        val cutoffTime = System.currentTimeMillis() - MAX_ERROR_AGE
        synchronized(errorHistory) {
            val iterator = errorHistory.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().timestamp < cutoffTime) {
                    iterator.remove()
                }
            }
        }
    }

    companion object {
        private const val CLEANUP_INTERVAL = 60000L // 1 minute
        private const val MAX_ERROR_AGE = 3600000L // 1 hour

        private const val TIMEOUT_WINDOW_MS = 60000L // 1 minute
        private const val CONNECTION_WINDOW_MS = 300000L // 5 minutes
        private const val CHANNEL_WINDOW_MS = 120000L // 2 minutes
        private const val UNKNOWN_WINDOW_MS = 300000L // 5 minutes

        private const val MAX_TIMEOUT_RETRIES = 3
        private const val MAX_CONNECTION_RETRIES = 5
        private const val MAX_CHANNEL_RETRIES = 3
        private const val MAX_UNKNOWN_RETRIES = 2
    }
}