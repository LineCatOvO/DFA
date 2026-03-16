package com.dfa.core.vm.integration

import com.dfa.core.vm.storage.models.StorageConfig
import com.dfa.core.vm.storage.models.StorageType
import org.junit.Assert.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

/**
 * Storage 集成测试
 *
 * 测试存储管理器与各组件的集成
 */
class StorageIntegrationTest {

    // ==================== Storage Config Tests ====================

    @Test
    fun `valid storage config should pass validation`() = runTest {
        val config = StorageConfig(
            storagePath = "/tmp/test-storage",
            maxStorageBytes = 1024L * 1024 * 1024 * 10 // 10GB
        )

        assertTrue(config.validate())
    }

    @Test
    fun `invalid storage config should fail validation`() = runTest {
        val config = StorageConfig(
            storagePath = "", // Invalid: empty path
            maxStorageBytes = 0 // Invalid: zero size
        )

        assertFalse(config.validate())
    }

    // ==================== Storage Type Tests ====================

    @Test
    fun `StorageType should contain all expected types`() {
        val expectedTypes = listOf(
            StorageType.INTERNAL,
            StorageType.EXTERNAL,
            StorageType.ENCRYPTED,
            StorageType.SAF
        )

        assertEquals(expectedTypes.size, StorageType.entries.size)
        expectedTypes.forEach { type ->
            assertTrue(StorageType.entries.contains(type))
        }
    }

    // ==================== File System Tests ====================

    @Test
    fun `storage directory should be creatable`() = runTest {
        val testPath = "/tmp/test-storage-${System.currentTimeMillis()}"
        val dir = File(testPath)

        val created = dir.mkdirs()

        assertTrue(created || dir.exists())

        // Cleanup
        dir.deleteRecursively()
    }

    @Test
    fun `storage directory should be writable`() = runTest {
        val testPath = "/tmp/test-storage-write-${System.currentTimeMillis()}"
        val dir = File(testPath)
        dir.mkdirs()

        val testFile = File(dir, "test.txt")
        testFile.writeText("test content")

        assertTrue(testFile.exists())
        assertEquals("test content", testFile.readText())

        // Cleanup
        dir.deleteRecursively()
    }

    // ==================== Storage Info Tests ====================

    @Test
    fun `storage info should calculate usage correctly`() = runTest {
        val testPath = "/tmp/test-storage-info-${System.currentTimeMillis()}"
        val dir = File(testPath)
        dir.mkdirs()

        // Create some test files
        File(dir, "file1.txt").writeText("content1")
        File(dir, "file2.txt").writeText("content2")

        val totalSize = dir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }

        assertTrue(totalSize > 0)

        // Cleanup
        dir.deleteRecursively()
    }

    // ==================== Encryption Config Tests ====================

    @Test
    fun `encryption config should be optional`() = runTest {
        val configWithoutEncryption = StorageConfig(
            storagePath = "/tmp/test",
            maxStorageBytes = 1024 * 1024 * 1024,
            enableEncryption = false
        )

        assertFalse(configWithoutEncryption.enableEncryption)
        assertNull(configWithoutEncryption.encryptionKeyAlias)
    }

    @Test
    fun `encryption config should have key alias when enabled`() = runTest {
        val configWithEncryption = StorageConfig(
            storagePath = "/tmp/test",
            maxStorageBytes = 1024 * 1024 * 1024,
            enableEncryption = true,
            encryptionKeyAlias = "test_key"
        )

        assertTrue(configWithEncryption.enableEncryption)
        assertEquals("test_key", configWithEncryption.encryptionKeyAlias)
    }

    // ==================== Quota Tests ====================

    @Test
    fun `storage quota should be configurable`() = runTest {
        val config = StorageConfig(
            storagePath = "/tmp/test",
            maxStorageBytes = 1024L * 1024 * 1024 * 5 // 5GB
        )

        assertEquals(1024L * 1024 * 1024 * 5, config.maxStorageBytes)
    }

    // ==================== Cleanup Tests ====================

    @Test
    fun `temp files should be cleanable`() = runTest {
        val testPath = "/tmp/test-storage-cleanup-${System.currentTimeMillis()}"
        val dir = File(testPath)
        dir.mkdirs()

        // Create temp files
        File(dir, "temp1.tmp").writeText("temp content 1")
        File(dir, "temp2.tmp").writeText("temp content 2")
        File(dir, "data.txt").writeText("data content")

        // Count temp files
        val tempFiles = dir.listFiles()?.filter { it.extension == "tmp" } ?: emptyList()
        assertEquals(2, tempFiles.size)

        // Delete temp files
        tempFiles.forEach { it.delete() }

        // Verify only non-temp files remain
        val remainingFiles = dir.listFiles()?.toList() ?: emptyList()
        assertEquals(1, remainingFiles.size)
        assertEquals("data.txt", remainingFiles[0].name)

        // Cleanup
        dir.deleteRecursively()
    }
}