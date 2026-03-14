package com.dfa.core.vm.communication

import com.google.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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
        assertThat(manager.activeSessions.value).isEmpty()
    }

    // ==================== Upload Tests ====================

    @Test
    fun `upload should fail for non-existent file`() = runTest {
        val file = File("/non/existent/file.txt")

        val result = manager.upload("channel-1", file)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `upload should fail for non-existent channel`() = runTest {
        val tempFile = File.createTempFile("test", ".txt")
        tempFile.writeText("test content")
        every { communicationManager.getChannel(any()) } returns null

        val result = manager.upload("non-existent-channel", tempFile)

        assertThat(result.isFailure).isTrue()
        tempFile.delete()
    }

    // ==================== Download Tests ====================

    @Test
    fun `download should fail for non-existent channel`() = runTest {
        val tempFile = File.createTempFile("download", ".txt")
        every { communicationManager.getChannel(any()) } returns null

        val result = manager.download("non-existent-channel", "/remote/path", tempFile)

        assertThat(result.isFailure).isTrue()
        tempFile.delete()
    }

    // ==================== Get Session Tests ====================

    @Test
    fun `getSession should return null for non-existent session`() {
        val result = manager.getSession("non-existent")
        assertThat(result).isNull()
    }

    // ==================== Cancel Transfer Tests ====================

    @Test
    fun `cancelTransfer should fail for non-existent session`() = runTest {
        val result = manager.cancelTransfer("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Cancel All Transfers Tests ====================

    @Test
    fun `cancelAllTransfers should succeed with no sessions`() = runTest {
        // Should not throw
        manager.cancelAllTransfers()
        assertThat(manager.activeSessions.value).isEmpty()
    }

    // ==================== Get Statistics Tests ====================

    @Test
    fun `getStatistics should return initial statistics`() {
        val stats = manager.getStatistics()

        assertThat(stats.totalUploads).isEqualTo(0)
        assertThat(stats.totalDownloads).isEqualTo(0)
        assertThat(stats.totalBytesUploaded).isEqualTo(0)
        assertThat(stats.totalBytesDownloaded).isEqualTo(0)
        assertThat(stats.activeUploads).isEqualTo(0)
        assertThat(stats.activeDownloads).isEqualTo(0)
        assertThat(stats.failedTransfers).isEqualTo(0)
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

        assertThat(stats.totalUploads).isEqualTo(10)
        assertThat(stats.totalDownloads).isEqualTo(5)
        assertThat(stats.totalBytesUploaded).isEqualTo(1024 * 1024)
        assertThat(stats.totalBytesDownloaded).isEqualTo(2048 * 1024)
        assertThat(stats.activeUploads).isEqualTo(2)
        assertThat(stats.activeDownloads).isEqualTo(1)
        assertThat(stats.failedTransfers).isEqualTo(3)
    }

    // ==================== FileTransferConfig Tests ====================

    @Test
    fun `FileTransferConfig should have default values`() {
        val config = FileTransferConfig()

        assertThat(config.chunkSize).isGreaterThan(0)
        assertThat(config.timeoutMs).isGreaterThan(0)
        assertThat(config.maxRetries).isGreaterThan(0)
        assertThat(config.maxFileSize).isGreaterThan(0)
    }

    @Test
    fun `FileTransferConfig copy should work correctly`() {
        val original = FileTransferConfig()
        val copied = original.copy(chunkSize = 8192)

        assertThat(copied.chunkSize).isEqualTo(8192)
        assertThat(original.chunkSize).isNotEqualTo(8192)
    }

    // ==================== TransferDirection Tests ====================

    @Test
    fun `TransferDirection should contain UPLOAD and DOWNLOAD`() {
        assertThat(TransferDirection.entries).contains(TransferDirection.UPLOAD)
        assertThat(TransferDirection.entries).contains(TransferDirection.DOWNLOAD)
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

        assertThat(TransferState.entries.size).isEqualTo(expectedStates.size)
        expectedStates.forEach { state ->
            assertThat(TransferState.entries.contains(state)).isTrue()
        }
    }

    // ==================== TransferProgress Tests ====================

    @Test
    fun `TransferProgress should calculate progress percentage correctly`() {
        val progress = TransferProgress(
            bytesTransferred = 500,
            totalBytes = 1000,
            speed = 100
        )

        assertThat(progress.progressPercent).isEqualTo(50)
    }

    @Test
    fun `TransferProgress progressPercent should be capped at 100`() {
        val progress = TransferProgress(
            bytesTransferred = 1500,
            totalBytes = 1000,
            speed = 100
        )

        assertThat(progress.progressPercent).isEqualTo(100)
    }

    @Test
    fun `TransferProgress calculate should create valid progress`() {
        val progress = TransferProgress.calculate(
            bytesTransferred = 500,
            totalBytes = 1000,
            elapsedTimeMs = 5000
        )

        assertThat(progress.bytesTransferred).isEqualTo(500)
        assertThat(progress.totalBytes).isEqualTo(1000)
        assertThat(progress.speed).isGreaterThan(0)
    }

    // ==================== TransferError Tests ====================

    @Test
    fun `TransferError NetworkError should contain message`() {
        val error = TransferError.NetworkError("Connection failed")

        assertThat(error.message).isEqualTo("Connection failed")
    }

    @Test
    fun `TransferError StorageError should contain message`() {
        val error = TransferError.StorageError("Disk full")

        assertThat(error.message).isEqualTo("Disk full")
    }

    @Test
    fun `TransferError SizeLimitError should contain message`() {
        val error = TransferError.SizeLimitError("File too large")

        assertThat(error.message).isEqualTo("File too large")
    }

    @Test
    fun `TransferError UnknownError should contain message`() {
        val error = TransferError.UnknownError("Unknown error")

        assertThat(error.message).isEqualTo("Unknown error")
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

        assertThat(result.sessionId).isEqualTo("session-1")
        assertThat(result.fileName).isEqualTo("test.txt")
        assertThat(result.bytesTransferred).isEqualTo(1024)
        assertThat(result.durationMs).isEqualTo(5000)
    }

    @Test
    fun `TransferResult Cancelled should contain sessionId and bytesTransferred`() {
        val result = TransferResult.Cancelled(
            sessionId = "session-1",
            bytesTransferred = 512
        )

        assertThat(result.sessionId).isEqualTo("session-1")
        assertThat(result.bytesTransferred).isEqualTo(512)
    }
}