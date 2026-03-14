package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.image.ImageFormatDetector
import com.dfa.core.vm.storage.image.Qcow2Handler
import com.dfa.core.vm.storage.image.RawImageHandler
import com.dfa.core.vm.storage.models.CreateDiskImageRequest
import com.dfa.core.vm.storage.models.DiskImageFormat
import com.dfa.core.vm.storage.models.DiskImageInfo
import com.dfa.core.vm.storage.models.DiskImageState
import com.google.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * DiskImageManagerImpl 单元测试
 */
class DiskImageManagerImplTest {

    private lateinit var qcow2Handler: Qcow2Handler
    private lateinit var rawImageHandler: RawImageHandler
    private lateinit var imageFormatDetector: ImageFormatDetector
    private lateinit var storageConfig: StorageConfigProvider
    private lateinit var manager: DiskImageManagerImpl

    @Before
    fun setup() {
        qcow2Handler = mockk(relaxed = true)
        rawImageHandler = mockk(relaxed = true)
        imageFormatDetector = mockk(relaxed = true)
        storageConfig = mockk(relaxed = true)

        every { storageConfig.getDefaultImagePath() } returns "/tmp/images"
        every { storageConfig.getStoragePath() } returns "/tmp"
        every { storageConfig.getMaxStorageBytes() } returns 1024L * 1024 * 1024 * 10

        manager = DiskImageManagerImpl(
            qcow2Handler,
            rawImageHandler,
            imageFormatDetector,
            storageConfig
        )
    }

    // ==================== Create Image Tests ====================

    @Test
    fun `createImage should fail with invalid request`() = runTest {
        val request = CreateDiskImageRequest(
            name = "",
            format = DiskImageFormat.QCOW2,
            sizeBytes = 0
        )

        val result = manager.createImage(request)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `createImage should succeed with valid request`() = runTest {
        val request = CreateDiskImageRequest(
            name = "test-image",
            format = DiskImageFormat.RAW,
            sizeBytes = 1024 * 1024 * 1024
        )
        val imageInfo = DiskImageInfo(
            id = "img-1",
            path = "/tmp/images/test-image.raw",
            format = DiskImageFormat.RAW,
            virtualSizeBytes = 1024 * 1024 * 1024,
            actualSizeBytes = 0,
            state = DiskImageState.READY
        )
        coEvery { rawImageHandler.createImage(any(), any(), any()) } returns Result.success(imageInfo)

        val result = manager.createImage(request)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Delete Image Tests ====================

    @Test
    fun `deleteImage should fail for non-existent image`() = runTest {
        val result = manager.deleteImage("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Get Image Info Tests ====================

    @Test
    fun `getImageInfo should fail for non-existent image`() = runTest {
        val result = manager.getImageInfo("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getImageInfoByPath should fail for non-existent path`() = runTest {
        coEvery { imageFormatDetector.detectFormat(any()) } returns Result.failure(Exception("Not found"))

        val result = manager.getImageInfoByPath("/non/existent/path")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== List Images Tests ====================

    @Test
    fun `listImages should return empty list initially`() = runTest {
        val result = manager.listImages()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    // ==================== List Images By Vm Tests ====================

    @Test
    fun `listImagesByVm should return empty list for non-existent vm`() = runTest {
        val result = manager.listImagesByVm("non-existent")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    // ==================== Validate Image Tests ====================

    @Test
    fun `validateImage should fail for non-existent image`() = runTest {
        val result = manager.validateImage("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Resize Image Tests ====================

    @Test
    fun `resizeImage should fail for non-existent image`() = runTest {
        val result = manager.resizeImage("non-existent", 1024 * 1024 * 2048)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Create Snapshot Tests ====================

    @Test
    fun `createSnapshot should fail for non-existent image`() = runTest {
        val request = CreateSnapshotRequest(
            imageId = "non-existent",
            name = "snapshot-1"
        )

        val result = manager.createSnapshot(request)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Delete Snapshot Tests ====================

    @Test
    fun `deleteSnapshot should fail for non-existent snapshot`() = runTest {
        val result = manager.deleteSnapshot("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== List Snapshots Tests ====================

    @Test
    fun `listSnapshots should return empty list for non-existent image`() = runTest {
        val result = manager.listSnapshots("non-existent")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    // ==================== Restore Snapshot Tests ====================

    @Test
    fun `restoreSnapshot should fail for non-existent snapshot`() = runTest {
        val result = manager.restoreSnapshot("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Lock Image Tests ====================

    @Test
    fun `lockImage should fail for non-existent image`() = runTest {
        val result = manager.lockImage("non-existent", "vm-1")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Unlock Image Tests ====================

    @Test
    fun `unlockImage should fail for non-existent image`() = runTest {
        val result = manager.unlockImage("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Detect Format Tests ====================

    @Test
    fun `detectFormat should delegate to detector`() = runTest {
        coEvery { imageFormatDetector.detectFormat("/path/to/image") } returns Result.success(DiskImageFormat.QCOW2)

        val result = manager.detectFormat("/path/to/image")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(DiskImageFormat.QCOW2)
    }

    // ==================== Copy Image Tests ====================

    @Test
    fun `copyImage should fail for non-existent source`() = runTest {
        val result = manager.copyImage("non-existent", "copy")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Import Image Tests ====================

    @Test
    fun `importImage should fail for non-existent source`() = runTest {
        coEvery { imageFormatDetector.detectFormat(any()) } returns Result.failure(Exception("Not found"))

        val result = manager.importImage("/non/existent", "imported")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Export Image Tests ====================

    @Test
    fun `exportImage should fail for non-existent image`() = runTest {
        val result = manager.exportImage("non-existent", "/export/path")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Get Total Image Size Tests ====================

    @Test
    fun `getTotalImageSize should return 0 initially`() = runTest {
        val result = manager.getTotalImageSize()

        assertThat(result).isEqualTo(0L)
    }

    // ==================== Cleanup Unused Images Tests ====================

    @Test
    fun `cleanupUnusedImages should return 0 when no images`() = runTest {
        val result = manager.cleanupUnusedImages()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(0)
    }

    // ==================== Convert Image Tests ====================

    @Test
    fun `convertImage should return flow`() {
        val flow = manager.convertImage(
            sourcePath = "/source/image.qcow2",
            targetPath = "/target/image.raw",
            targetFormat = DiskImageFormat.RAW
        )
        assertThat(flow).isNotNull()
    }
}