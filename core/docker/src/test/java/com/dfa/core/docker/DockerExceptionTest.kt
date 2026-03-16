package com.dfa.core.docker

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * DockerException 单元测试
 *
 * 测试DockerException及其子类的属性和方法。
 */
class DockerExceptionTest {

    // ==================== DockerException 基类测试 ====================

    @Test
    fun `DockerException should have message`() {
        val exception = DockerException("Test error")

        assertThat(exception.message).isEqualTo("Test error")
    }

    @Test
    fun `DockerException should have cause`() {
        val cause = RuntimeException("Original error")
        val exception = DockerException("Test error", cause)

        assertThat(exception.cause).isEqualTo(cause)
    }

    @Test
    fun `DockerException should have statusCode`() {
        val exception = DockerException("Test error", statusCode = 404)

        assertThat(exception.statusCode).isEqualTo(404)
    }

    @Test
    fun `DockerException should have null statusCode by default`() {
        val exception = DockerException("Test error")

        assertThat(exception.statusCode).isNull()
    }

    // ==================== DockerConnectionException 测试 ====================

    @Test
    fun `DockerConnectionException should have default message`() {
        val exception = DockerConnectionException()

        assertThat(exception.message).isEqualTo("Failed to connect to Docker daemon")
    }

    @Test
    fun `DockerConnectionException should accept custom message`() {
        val exception = DockerConnectionException("Custom connection error")

        assertThat(exception.message).isEqualTo("Custom connection error")
    }

    @Test
    fun `DockerConnectionException should accept cause`() {
        val cause = RuntimeException("Network error")
        val exception = DockerConnectionException(cause = cause)

        assertThat(exception.cause).isEqualTo(cause)
    }

    // ==================== DockerTimeoutException 测试 ====================

    @Test
    fun `DockerTimeoutException should have default message`() {
        val exception = DockerTimeoutException()

        assertThat(exception.message).isEqualTo("Connection to Docker daemon timed out")
    }

    @Test
    fun `DockerTimeoutException should accept custom message`() {
        val exception = DockerTimeoutException("Custom timeout")

        assertThat(exception.message).isEqualTo("Custom timeout")
    }

    @Test
    fun `DockerTimeoutException should accept cause`() {
        val cause = RuntimeException("Timeout cause")
        val exception = DockerTimeoutException(cause = cause)

        assertThat(exception.cause).isEqualTo(cause)
    }

    // ==================== DockerAuthException 测试 ====================

    @Test
    fun `DockerAuthException should have default message`() {
        val exception = DockerAuthException()

        assertThat(exception.message).isEqualTo("Authentication failed")
    }

    @Test
    fun `DockerAuthException should have statusCode 401`() {
        val exception = DockerAuthException()

        assertThat(exception.statusCode).isEqualTo(401)
    }

    @Test
    fun `DockerAuthException should accept custom message`() {
        val exception = DockerAuthException("Invalid credentials")

        assertThat(exception.message).isEqualTo("Invalid credentials")
        assertThat(exception.statusCode).isEqualTo(401)
    }

    // ==================== ContainerException 测试 ====================

    @Test
    fun `ContainerException should have containerId`() {
        val exception = ContainerException(
            message = "Container error",
            containerId = "container-123"
        )

        assertThat(exception.containerId).isEqualTo("container-123")
    }

    @Test
    fun `ContainerException should have null containerId by default`() {
        val exception = ContainerException(message = "Container error")

        assertThat(exception.containerId).isNull()
    }

    // ==================== ContainerNotFoundException 测试 ====================

    @Test
    fun `ContainerNotFoundException should have correct message`() {
        val exception = ContainerNotFoundException("container-123")

        assertThat(exception.message).isEqualTo("Container not found: container-123")
        assertThat(exception.containerId).isEqualTo("container-123")
        assertThat(exception.statusCode).isEqualTo(404)
    }

    @Test
    fun `ContainerNotFoundException should accept custom message`() {
        val exception = ContainerNotFoundException(
            containerId = "container-456",
            message = "Custom not found message"
        )

        assertThat(exception.message).isEqualTo("Custom not found message")
        assertThat(exception.containerId).isEqualTo("container-456")
    }

