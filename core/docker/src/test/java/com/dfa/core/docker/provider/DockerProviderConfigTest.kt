package com.dfa.core.docker.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * DockerProviderConfig 单元测试
 *
 * 测试各种DockerProvider配置类的属性和方法。
 */
class DockerProviderConfigTest {

    // ==================== QemuDockerProviderConfig 测试 ====================

    @Test
    fun `QemuDockerProviderConfig should have correct default values`() {
        val config = QemuDockerProviderConfig(
            providerId = "qemu-1",
            vmId = "vm-1"
        )

        assertThat(config.providerId).isEqualTo("qemu-1")
        assertThat(config.vmId).isEqualTo("vm-1")
        assertThat(config.socketPath).isEqualTo("/var/run/docker.sock")
        assertThat(config.sshHost).isEqualTo("localhost")
        assertThat(config.sshPort).isEqualTo(22)
        assertThat(config.sshUser).isEqualTo("root")
        assertThat(config.sshKeyPath).isNull()
        assertThat(config.autoStart).isTrue()
        assertThat(config.connectionTimeout).isEqualTo(30000L)
        assertThat(config.requestTimeout).isEqualTo(60000L)
        assertThat(config.memoryMB).isEqualTo(4096)
        assertThat(config.cpus).isEqualTo(4)
        assertThat(config.diskSizeGB).isEqualTo(50)
        assertThat(config.imageDir).isEqualTo("/var/lib/docker")
    }

    @Test
    fun `QemuDockerProviderConfig should accept custom values`() {
        val config = QemuDockerProviderConfig(
            providerId = "qemu-custom",
            vmId = "vm-custom",
            socketPath = "/custom/docker.sock",
            sshHost = "192.168.1.100",
            sshPort = 2222,
            sshUser = "admin",
            sshKeyPath = "/home/admin/.ssh/id_rsa",
            autoStart = false,
            connectionTimeout = 60000L,
            requestTimeout = 120000L,
            memoryMB = 8192,
            cpus = 8,
            diskSizeGB = 100,
            imageDir = "/custom/images"
        )

        assertThat(config.providerId).isEqualTo("qemu-custom")
        assertThat(config.vmId).isEqualTo("vm-custom")
        assertThat(config.socketPath).isEqualTo("/custom/docker.sock")
        assertThat(config.sshHost).isEqualTo("192.168.1.100")
        assertThat(config.sshPort).isEqualTo(2222)
        assertThat(config.sshUser).isEqualTo("admin")
        assertThat(config.sshKeyPath).isEqualTo("/home/admin/.ssh/id_rsa")
        assertThat(config.autoStart).isFalse()
        assertThat(config.connectionTimeout).isEqualTo(60000L)
        assertThat(config.requestTimeout).isEqualTo(120000L)
        assertThat(config.memoryMB).isEqualTo(8192)
        assertThat(config.cpus).isEqualTo(8)
        assertThat(config.diskSizeGB).isEqualTo(100)
        assertThat(config.imageDir).isEqualTo("/custom/images")
    }

    @Test
    fun `QemuDockerProviderConfig getProviderType should return QEMU`() {
        val config = QemuDockerProviderConfig(
            providerId = "qemu-1",
            vmId = "vm-1"
        )

        assertThat(config.getProviderType()).isEqualTo(DockerProviderType.QEMU)
    }

    @Test
    fun `QemuDockerProviderConfig sshConnectionString should format correctly`() {
        val config = QemuDockerProviderConfig(
            providerId = "qemu-1",
            vmId = "vm-1",
            sshHost = "192.168.1.100",
            sshPort = 2222,
            sshUser = "admin"
        )

        assertThat(config.sshConnectionString).isEqualTo("admin@192.168.1.100:2222")
    }

    @Test
    fun `QemuDockerProviderConfig dockerHost should format correctly`() {
        val config = QemuDockerProviderConfig(
            providerId = "qemu-1",
            vmId = "vm-1",
            sshHost = "192.168.1.100"
        )

        assertThat(config.dockerHost).isEqualTo("tcp://192.168.1.100:2375")
    }

