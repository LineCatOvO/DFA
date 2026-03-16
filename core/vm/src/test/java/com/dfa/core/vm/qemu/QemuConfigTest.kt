package com.dfa.core.vm.qemu

import com.google.truth.Truth.assertThat
import org.junit.Test

/**
 * QemuConfig单元测试
 *
 * 测试QEMU虚拟机配置的默认值、Builder模式、配置验证和序列化功能
 */
class QemuConfigTest {

    // ==================== 默认值验证测试 ====================

    @Test
    fun `default config should have correct default values`() {
        val config = QemuConfig.default("test-vm", "Test VM")

        assertThat(config.id).isEqualTo("test-vm")
        assertThat(config.name).isEqualTo("Test VM")
        assertThat(config.targetArch).isEqualTo(QemuTargetArch.X86_64)
        assertThat(config.accelerator).isEqualTo(QemuAccelerator.TCG)
        assertThat(config.machineType).isEqualTo(QemuMachineType.PC)
        assertThat(config.cpuModel).isEqualTo(QemuCpuModel.QEMU64)
        assertThat(config.memoryMb).isEqualTo(2048)
        assertThat(config.cpuCores).isEqualTo(2)
        assertThat(config.enableKvm).isFalse()
        assertThat(config.enableUsb).isTrue()
        assertThat(config.enableGpu).isFalse()
    }

    @Test
    fun `QemuDiskConfig should have correct default values`() {
        val diskConfig = QemuDiskConfig(path = "/tmp/disk.qcow2")

        assertThat(diskConfig.path).isEqualTo("/tmp/disk.qcow2")
        assertThat(diskConfig.format).isEqualTo(QemuDiskFormat.QCOW2)
        assertThat(diskConfig.sizeGb).isEqualTo(10)
        assertThat(diskConfig.`interface`).isEqualTo("virtio")
        assertThat(diskConfig.cacheMode).isEqualTo("writeback")
        assertThat(diskConfig.readOnly).isFalse()
        assertThat(diskConfig.discard).isFalse()
        assertThat(diskConfig.bootIndex).isNull()
    }

    @Test
    fun `QemuNetworkConfig should have correct default values`() {
        val networkConfig = QemuNetworkConfig()

        assertThat(networkConfig.mode).isEqualTo(QemuNetworkMode.USER)
        assertThat(networkConfig.device).isEqualTo("virtio-net-pci")
        assertThat(networkConfig.macAddress).isNull()
        assertThat(networkConfig.portForwards).isEmpty()
        assertThat(networkConfig.enableDhcp).isTrue()
    }

    @Test
    fun `QemuDisplayConfig should have correct default values`() {
        val displayConfig = QemuDisplayConfig()

        assertThat(displayConfig.type).isEqualTo(QemuDisplayType.NONE)
        assertThat(displayConfig.vncDisplay).isEqualTo(0)
        assertThat(displayConfig.enableGl).isFalse()
    }

    @Test
    fun `QemuSerialConfig should have correct default values`() {
        val serialConfig = QemuSerialConfig()

        assertThat(serialConfig.enabled).isTrue()
        assertThat(serialConfig.mode).isEqualTo(QemuSerialMode.Stdio)
        assertThat(serialConfig.numPorts).isEqualTo(1)
    }

    @Test
    fun `QemuAudioConfig should have correct default values`() {
        val audioConfig = QemuAudioConfig()

        assertThat(audioConfig.enabled).isFalse()
        assertThat(audioConfig.backend).isEqualTo(QemuAudioBackend.NONE)
        assertThat(audioConfig.deviceId).isEqualTo("intel-hda")
    }

    // ==================== Builder模式测试 ====================

    @Test
    fun `Builder should create config with all properties`() {
        val config = QemuConfig.Builder()
            .id("custom-vm")
            .name("Custom VM")
            .targetArch(QemuTargetArch.AARCH64)
            .accelerator(QemuAccelerator.KVM)
            .machineType(QemuMachineType.Q35)
            .cpuModel(QemuCpuModel.HOST)
            .memoryMb(4096)
            .cpuCores(4)
            .enableKvm(true)
            .enableUsb(false)
            .enableGpu(true)
            .build()

        assertThat(config.id).isEqualTo("custom-vm")
        assertThat(config.name).isEqualTo("Custom VM")
        assertThat(config.targetArch).isEqualTo(QemuTargetArch.AARCH64)
        assertThat(config.accelerator).isEqualTo(QemuAccelerator.KVM)
        assertThat(config.machineType).isEqualTo(QemuMachineType.Q35)
        assertThat(config.cpuModel).isEqualTo(QemuCpuModel.HOST)
        assertThat(config.memoryMb).isEqualTo(4096)
        assertThat(config.cpuCores).isEqualTo(4)
        assertThat(config.enableKvm).isTrue()
        assertThat(config.enableUsb).isFalse()
        assertThat(config.enableGpu).isTrue()
    }

