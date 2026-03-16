package com.dfa.core.docker

/**
 * Docker异常基类
 * 
 * 所有Docker操作相关的异常都继承自此类。
 * 
 * @param message 异常消息
 * @param cause 原始异常
 * @param statusCode HTTP状态码（如适用）
 * @since 1.0.0
 */
open class DockerException(
    message: String,
    cause: Throwable? = null,
    val statusCode: Int? = null
) : RuntimeException(message, cause)

// ==================== 连接异常 ====================

/**
 * Docker连接异常
 * 
 * 当无法连接到Docker守护进程时抛出。
 */
class DockerConnectionException(
    message: String = "Failed to connect to Docker daemon",
    cause: Throwable? = null
) : DockerException(message, cause)

/**
 * Docker连接超时异常
 * 
 * 当连接Docker守护进程超时时抛出。
 */
class DockerTimeoutException(
    message: String = "Connection to Docker daemon timed out",
    cause: Throwable? = null
) : DockerException(message, cause)

/**
 * Docker认证异常
 * 
 * 当认证失败时抛出。
 */
class DockerAuthException(
    message: String = "Authentication failed",
    cause: Throwable? = null
) : DockerException(message, cause, statusCode = 401)

// ==================== 容器异常 ====================

/**
 * 容器异常基类
 */
open class ContainerException(
    message: String,
    cause: Throwable? = null,
    statusCode: Int? = null,
    val containerId: String? = null
) : DockerException(message, cause, statusCode)

/**
 * 容器未找到异常
 * 
 * 当指定的容器不存在时抛出。
 */
class ContainerNotFoundException(
    containerId: String,
    message: String = "Container not found: $containerId"
) : ContainerException(message, statusCode = 404, containerId = containerId)

/**
 * 容器已存在异常
 * 
 * 当尝试创建已存在的容器时抛出。
 */
class ContainerAlreadyExistsException(
    containerName: String,
    message: String = "Container already exists: $containerName"
) : ContainerException(message, statusCode = 409)

/**
 * 容器状态异常
 * 
 * 当容器处于不允许操作的状态时抛出。
 */
class ContainerStateException(
    containerId: String,
    currentState: String,
    expectedState: String,
    message: String = "Container $containerId is in $currentState state, expected $expectedState"
) : ContainerException(message, statusCode = 409, containerId = containerId)

/**
 * 容器操作异常
 * 
 * 当容器操作失败时抛出。
 */
class ContainerOperationException(
    containerId: String,
    operation: String,
    message: String = "Failed to $operation container: $containerId",
    cause: Throwable? = null
) : ContainerException(message, cause, containerId = containerId)

/**
 * 容器创建异常
 * 
 * 当容器创建失败时抛出。
 */
class ContainerCreateException(
    message: String,
    cause: Throwable? = null
) : ContainerException(message, cause, statusCode = 400)

// ==================== 镜像异常 ====================

/**
 * 镜像异常基类
 */
open class ImageException(
    message: String,
    cause: Throwable? = null,
    statusCode: Int? = null,
    val imageId: String? = null
) : DockerException(message, cause, statusCode)

/**
 * 镜像未找到异常
 * 
 * 当指定的镜像不存在时抛出。
 */
class ImageNotFoundException(
    imageId: String,
    message: String = "Image not found: $imageId"
) : ImageException(message, statusCode = 404, imageId = imageId)

/**
 * 镜像拉取异常
 * 
 * 当镜像拉取失败时抛出。
 */
class ImagePullException(
    imageName: String,
    message: String = "Failed to pull image: $imageName",
    cause: Throwable? = null
) : ImageException(message, cause, imageId = imageName)

/**
 * 镜像推送异常
 * 
 * 当镜像推送失败时抛出。
 */
class ImagePushException(
    imageName: String,
    message: String = "Failed to push image: $imageName",
    cause: Throwable? = null
) : ImageException(message, cause, imageId = imageName)

/**
 * 镜像构建异常
 * 
 * 当镜像构建失败时抛出。
 */
class ImageBuildException(
    message: String,
    cause: Throwable? = null,
    val buildLog: String? = null
) : ImageException(message, cause)

/**
 * 镜像删除异常
 * 
 * 当镜像删除失败时抛出。
 */
class ImageRemoveException(
    imageId: String,
    message: String = "Failed to remove image: $imageId",
    cause: Throwable? = null
) : ImageException(message, cause, imageId = imageId)

// ==================== 网络异常 ====================

/**
 * 网络异常基类
 */
open class NetworkException(
    message: String,
    cause: Throwable? = null,
    statusCode: Int? = null,
    val networkId: String? = null
) : DockerException(message, cause, statusCode)

/**
 * 网络未找到异常
 * 
 * 当指定的网络不存在时抛出。
 */
class NetworkNotFoundException(
    networkId: String,
    message: String = "Network not found: $networkId"
) : NetworkException(message, statusCode = 404, networkId = networkId)

/**
 * 网络创建异常
 * 
 * 当网络创建失败时抛出。
 */
class NetworkCreateException(
    networkName: String,
    message: String = "Failed to create network: $networkName",
    cause: Throwable? = null
) : NetworkException(message, cause)

/**
 * 网络连接异常
 * 
 * 当容器连接到网络失败时抛出。
 */
class NetworkConnectException(
    networkId: String,
    containerId: String,
    message: String = "Failed to connect container $containerId to network $networkId",
    cause: Throwable? = null
) : NetworkException(message, cause, networkId = networkId)

/**
 * 网络断开异常
 * 
 * 当容器从网络断开失败时抛出。
 */
