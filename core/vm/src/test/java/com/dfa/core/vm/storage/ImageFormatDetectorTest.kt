package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.image.ImageFormatDetector
import com.dfa.core.vm.storage.models.DiskImageFormat
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/**
 * ImageFormatDetector单元测试
 */
class ImageFormatDetectorTest {

    private lateinit var imageFormatDetector: ImageFormatDetector
    private lateinit var tempDir: File

    @Before
    fun setup() {
        imageFormatDetector = ImageFormatDetector()
        tempDir = File(System.getProperty("java.io.tmpdir"), "image_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @Test
    fun `detectFormatFromExtension should detect RAW format`() {
        val file = File(tempDir, "test.raw")
        assertEquals("Should detect RAW format", DiskImageFormat.RAW, imageFormatDetector.detectFormatFromExtension(file))

        val imgFile = File(tempDir, "test.img")
        assertEquals("Should detect RAW format for .img", DiskImageFormat.RAW, imageFormatDetector.detectFormatFromExtension(imgFile))
    }

    @Test
    fun `detectFormatFromExtension should detect QCOW2 format`() {
        val file = File(tempDir, "test.qcow2")
        assertEquals("Should detect QCOW2 format", DiskImageFormat.QCOW2, imageFormatDetector.detectFormatFromExtension(file))
    }

    @Test
    fun `detectFormatFromExtension should detect VMDK format`() {
        val file = File(tempDir, "test.vmdk")
        assertEquals("Should detect VMDK format", DiskImageFormat.VMDK, imageFormatDetector.detectFormatFromExtension(file))
    }

    @Test
    fun `detectFormatFromExtension should detect VDI format`() {
        val file = File(tempDir, "test.vdi")
        assertEquals("Should detect VDI format", DiskImageFormat.VDI, imageFormatDetector.detectFormatFromExtension(file))
    }

    @Test
    fun `detectFormatFromExtension should detect VHD format`() {
        val file = File(tempDir, "test.vhd")
        assertEquals("Should detect VHD format", DiskImageFormat.VHD, imageFormatDetector.detectFormatFromExtension(file))
    }

    @Test
    fun `detectFormatFromHeader should detect QCOW2 magic`() {
        // QCOW2 magic: 0x514649fb ("QFI\xfb")
        val header = byteArrayOf(0x51, 0x46, 0x49, 0xfb.toByte(), 0x00, 0x00, 0x00, 0x03)
        assertEquals("Should detect QCOW2 format", DiskImageFormat.QCOW2, imageFormatDetector.detectFormatFromHeader(header))
    }

    @Test
    fun `detectFormatFromHeader should return null for unknown format`() {
        val header = ByteArray(512) { 0x00 }
        assertNull("Should return null for unknown format", imageFormatDetector.detectFormatFromHeader(header))
    }

    @Test
    fun `supportsSnapshots should return correct values`() {
        assertTrue("QCOW2 should support snapshots", imageFormatDetector.supportsSnapshots(DiskImageFormat.QCOW2))
        assertTrue("VDI should support snapshots", imageFormatDetector.supportsSnapshots(DiskImageFormat.VDI))
        assertFalse("RAW should not support snapshots", imageFormatDetector.supportsSnapshots(DiskImageFormat.RAW))
    }

    @Test
    fun `supportsCompression should return correct values`() {
        assertTrue("QCOW2 should support compression", imageFormatDetector.supportsCompression(DiskImageFormat.QCOW2))
        assertFalse("RAW should not support compression", imageFormatDetector.supportsCompression(DiskImageFormat.RAW))
    }

    @Test
    fun `supportsEncryption should return correct values`() {
        assertTrue("QCOW2 should support encryption", imageFormatDetector.supportsEncryption(DiskImageFormat.QCOW2))
        assertFalse("RAW should not support encryption", imageFormatDetector.supportsEncryption(DiskImageFormat.RAW))
    }

    @Test
    fun `getFormatDescription should return non-empty description`() {
        for (format in DiskImageFormat.entries) {
            val description = imageFormatDetector.getFormatDescription(format)
            assertTrue("Description should not be empty for $format", description.isNotEmpty())
        }
    }

    private fun cleanup() {
        tempDir.deleteRecursively()
    }
}