package com.dfa.core.vm

import com.google.truth.Truth.assertThat
import org.junit.Test

/**
 * VmException 单元测试
 */
class VmExceptionTest {

    // ==================== ConfigurationException Tests ====================

    @Test
    fun `ConfigurationException should contain message and configId`() {
        val exception = VmException.ConfigurationException(
            message = "Invalid configuration",
            configId = "vm-123"
        )

        assertThat(exception.message).isEqualTo("Invalid configuration")
        assertThat(exception.configId).isEqualTo("vm-123")
    }

    @Test
    fun `ConfigurationException configId should be nullable`() {
        val exception = VmException.ConfigurationException(
            message = "Invalid configuration"
        )

        assertThat(exception.message).isEqualTo("Invalid configuration")
        assertThat(exception.configId).isNull()
    }

    // ==================== ResourceException Tests ====================

    @Test
    fun `ResourceException should contain all required properties`() {
        val exception = VmException.ResourceException(
            message = "Insufficient memory",
            resourceType = VmException.ResourceException.ResourceType.MEMORY,
            required = 4096,
            available = 2048
        )

        assertThat(exception.message).isEqualTo("Insufficient memory")
        assertThat(exception.resourceType).isEqualTo(VmException.ResourceException.ResourceType.MEMORY)
        assertThat(exception.required).isEqualTo(4096)
        assertThat(exception.available).isEqualTo(2048)
    }

    @Test
    fun `ResourceType should contain all expected types`() {
        val expectedTypes = listOf(
            VmException.ResourceException.ResourceType.MEMORY,
            VmException.ResourceException.ResourceType.CPU,
            VmException.ResourceException.ResourceType.DISK,
            VmException.ResourceException.ResourceType.NETWORK,
            VmException.ResourceException.ResourceType.GPU
        )

        assertThat(VmException.ResourceException.ResourceType.entries.size).isEqualTo(expectedTypes.size)
        expectedTypes.forEach { type ->
            assertThat(VmException.ResourceException.ResourceType.entries.contains(type)).isTrue()
        }
    }

    // ==================== StateException Tests ====================

    @Test
    fun `StateException should contain current and expected state`() {
        val exception = VmException.StateException(
            message = "Invalid state transition",
            currentState = VmState.RUNNING,
            expectedState = VmState.STOPPED
        )

        assertThat(exception.message).isEqualTo("Invalid state transition")
        assertThat(exception.currentState).isEqualTo(VmState.RUNNING)
        assertThat(exception.expectedState).isEqualTo(VmState.STOPPED)
    }

    // ==================== TimeoutException Tests ====================

    @Test
    fun `TimeoutException should contain operation and timeout`() {
        val exception = VmException.TimeoutException(
            message = "Operation timed out",
            operation = "start",
            timeoutMs = 30000
        )

        assertThat(exception.message).isEqualTo("Operation timed out")
        assertThat(exception.operation).isEqualTo("start")
        assertThat(exception.timeoutMs).isEqualTo(30000)
    }

    // ==================== PermissionException Tests ====================

    @Test
    fun `PermissionException should contain message and optional permission`() {
        val exception = VmException.PermissionException(
            message = "Permission denied",
            permission = "android.permission.VIRTUAL_MACHINE"
        )

        assertThat(exception.message).isEqualTo("Permission denied")
        assertThat(exception.permission).isEqualTo("android.permission.VIRTUAL_MACHINE")
    }

    @Test
    fun `PermissionException permission should be nullable`() {
        val exception = VmException.PermissionException(
            message = "Permission denied"
        )

        assertThat(exception.permission).isNull()
    }

    // ==================== AvfException Tests ====================

    @Test
    fun `AvfException should contain message, cause and errorCode`() {
        val cause = RuntimeException("Underlying error")
        val exception = VmException.AvfException(
            message = "AVF error",
            cause = cause,
            errorCode = 500
        )

        assertThat(exception.message).isEqualTo("AVF error")
        assertThat(exception.cause).isEqualTo(cause)
        assertThat(exception.errorCode).isEqualTo(500)
    }

    @Test
    fun `AvfException cause and errorCode should be nullable`() {
        val exception = VmException.AvfException(
            message = "AVF error"
        )

        assertThat(exception.cause).isNull()
        assertThat(exception.errorCode).isNull()
    }

    // ==================== VmNotFoundException Tests ====================

    @Test
    fun `VmNotFoundException should contain vmId`() {
        val exception = VmException.VmNotFoundException(
            message = "VM not found",
            vmId = "vm-123"
        )

        assertThat(exception.message).isEqualTo("VM not found")
        assertThat(exception.vmId).isEqualTo("vm-123")
    }

    // ==================== VmAlreadyExistsException Tests ====================

    @Test
    fun `VmAlreadyExistsException should contain vmId`() {
        val exception = VmException.VmAlreadyExistsException(
            message = "VM already exists",
            vmId = "vm-123"
        )

        assertThat(exception.message).isEqualTo("VM already exists")
        assertThat(exception.vmId).isEqualTo("vm-123")
    }

    // ==================== OperationNotSupportedException Tests ====================

