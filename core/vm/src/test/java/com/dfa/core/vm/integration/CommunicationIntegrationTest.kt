package com.dfa.core.vm.integration

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationManagerImpl
import com.dfa.core.vm.communication.CommunicationState
import com.dfa.core.vm.protocol.MessageCodecImpl
import com.google.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Communication 集成测试
 *
 * 测试通信管理器与各通道的集成
 */
class CommunicationIntegrationTest {

    private lateinit var messageCodec: MessageCodecImpl
    private lateinit var manager: CommunicationManagerImpl

    @Before
    fun setup() {
        messageCodec = MessageCodecImpl()
        manager = CommunicationManagerImpl(messageCodec)
    }

    // ==================== Manager Lifecycle Tests ====================

    @Test
    fun `manager should start in DISCONNECTED state`() = runTest {
        assertThat(manager.state.value).isEqualTo(CommunicationState.DISCONNECTED)
    }

    @Test
    fun `manager should not be initialized initially`() = runTest {
        assertThat(manager.isInitialized.value).isFalse()
    }

    // ==================== Channel Configuration Tests ====================

    @Test
    fun `valid channel config should pass validation`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = "/dev/vport0p1"
        )

        assertThat(config.validate()).isTrue()
    }

    @Test
    fun `invalid channel config should fail validation`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = null // Invalid: path required
        )

        assertThat(config.validate()).isFalse()
    }

    // ==================== Message Codec Tests ====================

    @Test
    fun `message codec should encode and decode correctly`() = runTest {
        val originalMessage = "Test message for integration"
        val encoded = messageCodec.encode(originalMessage)
        val decoded = messageCodec.decode<String>(encoded)

        assertThat(decoded.isSuccess).isTrue()
        assertThat(decoded.getOrNull()).isEqualTo(originalMessage)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `initialize with empty configs should fail`() = runTest {
        val result = manager.initialize(emptyList())

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `send to non-existent channel should fail`() = runTest {
        val result = manager.send("non-existent", byteArrayOf(1, 2, 3))

        assertThat(result.isFailure).isTrue()
    }

    // ==================== State Management Tests ====================

    @Test
    fun `release should reset all state`() = runTest {
        manager.release()

        assertThat(manager.state.value).isEqualTo(CommunicationState.DISCONNECTED)
        assertThat(manager.isInitialized.value).isFalse()
        assertThat(manager.activeConnections.value).isEmpty()
    }

    // ==================== Broadcast Tests ====================

    @Test
    fun `broadcast with no channels should return empty map`() = runTest {
        val results = manager.broadcast(byteArrayOf(1, 2, 3))

        assertThat(results).isEmpty()
    }

    // ==================== Connection Management Tests ====================

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
}