    // ==================== ContainerAlreadyExistsException 测试 ====================

    @Test
    fun `ContainerAlreadyExistsException should have correct message`() {
        val exception = ContainerAlreadyExistsException("my-container")

        assertThat(exception.message).isEqualTo("Container already exists: my-container")
        assertThat(exception.statusCode).isEqualTo(409)
    }

    // ==================== ContainerStateException 测试 ====================

    @Test
    fun `ContainerStateException should have correct message`() {
        val exception = ContainerStateException(
            containerId = "container-123",
            currentState = "paused",
            expectedState = "running"
        )

        assertThat(exception.message).isEqualTo("Container container-123 is in paused state, expected running")
        assertThat(exception.containerId).isEqualTo("container-123")
        assertThat(exception.statusCode).isEqualTo(409)
    }

    // ==================== ContainerOperationException 测试 ====================

    @Test
    fun `ContainerOperationException should have correct message`() {
        val exception = ContainerOperationException(
            containerId = "container-123",
            operation = "start"
        )

        assertThat(exception.message).isEqualTo("Failed to start container: container-123")
        assertThat(exception.containerId).isEqualTo("container-123")
    }

    @Test
    fun `ContainerOperationException should accept cause`() {
        val cause = RuntimeException("Start failed")
        val exception = ContainerOperationException(
            containerId = "container-123",
            operation = "stop",
            cause = cause
        )

        assertThat(exception.cause).isEqualTo(cause)
    }

    // ==================== ContainerCreateException 测试 ====================

    @Test
    fun `ContainerCreateException should have message`() {
        val exception = ContainerCreateException("Failed to create container")

        assertThat(exception.message).isEqualTo("Failed to create container")
        assertThat(exception.statusCode).isEqualTo(400)
    }

    @Test
    fun `ContainerCreateException should accept cause`() {
        val cause = RuntimeException("Invalid config")
        val exception = ContainerCreateException("Create failed", cause)

        assertThat(exception.cause).isEqualTo(cause)
    }

    // ==================== ImageException 测试 ====================

    @Test
    fun `ImageException should have imageId`() {
        val exception = ImageException(
            message = "Image error",
            imageId = "nginx:latest"
        )

        assertThat(exception.imageId).isEqualTo("nginx:latest")
    }

    @Test
    fun `ImageException should have null imageId by default`() {
        val exception = ImageException(message = "Image error")

        assertThat(exception.imageId).isNull()
    }

    // ==================== ImageNotFoundException 测试 ====================

    @Test
    fun `ImageNotFoundException should have correct message`() {
        val exception = ImageNotFoundException("nginx:latest")

        assertThat(exception.message).isEqualTo("Image not found: nginx:latest")
        assertThat(exception.imageId).isEqualTo("nginx:latest")
        assertThat(exception.statusCode).isEqualTo(404)
    }

    // ==================== ImagePullException 测试 ====================

    @Test
    fun `ImagePullException should have correct message`() {
        val exception = ImagePullException("nginx:latest")

        assertThat(exception.message).isEqualTo("Failed to pull image: nginx:latest")
        assertThat(exception.imageId).isEqualTo("nginx:latest")
    }

    @Test
    fun `ImagePullException should accept cause`() {
        val cause = RuntimeException("Network error")
        val exception = ImagePullException("nginx:latest", cause = cause)

        assertThat(exception.cause).isEqualTo(cause)
    }

    // ==================== ImagePushException 测试 ====================

    @Test
    fun `ImagePushException should have correct message`() {
        val exception = ImagePushException("my-image:v1")

        assertThat(exception.message).isEqualTo("Failed to push image: my-image:v1")
        assertThat(exception.imageId).isEqualTo("my-image:v1")
    }

    // ==================== ImageBuildException 测试 ====================

    @Test
    fun `ImageBuildException should have message`() {
        val exception = ImageBuildException("Build failed")

        assertThat(exception.message).isEqualTo("Build failed")
    }

