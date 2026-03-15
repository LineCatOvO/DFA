package com.dfa.core.vm.qemu

import com.google.truth.Truth.assertThat
import org.junit.Test

/**
 * QemuModels单元测试
 *
 * 测试QEMU模型类的枚举值、属性和方法
 */
class QemuModelsTest {

    // ==================== QemuAccelerator测试 ====================

    @Test
    fun `QemuAccelerator TCG should have correct properties`() {
        assertThat(QemuAccelerator.TCG.toQemuArg()).isEqualTo("tcg")
        assertThat(QemuAccelerator.TCG.requiredPlatform).isNull()
    }

    @Test
    fun `QemuAccelerator KVM should have correct properties`() {
        assertThat(QemuAccelerator.KVM.toQemuArg()).isEqualTo("kvm")
        assertThat(QemuAccelerator.KVM.requiredPlatform).isEqualTo("Linux")
    }

    @Test
    fun `QemuAccelerator HVF should have correct properties`() {
        assertThat(QemuAccelerator.HVF.toQemuArg()).isEqualTo("hvf")
        assertThat(QemuAccelerator.HVF.requiredPlatform).isEqualTo("macOS")
    }

    @Test
    fun `QemuAccelerator WHPX should have correct properties`() {
        assertThat(QemuAccelerator.WHPX.toQemuArg()).isEqualTo("whpx")
        assertThat(QemuAccelerator.WHPX.requiredPlatform).isEqualTo("Windows")
    }

    @Test
    fun `QemuAccelerator HAX should have correct properties`() {
        assertThat(QemuAccelerator.HAX.toQemuArg()).isEqualTo("hax")
        assertThat(QemuAccelerator.HAX.requiredPlatform).isEqualTo("Windows/macOS")
    }

    // ==================== QemuMachineType测试 ====================

    @Test
    fun `QemuMachineType PC should have correct properties`() {
        assertThat(QemuMachineType.PC.toQemuArg()).isEqualTo("pc")
        assertThat(QemuMachineType.PC.isX86).isTrue()
        assertThat(QemuMachineType.PC.isArm).isFalse()
    }

    @Test
    fun `QemuMachineType Q35 should have correct properties`() {
        assertThat(QemuMachineType.Q35.toQemuArg()).isEqualTo("q35")
        assertThat(QemuMachineType.Q35.isX86).isTrue()
    }

    @Test
    fun `QemuMachineType VIRT should have correct properties`() {
        assertThat(QemuMachineType.VIRT.toQemuArg()).isEqualTo("virt")
        assertThat(QemuMachineType.VIRT.isX86).isFalse()
        assertThat(QemuMachineType.VIRT.isArm).isTrue()
    }

    @Test
    fun `QemuMachineType RASPI3 should have correct properties`() {
        assertThat(QemuMachineType.RASPI3.toQemuArg()).isEqualTo("raspi3")
        assertThat(QemuMachineType.RASPI3.isArm).isTrue()
    }

    @Test
    fun `QemuMachineType RISCV_VIRT should have correct properties`() {
        assertThat(QemuMachineType.RISCV_VIRT.toQemuArg()).isEqualTo("virt")
        assertThat(QemuMachineType.RISCV_VIRT.isX86).isFalse()
        assertThat(QemuMachineType.RISCV_VIRT.isArm).isFalse()
    }

    // ==================== QemuCpuModel测试 ====================

    @Test
    fun `QemuCpuModel QEMU64 should have correct properties`() {
        assertThat(QemuCpuModel.QEMU64.toQemuArg()).isEqualTo("qemu64")
        assertThat(QemuCpuModel.QEMU64.isX86).isTrue()
        assertThat(QemuCpuModel.QEMU64.isArm).isFalse()
    }

    @Test
    fun `QemuCpuModel HOST should have correct properties`() {
        assertThat(QemuCpuModel.HOST.toQemuArg()).isEqualTo("host")
        assertThat(QemuCpuModel.HOST.isX86).isTrue()
    }

    @Test
    fun `QemuCpuModel CORTEX_A57 should have correct properties`() {
        assertThat(QemuCpuModel.CORTEX_A57.toQemuArg()).isEqualTo("cortex-a57")
        assertThat(QemuCpuModel.CORTEX_A57.isX86).isFalse()
        assertThat(QemuCpuModel.CORTEX_A57.isArm).isTrue()
    }

    @Test
    fun `QemuCpuModel CORTEX_A53 should have correct properties`() {
        assertThat(QemuCpuModel.CORTEX_A53.toQemuArg()).isEqualTo("cortex-a53")
        assertThat(QemuCpuModel.CORTEX_A53.isArm).isTrue()
    }