    @Test
    fun `OperationNotSupportedException should contain operation and state`() {
        val exception = VmException.OperationNotSupportedException(
            message = "Operation not supported",
            operation = "migrate",
            currentState = VmState.CREATED
        )

        assertThat(exception.message).isEqualTo("Operation not supported")
        assertThat(exception.operation).isEqualTo("migrate")
        assertThat(exception.currentState).isEqualTo(VmState.CREATED)
    }

    // ==================== VmExceptionHandler Tests ====================

    @Test
    fun `fromVmError should convert ConfigurationError to ConfigurationException`() {
        val error = VmError.ConfigurationError("Invalid config")
        val exception = VmExceptionHandler.fromVmError(error, "vm-123")

        assertThat(exception).isInstanceOf(VmException.ConfigurationException::class.java)
        assertThat(exception.message).isEqualTo("Invalid config")
    }

    @Test
    fun `fromVmError should convert ResourceError to ResourceException`() {
        val error = VmError.ResourceError("Insufficient resources")
        val exception = VmExceptionHandler.fromVmError(error)

        assertThat(exception).isInstanceOf(VmException.ResourceException::class.java)
        assertThat(exception.message).isEqualTo("Insufficient resources")
    }

    @Test
    fun `fromVmError should convert NetworkError to AvfException`() {
        val error = VmError.NetworkError("Network failed")
        val exception = VmExceptionHandler.fromVmError(error)

        assertThat(exception).isInstanceOf(VmException.AvfException::class.java)
        assertThat(exception.message).isEqualTo("Network failed")
    }

    @Test
    fun `fromVmError should convert PermissionError to PermissionException`() {
        val error = VmError.PermissionError("Permission denied")
        val exception = VmExceptionHandler.fromVmError(error)

        assertThat(exception).isInstanceOf(VmException.PermissionException::class.java)
        assertThat(exception.message).isEqualTo("Permission denied")
    }

    @Test
    fun `fromVmError should convert TimeoutError to TimeoutException`() {
        val error = VmError.TimeoutError("Operation timed out")
        val exception = VmExceptionHandler.fromVmError(error)

        assertThat(exception).isInstanceOf(VmException.TimeoutException::class.java)
        assertThat(exception.message).isEqualTo("Operation timed out")
    }

    @Test
    fun `fromVmError should convert UnknownError to AvfException`() {
        val cause = RuntimeException("Unknown cause")
        val error = VmError.UnknownError("Unknown error", cause)
        val exception = VmExceptionHandler.fromVmError(error)

        assertThat(exception).isInstanceOf(VmException.AvfException::class.java)
        assertThat(exception.message).isEqualTo("Unknown error")
        assertThat(exception.cause).isEqualTo(cause)
    }

    // ==================== isRetryable Tests ====================

    @Test
    fun `isRetryable should return true for TimeoutException`() {
        val exception = VmException.TimeoutException("Timeout", "start", 30000)
        assertThat(VmExceptionHandler.isRetryable(exception)).isTrue()
    }

    @Test
    fun `isRetryable should return true for AvfException with 5xx error code`() {
        val exception = VmException.AvfException("Error", errorCode = 500)
        assertThat(VmExceptionHandler.isRetryable(exception)).isTrue()
    }

    @Test
    fun `isRetryable should return false for AvfException with 4xx error code`() {
        val exception = VmException.AvfException("Error", errorCode = 400)
        assertThat(VmExceptionHandler.isRetryable(exception)).isFalse()
    }

    @Test
    fun `isRetryable should return false for AvfException without error code`() {
        val exception = VmException.AvfException("Error")
        assertThat(VmExceptionHandler.isRetryable(exception)).isFalse()
    }

    @Test
    fun `isRetryable should return false for ResourceException`() {
        val exception = VmException.ResourceException(
            "Insufficient memory",
            VmException.ResourceException.ResourceType.MEMORY,
            4096,
            2048
        )
        assertThat(VmExceptionHandler.isRetryable(exception)).isFalse()
    }

    @Test
    fun `isRetryable should return false for StateException`() {
        val exception = VmException.StateException("Invalid state", VmState.RUNNING, VmState.STOPPED)
        assertThat(VmExceptionHandler.isRetryable(exception)).isFalse()
    }

    @Test
    fun `isRetryable should return false for ConfigurationException`() {
        val exception = VmException.ConfigurationException("Invalid config")
        assertThat(VmExceptionHandler.isRetryable(exception)).isFalse()
    }

    @Test
    fun `isRetryable should return false for PermissionException`() {
        val exception = VmException.PermissionException("Permission denied")
        assertThat(VmExceptionHandler.isRetryable(exception)).isFalse()
    }

    @Test
    fun `isRetryable should return false for VmNotFoundException`() {
        val exception = VmException.VmNotFoundException("VM not found", "vm-123")
        assertThat(VmExceptionHandler.isRetryable(exception)).isFalse()
    }

    @Test
    fun `isRetryable should return false for VmAlreadyExistsException`() {
        val exception = VmException.VmAlreadyExistsException("VM exists", "vm-123")
        assertThat(VmExceptionHandler.isRetryable(exception)).isFalse()
    }

    @Test
    fun `isRetryable should return false for OperationNotSupportedException`() {
        val exception = VmException.OperationNotSupportedException("Not supported", "migrate", VmState.CREATED)
        assertThat(VmExceptionHandler.isRetryable(exception)).isFalse()
    }
}