package com.dfa.core.vm

/**
 * 虚拟机异常基类
 */
sealed class VmException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)

    /**
     * 配置异常
     */
    data class ConfigurationException(
        override val message: String,
        val configId: String? = null
    ) : VmException(message)

    /**
     * 资源异常
     */
    data class ResourceException(
        override val message: String,
        val resourceType: ResourceType,
        val required: Long,
        val available: Long
    ) : VmException(message) {
        enum class ResourceType {
            MEMORY, CPU, DISK, NETWORK, GPU
        }
    }

    /**
     * 状态异常
     */
    data class StateException(
        override val message: String,
        val currentState: VmState,
        val expectedState: VmState
    ) : VmException(message)

    /**
     * 操作超时异常
     */
    data class TimeoutException(
        override val message: String,
        val operation: String,
        val timeoutMs: Long
    ) : VmException(message)

    /**
     * 权限异常
     */
    data class PermissionException(
        override val message: String,
        val permission: String? = null
    ) : VmException(message)

    /**
     * AVF异常
     */
    data class AvfException(
        override val message: String,
        override val cause: Throwable? = null,
        val errorCode: Int? = null
    ) : VmException(message, cause)

    /**
     * 虚拟机未找到异常
     */
    data class VmNotFoundException(
        override val message: String,
        val vmId: String
    ) : VmException(message)

    /**
     * 虚拟机已存在异常
     */
    data class VmAlreadyExistsException(
        override val message: String,
        val vmId: String
    ) : VmException(message)

    /**
     * 操作不支持异常
     */
    data class OperationNotSupportedException(
        override val message: String,
        val operation: String,
        val currentState: VmState
    ) : VmException(message)
}

/**
 * 异常处理工具
 */
object VmExceptionHandler {
    /**
     * 将VmError转换为VmException
     */
    fun fromVmError(error: VmError, vmId: String? = null): VmException {
        return when (error) {
            is VmError.ConfigurationError -> VmException.ConfigurationException(
                message = error.message,
                configId = vmId
            )
            is VmError.ResourceError -> VmException.ResourceException(
                message = error.message,
                resourceType = VmException.ResourceException.ResourceType.MEMORY,
                required = 0,
                available = 0
            )
            is VmError.NetworkError -> VmException.AvfException(
                message = error.message
            )
            is VmError.PermissionError -> VmException.PermissionException(
                message = error.message
            )
            is VmError.TimeoutError -> VmException.TimeoutException(
                message = error.message,
                operation = "unknown",
                timeoutMs = 0
            )
            is VmError.UnknownError -> VmException.AvfException(
                message = error.message,
                cause = error.cause
            )
        }
    }

    /**
     * 判断异常是否可重试
     */
    fun isRetryable(exception: VmException): Boolean {
        return when (exception) {
            is VmException.TimeoutException -> true
            is VmException.AvfException -> exception.errorCode?.let { it in 500..599 } ?: false
            is VmException.ResourceException -> false
            is VmException.StateException -> false
            is VmException.ConfigurationException -> false
            is VmException.PermissionException -> false
            is VmException.VmNotFoundException -> false
            is VmException.VmAlreadyExistsException -> false
            is VmException.OperationNotSupportedException -> false
        }
    }
}