    @Test
    fun `ImageBuildException should have buildLog`() {
        val exception = ImageBuildException(
            message = "Build failed",
            buildLog = "Step 1/10 : FROM nginx\nERROR: ..."
        )

        assertThat(exception.buildLog).isEqualTo("Step 1/10 : FROM nginx\nERROR: ...")
    }

    @Test
    fun `ImageBuildException should accept cause`() {
        val cause = RuntimeException("Dockerfile error")
        val exception = ImageBuildException("Build failed", cause)

        assertThat(exception.cause).isEqualTo(cause)
    }

    // ==================== ImageRemoveException 测试 ====================

    @Test
    fun `ImageRemoveException should have correct message`() {
        val exception = ImageRemoveException("nginx:latest")

        assertThat(exception.message).isEqualTo("Failed to remove image: nginx:latest")
        assertThat(exception.imageId).isEqualTo("nginx:latest")
    }

    // ==================== NetworkException 测试 ====================

    @Test
    fun `NetworkException should have networkId`() {
        val exception = NetworkException(
            message = "Network error",
            networkId = "network-123"
        )

        assertThat(exception.networkId).isEqualTo("network-123")
    }

    // ==================== NetworkNotFoundException 测试 ====================

    @Test
    fun `NetworkNotFoundException should have correct message`() {
        val exception = NetworkNotFoundException("network-123")

        assertThat(exception.message).isEqualTo("Network not found: network-123")
        assertThat(exception.networkId).isEqualTo("network-123")
        assertThat(exception.statusCode).isEqualTo(404)
    }

    // ==================== NetworkCreateException 测试 ====================

    @Test
    fun `NetworkCreateException should have correct message`() {
        val exception = NetworkCreateException("my-network")

        assertThat(exception.message).isEqualTo("Failed to create network: my-network")
    }

    // ==================== NetworkConnectException 测试 ====================

    @Test
    fun `NetworkConnectException should have correct message`() {
        val exception = NetworkConnectException(
            networkId = "network-123",
            containerId = "container-456"
        )

        assertThat(exception.message).isEqualTo("Failed to connect container container-456 to network network-123")
        assertThat(exception.networkId).isEqualTo("network-123")
    }

    // ==================== NetworkDisconnectException 测试 ====================

    @Test
    fun `NetworkDisconnectException should have correct message`() {
        val exception = NetworkDisconnectException(
            networkId = "network-123",
            containerId = "container-456"
        )

        assertThat(exception.message).isEqualTo("Failed to disconnect container container-456 from network network-123")
        assertThat(exception.networkId).isEqualTo("network-123")
    }

    // ==================== VolumeException 测试 ====================

    @Test
    fun `VolumeException should have volumeName`() {
        val exception = VolumeException(
            message = "Volume error",
            volumeName = "my-volume"
        )

        assertThat(exception.volumeName).isEqualTo("my-volume")
    }

    // ==================== VolumeNotFoundException 测试 ====================

    @Test
    fun `VolumeNotFoundException should have correct message`() {
        val exception = VolumeNotFoundException("my-volume")

        assertThat(exception.message).isEqualTo("Volume not found: my-volume")
        assertThat(exception.volumeName).isEqualTo("my-volume")
        assertThat(exception.statusCode).isEqualTo(404)
    }

    // ==================== VolumeCreateException 测试 ====================

    @Test
    fun `VolumeCreateException should have correct message with volumeName`() {
        val exception = VolumeCreateException("my-volume")

        assertThat(exception.message).isEqualTo("Failed to create volume: my-volume")
        assertThat(exception.volumeName).isEqualTo("my-volume")
    }

    @Test
    fun `VolumeCreateException should have correct message without volumeName`() {
        val exception = VolumeCreateException(null)

        assertThat(exception.message).isEqualTo("Failed to create volume")
        assertThat(exception.volumeName).isNull()
    }

    // ==================== VolumeRemoveException 测试 ====================

    @Test
    fun `VolumeRemoveException should have correct message`() {
        val exception = VolumeRemoveException("my-volume")

        assertThat(exception.message).isEqualTo("Failed to remove volume: my-volume")
        assertThat(exception.volumeName).isEqualTo("my-volume")
    }