    // ==================== AvfDockerProviderConfig 测试 ====================

    @Test
    fun `AvfDockerProviderConfig should have correct default values`() {
        val config = AvfDockerProviderConfig(
            providerId = "avf-1",
            vmId = "vm-1",
            vmBundlePath = "/path/to/vm.bundle"
        )

        assertThat(config.providerId).isEqualTo("avf-1")
        assertThat(config.vmId).isEqualTo("vm-1")
        assertThat(config.socketPath).isEqualTo("/var/run/docker.sock")
        assertThat(config.vmBundlePath).isEqualTo("/path/to/vm.bundle")
        assertThat(config.autoStart).isTrue()
        assertThat(config.connectionTimeout).isEqualTo(30000L)
        assertThat(config.requestTimeout).isEqualTo(60000L)
        assertThat(config.memoryMB).isEqualTo(4096)
        assertThat(config.cpus).isEqualTo(4)
        assertThat(config.diskSizeGB).isEqualTo(50)
        assertThat(config.useRosetta).isTrue()
        assertThat(config.networkMode).isEqualTo(AvfNetworkMode.BRIDGED)
    }

    @Test
    fun `AvfDockerProviderConfig should accept custom values`() {
        val config = AvfDockerProviderConfig(
            providerId = "avf-custom",
            vmId = "vm-custom",
            socketPath = "/custom/docker.sock",
            vmBundlePath = "/custom/vm.bundle",
            autoStart = false,
            connectionTimeout = 60000L,
            requestTimeout = 120000L,
            memoryMB = 8192,
            cpus = 8,
            diskSizeGB = 100,
            useRosetta = false,
            networkMode = AvfNetworkMode.NAT
        )

        assertThat(config.providerId).isEqualTo("avf-custom")
        assertThat(config.vmId).isEqualTo("vm-custom")
        assertThat(config.socketPath).isEqualTo("/custom/docker.sock")
        assertThat(config.vmBundlePath).isEqualTo("/custom/vm.bundle")
        assertThat(config.autoStart).isFalse()
        assertThat(config.connectionTimeout).isEqualTo(60000L)
        assertThat(config.requestTimeout).isEqualTo(120000L)
        assertThat(config.memoryMB).isEqualTo(8192)
        assertThat(config.cpus).isEqualTo(8)
        assertThat(config.diskSizeGB).isEqualTo(100)
        assertThat(config.useRosetta).isFalse()
        assertThat(config.networkMode).isEqualTo(AvfNetworkMode.NAT)
    }

    @Test
    fun `AvfDockerProviderConfig getProviderType should return AVF`() {
        val config = AvfDockerProviderConfig(
            providerId = "avf-1",
            vmId = "vm-1",
            vmBundlePath = "/path/to/vm.bundle"
        )

        assertThat(config.getProviderType()).isEqualTo(DockerProviderType.AVF)
    }

    @Test
    fun `AvfDockerProviderConfig vmConfigSummary should format correctly`() {
        val config = AvfDockerProviderConfig(
            providerId = "avf-1",
            vmId = "vm-1",
            vmBundlePath = "/path/to/vm.bundle",
            cpus = 8,
            memoryMB = 8192,
            diskSizeGB = 100
        )

        assertThat(config.vmConfigSummary).isEqualTo("AVF VM[vm-1]: 8CPUs, 8192MB RAM, 100GB Disk")
    }

    // ==================== LocalDockerProviderConfig 测试 ====================

    @Test
    fun `LocalDockerProviderConfig should have correct default values`() {
        val config = LocalDockerProviderConfig(
            providerId = "local-1"
        )

        assertThat(config.providerId).isEqualTo("local-1")
        assertThat(config.socketPath).isEqualTo("/var/run/docker.sock")
        assertThat(config.host).isNull()
        assertThat(config.tlsConfig).isNull()
        assertThat(config.autoStart).isTrue()
        assertThat(config.connectionTimeout).isEqualTo(10000L)
        assertThat(config.requestTimeout).isEqualTo(30000L)
    }