    @Test
    fun `Builder should add disks correctly`() {
        val disk1 = QemuDiskConfig(path = "/disk1.qcow2", sizeGb = 20)
        val disk2 = QemuDiskConfig(path = "/disk2.qcow2", sizeGb = 40)

        val config = QemuConfig.Builder()
            .id("vm-with-disks")
            .name("VM with Disks")
            .addDisk(disk1)
            .addDisk(disk2)
            .build()

        assertThat(config.disks).hasSize(2)
        assertThat(config.disks[0].path).isEqualTo("/disk1.qcow2")
        assertThat(config.disks[1].path).isEqualTo("/disk2.qcow2")
    }

    @Test
    fun `Builder should set disks list correctly`() {
        val disks = listOf(
            QemuDiskConfig(path = "/disk1.qcow2"),
            QemuDiskConfig(path = "/disk2.qcow2")
        )

        val config = QemuConfig.Builder()
            .id("vm-with-disks")
            .name("VM with Disks")
            .disks(disks)
            .build()

        assertThat(config.disks).hasSize(2)
    }

    @Test
    fun `Builder should set network config correctly`() {
        val networkConfig = QemuNetworkConfig(
            mode = QemuNetworkMode.BRIDGE,
            bridgeName = "br0"
        )

        val config = QemuConfig.Builder()
            .id("vm-network")
            .name("VM Network")
            .network(networkConfig)
            .build()

        assertThat(config.network.mode).isEqualTo(QemuNetworkMode.BRIDGE)
        assertThat(config.network.bridgeName).isEqualTo("br0")
    }

    @Test
    fun `Builder should set display config correctly`() {
        val displayConfig = QemuDisplayConfig(
            type = QemuDisplayType.VNC,
            vncDisplay = 1,
            vncPassword = "secret"
        )

        val config = QemuConfig.Builder()
            .id("vm-display")
            .name("VM Display")
            .display(displayConfig)
            .build()

        assertThat(config.display.type).isEqualTo(QemuDisplayType.VNC)
        assertThat(config.display.vncDisplay).isEqualTo(1)
        assertThat(config.display.vncPassword).isEqualTo("secret")
    }

    @Test
    fun `Builder should add extra args correctly`() {
        val config = QemuConfig.Builder()
            .id("vm-extra")
            .name("VM Extra")
            .addExtraArg("-device")
            .addExtraArg("virtio-balloon")
            .build()

        assertThat(config.extraArgs).containsExactly("-device", "virtio-balloon")
    }