    // ==================== VolumeInUseException 测试 ====================

    @Test
    fun `VolumeInUseException should have correct message`() {
        val exception = VolumeInUseException("my-volume")

        assertThat(exception.message).isEqualTo("Volume my-volume is in use and cannot be removed")
        assertThat(exception.volumeName).isEqualTo("my-volume")
        assertThat(exception.statusCode).isEqualTo(409)
    }

    // ==================== DockerApiException 测试 ====================

    @Test
    fun `DockerApiException should have errorCode`() {
        val exception = DockerApiException(
            message = "API error",
            errorCode = "E001"
        )

        assertThat(exception.errorCode).isEqualTo("E001")
    }

    @Test
    fun `DockerApiException should have statusCode`() {
        val exception = DockerApiException(
            message = "API error",
            statusCode = 500
        )

        assertThat(exception.statusCode).isEqualTo(500)
    }

    @Test
    fun `DockerApiException should accept cause`() {
        val cause = RuntimeException("API failure")
        val exception = DockerApiException(
            message = "API error",
            cause = cause
        )

        assertThat(exception.cause).isEqualTo(cause)
    }

    // ==================== DockerRateLimitException 测试 ====================

    @Test
    fun `DockerRateLimitException should have default message`() {
        val exception = DockerRateLimitException()

        assertThat(exception.message).isEqualTo("API rate limit exceeded")
        assertThat(exception.statusCode).isEqualTo(429)
    }

    @Test
    fun `DockerRateLimitException should have retryAfter`() {
        val exception = DockerRateLimitException(retryAfter = 60)

        assertThat(exception.retryAfter).isEqualTo(60)
    }

    // ==================== DockerApiVersionException 测试 ====================

    @Test
    fun `DockerApiVersionException should have correct message`() {
        val exception = DockerApiVersionException(
            clientVersion = "1.40",
            serverVersion = "1.41"
        )

        assertThat(exception.message).isEqualTo("API version mismatch: client=1.40, server=1.41")
        assertThat(exception.clientVersion).isEqualTo("1.40")
        assertThat(exception.serverVersion).isEqualTo("1.41")
        assertThat(exception.statusCode).isEqualTo(400)
    }

    // ==================== DockerConfigException 测试 ====================

    @Test
    fun `DockerConfigException should have message`() {
        val exception = DockerConfigException("Invalid configuration")

        assertThat(exception.message).isEqualTo("Invalid configuration")
    }

    @Test
    fun `DockerConfigException should accept cause`() {
        val cause = RuntimeException("Config parse error")
        val exception = DockerConfigException("Invalid config", cause)

        assertThat(exception.cause).isEqualTo(cause)
    }

    // ==================== InvalidParameterException 测试 ====================

    @Test
    fun `InvalidParameterException should have correct message`() {
        val exception = InvalidParameterException(
            parameterName = "timeout",
            reason = "must be positive"
        )

        assertThat(exception.message).isEqualTo("Invalid parameter 'timeout': must be positive")
        assertThat(exception.statusCode).isEqualTo(400)
    }

    // ==================== ExecException 测试 ====================

    @Test
    fun `ExecException should have all properties`() {
        val exception = ExecException(
            containerId = "container-123",
            command = listOf("ls", "-la"),
            exitCode = 1,
            stdout = "output",
            stderr = "error"
        )

        assertThat(exception.containerId).isEqualTo("container-123")
        assertThat(exception.command).isEqualTo(listOf("ls", "-la"))
        assertThat(exception.exitCode).isEqualTo(1)
        assertThat(exception.stdout).isEqualTo("output")
        assertThat(exception.stderr).isEqualTo("error")
        assertThat(exception.message).isEqualTo("Command execution failed in container container-123 with exit code 1")
    }

    // ==================== HealthCheckException 测试 ====================

    @Test
    fun `HealthCheckException should have correct message`() {
        val exception = HealthCheckException("container-123")

        assertThat(exception.message).isEqualTo("Health check failed for container: container-123")
        assertThat(exception.containerId).isEqualTo("container-123")
    }

