package com.dfa.core.vm.image

import com.google.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * ImageValidator 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImageValidatorTest {

    private lateinit var validator: ImageValidator

    @Before
    fun setup() {
        validator = ImageValidator()
    }

    @Test
    fun `validate should return invalid when local path is null`() = runTest {
        // Given
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            localPath = null
        )

        // When
        val result = validator.validate(imageInfo)

        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).contains("null")
    }

    @Test
    fun `validate should return invalid when file does not exist`() = runTest {
        // Given
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            localPath = "/non/existent/path/image.qcow2"
        )

        // When
        val result = validator.validate(imageInfo)

        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).contains("does not exist")
    }

    @Test
    fun `validateFormat should return false for unsupported extension`() {
        // Given - create a temp file with unsupported extension
        val tempFile = java.io.File.createTempFile("test", ".txt")
        tempFile.writeText("test content")

        try {
            // When
            val result = validator.validateFormat(tempFile)

            // Then
            assertThat(result).isFalse()
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `validateFormat should return false for file smaller than 1MB`() {
        // Given - create a small qcow2 file
        val tempFile = java.io.File.createTempFile("test", ".qcow2")
        tempFile.writeBytes(ByteArray(1024)) // 1KB

        try {
            // When
            val result = validator.validateFormat(tempFile)

            // Then
            assertThat(result).isFalse()
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `quickValidate should return false for non-existent file`() {
        // When
        val result = validator.quickValidate("/non/existent/path.qcow2")

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `ImageFormatInfo should have correct properties`() {
        // Given
        val info = ImageFormatInfo(
            format = "qcow2",
            version = 3,
            virtualSize = 10L * 1024 * 1024 * 1024, // 10GB
            actualSize = 1024L * 1024 * 1024 // 1GB
        )

        // When & Then
        assertThat(info.format).isEqualTo("qcow2")
        assertThat(info.version).isEqualTo(3)
        assertThat(info.virtualSize).isEqualTo(10L * 1024 * 1024 * 1024)
        assertThat(info.actualSize).isEqualTo(1024L * 1024 * 1024)
    }
}