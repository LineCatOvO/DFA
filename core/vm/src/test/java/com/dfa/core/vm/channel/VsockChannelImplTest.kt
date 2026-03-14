package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationState
import com.google.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * VsockChannelImpl 单元测试
 */
class VsockChannelImplTest {

    private lateinit var channel: VsockChannelImpl

    @Before
    fun setup() {
        channel = VsockChannelImpl()
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `initial state should be DISCONNECTED`() {
        assertThat(channel.state.value).isEqualTo(CommunicationState.DISCONNECTED)
    }

    @Test
    fun `channelId should be non-empty`() {
        assertThat(channel.channelId).isNotEmpty()
    }

    @Test
    fun `channelType should be VSOCK`() {
        assertThat(channel.channelType).isEqualTo(ChannelType.VSOCK)
    }

    @Test
    fun `initial isConnected should be false`() {
        assertThat(channel.isConnected()).isFalse()
    }

    // ==================== Vsock Properties Tests ====================

    @Test
    fun `vsockPort should be 0 initially`() {
        assertThat(channel.vsockPort).isEqualTo(0)
    }

    @Test
    fun `hostCid should have default value`() {
        assertThat(channel.hostCid).isEqualTo(VsockChannelConfig.VMADDR_CID_HOST)
    }

    @Test
    fun `clientCid should have default value`() {
        assertThat(channel.clientCid).isEqualTo(VsockChannelConfig.VMADDR_CID_ANY)
    }

    // ==================== Vsock Address Tests ====================

    @Test
    fun `getVsockAddress should return correct format`() {
        val address = channel.getVsockAddress()

        assertThat(address).startsWith("vsock://")
        assertThat(address).contains(":")
    }

    // ==================== Connect Tests ====================

    @Test
    fun `connect should fail with wrong channel type`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = "/dev/test"
        )

        val result = channel.connect(config)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `connect should fail with invalid port`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VSOCK,
            port = 0 // Invalid port
        )

        val result = channel.connect(config)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `connect should fail with negative port`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VSOCK,
            port = -1
        )

        val result = channel.connect(config)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Send Tests ====================

    @Test
    fun `send should fail when not connected`() = runTest {
        val result = channel.send(byteArrayOf(1, 2, 3))

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `send with timeout should fail when not connected`() = runTest {
        val result = channel.send(byteArrayOf(1, 2, 3), 1000)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Disconnect Tests ====================

    @Test
    fun `disconnect should succeed when not connected`() = runTest {
        val result = channel.disconnect()

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Connection Info Tests ====================

    @Test
    fun `getConnectionInfo should return valid info`() {
        val info = channel.getConnectionInfo()

        assertThat(info.channelId).isEqualTo(channel.channelId)
        assertThat(info.type).isEqualTo(ChannelType.VSOCK)
        assertThat(info.state).isEqualTo(CommunicationState.DISCONNECTED)
    }

    // ==================== Set Timeout Tests ====================

    @Test
    fun `setTimeout should fail when not connected`() = runTest {
        val result = channel.setTimeout(5000)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Socket Options Tests ====================

    @Test
    fun `getSocketOptions should return default options`() = runTest {
        val options = channel.getSocketOptions()

        assertThat(options.sendBufferSize).isGreaterThan(0)
        assertThat(options.receiveBufferSize).isGreaterThan(0)
    }

    @Test
    fun `setSocketOptions should fail when not connected`() = runTest {
        val options = VsockSocketOptions(
            sendBufferSize = 8192,
            receiveBufferSize = 8192
        )

        val result = channel.setSocketOptions(options)

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Release Tests ====================

    @Test
    fun `release should reset state`() = runTest {
        channel.release()

        assertThat(channel.state.value).isEqualTo(CommunicationState.DISCONNECTED)
        assertThat(channel.isConnected()).isFalse()
    }

    // ==================== Receive Data Tests ====================

    @Test
    fun `receiveData should be available`() {
        val flow = channel.receiveData
        assertThat(flow).isNotNull()
    }

    // ==================== VsockChannelConfig Tests ====================

    @Test
    fun `VsockChannelConfig should have default values`() {
        val config = VsockChannelConfig(port = 1234)

        assertThat(config.port).isEqualTo(1234)
        assertThat(config.type).isEqualTo(ChannelType.VSOCK)
        assertThat(config.hostCid).isEqualTo(VsockChannelConfig.VMADDR_CID_HOST)
        assertThat(config.clientCid).isEqualTo(VsockChannelConfig.VMADDR_CID_ANY)
        assertThat(config.timeoutMs).isEqualTo(ChannelConfig.DEFAULT_TIMEOUT)
    }

    @Test
    fun `VsockChannelConfig validate should return true for valid config`() {
        val config = VsockChannelConfig(port = 1234)

        assertThat(config.validate()).isTrue()
    }

    @Test
    fun `VsockChannelConfig validate should return false for invalid port`() {
        val config = VsockChannelConfig(port = 0)

        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `VsockChannelConfig constants should be correct`() {
        assertThat(VsockChannelConfig.VMADDR_CID_ANY).isEqualTo(-1)
        assertThat(VsockChannelConfig.VMADDR_CID_HOST).isEqualTo(2)
        assertThat(VsockChannelConfig.VMADDR_CID_HYPERVISOR).isEqualTo(0)
        assertThat(VsockChannelConfig.VMADDR_CID_LOCAL).isEqualTo(1)
    }

    // ==================== VsockSocketOptions Tests ====================

    @Test
    fun `VsockSocketOptions should have correct defaults`() {
        val options = VsockSocketOptions()

        assertThat(options.sendBufferSize).isGreaterThan(0)
        assertThat(options.receiveBufferSize).isGreaterThan(0)
        assertThat(options.keepAlive).isTrue()
        assertThat(options.tcpNoDelay).isTrue()
        assertThat(options.soTimeout).isGreaterThan(0)
    }

    @Test
    fun `VsockSocketOptions copy should work correctly`() {
        val original = VsockSocketOptions()
        val copied = original.copy(sendBufferSize = 16384)

        assertThat(copied.sendBufferSize).isEqualTo(16384)
        assertThat(original.sendBufferSize).isNotEqualTo(16384)
    }
}