    // ==================== isRetryable 扩展函数测试 ====================

    @Test
    fun `DockerTimeoutException should be retryable`() {
        val exception = DockerTimeoutException()

        assertThat(exception.isRetryable()).isTrue()
    }

    @Test
    fun `DockerConnectionException should be retryable`() {
        val exception = DockerConnectionException()

        assertThat(exception.isRetryable()).isTrue()
    }

    @Test
    fun `DockerRateLimitException should be retryable`() {
        val exception = DockerRateLimitException()

        assertThat(exception.isRetryable()).isTrue()
    }

    @Test
    fun `ContainerNotFoundException should not be retryable`() {
        val exception = ContainerNotFoundException("container-123")

        assertThat(exception.isRetryable()).isFalse()
    }

    @Test
    fun `ImageNotFoundException should not be retryable`() {
        val exception = ImageNotFoundException("nginx:latest")

        assertThat(exception.isRetryable()).isFalse()
    }

    @Test
    fun `DockerAuthException should not be retryable`() {
        val exception = DockerAuthException()

        assertThat(exception.isRetryable()).isFalse()
    }

    // ==================== getErrorCode 扩展函数测试 ====================

    @Test
    fun `DockerConnectionException should have error code CONNECTION_ERROR`() {
        val exception = DockerConnectionException()

        assertThat(exception.getErrorCode()).isEqualTo("CONNECTION_ERROR")
    }

    @Test
    fun `DockerTimeoutException should have error code TIMEOUT_ERROR`() {
        val exception = DockerTimeoutException()

        assertThat(exception.getErrorCode()).isEqualTo("TIMEOUT_ERROR")
    }

    @Test
    fun `DockerAuthException should have error code AUTH_ERROR`() {
        val exception = DockerAuthException()

        assertThat(exception.getErrorCode()).isEqualTo("AUTH_ERROR")
    }

    @Test
    fun `ContainerNotFoundException should have error code CONTAINER_NOT_FOUND`() {
        val exception = ContainerNotFoundException("container-123")

        assertThat(exception.getErrorCode()).isEqualTo("CONTAINER_NOT_FOUND")
    }

    @Test
    fun `ContainerAlreadyExistsException should have error code CONTAINER_EXISTS`() {
        val exception = ContainerAlreadyExistsException("my-container")

        assertThat(exception.getErrorCode()).isEqualTo("CONTAINER_EXISTS")
    }

    @Test
    fun `ContainerStateException should have error code CONTAINER_STATE_ERROR`() {
        val exception = ContainerStateException("id", "paused", "running")

        assertThat(exception.getErrorCode()).isEqualTo("CONTAINER_STATE_ERROR")
    }

    @Test
    fun `ImageNotFoundException should have error code IMAGE_NOT_FOUND`() {
        val exception = ImageNotFoundException("nginx:latest")

        assertThat(exception.getErrorCode()).isEqualTo("IMAGE_NOT_FOUND")
    }

    @Test
    fun `ImagePullException should have error code IMAGE_PULL_ERROR`() {
        val exception = ImagePullException("nginx:latest")

        assertThat(exception.getErrorCode()).isEqualTo("IMAGE_PULL_ERROR")
    }

    @Test
    fun `ImagePushException should have error code IMAGE_PUSH_ERROR`() {
        val exception = ImagePushException("my-image:v1")

        assertThat(exception.getErrorCode()).isEqualTo("IMAGE_PUSH_ERROR")
    }

    @Test
    fun `ImageBuildException should have error code IMAGE_BUILD_ERROR`() {
        val exception = ImageBuildException("Build failed")

        assertThat(exception.getErrorCode()).isEqualTo("IMAGE_BUILD_ERROR")
    }

    @Test
    fun `NetworkNotFoundException should have error code NETWORK_NOT_FOUND`() {
        val exception = NetworkNotFoundException("network-123")

        assertThat(exception.getErrorCode()).isEqualTo("NETWORK_NOT_FOUND")
    }

