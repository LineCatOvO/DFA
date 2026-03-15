package com.dfa.core.docker.provider

import com.dfa.core.docker.DockerException

/**
 * Docker Provider异常基类
 *
 * 所有Docker Provider相关的异常都继承自此类。
 * 使用sealed class限制异常类型层次。
 *
 * @param message 异常消息
 * @param cause 原始异常
 * @param providerId Provider标识符
 * @since 1.0.0
 */
sealed class DockerProviderException(
    message: String,
    cause: Throwable? = null,
    val providerId: String? = null
) : DockerException(message, cause)

/**
 * Provider未找到异常
 *
 * 当指定的Provider不存在时抛出。
 *
 * @param providerId Provider标识符
 * @param message 异常消息
 * @since 1.0.0
 */
class ProviderNotFoundException(
    providerId: String,
    message: String = "Docker provider not found: $providerId"
) : DockerProviderException(message, providerId = providerId)

/**
 * Provider不可用异常
 *
 * 当Provider无法提供服务时抛出。
 *
 * @param providerId Provider标识符
 * @param reason 不可用原因
 * @param cause 原始异常
 * @since 1.0.0
 */
class ProviderUnavailableException(
    providerId: String,
    val reason: String,
    cause: Throwable? = null,
    message: String = "Docker provider '$providerId' is unavailable: $reason"
) : DockerProviderException(message, cause, providerId)

/**
 * Provider初始化异常
 *
 * 当Provider初始化失败时抛出。
 *
 * @param providerId Provider标识符
 * @param reason 初始化失败原因
 * @param cause 原始异常
 * @since 1.0.0
 */
class ProviderInitializationException(
    providerId: String,
    val reason: String,
    cause: Throwable? = null,
    message: String = "Failed to initialize Docker provider '$providerId': $reason"
) : DockerProviderException(message, cause, providerId)

/**
 * Provider超时异常
 *
 * 当Provider操作超时时抛出。
 *
 * @param providerId Provider标识符
 * @param operation 超时的操作名称
 * @param timeoutMs 超时时间（毫秒）
 * @param cause 原始异常
 * @since 1.0.0
 */
class ProviderTimeoutException(
    providerId: String,
    val operation: String,
    val timeoutMs: Long,
    cause: Throwable? = null,
    message: String = "Docker provider '$providerId' operation '$operation' timed out after ${timeoutMs}ms"
) : DockerProviderException(message, cause, providerId)

/**
 * Provider状态异常
 *
 * 当Provider处于不允许操作的状态时抛出。
 *
 * @param providerId Provider标识符
 * @param currentState 当前状态
 * @param expectedStates 期望的状态列表
 * @since 1.0.0
 */
class ProviderStateException(
    providerId: String,
    val currentState: DockerProviderState,
    val expectedStates: List<DockerProviderState>,
    message: String = "Docker provider '$providerId' is in $currentState state, expected one of $expectedStates"
) : DockerProviderException(message, providerId = providerId)

/**
 * Provider配置异常
 *
 * 当Provider配置无效时抛出。
 *
 * @param providerId Provider标识符
 * @param configField 配置字段名
 * @param reason 无效原因
 * @since 1.0.0
 */
class ProviderConfigException(
    providerId: String?,
    val configField: String,
    val reason: String,
    message: String = if (providerId != null) {
        "Invalid configuration for provider '$providerId': field '$configField' - $reason"
    } else {
        "Invalid configuration: field '$configField' - $reason"
    }
) : DockerProviderException(message, providerId = providerId)

/**
 * Provider启动异常
 *
 * 当Provider启动失败时抛出。
 *
 * @param providerId Provider标识符
 * @param reason 启动失败原因
 * @param cause 原始异常
 * @since 1.0.0
 */
class ProviderStartException(
    providerId: String,
    val reason: String,
    cause: Throwable? = null,
    message: String = "Failed to start Docker provider '$providerId': $reason"
) : DockerProviderException(message, cause, providerId)

/**
 * Provider停止异常
 *
 * 当Provider停止失败时抛出。
 *
 * @param providerId Provider标识符
 * @param reason 停止失败原因
 * @param cause 原始异常
 * @since 1.0.0
 */
class ProviderStopException(
    providerId: String,
    val reason: String,
    cause: Throwable? = null,
    message: String = "Failed to stop Docker provider '$providerId': $reason"
) : DockerProviderException(message, cause, providerId)

/**
 * Provider销毁异常
 *
 * 当Provider销毁失败时抛出。
 *
 * @param providerId Provider标识符
 * @param reason 销毁失败原因
 * @param cause 原始异常
 * @since 1.0.0
 */
class ProviderDestroyException(
    providerId: String,
    val reason: String,
    cause: Throwable? = null,
    message: String = "Failed to destroy Docker provider '$providerId': $reason"
) : DockerProviderException(message, cause, providerId)

// ==================== 扩展函数 ====================

/**
 * 检查是否为可重试的Provider异常
 *
 * @return 如果异常可以重试则返回true
 */
fun DockerProviderException.isRetryable(): Boolean {
    return when (this) {
        is ProviderTimeoutException,
        is ProviderUnavailableException -> true
        else -> false
    }
}

/**
 * 获取Provider异常的错误代码
 *
 * @return 错误代码字符串
 */
fun DockerProviderException.getErrorCode(): String {
    return when (this) {
        is ProviderNotFoundException -> "PROVIDER_NOT_FOUND"
        is ProviderUnavailableException -> "PROVIDER_UNAVAILABLE"
        is ProviderInitializationException -> "PROVIDER_INIT_ERROR"
        is ProviderTimeoutException -> "PROVIDER_TIMEOUT"
        is ProviderStateException -> "PROVIDER_STATE_ERROR"
        is ProviderConfigException -> "PROVIDER_CONFIG_ERROR"
        is ProviderStartException -> "PROVIDER_START_ERROR"
        is ProviderStopException -> "PROVIDER_STOP_ERROR"
        is ProviderDestroyException -> "PROVIDER_DESTROY_ERROR"
    }
}