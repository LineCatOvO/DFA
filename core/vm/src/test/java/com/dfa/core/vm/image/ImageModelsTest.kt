package com.dfa.core.vm.image

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * ImageModels 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImageModelsTest {

    @Test
    fun `ImageInfo progress should return correct percentage`() {
        // Given
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            size = 1000,
            downloadedBytes = 500
        )

        // When
        val progress = imageInfo.progress

        // Then
        assertThat(progress).isEqualTo(50)
    }

    @Test
    fun `ImageInfo progress should be capped at 100`() {
        // Given
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            size = 1000,
            downloadedBytes = 1500
        )

        // When
        val progress = imageInfo.progress

        // Then
        assertThat(progress).isEqualTo(100)
    }

    @Test
    fun `ImageInfo progress should return 0 when size is 0`() {
        // Given
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            size = 0,
            downloadedBytes = 0
        )

        // When
        val progress = imageInfo.progress

        // Then
        assertThat(progress).isEqualTo(0)
    }

    @Test
    fun `ImageInfo isDownloaded should return true when state is DOWNLOADED`() {
        // Given
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            state = ImageState.DOWNLOADED
        )

        // When & Then
        assertThat(imageInfo.isDownloaded).isTrue()
    }

    @Test
    fun `ImageInfo isReady should return true when state is READY`() {
        // Given
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            state = ImageState.READY
        )

        // When & Then
        assertThat(imageInfo.isReady).isTrue()
    }

    @Test
    fun `ImageInfo isDownloading should return true when state is DOWNLOADING`() {
        // Given
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            state = ImageState.DOWNLOADING
        )

        // When & Then
        assertThat(imageInfo.isDownloading).isTrue()
    }

    @Test
    fun `ImageDownloadProgress estimatedTimeRemaining should calculate correctly`() {
        // Given
        val progress = ImageDownloadProgress(
            imageId = "test-image",
            downloadedBytes = 500,
            totalBytes = 1000,
            speed = 100
        )

        // When
        val remaining = progress.estimatedTimeRemaining

        // Then
        assertThat(remaining).isEqualTo(5L) // (1000 - 500) / 100 = 5
    }

    @Test
    fun `ImageDownloadProgress estimatedTimeRemaining should return null when speed is 0`() {
        // Given
        val progress = ImageDownloadProgress(
            imageId = "test-image",
            downloadedBytes = 500,
            totalBytes = 1000,
            speed = 0
        )

        // When & Then
        assertThat(progress.estimatedTimeRemaining).isNull()
    }

    @Test
    fun `ImageError NetworkError should contain message and cause`() {
        // Given
        val cause = RuntimeException("Connection failed")
        val error = ImageError.NetworkError("Network error", cause)

        // When & Then
        assertThat(error.message).isEqualTo("Network error")
        assertThat(error.cause).isEqualTo(cause)
    }

    @Test
    fun `ImageError ChecksumError should contain expected and actual checksums`() {
        // Given
        val error = ImageError.ChecksumError(
            message = "Checksum mismatch",
            expected = "abc123",
            actual = "def456"
        )

        // When & Then
        assertThat(error.message).isEqualTo("Checksum mismatch")
        assertThat(error.expected).isEqualTo("abc123")
        assertThat(error.actual).isEqualTo("def456")
    }

    @Test
    fun `ImageValidationResult should indicate valid when isValid is true`() {
        // Given
        val result = ImageValidationResult(
            imageId = "test-image",
            isValid = true,
            checksumMatch = true,
            fileSize = 1024
        )

        // When & Then
        assertThat(result.isValid).isTrue()
        assertThat(result.checksumMatch).isTrue()
        assertThat(result.errorMessage).isNull()
    }

    @Test
    fun `ImageValidationResult should contain error message when invalid`() {
        // Given
        val result = ImageValidationResult(
            imageId = "test-image",
            isValid = false,
            checksumMatch = false,
            fileSize = 1024,
            errorMessage = "Checksum mismatch"
        )

        // When & Then
        assertThat(result.isValid).isFalse()
        assertThat(result.checksumMatch).isFalse()
        assertThat(result.errorMessage).isEqualTo("Checksum mismatch")
    }
}