    @Test
    fun `VolumeNotFoundException should have error code VOLUME_NOT_FOUND`() {
        val exception = VolumeNotFoundException("my-volume")

        assertThat(exception.getErrorCode()).isEqualTo("VOLUME_NOT_FOUND")
    }

    @Test
    fun `VolumeInUseException should have error code VOLUME_IN_USE`() {
        val exception = VolumeInUseException("my-volume")

        assertThat(exception.getErrorCode()).isEqualTo("VOLUME_IN_USE")
    }

    @Test
    fun `DockerApiException should have error code API_ERROR`() {
        val exception = DockerApiException("API error")

        assertThat(exception.getErrorCode()).isEqualTo("API_ERROR")
    }

    @Test
    fun `DockerRateLimitException should have error code RATE_LIMIT_ERROR`() {
        val exception = DockerRateLimitException()

        assertThat(exception.getErrorCode()).isEqualTo("RATE_LIMIT_ERROR")
    }

    @Test
    fun `DockerApiVersionException should have error code API_VERSION_ERROR`() {
        val exception = DockerApiVersionException("1.40", "1.41")

        assertThat(exception.getErrorCode()).isEqualTo("API_VERSION_ERROR")
    }

    @Test
    fun `DockerConfigException should have error code CONFIG_ERROR`() {
        val exception = DockerConfigException("Invalid config")

        assertThat(exception.getErrorCode()).isEqualTo("CONFIG_ERROR")
    }

    @Test
    fun `InvalidParameterException should have error code INVALID_PARAMETER`() {
        val exception = InvalidParameterException("param", "invalid")

        assertThat(exception.getErrorCode()).isEqualTo("INVALID_PARAMETER")
    }

    @Test
    fun `ExecException should have error code EXEC_ERROR`() {
        val exception = ExecException("id", listOf("cmd"), 1, "", "")

        assertThat(exception.getErrorCode()).isEqualTo("EXEC_ERROR")
    }

    @Test
    fun `HealthCheckException should have error code HEALTH_CHECK_ERROR`() {
        val exception = HealthCheckException("container-123")

        assertThat(exception.getErrorCode()).isEqualTo("HEALTH_CHECK_ERROR")
    }

    @Test
    fun `Unknown exception should have error code UNKNOWN_ERROR`() {
        val exception = DockerException("Unknown error")

        assertThat(exception.getErrorCode()).isEqualTo("UNKNOWN_ERROR")
    }

    // ==================== 异常继承关系测试 ====================

    @Test
    fun `ContainerNotFoundException should be ContainerException`() {
        val exception = ContainerNotFoundException("container-123")

        assertThat(exception).isInstanceOf(ContainerException::class.java)
        assertThat(exception).isInstanceOf(DockerException::class.java)
    }

    @Test
    fun `ImageNotFoundException should be ImageException`() {
        val exception = ImageNotFoundException("nginx:latest")

        assertThat(exception).isInstanceOf(ImageException::class.java)
        assertThat(exception).isInstanceOf(DockerException::class.java)
    }

    @Test
    fun `NetworkNotFoundException should be NetworkException`() {
        val exception = NetworkNotFoundException("network-123")

        assertThat(exception).isInstanceOf(NetworkException::class.java)
        assertThat(exception).isInstanceOf(DockerException::class.java)
    }

    @Test
    fun `VolumeNotFoundException should be VolumeException`() {
        val exception = VolumeNotFoundException("my-volume")

        assertThat(exception).isInstanceOf(VolumeException::class.java)
        assertThat(exception).isInstanceOf(DockerException::class.java)
    }

    @Test
    fun `DockerConnectionException should be DockerException`() {
        val exception = DockerConnectionException()

        assertThat(exception).isInstanceOf(DockerException::class.java)
    }

    @Test
    fun `DockerTimeoutException should be DockerException`() {
        val exception = DockerTimeoutException()

        assertThat(exception).isInstanceOf(DockerException::class.java)
    }

    @Test
    fun `DockerAuthException should be DockerException`() {
        val exception = DockerAuthException()

        assertThat(exception).isInstanceOf(DockerException::class.java)
    }
}