package com.dfa.core.vm.image

import com.google.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * ImageManagerImpl 单元测试
 */
class ImageManagerImplTest {

    private lateinit var downloader: ImageDownloader
    private lateinit var cache: ImageCache
    private lateinit var validator: ImageValidator
    private lateinit var manager: ImageManagerImpl

    @Before
    fun setup() {
        downloader = mockk(relaxed = true)
        cache = mockk(relaxed = true)
        validator = mockk(relaxed = true)
        manager = ImageManagerImpl(downloader, cache, validator)
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `initial state should be NOT_DOWNLOADED`() {
        assertThat(manager.currentImageState.value).isEqualTo(ImageState.NOT_DOWNLOADED)
    }

    @Test
    fun `initial imageInfo should be null`() {
        assertThat(manager.currentImageInfo.value).isNull()
    }

    @Test
    fun `initial downloadProgress should be null`() {
        assertThat(manager.downloadProgress.value).isNull()
    }

    // ==================== Initialize Tests ====================

    @Test
    fun `initialize should succeed`() = runTest {
        coEvery { cache.getCachedImage(any()) } returns null

        val result = manager.initialize()

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `initialize should set READY state when cached image exists`() = runTest {
        val cachedImage = ImageInfo(
            id = ImageConstants.DEFAULT_IMAGE_ID,
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            state = ImageState.READY
        )
        coEvery { cache.getCachedImage(ImageConstants.DEFAULT_IMAGE_ID) } returns cachedImage

        manager.initialize()

        assertThat(manager.currentImageState.value).isEqualTo(ImageState.READY)
    }

    // ==================== Get Image Info Tests ====================

    @Test
    fun `getImageInfo should return cached image`() = runTest {
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2"
        )
        coEvery { cache.getCachedImage("test-image") } returns imageInfo

        val result = manager.getImageInfo("test-image")

        assertThat(result).isEqualTo(imageInfo)
    }

    @Test
    fun `getImageInfo should return null for non-existent image`() = runTest {
        coEvery { cache.getCachedImage("non-existent") } returns null

        val result = manager.getImageInfo("non-existent")

        assertThat(result).isNull()
    }

    // ==================== Get Cached Images Tests ====================

    @Test
    fun `getCachedImages should return list from cache`() = runTest {
        val images = listOf(
            ImageInfo("img1", "Image 1", "url1"),
            ImageInfo("img2", "Image 2", "url2")
        )
        coEvery { cache.getAllCachedImages() } returns images

        val result = manager.getCachedImages()

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(images)
    }

    // ==================== Is Image Cached Tests ====================

    @Test
    fun `isImageCached should return true for cached image`() = runTest {
        coEvery { cache.isCached("cached-image") } returns true

        val result = manager.isImageCached("cached-image")

        assertThat(result).isTrue()
    }

    @Test
    fun `isImageCached should return false for non-cached image`() = runTest {
        coEvery { cache.isCached("non-cached") } returns false

        val result = manager.isImageCached("non-cached")

        assertThat(result).isFalse()
    }

    // ==================== Validate Image Tests ====================

    @Test
    fun `validateImage should return validation result`() = runTest {
        val imageInfo = ImageInfo("test-image", "Test", "url", localPath = "/path/to/image")
        coEvery { cache.getCachedImage("test-image") } returns imageInfo
        coEvery { validator.validate(any()) } returns ImageValidationResult(
            imageId = "test-image",
            isValid = true,
            checksumMatch = true
        )

        val result = manager.validateImage("test-image")

        assertThat(result.isValid).isTrue()
    }

    @Test
    fun `validateImage should return invalid for non-existent image`() = runTest {
        coEvery { cache.getCachedImage("non-existent") } returns null

        val result = manager.validateImage("non-existent")

        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).contains("not found")
    }

    // ==================== Delete Image Tests ====================

    @Test
    fun `deleteImage should return true when deletion succeeds`() = runTest {
        coEvery { cache.removeFromCache("test-image") } returns true

        val result = manager.deleteImage("test-image")

        assertThat(result).isTrue()
    }

    @Test
    fun `deleteImage should return false when deletion fails`() = runTest {
        coEvery { cache.removeFromCache("test-image") } returns false

        val result = manager.deleteImage("test-image")

        assertThat(result).isFalse()
    }

    // ==================== Get Image Local Path Tests ====================

    @Test
    fun `getImageLocalPath should return path for cached image`() = runTest {
        val imageInfo = ImageInfo("test-image", "Test", "url", localPath = "/path/to/image")
        coEvery { cache.getCachedImage("test-image") } returns imageInfo

        val result = manager.getImageLocalPath("test-image")

        assertThat(result).isEqualTo("/path/to/image")
    }

    @Test
    fun `getImageLocalPath should return null for non-cached image`() = runTest {
        coEvery { cache.getCachedImage("non-existent") } returns null

        val result = manager.getImageLocalPath("non-existent")

        assertThat(result).isNull()
    }

    // ==================== Get Cache Stats Tests ====================

    @Test
    fun `getCacheStats should return stats from cache`() = runTest {
        val stats = CacheStats(
            totalImages = 5,
            totalSize = 1024 * 1024 * 100
        )
        coEvery { cache.getCacheStats() } returns stats

        val result = manager.getCacheStats()

        assertThat(result.totalImages).isEqualTo(5)
        assertThat(result.totalSize).isEqualTo(1024 * 1024 * 100L)
    }

    // ==================== Clear Cache Tests ====================

    @Test
    fun `clearCache should return cleared size`() = runTest {
        coEvery { cache.clearCache(any()) } returns 1024 * 1024L

        val result = manager.clearCache(keepRecent = 2)

        assertThat(result).isEqualTo(1024 * 1024L)
    }

    // ==================== Get Predefined Images Tests ====================

    @Test
    fun `getPredefinedImages should return list from constants`() {
        val images = manager.getPredefinedImages()

        assertThat(images).isNotEmpty()
    }

    // ==================== Release Tests ====================

    @Test
    fun `release should reset state`() = runTest {
        manager.release()

        assertThat(manager.currentImageState.value).isEqualTo(ImageState.NOT_DOWNLOADED)
        assertThat(manager.currentImageInfo.value).isNull()
        assertThat(manager.downloadProgress.value).isNull()
    }
}