    @Test
    fun `LocalDockerProviderConfig should accept custom values`() {
        val tlsConfig = DockerTlsConfig(
            certPath = "/path/to/certs"
        )
        val config = LocalDockerProviderConfig(
            providerId = "local-custom",
            socketPath = "/custom/docker.sock",
            host = "tcp://localhost:2375",
            tlsConfig = tlsConfig,
            autoStart = false,
            connectionTimeout = 20000L,
            requestTimeout = 60000L
        )

        assertThat(config.providerId).isEqualTo("local-custom")
        assertThat(config.socketPath).isEqualTo("/custom/docker.sock")
        assertThat(config.host).isEqualTo("tcp://localhost:2375")
        assertThat(config.tlsConfig).isEqualTo(tlsConfig)
        assertThat(config.autoStart).isFalse()
        assertThat(config.connectionTimeout).isEqualTo(20000L)
        assertThat(config.requestTimeout).isEqualTo(60000L)
    }

    @Test
    fun `LocalDockerProviderConfig getProviderType should return LOCAL`() {
        val config = LocalDockerProviderConfig(
            providerId = "local-1"
        )

        assertThat(config.getProviderType()).isEqualTo(DockerProviderType.LOCAL)
    }

    @Test
    fun `LocalDockerProviderConfig dockerHost should use host when provided`() {
        val config = LocalDockerProviderConfig(
            providerId = "local-1",
            host = "tcp://localhost:2375"
        )

        assertThat(config.dockerHost).isEqualTo("tcp://localhost:2375")
    }

    @Test
    fun `LocalDockerProviderConfig dockerHost should use socket when host is null`() {
        val config = LocalDockerProviderConfig(
            providerId = "local-1",
            socketPath = "/var/run/docker.sock"
        )

        assertThat(config.dockerHost).isEqualTo("unix:///var/run/docker.sock")
    }

    @Test
    fun `LocalDockerProviderConfig useTls should be true when tlsConfig is set`() {
        val config = LocalDockerProviderConfig(
            providerId = "local-1",
            tlsConfig = DockerTlsConfig(certPath = "/path/to/certs")
        )

        assertThat(config.useTls).isTrue()
    }

    @Test
    fun `LocalDockerProviderConfig useTls should be false when tlsConfig is null`() {
        val config = LocalDockerProviderConfig(
            providerId = "local-1"
        )

        assertThat(config.useTls).isFalse()
    }

    // ==================== AvfNetworkMode 测试 ====================

    @Test
    fun `AvfNetworkMode should contain all expected values`() {
        val expectedModes = listOf(
            AvfNetworkMode.BRIDGED,
            AvfNetworkMode.NAT,
            AvfNetworkMode.HOST_ONLY
        )

        assertThat(AvfNetworkMode.entries.size).isEqualTo(expectedModes.size)
        expectedModes.forEach { mode ->
            assertThat(AvfNetworkMode.entries.contains(mode)).isTrue()
        }
    }

    // ==================== DockerTlsConfig 测试 ====================

    @Test
    fun `DockerTlsConfig should have correct default values`() {
        val config = DockerTlsConfig(
            certPath = "/path/to/certs"
        )

        assertThat(config.certPath).isEqualTo("/path/to/certs")
        assertThat(config.certFile).isEqualTo("cert.pem")
        assertThat(config.keyFile).isEqualTo("key.pem")
        assertThat(config.caFile).isEqualTo("ca.pem")
        assertThat(config.verify).isTrue()
    }

    @Test
    fun `DockerTlsConfig should accept custom values`() {
        val config = DockerTlsConfig(
            certPath = "/custom/certs",
            certFile = "client-cert.pem",
            keyFile = "client-key.pem",
            caFile = "ca-cert.pem",
            verify = false
        )

        assertThat(config.certPath).isEqualTo("/custom/certs")
        assertThat(config.certFile).isEqualTo("client-cert.pem")
        assertThat(config.keyFile).isEqualTo("client-key.pem")
        assertThat(config.caFile).isEqualTo("ca-cert.pem")
        assertThat(config.verify).isFalse()
    }