    @Test(expected = IllegalStateException::class)
    fun `Builder should throw when id is empty`() {
        QemuConfig.Builder()
            .name("Test VM")
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun `Builder should throw when name is empty`() {
        QemuConfig.Builder()
            .id("test-vm")
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun `Builder should throw when memory is not positive`() {
        QemuConfig.Builder()
            .id("test-vm")
            .name("Test VM")
            .memoryMb(0)
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun `Builder should throw when cpuCores is not positive`() {
        QemuConfig.Builder()
            .id("test-vm")
            .name("Test VM")
            .cpuCores(0)
            .build()
    }

    @Test
    fun `toBuilder should create equivalent config`() {
        val original = QemuConfig.Builder()
            .id("original-vm")
            .name("Original VM")
            .memoryMb(8192)
            .cpuCores(8)
            .enableKvm(true)
            .build()

        val rebuilt = original.toBuilder().build()

        assertThat(rebuilt.id).isEqualTo(original.id)
        assertThat(rebuilt.name).isEqualTo(original.name)
        assertThat(rebuilt.memoryMb).isEqualTo(original.memoryMb)
        assertThat(rebuilt.cpuCores).isEqualTo(original.cpuCores)
        assertThat(rebuilt.enableKvm).isEqualTo(original.enableKvm)
    }

    @Test
    fun `qemuConfig extension function should work correctly`() {
        val config = qemuConfig {
            id("extension-vm")
            name("Extension VM")
            memoryMb(4096)
        }

        assertThat(config.id).isEqualTo("extension-vm")
        assertThat(config.name).isEqualTo("Extension VM")
        assertThat(config.memoryMb).isEqualTo(4096)
    }

    // ==================== 配置验证测试 ====================

    @Test
    fun `validate should return true for valid config`() {
        val config = QemuConfig.default("valid-vm", "Valid VM")
        assertThat(config.validate()).isTrue()
    }

    @Test
    fun `validate should return false when id is empty`() {
        val config = QemuConfig(
            id = "",
            name = "Test VM"
        )
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `validate should return false when name is empty`() {
        val config = QemuConfig(
            id = "test-vm",
            name = ""
        )
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `validate should return false when memory is not positive`() {
        val config = QemuConfig(
            id = "test-vm",
            name = "Test VM",
            memoryMb = 0
        )
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `validate should return false when cpuCores is not positive`() {
        val config = QemuConfig(
            id = "test-vm",
            name = "Test VM",
            cpuCores = 0
        )
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `validate should return false when disk config is invalid`() {
        val invalidDisk = QemuDiskConfig(path = "") // Empty path
        val config = QemuConfig(
            id = "test-vm",
            name = "Test VM",
            disks = listOf(invalidDisk)
        )
        assertThat(config.validate()).isFalse()
    }

    @Test
    fun `QemuDiskConfig validate should return true for valid config`() {
        val diskConfig = QemuDiskConfig(path = "/tmp/disk.qcow2", sizeGb = 10)
        assertThat(diskConfig.validate()).isTrue()
    }

    @Test
    fun `QemuDiskConfig validate should return false when path is empty`() {
        val diskConfig = QemuDiskConfig(path = "")
        assertThat(diskConfig.validate()).isFalse()
    }

    @Test
    fun `QemuDiskConfig validate should return false when sizeGb is not positive`() {
        val diskConfig = QemuDiskConfig(path = "/tmp/disk.qcow2", sizeGb = 0)
        assertThat(diskConfig.validate()).isFalse()
    }

    @Test
    fun `QemuPortForward validate should return true for valid config`() {
        val portForward = QemuPortForward(
            protocol = "tcp",
            hostPort = 8080,
            guestPort = 80
        )
        assertThat(portForward.validate()).isTrue()
    }

    @Test
    fun `QemuPortForward validate should return false for invalid port`() {
        val portForward = QemuPortForward(
            protocol = "tcp",
            hostPort = 0,
            guestPort = 80
        )
        assertThat(portForward.validate()).isFalse()
    }

    @Test
    fun `QemuPortForward validate should return false for invalid protocol`() {
        val portForward = QemuPortForward(
            protocol = "invalid",
            hostPort = 8080,
            guestPort = 80
        )
        assertThat(portForward.validate()).isFalse()
    }

    @Test
    fun `QemuNetworkConfig validate should return false when bridge mode without bridge name`() {
        val networkConfig = QemuNetworkConfig(
            mode = QemuNetworkMode.BRIDGE,
            bridgeName = null
        )
        assertThat(networkConfig.validate()).isFalse()
    }

    @Test
    fun `QemuNetworkConfig validate should return false when tap mode without tap device`() {
        val networkConfig = QemuNetworkConfig(
            mode = QemuNetworkMode.TAP,
            tapDevice = null
        )
        assertThat(networkConfig.validate()).isFalse()
    }

    @Test
    fun `QemuDisplayConfig validate should return true for VNC with valid display`() {
        val displayConfig = QemuDisplayConfig(
            type = QemuDisplayType.VNC,
            vncDisplay = 0
        )
        assertThat(displayConfig.validate()).isTrue()
    }

    @Test
    fun `QemuDisplayConfig validate should return true for SPICE with valid port`() {
        val displayConfig = QemuDisplayConfig(
            type = QemuDisplayType.SPICE,
            spicePort = 5900
        )
        assertThat(displayConfig.validate()).isTrue()
    }

    @Test
    fun `QemuSerialConfig validate should return true for valid numPorts`() {
        val serialConfig = QemuSerialConfig(numPorts = 2)
        assertThat(serialConfig.validate()).isTrue()
    }

    @Test
    fun `QemuSerialConfig validate should return false for invalid numPorts`() {
        val serialConfig = QemuSerialConfig(numPorts = 0)
        assertThat(serialConfig.validate()).isFalse()
    }

    // ==================== QEMU命令行参数测试 ====================

    @Test
    fun `getQemuExecutable should return correct command for architecture`() {
        val config = QemuConfig.default("test", "Test")

        assertThat(config.getQemuExecutable()).isEqualTo("qemu-system-x86_64")

        val armConfig = config.copy(targetArch = QemuTargetArch.AARCH64)
        assertThat(armConfig.getQemuExecutable()).isEqualTo("qemu-system-aarch64")
    }

    @Test
    fun `QemuDiskConfig toQemuArgs should generate correct arguments`() {
        val diskConfig = QemuDiskConfig(
            path = "/tmp/disk.qcow2",
            format = QemuDiskFormat.QCOW2,
            cacheMode = "writeback"
        )

        val args = diskConfig.toQemuArgs()

        assertThat(args).contains("-drive")
        assertThat(args.any { it.contains("file=/tmp/disk.qcow2") }).isTrue()
        assertThat(args.any { it.contains("format=qcow2") }).isTrue()
        assertThat(args.any { it.contains("cache=writeback") }).isTrue()
    }

    @Test
    fun `QemuDiskConfig toQemuArgs should include readonly when enabled`() {
        val diskConfig = QemuDiskConfig(
            path = "/tmp/disk.qcow2",
            readOnly = true
        )

        val args = diskConfig.toQemuArgs()

        assertThat(args.any { it.contains("readonly=on") }).isTrue()
    }

    @Test
    fun `QemuPortForward toQemuArg should generate correct argument`() {
        val portForward = QemuPortForward(
            protocol = "tcp",
            hostPort = 8080,
            guestPort = 80,
            hostAddress = "0.0.0.0"
        )

        val arg = portForward.toQemuArg()

        assertThat(arg).isEqualTo("hostfwd=tcp:0.0.0.0:8080-:80")
    }

    @Test
    fun `QemuNetworkConfig toQemuArgs should generate user mode arguments`() {
        val networkConfig = QemuNetworkConfig(
            mode = QemuNetworkMode.USER,
            portForwards = listOf(
                QemuPortForward(hostPort = 8080, guestPort = 80)
            )
        )

        val args = networkConfig.toQemuArgs()

        assertThat(args).contains("-netdev")
        assertThat(args.any { it.contains("user") }).isTrue()
        assertThat(args.any { it.contains("hostfwd=tcp:0.0.0.0:8080-:80") }).isTrue()
    }

    @Test
    fun `QemuNetworkConfig toQemuArgs should generate none mode arguments`() {
        val networkConfig = QemuNetworkConfig(mode = QemuNetworkMode.NONE)

        val args = networkConfig.toQemuArgs()

        assertThat(args).contains("-net")
        assertThat(args).contains("none")
    }

    @Test
    fun `QemuDisplayConfig toQemuArgs should generate VNC arguments`() {
        val displayConfig = QemuDisplayConfig(
            type = QemuDisplayType.VNC,
            vncDisplay = 0
        )

        val args = displayConfig.toQemuArgs()

        assertThat(args).contains("-vnc")
        assertThat(args).contains(":0")
    }

    @Test
    fun `QemuDisplayConfig toQemuArgs should generate none display arguments`() {
        val displayConfig = QemuDisplayConfig(type = QemuDisplayType.NONE)

        val args = displayConfig.toQemuArgs()

        assertThat(args).contains("-display")
        assertThat(args).contains("none")
    }

    @Test
    fun `QemuSerialConfig toQemuArgs should generate serial arguments when enabled`() {
        val serialConfig = QemuSerialConfig(
            enabled = true,
            mode = QemuSerialMode.Stdio,
            numPorts = 1
        )

        val args = serialConfig.toQemuArgs()

        assertThat(args).contains("-serial")
        assertThat(args).contains("stdio")
    }

    @Test
    fun `QemuSerialConfig toQemuArgs should return empty when disabled`() {
        val serialConfig = QemuSerialConfig(enabled = false)

        val args = serialConfig.toQemuArgs()

        assertThat(args).isEmpty()
    }

    @Test
    fun `QemuAudioConfig toQemuArgs should generate none audio when disabled`() {
        val audioConfig = QemuAudioConfig(enabled = false)

        val args = audioConfig.toQemuArgs()

        assertThat(args).contains("-audiodev")
        assertThat(args).contains("none,id=audio0")
    }
}