package com.dfa.core.vm.protocol

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MessageCodecImpl单元测试
 */
class MessageCodecImplTest {

    private lateinit var messageCodec: MessageCodecImpl

    @Before
    fun setup() {
        messageCodec = MessageCodecImpl(CodecConfig())
    }

    @Test
    fun `encodeRequest should return valid byte array`() = runTest {
        val request = Request(
            messageId = "test-request-1",
            method = RequestMethod.VM_STATUS,
            sourceId = "host",
            targetId = "vm"
        )

        val result = messageCodec.encodeRequest(request)

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()!!.isNotEmpty())
    }

    @Test
    fun `decodeRequest should return original request`() = runTest {
        val originalRequest = Request(
            messageId = "test-request-2",
            method = RequestMethod.VM_START,
            sourceId = "host",
            targetId = "vm",
            params = RequestParams(params = mapOf("key" to "value"))
        )

        val encodeResult = messageCodec.encodeRequest(originalRequest)
        assertTrue(encodeResult.isSuccess)

        val decodeResult = messageCodec.decodeRequest(encodeResult.getOrThrow())
        assertTrue(decodeResult.isSuccess)

        val decodedRequest = decodeResult.getOrThrow()
        assertEquals(originalRequest.header.messageId, decodedRequest.header.messageId)
        assertEquals(originalRequest.body.method, decodedRequest.body.method)
        assertEquals(originalRequest.header.sourceId, decodedRequest.header.sourceId)
        assertEquals(originalRequest.header.targetId, decodedRequest.header.targetId)
    }

    @Test
    fun `encodeNotification should return valid byte array`() = runTest {
        val notification = Notification(
            messageId = "test-notif-1",
            eventType = "vm.state.changed",
            sourceId = "vm",
            targetId = "host",
            eventData = mapOf("state" to "RUNNING")
        )

        val result = messageCodec.encodeNotification(notification)

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `decodeNotification should return original notification`() = runTest {
        val originalNotification = Notification(
            messageId = "test-notif-2",
            eventType = "vm.error",
            sourceId = "vm",
            targetId = "host",
            eventData = mapOf("error" to "timeout"),
            severity = NotificationSeverity.ERROR
        )

        val encodeResult = messageCodec.encodeNotification(originalNotification)
        assertTrue(encodeResult.isSuccess)

        val decodeResult = messageCodec.decodeNotification(encodeResult.getOrThrow())
        assertTrue(decodeResult.isSuccess)

        val decodedNotification = decodeResult.getOrThrow()
        assertEquals(originalNotification.header.messageId, decodedNotification.header.messageId)
        assertEquals(originalNotification.body.eventType, decodedNotification.body.eventType)
        assertEquals(originalNotification.body.severity, decodedNotification.body.severity)
    }

    @Test
    fun `encodeHeartbeat should return valid byte array`() = runTest {
        val heartbeat = Heartbeat(
            messageId = "test-hb-1",
            sourceId = "vm",
            targetId = "host",
            sequence = 1L,
            status = HeartbeatStatus.ALIVE
        )

        val result = messageCodec.encodeHeartbeat(heartbeat)

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `decodeHeartbeat should return original heartbeat`() = runTest {
        val originalHeartbeat = Heartbeat(
            messageId = "test-hb-2",
            sourceId = "vm",
            targetId = "host",
            sequence = 42L,
            status = HeartbeatStatus.BUSY
        )

        val encodeResult = messageCodec.encodeHeartbeat(originalHeartbeat)
        assertTrue(encodeResult.isSuccess)

        val decodeResult = messageCodec.decodeHeartbeat(encodeResult.getOrThrow())
        assertTrue(decodeResult.isSuccess)

        val decodedHeartbeat = decodeResult.getOrThrow()
        assertEquals(originalHeartbeat.header.messageId, decodedHeartbeat.header.messageId)
        assertEquals(originalHeartbeat.body.sequence, decodedHeartbeat.body.sequence)
        assertEquals(originalHeartbeat.body.status, decodedHeartbeat.body.status)
    }

    @Test
    fun `calculateChecksum should return consistent hash`() {
        val data = "test data".toByteArray()

        val checksum1 = messageCodec.calculateChecksum(data)
        val checksum2 = messageCodec.calculateChecksum(data)

        assertEquals(checksum1, checksum2)
        assertTrue(checksum1.isNotEmpty())
    }

    @Test
    fun `verifyChecksum should return true for valid checksum`() {
        val data = "test data".toByteArray()
        val checksum = messageCodec.calculateChecksum(data)

        assertTrue(messageCodec.verifyChecksum(data, checksum))
    }

    @Test
    fun `verifyChecksum should return false for invalid checksum`() {
        val data = "test data".toByteArray()
        val invalidChecksum = "invalid_checksum"

        assertFalse(messageCodec.verifyChecksum(data, invalidChecksum))
    }

    @Test
    fun `decode should handle different message types`() = runTest {
        // 测试请求消息
        val request = Request(
            messageId = "test-req",
            method = RequestMethod.VM_STATUS,
            sourceId = "host",
            targetId = "vm"
        )
        val requestResult = messageCodec.encodeRequest(request)
        assertTrue(requestResult.isSuccess)
        val decodedRequest = messageCodec.decode(requestResult.getOrThrow())
        assertTrue(decodedRequest.isSuccess)
        assertTrue(decodedRequest.getOrThrow() is MessageWrapper.RequestWrapper)

        // 测试心跳消息
        val heartbeat = Heartbeat(
            messageId = "test-hb",
            sourceId = "vm",
            targetId = "host",
            sequence = 1L
        )
        val heartbeatResult = messageCodec.encodeHeartbeat(heartbeat)
        assertTrue(heartbeatResult.isSuccess)
        val decodedHeartbeat = messageCodec.decode(heartbeatResult.getOrThrow())
        assertTrue(decodedHeartbeat.isSuccess)
        assertTrue(decodedHeartbeat.getOrThrow() is MessageWrapper.HeartbeatWrapper)
    }
}