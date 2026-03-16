package com.dfa.core.vm.integration

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationManagerImpl
import com.dfa.core.vm.communication.CommunicationState
import com.dfa.core.vm.protocol.MessageCodecImpl
import com.dfa.core.vm.protocol.Request
import com.dfa.core.vm.protocol.RequestMethod
import org.junit.Assert.*
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
        assertEquals(CommunicationState.DISCONNECTED, manager.state.value)
    }

    @Test
    fun `manager should not be initialized initially`() = runTest {
        assertFalse(manager.isInitialized.value)
    }

    // ==================== Channel Configuration Tests ====================

    @Test
    fun `valid channel config should pass validation`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = "/dev/vport0p1"
        )

        assertTrue(config.validate())
    }

    @Test
    fun `invalid channel config should fail validation`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = null // Invalid: path required
        )

        assertFalse(config.validate())
    }

    // ==================== Message Codec Tests ====================

    @Test
    fun `message codec should encode and decode request correctly`() = runTest {
        val request = Request(
            messageId = "test-id",
            method = RequestMethod.VM_START,
            sourceId = "host",
            targetId = "vm"
        )
        
        val encoded = messageCodec.encodeRequest(request)
        assertTrue(encoded.isSuccess)
        
        val decoded = messageCodec.decodeRequest(encoded.getOrThrow())
        assertTrue(decoded.isSuccess)
        assertEquals(RequestMethod.VM_START, decoded.getOrNull()?.body?.method)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `initialize with empty configs should fail`() = runTest {
        val result = manager.initialize(emptyList())

        assertTrue(result.isFailure)
    }

    @Test
    fun `send to non-existent channel should fail`() = runTest {
        val result = manager.send("non-existent", byteArrayOf(1, 2, 3))

        assertTrue(result.isFailure)
    }

    // ==================== State Management Tests ====================

    @Test
    fun `release should reset all state`() = runTest {
        manager.release()

        assertEquals(CommunicationState.DISCONNECTED, manager.state.value)
        assertFalse(manager.isInitialized.value)
        assertTrue(manager.activeConnections.value.isEmpty())
    }

    // ==================== Broadcast Tests ====================

    @Test
    fun `broadcast with no channels should return empty map`() = runTest {
        val results = manager.broadcast(byteArrayOf(1, 2, 3))

        assertTrue(results.isEmpty())
    }

    // ==================== Connection Management Tests ====================

    @Test
    fun `getChannel should return null for non-existent channel`() = runTest {
        val channel = manager.getChannel("non-existent")

        assertNull(channel)
    }

    @Test
    fun `getActiveChannels should return empty list initially`() = runTest {
        val channels = manager.getActiveChannels()

        assertTrue(channels.isEmpty())
    }
}