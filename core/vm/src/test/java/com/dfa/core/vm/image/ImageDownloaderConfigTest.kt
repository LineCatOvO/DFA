package com.dfa.core.vm.image

import com.google.truth.Truth.assertThat
import org.junit.Test

/**
 * ImageDownloaderConfig 单元测试
 */
class ImageDownloaderConfigTest {

    @Test
    fun `ImageDownloaderConfig should use default values`() {
        // When
        val config = ImageDownloaderConfig()

        // Then
        assertThat(config.connectTimeoutMs).isEqualTo(ImageConstants.CONNECT_TIMEOUT_MS)
        assertThat(config.readTimeoutMs).isEqualTo(ImageConstants.READ_TIMEOUT_MS)
        assertThat(config.bufferSize).isEqualTo(ImageConstants.DOWNLOAD_BUFFER_SIZE)
        assertThat(config.maxRetryCount).isEqualTo(ImageConstants.MAX_RETRY_COUNT)
    }

    @Test
    fun `ImageDownloaderConfig should allow custom values`() {
        // Given
        val customConnectTimeout = 5000L
        val customReadTimeout = 30000L
        val customBufferSize = 16384
        val customRetryCount = 5

        // When
        val config = ImageDownloaderConfig(
            connectTimeoutMs = customConnectTimeout,
            readTimeoutMs = customReadTimeout,
            bufferSize = customBufferSize,
            maxRetryCount = customRetryCount
        )

        // Then
        assertThat(config.connectTimeoutMs).isEqualTo(customConnectTimeout)
        assertThat(config.readTimeoutMs).isEqualTo(customReadTimeout)
        assertThat(config.bufferSize).isEqualTo(customBufferSize)
        assertThat(config.maxRetryCount).isEqualTo(customRetryCount)
    }

    @Test
    fun `ImageDownloadRequest should have correct properties`() {
        // Given
        val request = ImageDownloadRequest(
            url = "https://example.com/image.qcow2",
            targetPath = "/path/to/image.qcow2",
            checksum = "abc123",
            checksumType = "sha256",
            overwrite = true
        )

        // When & Then
        assertThat(request.url).isEqualTo("https://example.com/image.qcow2")
        assertThat(request.targetPath).isEqualTo("/path/to/image.qcow2")
        assertThat(request.checksum).isEqualTo("abc123")
        assertThat(request.checksumType).isEqualTo("sha256")
        assertThat(request.overwrite).isTrue()
    }

    @Test
    fun `ImageDownloadRequest should use default values`() {
        // When
        val request = ImageDownloadRequest(
            url = "https://example.com/image.qcow2",
            targetPath = "/path/to/image.qcow2"
        )

        // Then
        assertThat(request.checksum).isNull()
        assertThat(request.checksumType).isEqualTo("sha256")
        assertThat(request.overwrite).isFalse()
    }
}