    @Test
    fun `DockerTlsConfig fullCertPath should combine paths correctly`() {
        val config = DockerTlsConfig(
            certPath = "/path/to/certs",
            certFile = "cert.pem"
        )

        assertThat(config.fullCertPath).isEqualTo("/path/to/certs/cert.pem")
    }

    @Test
    fun `DockerTlsConfig fullKeyPath should combine paths correctly`() {
        val config = DockerTlsConfig(
            certPath = "/path/to/certs",
            keyFile = "key.pem"
        )

        assertThat(config.fullKeyPath).isEqualTo("/path/to/certs/key.pem")
    }

    @Test
    fun `DockerTlsConfig fullCaPath should combine paths correctly`() {
        val config = DockerTlsConfig(
            certPath = "/path/to/certs",
            caFile = "ca.pem"
        )

        assertThat(config.fullCaPath).isEqualTo("/path/to/certs/ca.pem")
    }

    // ==================== 配置验证测试 ====================

    @Test
    fun `QemuDockerProviderConfig should be a data class with correct equals`() {
        val config1 = QemuDockerProviderConfig(
            providerId = "qemu-1",
            vmId = "vm-1"
        )
        val config2 = QemuDockerProviderConfig(
            providerId = "qemu-1",
            vmId = "vm-1"
        )
        val config3 = QemuDockerProviderConfig(
            providerId = "qemu-2",
            vmId = "vm-2"
        )

        assertThat(config1).isEqualTo(config2)
        assertThat(config1).isNotEqualTo(config3)
    }

    @Test
    fun `AvfDockerProviderConfig should be a data class with correct equals`() {
        val config1 = AvfDockerProviderConfig(
            providerId = "avf-1",
            vmId = "vm-1",
            vmBundlePath = "/path/to/vm.bundle"
        )
        val config2 = AvfDockerProviderConfig(
            providerId = "avf-1",
            vmId = "vm-1",
            vmBundlePath = "/path/to/vm.bundle"
        )
        val config3 = AvfDockerProviderConfig(
            providerId = "avf-2",
            vmId = "vm-2",
            vmBundlePath = "/other/vm.bundle"
        )

        assertThat(config1).isEqualTo(config2)
        assertThat(config1).isNotEqualTo(config3)
    }

    @Test
    fun `LocalDockerProviderConfig should be a data class with correct equals`() {
        val config1 = LocalDockerProviderConfig(
            providerId = "local-1"
        )
        val config2 = LocalDockerProviderConfig(
            providerId = "local-1"
        )
        val config3 = LocalDockerProviderConfig(
            providerId = "local-2"
        )

        assertThat(config1).isEqualTo(config2)
        assertThat(config1).isNotEqualTo(config3)
    }

    @Test
    fun `DockerTlsConfig should be a data class with correct equals`() {
        val config1 = DockerTlsConfig(
            certPath = "/path/to/certs"
        )
        val config2 = DockerTlsConfig(
            certPath = "/path/to/certs"
        )
        val config3 = DockerTlsConfig(
            certPath = "/other/certs"
        )

        assertThat(config1).isEqualTo(config2)
        assertThat(config1).isNotEqualTo(config3)
    }

    // ==================== Sealed Class 测试 ====================

    @Test
    fun `all config types should extend DockerProviderConfig`() {
        val configs: List<DockerProviderConfig> = listOf(
            QemuDockerProviderConfig(providerId = "qemu-1", vmId = "vm-1"),
            AvfDockerProviderConfig(providerId = "avf-1", vmId = "vm-1", vmBundlePath = "/path"),
            LocalDockerProviderConfig(providerId = "local-1")
        )

        assertThat(configs).hasSize(3)
        configs.forEach { config ->
            assertThat(config.providerId).isNotEmpty()
            assertThat(config.getProviderType()).isNotEqualTo(DockerProviderType.UNKNOWN)
        }
    }
}