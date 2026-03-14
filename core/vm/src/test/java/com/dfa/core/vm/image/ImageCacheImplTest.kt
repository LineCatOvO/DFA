package com.dfa.core.vm.image

import android.content.Context
import com.google.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * ImageCacheImpl 单元测试
 */
class ImageCacheImplTest {

    private lateinit var context: Context
    private lateinit var cache: ImageCacheImpl

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        val filesDir = File("/tmp/test-cache")
        filesDir.mkdirs()
        every { context.filesDir } returns filesDir
        cache = ImageCacheImpl(context)
    }

    // ==================== Get Cached Image Tests ====================

    @Test
    fun `getCachedImage should return null for non-existent image`() = runTest {
        val result = cache.getCachedImage("non-existent")
        assertThat(result).isNull()
    }

    // ==================== Get All Cached Images Tests ====================

    @Test
    fun `getAllCachedImages should return empty list initially`() = runTest {
        val result = cache.getAllCachedImages()
        assertThat(result).isEmpty()
    }

    // ==================== Is Cached Tests ====================

    @Test
    fun `isCached should return false for non-existent image`() = runTest {
        val result = cache.isCached("non-existent")
        assertThat(result).isFalse()
    }

    // ==================== Get Cache Size Tests ====================

    @Test
    fun `getCacheSize should return 0 for empty cache`() = runTest {
        val result = cache.getCacheSize()
        assertThat(result).isEqualTo(0L)
    }

    // ==================== Clear Cache Tests ====================

    @Test
    fun `clearCache should return 0 for empty cache`() = runTest {
        val result = cache.clearCache(keepRecent = 0)
        assertThat(result).isEqualTo(0L)
    }

    // ==================== Get Cache Directory Tests ====================

    @Test
    fun `getCacheDirectory should return valid path`() {
        val result = cache.getCacheDirectory()
        assertThat(result).isNotEmpty()
        assertThat(result).contains(ImageConstants.IMAGE_SUBDIRECTORY)
    }

    // ==================== Get Local Path Tests ====================

    @Test
    fun `getLocalPath should return valid path for image id`() {
        val result = cache.getLocalPath("test-image")
        assertThat(result).isNotEmpty()
        assertThat(result).contains("test-image")
    }

    @Test
    fun `getLocalPath should return same path for same image id`() {
        val path1 = cache.getLocalPath("test-image")
        val path2 = cache.getLocalPath("test-image")
        assertThat(path1).isEqualTo(path2)
    }

    // ==================== Update Access Time Tests ====================

    @Test
    fun `updateAccessTime should not throw for non-existent image`() = runTest {
        // Should not throw
        cache.updateAccessTime("non-existent")
    }

    // ==================== Get Cache Stats Tests ====================

    @Test
    fun `getCacheStats should return valid stats for empty cache`() = runTest {
        val result = cache.getCacheStats()

        assertThat(result.totalImages).isEqualTo(0)
        assertThat(result.totalSize).isEqualTo(0L)
    }

    // ==================== Save To Cache Tests ====================

    @Test
    fun `saveToCache should return false for null localPath`() = runTest {
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            localPath = null
        )

        val result = cache.saveToCache(imageInfo)
        assertThat(result).isFalse()
    }

    @Test
    fun `saveToCache should return false for non-existent file`() = runTest {
        val imageInfo = ImageInfo(
            id = "test-image",
            name = "Test Image",
            url = "https://example.com/image.qcow2",
            localPath = "/non/existent/path.qcow2"
        )

        val result = cache.saveToCache(imageInfo)
        assertThat(result).isFalse()
    }

    // ==================== Remove From Cache Tests ====================

    @Test
    fun `removeFromCache should return true for non-existent image`() = runTest {
        val result = cache.removeFromCache("non-existent")
        assertThat(result).isTrue()
    }

    // ==================== CacheStats Tests ====================

    @Test
    fun `CacheStats should have correct properties`() {
        val stats = CacheStats(
            totalImages = 10,
            totalSize = 1024 * 1024 * 500,
            oldestAccessTime = 1000L,
            newestAccessTime = 2000L
        )

        assertThat(stats.totalImages).isEqualTo(10)
        assertThat(stats.totalSize).isEqualTo(1024 * 1024 * 500L)
        assertThat(stats.oldestAccessTime).isEqualTo(1000L)
        assertThat(stats.newestAccessTime).isEqualTo(2000L)
    }

    @Test
    fun `CacheStats formattedSize should return human readable format`() {
        val stats = CacheStats(totalSize = 1024 * 1024 * 100) // 100 MB

        assertThat(stats.formattedSize).contains("MB")
    }
}