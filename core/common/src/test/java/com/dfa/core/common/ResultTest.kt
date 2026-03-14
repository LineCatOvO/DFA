package com.dfa.core.common

import com.google.truth.Truth.assertThat
import org.junit.Test

/**
 * Result 密封类单元测试
 */
class ResultTest {

    @Test
    fun `Success isSuccess should return true`() {
        val result = Result.Success("data")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.isError).isFalse()
    }

    @Test
    fun `Error isError should return true`() {
        val result = Result.Error(RuntimeException("error"))

        assertThat(result.isError).isTrue()
        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Success getOrNull should return data`() {
        val result = Result.Success("test data")

        assertThat(result.getOrNull()).isEqualTo("test data")
    }

    @Test
    fun `Error getOrNull should return null`() {
        val result = Result.Error(RuntimeException("error"))

        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `Success exceptionOrNull should return null`() {
        val result = Result.Success("data")

        assertThat(result.exceptionOrNull()).isNull()
    }

    @Test
    fun `Error exceptionOrNull should return exception`() {
        val exception = RuntimeException("test error")
        val result = Result.Error(exception)

        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `map should transform Success data`() {
        val result = Result.Success(5)
        val mapped = result.map { it * 2 }

        assertThat(mapped).isInstanceOf(Result.Success::class.java)
        assertThat((mapped as Result.Success).data).isEqualTo(10)
    }

    @Test
    fun `map should not transform Error`() {
        val exception = RuntimeException("error")
        val result: Result<Int> = Result.Error(exception)
        val mapped = result.map { it * 2 }

        assertThat(mapped).isInstanceOf(Result.Error::class.java)
        assertThat((mapped as Result.Error).exception).isEqualTo(exception)
    }

    @Test
    fun `onSuccess should execute action for Success`() {
        var executed = false
        var receivedData = ""
        val result = Result.Success("test")

        result.onSuccess {
            executed = true
            receivedData = it
        }

        assertThat(executed).isTrue()
        assertThat(receivedData).isEqualTo("test")
    }

    @Test
    fun `onSuccess should not execute action for Error`() {
        var executed = false
        val result: Result<String> = Result.Error(RuntimeException("error"))

        result.onSuccess { executed = true }

        assertThat(executed).isFalse()
    }

    @Test
    fun `onError should execute action for Error`() {
        var executed = false
        var receivedException: Throwable? = null
        val exception = RuntimeException("test error")
        val result: Result<String> = Result.Error(exception)

        result.onError {
            executed = true
            receivedException = it
        }

        assertThat(executed).isTrue()
        assertThat(receivedException).isEqualTo(exception)
    }

    @Test
    fun `onError should not execute action for Success`() {
        var executed = false
        val result = Result.Success("data")

        result.onError { executed = true }

        assertThat(executed).isFalse()
    }

    @Test
    fun `chaining onSuccess and onError should work correctly for Success`() {
        var successExecuted = false
        var errorExecuted = false

        Result.Success("data")
            .onSuccess { successExecuted = true }
            .onError { errorExecuted = true }

        assertThat(successExecuted).isTrue()
        assertThat(errorExecuted).isFalse()
    }

    @Test
    fun `chaining onSuccess and onError should work correctly for Error`() {
        var successExecuted = false
        var errorExecuted = false

        Result.Error(RuntimeException("error"))
            .onSuccess { successExecuted = true }
            .onError { errorExecuted = true }

        assertThat(successExecuted).isFalse()
        assertThat(errorExecuted).isTrue()
    }

    @Test
    fun `Success should hold complex data types`() {
        data class Person(val name: String, val age: Int)
        val person = Person("Alice", 30)
        val result = Result.Success(person)

        assertThat(result.getOrNull()).isEqualTo(person)
    }

    @Test
    fun `Success should hold nullable data`() {
        val result = Result.Success<String?>(null)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `Error should preserve exception message`() {
        val exception = RuntimeException("Custom error message")
        val result = Result.Error(exception)

        assertThat(result.exceptionOrNull()?.message).isEqualTo("Custom error message")
    }

    @Test
    fun `Error should preserve exception cause chain`() {
        val cause = IllegalArgumentException("Root cause")
        val exception = RuntimeException("Wrapper", cause)
        val result = Result.Error(exception)

        assertThat(result.exceptionOrNull()?.cause).isEqualTo(cause)
    }
}