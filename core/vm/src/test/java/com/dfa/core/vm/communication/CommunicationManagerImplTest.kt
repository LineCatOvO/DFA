package com.dfa.core.vm.communication

import com.dfa.core.vm.protocol.MessageCodec
import com.google.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * CommunicationManagerImpl 单元测试
 */
class CommunicationManagerImplTest {

    private lateinit var messageCodec: MessageCodec
    private lateinit var manager: CommunicationManagerImpl

    @Before
    fun setup() {
        messageCodec = mockk(relaxed = true)
        manager = CommunicationManagerImpl(messageCodec)
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `initial state should be DISCONNECTED`() = runTest {
        assertThat(manager.state.value).isEqualTo(CommunicationState.DISCONNECTED)
    }

    @Test
    fun `initial isInitialized should be false`() = runTest {
        assertThat(manager.isInitialized.value).isFalse()
    }

    @Test
    fun `initial activeConnections should be empty`() = runTest {
        assertThat(manager.activeConnections.value).isEmpty()
    }

    // ==================== Initialize Tests ====================

    @Test
    fun `initialize should fail with empty configs`() = runTest {
        val result = manager.initialize(emptyList())

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `initialize should fail with invalid config`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = null // Invalid: path is required
        )

        val result = manager.initialize(listOf(config))

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Channel Management Tests ====================

    @Test
    fun `getChannel should return null for non-existent channel`() = runTest {
        val channel = manager.getChannel("non-existent")
        assertThat(channel).isNull()
    }

    @Test
    fun `getActiveChannels should return empty list initially`() = runTest {
        val channels = manager.getActiveChannels()
        assertThat(channels).isEmpty()
    }

    // ==================== Send Tests ====================

    @Test
    fun `send should fail for non-existent channel`() = runTest {
        val result = manager.send("non-existent", byteArrayOf(1, 2, 3))

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Broadcast Tests ====================

    @Test
    fun `broadcast should return empty map when no channels`() = runTest {
        val results = manager.broadcast(byteArrayOf(1, 2, 3))

        assertThat(results).isEmpty()
    }

    // ==================== Connection Info Tests ====================

    @Test
    fun `getConnectionInfo should return null for non-existent channel`() = runTest {
        val info = manager.getConnectionInfo("non-existent")
        assertThat(info).isNull()
    }

    // ==================== Reconnect Tests ====================

    @Test
    fun `reconnect should fail for non-existent channel`() = runTest {
        val result = manager.reconnect("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Close All Tests ====================

    @Test
    fun `closeAll should succeed with no channels`() = runTest {
        // Should not throw
        manager.closeAll()
        assertThat(manager.state.value).isEqualTo(CommunicationState.DISCONNECTED)
    }

    // ==================== Release Tests ====================

    @Test
    fun `release should reset state`() = runTest {
        manager.release()

        assertThat(manager.state.value).isEqualTo(CommunicationState.DISCONNECTED)
        assertThat(manager.isInitialized.value).isFalse()
        assertThat(manager.activeConnections.value).isEmpty()
    }

    // ==================== State Transition Tests ====================

    @Test
    fun `state should be DISCONNECTED after release`() = runTest {
        manager.release()

        assertThat(manager.state.value).isEqualTo(CommunicationState.DISCONNECTED)
    }

    // ==================== Receive Tests ====================

    @Test
    fun `receive should return flow for all channels when channelId is null`() = runTest {
        val flow = manager.receive(null)
        assertThat(flow).isNotNull()
    }

    @Test
    fun `receive should return filtered flow for specific channel`() = runTest {
        val flow = manager.receive("channel-123")
        assertThat(flow).isNotNull()
    }
}