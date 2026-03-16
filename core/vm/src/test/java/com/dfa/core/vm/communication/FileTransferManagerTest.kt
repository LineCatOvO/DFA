package com.dfa.core.vm.communication

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * FileTransferManager 单元测试
 */
class FileTransferManagerTest {

    private lateinit var communicationManager: CommunicationManager
    private lateinit var manager: FileTransferManagerImpl

    @Before
    fun setup() {
        communicationManager = mockk(relaxed = true)
        manager = FileTransferManagerImpl(communicationManager)
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `initial activeSessions should be empty`() {
        assertTrue(manager.activeSessions.value.isEmpty())
    }

    // ==================== Upload Tests ====================

    @Test
    fun `upload should fail for non-existent file`() = runTest {
        val file = File("/non/existent/file.txt")

        val result = manager.upload("channel-1", file)

        assertTrue(result.isFailure)
    }

    @Test
    fun `upload should fail for non-existent channel`() = runTest {
        val tempFile = File.createTempFile("test", ".txt")
        tempFile.writeText("test content")
        every { communicationManager.getChannel(any()) } returns null

        val result = manager.upload("non-existent-channel", tempFile)

        assertTrue(result.isFailure)
        tempFile.delete()
    }

    // ==================== Download Tests ====================

    @Test
    fun `download should fail for non-existent channel`() = runTest {
        val tempFile = File.createTempFile("download", ".txt")
        every { communicationManager.getChannel(any()) } returns null

        val result = manager.download("non-existent-channel", "/remote/path", tempFile)

        assertTrue(result.isFailure)
        tempFile.delete()
    }

    // ==================== Get Session Tests ====================

    @Test
    fun `getSession should return null for non-existent session`() {
        val result = manager.getSession("non-existent")
        assertNull(result)
    }

    // ==================== Cancel Transfer Tests ====================

    @Test
    fun `cancelTransfer should fail for non-existent session`() = runTest {
        val result = manager.cancelTransfer("non-existent")

        assertTrue(result.isFailure)
    }

    // ==================== Cancel All Transfers Tests ====================

    @Test
    fun `cancelAllTransfers should succeed with no sessions`() = runTest {
        // Should not throw
        manager.cancelAllTransfers()
        assertTrue(manager.activeSessions.value.isEmpty())
    }

    // ==================== Get Statistics Tests ====================

    @Test
    fun `getStatistics should return initial statistics`() {
        val stats = manager.getStatistics()

        assertEquals(0, stats.totalUploads)
        assertEquals(0, stats.totalDownloads)
        assertEquals(0L, stats.totalBytesUploaded)
        assertEquals(0L, stats.totalBytesDownloaded)
        assertEquals(0, stats.activeUploads)
        assertEquals(0, stats.activeDownloads)
        assertEquals(0, stats.failedTransfers)
    }

    // ==================== TransferStatistics Tests ====================

    @Test
    fun `TransferStatistics should have correct properties`() {
        val stats = TransferStatistics(
            totalUploads = 10,
            totalDownloads = 5,
            totalBytesUploaded = 1024 * 1024,
            totalBytesDownloaded = 2048 * 1024,
            activeUploads = 2,
            activeDownloads = 1,
            failedTransfers = 3
        )

        assertEquals(10, stats.totalUploads)
        assertEquals(5, stats.totalDownloads)
        assertEquals(1024 * 1024, stats.totalBytesUploaded)
        assertEquals(2048 * 1024, stats.totalBytesDownloaded)
        assertEquals(2, stats.activeUploads)
        assertEquals(1, stats.activeDownloads)
        assertEquals(3, stats.failedTransfers)
    }

    // ==================== FileTransferConfig Tests ====================

    @Test
    fun `FileTransferConfig should have default values`() {
        val config = FileTransferConfig()

        assertTrue(config.chunkSize > 0)
        assertTrue(config.timeoutMs > 0)
        assertTrue(config.maxRetries >= 0)
        assertTrue(config.maxFileSize > 0)
    }

    @Test
    fun `FileTransferConfig copy should work correctly`() {
        val original = FileTransferConfig()
        val copied = original.copy(chunkSize = 8192)

        assertEquals(8192, copied.chunkSize)
        assertNotEquals(8192, original.chunkSize)
    }

    // ==================== TransferDirection Tests ====================

    @Test
    fun `TransferDirection should contain UPLOAD and DOWNLOAD`() {
        assertTrue(TransferDirection.entries.contains(TransferDirection.UPLOAD))
        assertTrue(TransferDirection.entries.contains(TransferDirection.DOWNLOAD))
    }

    // ==================== TransferState Tests ====================

    @Test
    fun `TransferState should contain all expected states`() {
        val expectedStates = listOf(
            TransferState.PENDING,
            TransferState.IN_PROGRESS,
            TransferState.PAUSED,
            TransferState.COMPLETED,
            TransferState.FAILED,
            TransferState.CANCELLED
        )

        assertEquals(expectedStates.size, TransferState.entries.size)
        expectedStates.forEach { state ->
            assertTrue(TransferState.entries.contains(state))
        }
    }

    // ==================== TransferProgress Tests ====================

    @Test
    fun `TransferProgress should calculate percentage correctly`() {
        val progress = TransferProgress(
            bytesTransferred = 500,
            totalBytes = 1000,
            percentage = 50f
        )

        assertEquals(500L, progress.bytesTransferred)
        assertEquals(1000L, progress.totalBytes)
        assertEquals(50f, progress.percentage, 0.01f)
    }

    @Test
    fun `TransferProgress calculate should create valid progress`() {
        val progress = TransferProgress.calculate(
            bytesTransferred = 500,
            totalBytes = 1000,
            elapsedTimeMs = 5000
        )

        assertEquals(500L, progress.bytesTransferred)
        assertEquals(1000L, progress.totalBytes)
        assertTrue(progress.percentage > 0)
    }

    // ==================== TransferError Tests ====================

    @Test
    fun `TransferError NetworkError should contain message`() {
        val error = TransferError.NetworkError("Connection failed")

        assertEquals("Connection failed", error.message)
    }

    @Test
    fun `TransferError StorageError should contain message`() {
        val error = TransferError.StorageError("Disk full")

        assertEquals("Disk full", error.message)
    }

    @Test
    fun `TransferError SizeLimitError should contain message`() {
        val error = TransferError.SizeLimitError("File too large")

        assertEquals("File too large", error.message)
    }

    @Test
    fun `TransferError UnknownError should contain message`() {
        val error = TransferError.UnknownError("Unknown error")

        assertEquals("Unknown error", error.message)
    }

    // ==================== TransferResult Tests ====================

    @Test
    fun `TransferResult Success should contain all properties`() {
        val result = TransferResult.Success(
            sessionId = "session-1",
            fileName = "test.txt",
            bytesTransferred = 1024,
            durationMs = 5000
        )

        assertEquals("session-1", result.sessionId)
        assertEquals("test.txt", result.fileName)
        assertEquals(1024L, result.bytesTransferred)
        assertEquals(5000L, result.durationMs)
    }

    @Test
    fun `TransferResult Cancelled should contain sessionId and bytesTransferred`() {
        val result = TransferResult.Cancelled(
            sessionId = "session-1",
            bytesTransferred = 512
        )

        assertEquals("session-1", result.sessionId)
        assertEquals(512L, result.bytesTransferred)
    }
}