package com.dfa.core.vm.image

import com.google.truth.Truth.assertThat
import org.junit.Test

/**
 * ImageConstants 单元测试
 */
class ImageConstantsTest {

    @Test
    fun `DEFAULT_IMAGE_URL should point to Debian cloud image`() {
        // When & Then
        assertThat(ImageConstants.DEFAULT_IMAGE_URL)
            .contains("cloud.debian.org")
        assertThat(ImageConstants.DEFAULT_IMAGE_URL)
            .contains("debian-12-nocloud-arm64.qcow2")
    }

    @Test
    fun `DEFAULT_IMAGE_NAME should be correct`() {
        // When & Then
        assertThat(ImageConstants.DEFAULT_IMAGE_NAME)
            .isEqualTo("debian-12-nocloud-arm64.qcow2")
    }

    @Test
    fun `DEFAULT_IMAGE_ID should be correct`() {
        // When & Then
        assertThat(ImageConstants.DEFAULT_IMAGE_ID)
            .isEqualTo("debian-12-nocloud-arm64")
    }

    @Test
    fun `SUPPORTED_FORMATS should contain qcow2`() {
        // When & Then
        assertThat(ImageConstants.SUPPORTED_FORMATS).contains("qcow2")
    }

    @Test
    fun `SUPPORTED_FORMATS should contain img`() {
        // When & Then
        assertThat(ImageConstants.SUPPORTED_FORMATS).contains("img")
    }

    @Test
    fun `SUPPORTED_FORMATS should contain raw`() {
        // When & Then
        assertThat(ImageConstants.SUPPORTED_FORMATS).contains("raw")
    }

    @Test
    fun `DOWNLOAD_BUFFER_SIZE should be positive`() {
        // When & Then
        assertThat(ImageConstants.DOWNLOAD_BUFFER_SIZE).isGreaterThan(0)
    }

    @Test
    fun `MAX_RETRY_COUNT should be at least 1`() {
        // When & Then
        assertThat(ImageConstants.MAX_RETRY_COUNT).isAtLeast(1)
    }

    @Test
    fun `PREDEFINED_IMAGES should not be empty`() {
        // When & Then
        assertThat(ImageConstants.PREDEFINED_IMAGES).isNotEmpty()
    }

    @Test
    fun `PREDEFINED_IMAGES should contain Debian image`() {
        // When & Then
        val debianImage = ImageConstants.PREDEFINED_IMAGES.find {
            it.id == ImageConstants.DEFAULT_IMAGE_ID
        }
        assertThat(debianImage).isNotNull()
    }

    @Test
    fun `Debian BASE_URL should be correct`() {
        // When & Then
        assertThat(ImageConstants.Debian.BASE_URL)
            .contains("cloud.debian.org")
        assertThat(ImageConstants.Debian.BASE_URL)
            .contains("bookworm")
    }

    @Test
    fun `Debian Variants should contain NOCLOUD_ARM64`() {
        // When & Then
        assertThat(ImageConstants.Debian.Variants.NOCLOUD_ARM64)
            .isEqualTo("debian-12-nocloud-arm64.qcow2")
    }

    @Test
    fun `Ubuntu BASE_URL should be correct`() {
        // When & Then
        assertThat(ImageConstants.Ubuntu.BASE_URL)
            .contains("cloud-images.ubuntu.com")
        assertThat(ImageConstants.Ubuntu.VERSION)
            .isEqualTo("22.04")
    }
}