package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.StorageConfig
import com.dfa.core.vm.storage.models.StorageState
import com.dfa.core.vm.storage.models.StorageType
import org.junit.Assert.*
import org.junit.Test

/**
 * StorageModels单元测试
 */
class StorageModelsTest {

    @Test
    fun `StorageConfig validation should pass for valid config`() {
        val config = StorageConfig(
            storagePath = "/data/storage",
            maxStorageBytes = 10L * 1024 * 1024 * 1024,
            enableEncryption = false
        )

        assertTrue("Valid config should pass validation", config.validate())
    }

    @Test
    fun `StorageConfig validation should fail for empty path`() {
        val config = StorageConfig(
            storagePath = "",
            maxStorageBytes = 10L * 1024 * 1024 * 1024
        )

        assertFalse("Empty path should fail validation", config.validate())
    }

    @Test
    fun `StorageConfig validation should fail for zero max storage`() {
        val config = StorageConfig(
            storagePath = "/data/storage",
            maxStorageBytes = 0
        )

        assertFalse("Zero max storage should fail validation", config.validate())
    }

    @Test
    fun `StorageConfig validation should require key alias when encryption enabled`() {
        val configWithoutKey = StorageConfig(
            storagePath = "/data/storage",
            maxStorageBytes = 10L * 1024 * 1024 * 1024,
            enableEncryption = true,
            encryptionKeyAlias = null
        )

        assertFalse("Encryption without key alias should fail validation", configWithoutKey.validate())

        val configWithKey = StorageConfig(
            storagePath = "/data/storage",
            maxStorageBytes = 10L * 1024 * 1024 * 1024,
            enableEncryption = true,
            encryptionKeyAlias = "test_key"
        )

        assertTrue("Encryption with key alias should pass validation", configWithKey.validate())
    }

    @Test
    fun `StorageInfo usagePercent should calculate correctly`() {
        val info = com.dfa.core.vm.storage.models.StorageInfo(
            path = "/data",
            type = StorageType.INTERNAL,
            totalBytes = 1000L,
            usedBytes = 500L,
            availableBytes = 500L
        )

        assertEquals("Usage percent should be 50", 50, info.usagePercent)
    }

    @Test
    fun `StorageInfo hasEnoughSpace should return correct result`() {
        val info = com.dfa.core.vm.storage.models.StorageInfo(
            path = "/data",
            type = StorageType.INTERNAL,
            totalBytes = 1000L,
            usedBytes = 500L,
            availableBytes = 500L
        )

        assertTrue("Should have enough space for 400 bytes", info.hasEnoughSpace(400))
        assertFalse("Should not have enough space for 600 bytes", info.hasEnoughSpace(600))
    }

    @Test
    fun `StorageInfo needsCleanup should return correct result`() {
        val info = com.dfa.core.vm.storage.models.StorageInfo(
            path = "/data",
            type = StorageType.INTERNAL,
            totalBytes = 1000L,
            usedBytes = 850L,
            availableBytes = 150L
        )

        assertTrue("Should need cleanup at 85%", info.needsCleanup(80))
        assertFalse("Should not need cleanup at 90%", info.needsCleanup(90))
    }

    @Test
    fun `StorageOperationResult success factory should create correct result`() {
        val result = com.dfa.core.vm.storage.models.StorageOperationResult.success(
            bytesProcessed = 1024,
            message = "Success"
        )

        assertTrue("Result should be success", result.success)
        assertEquals("Bytes processed should match", 1024L, result.bytesProcessed)
        assertEquals("Message should match", "Success", result.message)
        assertNull("Error should be null", result.error)
    }

    @Test
    fun `StorageOperationResult failure factory should create correct result`() {
        val result = com.dfa.core.vm.storage.models.StorageOperationResult.failure(
            error = "Test error",
            bytesProcessed = 512
        )

        assertFalse("Result should be failure", result.success)
        assertEquals("Error should match", "Test error", result.error)
        assertEquals("Bytes processed should match", 512L, result.bytesProcessed)
    }

    @Test
    fun `CleanupResult isSuccess should return true when no errors`() {
        val successResult = com.dfa.core.vm.storage.models.CleanupResult(
            imagesCleaned = 5,
            snapshotsCleaned = 3,
            tempFilesCleaned = 10,
            bytesReclaimed = 1024 * 1024,
            errors = emptyList()
        )

        assertTrue("Result with no errors should be success", successResult.isSuccess)

        val failureResult = com.dfa.core.vm.storage.models.CleanupResult(
            imagesCleaned = 5,
            errors = listOf("Error 1", "Error 2")
        )

        assertFalse("Result with errors should not be success", failureResult.isSuccess)
    }

    @Test
    fun `CleanupResult totalItemsCleaned should sum all items`() {
        val result = com.dfa.core.vm.storage.models.CleanupResult(
            imagesCleaned = 5,
            snapshotsCleaned = 3,
            tempFilesCleaned = 10
        )

        assertEquals("Total items should be sum of all", 18, result.totalItemsCleaned)
    }
}