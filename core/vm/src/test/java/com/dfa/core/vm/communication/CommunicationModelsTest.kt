package com.dfa.core.vm.communication

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CommunicationState测试
 */
class CommunicationStateTest {

    @Test
    fun `CommunicationState enum contains all expected values`() {
        val states = CommunicationState.values()
        assertTrue(states.contains(CommunicationState.DISCONNECTED))
        assertTrue(states.contains(CommunicationState.CONNECTING))
        assertTrue(states.contains(CommunicationState.CONNECTED))
        assertTrue(states.contains(CommunicationState.RECONNECTING))
        assertTrue(states.contains(CommunicationState.ERROR))
    }

    @Test
    fun `ChannelType enum contains all expected values`() {
        val types = ChannelType.values()
        assertTrue(types.contains(ChannelType.VIRTIO_SERIAL))
        assertTrue(types.contains(ChannelType.VSOCK))
        assertTrue(types.contains(ChannelType.SHARED_MEMORY))
    }

    @Test
    fun `ChannelConfig validates correctly for VirtIO`() {
        val validConfig = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = "/dev/virtio-ports/test"
        )
        assertTrue(validConfig.validate())

        val invalidConfig = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = null
        )
        assertFalse(invalidConfig.validate())
    }

    @Test
    fun `ChannelConfig validates correctly for Vsock`() {
        val validConfig = ChannelConfig(
            type = ChannelType.VSOCK,
            port = 1234
        )
        assertTrue(validConfig.validate())

        val invalidConfig = ChannelConfig(
            type = ChannelType.VSOCK,
            port = 0
        )
        assertFalse(invalidConfig.validate())
    }

    @Test
    fun `ConnectionInfo isConnected returns correct value`() {
        val connectedInfo = ConnectionInfo(
            channelId = "test",
            type = ChannelType.VIRTIO_SERIAL,
            state = CommunicationState.CONNECTED
        )
        assertTrue(connectedInfo.isConnected)

        val disconnectedInfo = ConnectionInfo(
            channelId = "test",
            type = ChannelType.VIRTIO_SERIAL,
            state = CommunicationState.DISCONNECTED
        )
        assertFalse(disconnectedInfo.isConnected)
    }

    @Test
    fun `ConnectionInfo connectionDuration calculates correctly`() {
        val now = System.currentTimeMillis()
        val info = ConnectionInfo(
            channelId = "test",
            type = ChannelType.VIRTIO_SERIAL,
            state = CommunicationState.CONNECTED,
            connectedAt = now - 5000
        )

        assertTrue(info.connectionDuration >= 5000)
    }
}

/**
 * FileTransfer测试
 */
class FileTransferTest {

    @Test
    fun `TransferProgress calculates percentage correctly`() {
        val progress = TransferProgress(
            bytesTransferred = 50,
            totalBytes = 100
        )
        assertEquals(50f, progress.percentage, 0.01f)
    }

    @Test
    fun `TransferProgress isComplete returns correct value`() {
        val completeProgress = TransferProgress(
            bytesTransferred = 100,
            totalBytes = 100
        )
        assertTrue(completeProgress.isComplete)

        val incompleteProgress = TransferProgress(
            bytesTransferred = 50,
            totalBytes = 100
        )
        assertFalse(incompleteProgress.isComplete)
    }

    @Test
    fun `TransferProgress calculate returns correct values`() {
        val progress = TransferProgress.calculate(
            bytesTransferred = 1000,
            totalBytes = 10000,
            elapsedTimeMs = 1000
        )

        assertEquals(1000L, progress.bytesTransferred)
        assertEquals(10000L, progress.totalBytes)
        assertEquals(10f, progress.percentage, 0.01f)
        assertEquals(1000L, progress.averageSpeed)
    }

    @Test
    fun `FileTransferConfig validates correctly`() {
        val validConfig = FileTransferConfig()
        assertTrue(validConfig.validate())

        val invalidConfig = FileTransferConfig(
            chunkSize = 0,
            maxConcurrentChunks = 0
        )
        assertFalse(invalidConfig.validate())
    }
}