    @Test
    fun `QemuCpuModel RV64 should have correct properties`() {
        assertThat(QemuCpuModel.RV64.toQemuArg()).isEqualTo("rv64")
        assertThat(QemuCpuModel.RV64.isX86).isFalse()
        assertThat(QemuCpuModel.RV64.isArm).isFalse()
    }

    // ==================== QemuDiskFormat测试 ====================

    @Test
    fun `QemuDiskFormat QCOW2 should have correct properties`() {
        assertThat(QemuDiskFormat.QCOW2.toQemuArg()).isEqualTo("qcow2")
        assertThat(QemuDiskFormat.QCOW2.supportsSnapshots).isTrue()
        assertThat(QemuDiskFormat.QCOW2.supportsCompression).isTrue()
        assertThat(QemuDiskFormat.QCOW2.isSparse).isTrue()
    }

    @Test
    fun `QemuDiskFormat RAW should have correct properties`() {
        assertThat(QemuDiskFormat.RAW.toQemuArg()).isEqualTo("raw")
        assertThat(QemuDiskFormat.RAW.supportsSnapshots).isFalse()
        assertThat(QemuDiskFormat.RAW.supportsCompression).isFalse()
        assertThat(QemuDiskFormat.RAW.isSparse).isFalse()
    }

    @Test
    fun `QemuDiskFormat VDI should have correct properties`() {
        assertThat(QemuDiskFormat.VDI.toQemuArg()).isEqualTo("vdi")
        assertThat(QemuDiskFormat.VDI.isSparse).isTrue()
    }

    @Test
    fun `QemuDiskFormat VMDK should have correct properties`() {
        assertThat(QemuDiskFormat.VMDK.toQemuArg()).isEqualTo("vmdk")
        assertThat(QemuDiskFormat.VMDK.isSparse).isTrue()
    }

    @Test
    fun `QemuDiskFormat QCOW should have correct properties`() {
        assertThat(QemuDiskFormat.QCOW.toQemuArg()).isEqualTo("qcow")
        assertThat(QemuDiskFormat.QCOW.supportsSnapshots).isTrue()
        assertThat(QemuDiskFormat.QCOW.supportsCompression).isFalse()
    }

    // ==================== QemuNetworkMode测试 ====================

    @Test
    fun `QemuNetworkMode USER should have correct properties`() {
        assertThat(QemuNetworkMode.USER.toQemuArg()).isEqualTo("user")
        assertThat(QemuNetworkMode.USER.requiresRoot).isFalse()
        assertThat(QemuNetworkMode.USER.supportsPortForwarding).isTrue()
    }

    @Test
    fun `QemuNetworkMode BRIDGE should have correct properties`() {
        assertThat(QemuNetworkMode.BRIDGE.toQemuArg()).isEqualTo("bridge")
        assertThat(QemuNetworkMode.BRIDGE.requiresRoot).isTrue()
        assertThat(QemuNetworkMode.BRIDGE.supportsPortForwarding).isFalse()
    }

    @Test
    fun `QemuNetworkMode TAP should have correct properties`() {
        assertThat(QemuNetworkMode.TAP.toQemuArg()).isEqualTo("tap")
        assertThat(QemuNetworkMode.TAP.requiresRoot).isTrue()
        assertThat(QemuNetworkMode.TAP.supportsPortForwarding).isFalse()
    }

    @Test
    fun `QemuNetworkMode NONE should have correct properties`() {
        assertThat(QemuNetworkMode.NONE.toQemuArg()).isEqualTo("none")
        assertThat(QemuNetworkMode.NONE.requiresRoot).isFalse()
        assertThat(QemuNetworkMode.NONE.supportsPortForwarding).isFalse()
    }

    @Test
    fun `QemuNetworkMode SOCKET should have correct properties`() {
        assertThat(QemuNetworkMode.SOCKET.toQemuArg()).isEqualTo("socket")
        assertThat(QemuNetworkMode.SOCKET.requiresRoot).isFalse()
    }

    @Test
    fun `QemuNetworkMode VDE should have correct properties`() {
        assertThat(QemuNetworkMode.VDE.toQemuArg()).isEqualTo("vde")
        assertThat(QemuNetworkMode.VDE.requiresRoot).isFalse()
    }

    // ==================== QemuDisplayType测试 ====================

