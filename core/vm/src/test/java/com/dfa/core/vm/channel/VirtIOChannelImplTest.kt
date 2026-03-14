package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType
import com.dfa.core.vm.communication.CommunicationState
import com.google.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * VirtIOChannelImpl 单元测试
 */
class VirtIOChannelImplTest {

    private lateinit var channel: VirtIOChannelImpl

    @Before
    fun setup() {
        channel = VirtIOChannelImpl()
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
    fun `channelType should be VIRTIO_SERIAL`() {
        assertThat(channel.channelType).isEqualTo(ChannelType.VIRTIO_SERIAL)
    }

    @Test
    fun `initial isConnected should be false`() {
        assertThat(channel.isConnected()).isFalse()
    }

    // ==================== Device Path Tests ====================

    @Test
    fun `devicePath should be empty initially`() {
        assertThat(channel.devicePath).isEmpty()
    }

    // ==================== Connect Tests ====================

    @Test
    fun `connect should fail with wrong channel type`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VSOCK,
            port = 1234
        )

        val result = channel.connect(config)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `connect should fail with null path`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = null
        )

        val result = channel.connect(config)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `connect should fail with non-existent device path`() = runTest {
        val config = ChannelConfig(
            type = ChannelType.VIRTIO_SERIAL,
            path = "/dev/nonexistent"
        )

        val result = channel.connect(config)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("not found")
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
        assertThat(info.type).isEqualTo(ChannelType.VIRTIO_SERIAL)
        assertThat(info.state).isEqualTo(CommunicationState.DISCONNECTED)
    }

    // ==================== Serial Status Tests ====================

    @Test
    fun `getSerialStatus should return default values when not connected`() = runTest {
        val status = channel.getSerialStatus()

        assertThat(status.isOpen).isFalse()
        assertThat(status.baudRate).isEqualTo(SerialPortStatus.DEFAULT_BAUD_RATE)
    }

    // ==================== Set Baud Rate Tests ====================

    @Test
    fun `setBaudRate should succeed`() = runTest {
        val result = channel.setBaudRate(115200)

        assertThat(result.isSuccess).isTrue()
    }

    // ==================== Flush Tests ====================

    @Test
    fun `flush should fail when not connected`() = runTest {
        val result = channel.flush()

        assertThat(result.isFailure).isTrue()
    }

    // ==================== Release Tests ====================

    @Test
    fun `release should reset state`() = runTest {
        channel.release()

        assertThat(channel.state.value).isEqualTo(CommunicationState.DISCONNECTED)
        assertThat(channel.isConnected()).isFalse()
    }

    // ==================== Supports Interrupt Tests ====================

    @Test
    fun `supportsInterrupt should be false initially`() {
        assertThat(channel.supportsInterrupt).isFalse()
    }

    // ==================== Receive Data Tests ====================

    @Test
    fun `receiveData should be available`() {
        val flow = channel.receiveData
        assertThat(flow).isNotNull()
    }

    // ==================== VirtIOChannelConfig Tests ====================

    @Test
    fun `VirtIOChannelConfig should have default values`() {
        val config = VirtIOChannelConfig(path = "/dev/test")

        assertThat(config.path).isEqualTo("/dev/test")
        assertThat(config.type).isEqualTo(ChannelType.VIRTIO_SERIAL)
        assertThat(config.baudRate).isEqualTo(SerialPortStatus.DEFAULT_BAUD_RATE)
        assertThat(config.timeoutMs).isEqualTo(ChannelConfig.DEFAULT_TIMEOUT)
    }

    @Test
    fun `VirtIOChannelConfig validate should return true for valid config`() {
        val config = VirtIOChannelConfig(path = "/dev/test")

        assertThat(config.validate()).isTrue()
    }

    @Test
    fun `VirtIOChannelConfig validate should return false for empty path`() {
        val config = VirtIOChannelConfig(path = "")

        assertThat(config.validate()).isFalse()
    }

    // ==================== SerialPortStatus Tests ====================

    @Test
    fun `SerialPortStatus should have correct defaults`() {
        val status = SerialPortStatus(
            isOpen = false,
            baudRate = 115200,
            dataBits = 8,
            stopBits = 1,
            parity = Parity.NONE,
            flowControl = FlowControl.NONE
        )

        assertThat(status.isOpen).isFalse()
        assertThat(status.baudRate).isEqualTo(115200)
        assertThat(status.dataBits).isEqualTo(8)
        assertThat(status.stopBits).isEqualTo(1)
        assertThat(status.parity).isEqualTo(Parity.NONE)
        assertThat(status.flowControl).isEqualTo(FlowControl.NONE)
    }

    // ==================== Parity Tests ====================

    @Test
    fun `Parity should contain all expected values`() {
        val expectedValues = listOf(
            Parity.NONE,
            Parity.ODD,
            Parity.EVEN,
            Parity.MARK,
            Parity.SPACE
        )

        assertThat(Parity.entries.size).isEqualTo(expectedValues.size)
        expectedValues.forEach { parity ->
            assertThat(Parity.entries.contains(parity)).isTrue()
        }
    }

    // ==================== FlowControl Tests ====================

    @Test
    fun `FlowControl should contain all expected values`() {
        val expectedValues = listOf(
            FlowControl.NONE,
            FlowControl.RTS_CTS,
            FlowControl.XON_XOFF
        )

        assertThat(FlowControl.entries.size).isEqualTo(expectedValues.size)
        expectedValues.forEach { flowControl ->
            assertThat(FlowControl.entries.contains(flowControl)).isTrue()
        }
    }
}