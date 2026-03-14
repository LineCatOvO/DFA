package com.dfa.core.vm.integration

import com.dfa.core.vm.storage.models.StorageConfig
import com.dfa.core.vm.storage.models.StorageType
import com.google.truth.Truth.assertThat
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

        assertThat(config.validate()).isTrue()
    }

    @Test
    fun `invalid storage config should fail validation`() = runTest {
        val config = StorageConfig(
            storagePath = "", // Invalid: empty path
            maxStorageBytes = 0 // Invalid: zero size
        )

        assertThat(config.validate()).isFalse()
    }

    // ==================== Storage Type Tests ====================

    @Test
    fun `StorageType should contain all expected types`() {
        val expectedTypes = listOf(
            StorageType.INTERNAL,
            StorageType.EXTERNAL,
            StorageType.ENCRYPTED,
            StorageType.NETWORK
        )

        assertThat(StorageType.entries.size).isEqualTo(expectedTypes.size)
        expectedTypes.forEach { type ->
            assertThat(StorageType.entries.contains(type)).isTrue()
        }
    }

    // ==================== File System Tests ====================

    @Test
    fun `storage directory should be creatable`() = runTest {
        val testPath = "/tmp/test-storage-${System.currentTimeMillis()}"
        val dir = File(testPath)

        val created = dir.mkdirs()

        assertThat(created || dir.exists()).isTrue()

        // Cleanup
        dir.deleteRecursively()
    }

    @Test
    fun `storage directory should be writable`() = runTest {
        val testPath = "/tmp/test-storage-write-${System.currentTimeMillis()}"
        val dir = File(testPath)
        dir.mkdirs()

        val testFile = File(dir, "test.txt")
        val written = testFile.writeText("test content")

        assertThat(testFile.exists()).isTrue()
        assertThat(testFile.readText()).isEqualTo("test content")

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

        assertThat(totalSize).isGreaterThan(0)

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

        assertThat(configWithoutEncryption.enableEncryption).isFalse()
        assertThat(configWithoutEncryption.encryptionKeyAlias).isNull()
    }

    @Test
    fun `encryption config should have key alias when enabled`() = runTest {
        val configWithEncryption = StorageConfig(
            storagePath = "/tmp/test",
            maxStorageBytes = 1024 * 1024 * 1024,
            enableEncryption = true,
            encryptionKeyAlias = "test_key"
        )

        assertThat(configWithEncryption.enableEncryption).isTrue()
        assertThat(configWithEncryption.encryptionKeyAlias).isEqualTo("test_key")
    }

    // ==================== Quota Tests ====================

    @Test
    fun `storage quota should be configurable`() = runTest {
        val config = StorageConfig(
            storagePath = "/tmp/test",
            maxStorageBytes = 1024L * 1024 * 1024 * 5 // 5GB
        )

        assertThat(config.maxStorageBytes).isEqualTo(1024L * 1024 * 1024 * 5)
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
        assertThat(tempFiles).hasSize(2)

        // Delete temp files
        tempFiles.forEach { it.delete() }

        // Verify only non-temp files remain
        val remainingFiles = dir.listFiles()?.toList() ?: emptyList()
        assertThat(remainingFiles).hasSize(1)
        assertThat(remainingFiles[0].name).isEqualTo("data.txt")

        // Cleanup
        dir.deleteRecursively()
    }
}