    @Test
    fun `QemuDisplayType NONE should have correct properties`() {
        assertThat(QemuDisplayType.NONE.toQemuArg()).isEqualTo("none")
        assertThat(QemuDisplayType.NONE.isRemote).isFalse()
        assertThat(QemuDisplayType.NONE.isLocal).isFalse()
        assertThat(QemuDisplayType.NONE.isHeadless).isTrue()
    }

    @Test
    fun `QemuDisplayType VNC should have correct properties`() {
        assertThat(QemuDisplayType.VNC.toQemuArg()).isEqualTo("vnc")
        assertThat(QemuDisplayType.VNC.isRemote).isTrue()
        assertThat(QemuDisplayType.VNC.isLocal).isFalse()
        assertThat(QemuDisplayType.VNC.isHeadless).isFalse()
    }

    @Test
    fun `QemuDisplayType SPICE should have correct properties`() {
        assertThat(QemuDisplayType.SPICE.toQemuArg()).isEqualTo("spice")
        assertThat(QemuDisplayType.SPICE.isRemote).isTrue()
        assertThat(QemuDisplayType.SPICE.isHeadless).isFalse()
    }

    @Test
    fun `QemuDisplayType SDL should have correct properties`() {
        assertThat(QemuDisplayType.SDL.toQemuArg()).isEqualTo("sdl")
        assertThat(QemuDisplayType.SDL.isRemote).isFalse()
        assertThat(QemuDisplayType.SDL.isLocal).isTrue()
        assertThat(QemuDisplayType.SDL.isHeadless).isFalse()
    }

    @Test
    fun `QemuDisplayType GTK should have correct properties`() {
        assertThat(QemuDisplayType.GTK.toQemuArg()).isEqualTo("gtk")
        assertThat(QemuDisplayType.GTK.isLocal).isTrue()
    }

    @Test
    fun `QemuDisplayType COCOA should have correct properties`() {
        assertThat(QemuDisplayType.COCOA.toQemuArg()).isEqualTo("cocoa")
        assertThat(QemuDisplayType.COCOA.isLocal).isTrue()
    }

    @Test
    fun `QemuDisplayType EGL_HEADLESS should have correct properties`() {
        assertThat(QemuDisplayType.EGL_HEADLESS.toQemuArg()).isEqualTo("egl-headless")
        assertThat(QemuDisplayType.EGL_HEADLESS.isHeadless).isTrue()
    }

    // ==================== QemuSerialMode测试 ====================

    @Test
    fun `QemuSerialMode Stdio should generate correct argument`() {
        assertThat(QemuSerialMode.Stdio.toQemuArg()).isEqualTo("stdio")
    }

    @Test
    fun `QemuSerialMode File should generate correct argument`() {
        val mode = QemuSerialMode.File(path = "/tmp/serial.log")
        assertThat(mode.toQemuArg()).isEqualTo("file:/tmp/serial.log")
    }

    @Test
    fun `QemuSerialMode UnixSocket should generate correct argument`() {
        val mode = QemuSerialMode.UnixSocket(
            path = "/tmp/serial.sock",
            server = true,
            wait = false
        )
        assertThat(mode.toQemuArg()).isEqualTo("unix:/tmp/serial.sock,server=on,wait=off")
    }

    @Test
    fun `QemuSerialMode TcpSocket should generate correct argument`() {
        val mode = QemuSerialMode.TcpSocket(
            host = "0.0.0.0",
            port = 4444,
            server = true,
            wait = false
        )
        assertThat(mode.toQemuArg()).isEqualTo("tcp:0.0.0.0:4444,server=on,wait=off")
    }

    @Test
    fun `QemuSerialMode Pty should generate correct argument`() {
        assertThat(QemuSerialMode.Pty.toQemuArg()).isEqualTo("pty")
    }

    @Test
    fun `QemuSerialMode Disabled should generate correct argument`() {
        assertThat(QemuSerialMode.Disabled.toQemuArg()).isEqualTo("none")
    }

    // ==================== QemuAudioBackend测试 ====================

    @Test
    fun `QemuAudioBackend NONE should generate correct env var`() {
        assertThat(QemuAudioBackend.NONE.toEnvVar()).isEqualTo("none")
    }

    @Test
    fun `QemuAudioBackend PA should generate correct env var`() {
        assertThat(QemuAudioBackend.PA.toEnvVar()).isEqualTo("pa")
    }

    @Test
    fun `QemuAudioBackend ALSA should generate correct env var`() {
        assertThat(QemuAudioBackend.ALSA.toEnvVar()).isEqualTo("alsa")
    }

