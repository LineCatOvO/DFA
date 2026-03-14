package com.dfa.core.vm.protocol

import org.junit.Assert.*
import org.junit.Test

/**
 * Message模型测试
 */
class MessageTest {

    @Test
    fun `MessageHeader validates correctly`() {
        val validHeader = MessageHeader(
            messageId = "test-msg-1",
            type = MessageType.REQUEST,
            sourceId = "host",
            targetId = "vm"
        )
        assertTrue(validHeader.validate())

        val invalidHeader = MessageHeader(
            messageId = "",
            type = MessageType.REQUEST,
            sourceId = "",
            targetId = "vm"
        )
        assertFalse(invalidHeader.validate())
    }

    @Test
    fun `MessageHeader isExpired returns correct value`() {
        val notExpiredHeader = MessageHeader(
            messageId = "test",
            type = MessageType.REQUEST,
            sourceId = "host",
            targetId = "vm",
            timestamp = System.currentTimeMillis(),
            ttl = 60000L
        )
        assertFalse(notExpiredHeader.isExpired())

        val expiredHeader = MessageHeader(
            messageId = "test",
            type = MessageType.REQUEST,
            sourceId = "host",
            targetId = "vm",
            timestamp = System.currentTimeMillis() - 120000,
            ttl = 60000L
        )
        assertTrue(expiredHeader.isExpired())
    }

    @Test
    fun `Request validates correctly`() {
        val validRequest = Request(
            messageId = "test-req-1",
            method = RequestMethod.VM_STATUS,
            sourceId = "host",
            targetId = "vm"
        )
        assertTrue(validRequest.validate())
    }

    @Test
    fun `Request withRetry increments retry count`() {
        val request = Request(
            messageId = "test-req-2",
            method = RequestMethod.VM_START,
            sourceId = "host",
            targetId = "vm"
        )
        assertEquals(0, request.body.retryCount)

        val retriedRequest = request.body.withRetry()
        assertEquals(1, retriedRequest.retryCount)
    }

    @Test
    fun `Response isSuccess returns correct value`() {
        val successResponse = ResponseBody<String>(
            code = ResponseCode.SUCCESS,
            data = "result"
        )
        assertTrue(successResponse.isSuccess)

        val errorResponse = ResponseBody<String>(
            code = ResponseCode.INTERNAL_ERROR,
            message = "Error"
        )
        assertFalse(errorResponse.isSuccess)
    }

    @Test
    fun `Response factory methods work correctly`() {
        val successResponse = ResponseBody.success("data", "Success")
        assertEquals(ResponseCode.SUCCESS, successResponse.code)
        assertEquals("data", successResponse.data)
        assertEquals("Success", successResponse.message)

        val errorResponse = ResponseBody.error<String>(
            ResponseCode.NOT_FOUND,
            "Not found"
        )
        assertEquals(ResponseCode.NOT_FOUND, errorResponse.code)
        assertEquals("Not found", errorResponse.message)
    }

    @Test
    fun `Notification validates correctly`() {
        val validNotification = Notification(
            messageId = "test-notif-1",
            eventType = "vm.state.changed",
            sourceId = "vm",
            targetId = "host"
        )
        assertTrue(validNotification.validate())

        val invalidNotification = Notification(
            messageId = "test-notif-2",
            eventType = "",
            sourceId = "vm",
            targetId = "host"
        )
        assertFalse(invalidNotification.validate())
    }

    @Test
    fun `Heartbeat validates correctly`() {
        val validHeartbeat = Heartbeat(
            messageId = "test-hb-1",
            sourceId = "vm",
            targetId = "host",
            sequence = 1L
        )
        assertTrue(validHeartbeat.validate())

        val invalidHeartbeat = Heartbeat(
            messageId = "test-hb-2",
            sourceId = "vm",
            targetId = "host",
            sequence = -1L
        )
        assertFalse(invalidHeartbeat.validate())
    }

    @Test
    fun `FlowControlMessage validates correctly`() {
        val validMessage = FlowControlMessage(
            messageId = "test-fc-1",
            sourceId = "vm",
            targetId = "host",
            action = FlowControlAction.PAUSE
        )
        assertTrue(validMessage.validate())
    }

    @Test
    fun `MessageFlags default values are correct`() {
        val flags = MessageFlags()
        assertFalse(flags.compressed)
        assertFalse(flags.encrypted)
        assertFalse(flags.requiresAck)
        assertFalse(flags.isFragmented)
        assertEquals(0, flags.fragmentIndex)
        assertEquals(1, flags.totalFragments)
    }

    @Test
    fun `RequestMethod enum contains all expected values`() {
        val methods = RequestMethod.values()
        assertTrue(methods.contains(RequestMethod.VM_START))
        assertTrue(methods.contains(RequestMethod.VM_STOP))
        assertTrue(methods.contains(RequestMethod.VM_PAUSE))
        assertTrue(methods.contains(RequestMethod.VM_RESUME))
        assertTrue(methods.contains(RequestMethod.FILE_READ))
        assertTrue(methods.contains(RequestMethod.FILE_WRITE))
        assertTrue(methods.contains(RequestMethod.PROCESS_START))
        assertTrue(methods.contains(RequestMethod.SYSTEM_INFO))
    }

    @Test
    fun `ResponseCode enum contains all expected values`() {
        val codes = ResponseCode.values()
        assertTrue(codes.contains(ResponseCode.SUCCESS))
        assertTrue(codes.contains(ResponseCode.BAD_REQUEST))
        assertTrue(codes.contains(ResponseCode.INTERNAL_ERROR))
        assertTrue(codes.contains(ResponseCode.NETWORK_ERROR))
    }

    @Test
    fun `MessageType enum contains all expected values`() {
        val types = MessageType.values()
        assertTrue(types.contains(MessageType.REQUEST))
        assertTrue(types.contains(MessageType.RESPONSE))
        assertTrue(types.contains(MessageType.NOTIFICATION))
        assertTrue(types.contains(MessageType.HEARTBEAT))
        assertTrue(types.contains(MessageType.FILE_TRANSFER))
        assertTrue(types.contains(MessageType.FLOW_CONTROL))
    }
}