class NetworkDisconnectException(
    networkId: String,
    containerId: String,
    message: String = "Failed to disconnect container $containerId from network $networkId",
    cause: Throwable? = null
) : NetworkException(message, cause, networkId = networkId)

// ==================== 卷异常 ====================

/**
 * 卷异常基类
 */
open class VolumeException(
    message: String,
    cause: Throwable? = null,
    statusCode: Int? = null,
    val volumeName: String? = null
) : DockerException(message, cause, statusCode)

/**
 * 卷未找到异常
 * 
 * 当指定的卷不存在时抛出。
 */
class VolumeNotFoundException(
    volumeName: String,
    message: String = "Volume not found: $volumeName"
) : VolumeException(message, statusCode = 404, volumeName = volumeName)

/**
 * 卷创建异常
 * 
 * 当卷创建失败时抛出。
 */
class VolumeCreateException(
    volumeName: String?,
    message: String = if (volumeName != null) "Failed to create volume: $volumeName" else "Failed to create volume",
    cause: Throwable? = null
) : VolumeException(message, cause, volumeName = volumeName)

/**
 * 卷删除异常
 * 
 * 当卷删除失败时抛出。
 */
class VolumeRemoveException(
    volumeName: String,
    message: String = "Failed to remove volume: $volumeName",
    cause: Throwable? = null
) : VolumeException(message, cause, volumeName = volumeName)

/**
 * 卷正在使用异常
 * 
 * 当尝试删除正在使用的卷时抛出。
 */
class VolumeInUseException(
    volumeName: String,
    message: String = "Volume $volumeName is in use and cannot be removed"
) : VolumeException(message, statusCode = 409, volumeName = volumeName)

// ==================== API异常 ====================

/**
 * Docker API异常
 * 
 * 当Docker API返回错误时抛出。
 */
class DockerApiException(
    message: String,
    val errorCode: String? = null,
    statusCode: Int? = null,
    cause: Throwable? = null
) : DockerException(message, cause, statusCode)

/**
 * Docker API速率限制异常
 * 
 * 当达到API速率限制时抛出。
 */
class DockerRateLimitException(
    message: String = "API rate limit exceeded",
    val retryAfter: Int? = null
) : DockerException(message, statusCode = 429)

/**
 * Docker API版本不兼容异常
 * 
 * 当API版本不兼容时抛出。
 */
class DockerApiVersionException(
    val clientVersion: String,
    val serverVersion: String,
    message: String = "API version mismatch: client=$clientVersion, server=$serverVersion"
) : DockerException(message, statusCode = 400)

// ==================== 配置异常 ====================

/**
 * Docker配置异常
 * 
 * 当配置无效时抛出。
 */
class DockerConfigException(
    message: String,
    cause: Throwable? = null
) : DockerException(message, cause)

/**
 * 无效参数异常
 * 
 * 当传入的参数无效时抛出。
 */
class InvalidParameterException(
    parameterName: String,
    reason: String,
    message: String = "Invalid parameter '$parameterName': $reason"
) : DockerException(message, statusCode = 400)

// ==================== 执行异常 ====================

/**
 * 执行异常
 * 
 * 当在容器中执行命令失败时抛出。
 */
class ExecException(
    val containerId: String,
    val command: List<String>,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    message: String = "Command execution failed in container $containerId with exit code $exitCode"
) : DockerException(message)

// ==================== 健康检查异常 ====================

/**
 * 健康检查异常
 * 
 * 当健康检查失败时抛出。
 */
class HealthCheckException(
    val containerId: String,
    message: String = "Health check failed for container: $containerId"
) : DockerException(message)

// ==================== 扩展函数 ====================

/**
 * 检查是否为可重试的异常
 * 
 * @return 如果异常可以重试则返回true
 */
fun DockerException.isRetryable(): Boolean {
    return when (this) {
        is DockerTimeoutException,
        is DockerConnectionException,
        is DockerRateLimitException -> true
        else -> false
    }
}

/**
 * 获取异常的错误代码
 * 
 * @return 错误代码字符串
 */
fun DockerException.getErrorCode(): String {
    return when (this) {
        is DockerConnectionException -> "CONNECTION_ERROR"
        is DockerTimeoutException -> "TIMEOUT_ERROR"
        is DockerAuthException -> "AUTH_ERROR"
        is ContainerNotFoundException -> "CONTAINER_NOT_FOUND"
        is ContainerAlreadyExistsException -> "CONTAINER_EXISTS"
        is ContainerStateException -> "CONTAINER_STATE_ERROR"
        is ImageNotFoundException -> "IMAGE_NOT_FOUND"
        is ImagePullException -> "IMAGE_PULL_ERROR"
        is ImagePushException -> "IMAGE_PUSH_ERROR"
        is ImageBuildException -> "IMAGE_BUILD_ERROR"
        is NetworkNotFoundException -> "NETWORK_NOT_FOUND"
        is VolumeNotFoundException -> "VOLUME_NOT_FOUND"
        is VolumeInUseException -> "VOLUME_IN_USE"
        is DockerApiException -> "API_ERROR"
        is DockerRateLimitException -> "RATE_LIMIT_ERROR"
        is DockerApiVersionException -> "API_VERSION_ERROR"
        is DockerConfigException -> "CONFIG_ERROR"
        is InvalidParameterException -> "INVALID_PARAMETER"
        is ExecException -> "EXEC_ERROR"
        is HealthCheckException -> "HEALTH_CHECK_ERROR"
        else -> "UNKNOWN_ERROR"
    }
}