    @Test
    fun `QemuAudioBackend OSS should generate correct env var`() {
        assertThat(QemuAudioBackend.OSS.toEnvVar()).isEqualTo("oss")
    }

    @Test
    fun `QemuAudioBackend COREAUDIO should generate correct env var`() {
        assertThat(QemuAudioBackend.COREAUDIO.toEnvVar()).isEqualTo("coreaudio")
    }

    @Test
    fun `QemuAudioBackend DSOUND should generate correct env var`() {
        assertThat(QemuAudioBackend.DSOUND.toEnvVar()).isEqualTo("dsound")
    }

    @Test
    fun `QemuAudioBackend SDL should generate correct env var`() {
        assertThat(QemuAudioBackend.SDL.toEnvVar()).isEqualTo("sdl")
    }

    @Test
    fun `QemuAudioBackend JACK should generate correct env var`() {
        assertThat(QemuAudioBackend.JACK.toEnvVar()).isEqualTo("jack")
    }

    @Test
    fun `QemuAudioBackend PIPEWIRE should generate correct env var`() {
        assertThat(QemuAudioBackend.PIPEWIRE.toEnvVar()).isEqualTo("pipewire")
    }

    // ==================== QemuTargetArch测试 ====================

    @Test
    fun `QemuTargetArch X86_64 should have correct properties`() {
        assertThat(QemuTargetArch.X86_64.archName).isEqualTo("x86_64")
        assertThat(QemuTargetArch.X86_64.systemEmulator).isEqualTo("qemu-system-x86_64")
        assertThat(QemuTargetArch.X86_64.userEmulator).isEqualTo("qemu-x86_64")
        assertThat(QemuTargetArch.X86_64.is64Bit).isTrue()
        assertThat(QemuTargetArch.X86_64.getSystemCommand()).isEqualTo("qemu-system-x86_64")
        assertThat(QemuTargetArch.X86_64.getUserCommand()).isEqualTo("qemu-x86_64")
    }

    @Test
    fun `QemuTargetArch I386 should have correct properties`() {
        assertThat(QemuTargetArch.I386.archName).isEqualTo("i386")
        assertThat(QemuTargetArch.I386.is64Bit).isFalse()
    }

    @Test
    fun `QemuTargetArch AARCH64 should have correct properties`() {
        assertThat(QemuTargetArch.AARCH64.archName).isEqualTo("aarch64")
        assertThat(QemuTargetArch.AARCH64.systemEmulator).isEqualTo("qemu-system-aarch64")
        assertThat(QemuTargetArch.AARCH64.is64Bit).isTrue()
    }

    @Test
    fun `QemuTargetArch ARM should have correct properties`() {
        assertThat(QemuTargetArch.ARM.archName).isEqualTo("arm")
        assertThat(QemuTargetArch.ARM.is64Bit).isFalse()
    }

    @Test
    fun `QemuTargetArch RISCV64 should have correct properties`() {
        assertThat(QemuTargetArch.RISCV64.archName).isEqualTo("riscv64")
        assertThat(QemuTargetArch.RISCV64.is64Bit).isTrue()
    }

    @Test
    fun `QemuTargetArch RISCV32 should have correct properties`() {
        assertThat(QemuTargetArch.RISCV32.archName).isEqualTo("riscv32")
        assertThat(QemuTargetArch.RISCV32.is64Bit).isFalse()
    }

    @Test
    fun `QemuTargetArch MIPS64 should have correct properties`() {
        assertThat(QemuTargetArch.MIPS64.archName).isEqualTo("mips64")
        assertThat(QemuTargetArch.MIPS64.is64Bit).isTrue()
    }

    @Test
    fun `QemuTargetArch PPC64 should have correct properties`() {
        assertThat(QemuTargetArch.PPC64.archName).isEqualTo("ppc64")
        assertThat(QemuTargetArch.PPC64.is64Bit).isTrue()
    }

    @Test
    fun `QemuTargetArch SPARC64 should have correct properties`() {
        assertThat(QemuTargetArch.SPARC64.archName).isEqualTo("sparc64")
        assertThat(QemuTargetArch.SPARC64.is64Bit).isTrue()
    }

    // ==================== QemuBusType测试 ====================

    @Test
    fun `QemuBusType should have all expected values`() {
        assertThat(QemuBusType.values()).asList().containsExactly(
            QemuBusType.PCI,
            QemuBusType.PCIE,
            QemuBusType.ISA,
            QemuBusType.USB,
            QemuBusType.VIRTIO,
            QemuBusType.SCSI,
            QemuBusType.IDE,
            QemuBusType.SD
        )
    }
}