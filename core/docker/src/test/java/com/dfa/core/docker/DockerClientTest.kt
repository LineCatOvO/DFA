package com.dfa.core.docker

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * DockerClient 数据模型单元测试
 *
 * 测试DockerClient接口中定义的数据模型类。
 */
class DockerClientTest {

    // ==================== DockerVersion Tests ====================

    @Test
    fun `DockerVersion should have all required properties`() {
        val version = DockerVersion(
            version = "24.0.7",
            apiVersion = "1.43",
            gitCommit = "311b9ff",
            goVersion = "go1.21.3",
            os = "linux",
            arch = "arm64",
            kernelVersion = "6.1.0"
        )

        assertThat(version.version).isEqualTo("24.0.7")
        assertThat(version.apiVersion).isEqualTo("1.43")
        assertThat(version.gitCommit).isEqualTo("311b9ff")
        assertThat(version.goVersion).isEqualTo("go1.21.3")
        assertThat(version.os).isEqualTo("linux")
        assertThat(version.arch).isEqualTo("arm64")
        assertThat(version.kernelVersion).isEqualTo("6.1.0")
    }

    @Test
    fun `DockerVersion copy should work correctly`() {
        val original = DockerVersion(
            version = "24.0.7",
            apiVersion = "1.43",
            gitCommit = "311b9ff",
            goVersion = "go1.21.3",
            os = "linux",
            arch = "arm64",
            kernelVersion = "6.1.0"
        )

        val copied = original.copy(version = "25.0.0")

        assertThat(copied.version).isEqualTo("25.0.0")
        assertThat(copied.apiVersion).isEqualTo("1.43")
        assertThat(original.version).isEqualTo("24.0.7")
    }

    @Test
    fun `DockerVersion equals should work correctly`() {
        val version1 = DockerVersion(
            version = "24.0.7",
            apiVersion = "1.43",
            gitCommit = "311b9ff",
            goVersion = "go1.21.3",
            os = "linux",
            arch = "arm64",
            kernelVersion = "6.1.0"
        )
        val version2 = version1.copy()
        val version3 = version1.copy(version = "25.0.0")

        assertThat(version1).isEqualTo(version2)
        assertThat(version1).isNotEqualTo(version3)
    }

    // ==================== DockerSystemInfo Tests ====================

    @Test
    fun `DockerSystemInfo should have all required properties`() {
        val info = DockerSystemInfo(
            containers = 10,
            containersRunning = 5,
            containersStopped = 3,
            containersPaused = 2,
            images = 20,
            operatingSystem = "Ubuntu 22.04.3 LTS",
            architecture = "aarch64",
            cpus = 8,
            memory = 16777216000L,
            dockerRootDir = "/var/lib/docker",
            driver = "overlay2"
        )

        assertThat(info.containers).isEqualTo(10)
        assertThat(info.containersRunning).isEqualTo(5)
        assertThat(info.containersStopped).isEqualTo(3)
        assertThat(info.containersPaused).isEqualTo(2)
        assertThat(info.images).isEqualTo(20)
        assertThat(info.operatingSystem).isEqualTo("Ubuntu 22.04.3 LTS")
        assertThat(info.architecture).isEqualTo("aarch64")
        assertThat(info.cpus).isEqualTo(8)
        assertThat(info.memory).isEqualTo(16777216000L)
        assertThat(info.dockerRootDir).isEqualTo("/var/lib/docker")
        assertThat(info.driver).isEqualTo("overlay2")
    }

    @Test
    fun `DockerSystemInfo copy should work correctly`() {
        val original = DockerSystemInfo(
            containers = 10,
            containersRunning = 5,
            containersStopped = 3,
            containersPaused = 2,
            images = 20,
            operatingSystem = "Ubuntu 22.04.3 LTS",
            architecture = "aarch64",
            cpus = 8,
            memory = 16777216000L,
            dockerRootDir = "/var/lib/docker",
            driver = "overlay2"
        )

        val copied = original.copy(containers = 15, containersRunning = 10)

        assertThat(copied.containers).isEqualTo(15)
        assertThat(copied.containersRunning).isEqualTo(10)
        assertThat(original.containers).isEqualTo(10)
        assertThat(original.containersRunning).isEqualTo(5)
    }

    // ==================== ContainerCreateResult Tests ====================

    @Test
    fun `ContainerCreateResult should have id and warnings`() {
        val result = ContainerCreateResult(
            id = "container-abc123",
            warnings = listOf("Warning 1", "Warning 2")
        )

        assertThat(result.id).isEqualTo("container-abc123")
        assertThat(result.warnings).hasSize(2)
        assertThat(result.warnings).containsExactly("Warning 1", "Warning 2")
    }

    @Test
    fun `ContainerCreateResult should have empty warnings by default`() {
        val result = ContainerCreateResult(id = "container-abc123")

        assertThat(result.warnings).isEmpty()
    }

    // ==================== ContainerConfig Tests ====================

    @Test
    fun `ContainerConfig should have all required properties`() {
        val config = ContainerConfig(
            name = "test-container",
            image = "nginx:latest",
            command = listOf("nginx", "-g", "daemon off;"),
            entrypoint = listOf("/docker-entrypoint.sh"),
            env = mapOf("ENV" to "production", "DEBUG" to "false"),
            ports = listOf(PortBinding(80, 8080)),
            volumes = listOf(VolumeMount("/host/path", "/container/path")),
            networks = listOf("bridge"),
            hostname = "test-host",
            domainName = "example.com",
            user = "nginx",
            workingDir = "/app",
            labels = mapOf("app" to "nginx"),
            restartPolicy = RestartPolicy("always"),
            resources = ResourceLimits(memory = 512000000L),
            healthCheck = HealthCheck(test = listOf("CMD", "curl", "-f", "http://localhost/")),
            privileged = false,
            capabilities = ContainerCapabilities(add = listOf("NET_ADMIN"))
        )

        assertThat(config.name).isEqualTo("test-container")
        assertThat(config.image).isEqualTo("nginx:latest")
        assertThat(config.command).containsExactly("nginx", "-g", "daemon off;")
        assertThat(config.entrypoint).containsExactly("/docker-entrypoint.sh")
        assertThat(config.env).hasSize(2)
        assertThat(config.ports).hasSize(1)
        assertThat(config.volumes).hasSize(1)
        assertThat(config.networks).containsExactly("bridge")
        assertThat(config.hostname).isEqualTo("test-host")
        assertThat(config.domainName).isEqualTo("example.com")
        assertThat(config.user).isEqualTo("nginx")
        assertThat(config.workingDir).isEqualTo("/app")
        assertThat(config.labels).hasSize(1)
        assertThat(config.restartPolicy.name).isEqualTo("always")
        assertThat(config.resources.memory).isEqualTo(512000000L)
        assertThat(config.healthCheck).isNotNull()
        assertThat(config.privileged).isFalse()
        assertThat(config.capabilities.add).containsExactly("NET_ADMIN")
    }

    @Test
    fun `ContainerConfig should have default values`() {
        val config = ContainerConfig(image = "nginx:latest")

        assertThat(config.name).isNull()
        assertThat(config.command).isEmpty()
        assertThat(config.entrypoint).isEmpty()
        assertThat(config.env).isEmpty()
        assertThat(config.ports).isEmpty()
        assertThat(config.volumes).isEmpty()
        assertThat(config.networks).isEmpty()
        assertThat(config.hostname).isNull()
        assertThat(config.domainName).isNull()
        assertThat(config.user).isNull()
        assertThat(config.workingDir).isNull()
        assertThat(config.labels).isEmpty()
        assertThat(config.restartPolicy.name).isEqualTo("no")
        assertThat(config.healthCheck).isNull()
        assertThat(config.privileged).isFalse()
    }

    // ==================== PortBinding Tests ====================

    @Test
    fun `PortBinding should have all properties`() {
        val binding = PortBinding(
            containerPort = 80,
            hostPort = 8080,
            hostIp = "127.0.0.1",
            protocol = "tcp"
        )

        assertThat(binding.containerPort).isEqualTo(80)
        assertThat(binding.hostPort).isEqualTo(8080)
        assertThat(binding.hostIp).isEqualTo("127.0.0.1")
        assertThat(binding.protocol).isEqualTo("tcp")
    }

    @Test
    fun `PortBinding should have default values`() {
        val binding = PortBinding(containerPort = 80, hostPort = 8080)

        assertThat(binding.hostIp).isEqualTo("0.0.0.0")
        assertThat(binding.protocol).isEqualTo("tcp")
    }

    // ==================== VolumeMount Tests ====================

    @Test
    fun `VolumeMount should have all properties`() {
        val mount = VolumeMount(
            source = "/host/path",
            destination = "/container/path",
            mode = "ro",
            type = "bind"
        )

        assertThat(mount.source).isEqualTo("/host/path")
        assertThat(mount.destination).isEqualTo("/container/path")
        assertThat(mount.mode).isEqualTo("ro")
        assertThat(mount.type).isEqualTo("bind")
    }

    @Test
    fun `VolumeMount should have default values`() {
        val mount = VolumeMount(source = "/host/path", destination = "/container/path")

        assertThat(mount.mode).isEqualTo("rw")
        assertThat(mount.type).isEqualTo("bind")
    }

    // ==================== RestartPolicy Tests ====================

    @Test
    fun `RestartPolicy should have all properties`() {
        val policy = RestartPolicy(name = "on-failure", maximumRetryCount = 5)

        assertThat(policy.name).isEqualTo("on-failure")
        assertThat(policy.maximumRetryCount).isEqualTo(5)
    }

    @Test
    fun `RestartPolicy should have default values`() {
        val policy = RestartPolicy()

        assertThat(policy.name).isEqualTo("no")
        assertThat(policy.maximumRetryCount).isEqualTo(0)
    }

    // ==================== ResourceLimits Tests ====================

    @Test
    fun `ResourceLimits should have all properties`() {
        val limits = ResourceLimits(
            cpuShares = 1024L,
            memory = 512000000L,
            memorySwap = 1024000000L,
            cpuPeriod = 100000L,
            cpuQuota = 50000L,
            cpusetCpus = "0-3",
            cpusetMems = "0-1"
        )

        assertThat(limits.cpuShares).isEqualTo(1024L)
        assertThat(limits.memory).isEqualTo(512000000L)
        assertThat(limits.memorySwap).isEqualTo(1024000000L)
        assertThat(limits.cpuPeriod).isEqualTo(100000L)
        assertThat(limits.cpuQuota).isEqualTo(50000L)
        assertThat(limits.cpusetCpus).isEqualTo("0-3")
        assertThat(limits.cpusetMems).isEqualTo("0-1")
    }

    @Test
    fun `ResourceLimits should have null defaults`() {
        val limits = ResourceLimits()

        assertThat(limits.cpuShares).isNull()
        assertThat(limits.memory).isNull()
        assertThat(limits.memorySwap).isNull()
        assertThat(limits.cpuPeriod).isNull()
        assertThat(limits.cpuQuota).isNull()
        assertThat(limits.cpusetCpus).isNull()
        assertThat(limits.cpusetMems).isNull()
    }

    // ==================== HealthCheck Tests ====================

    @Test
    fun `HealthCheck should have all properties`() {
        val healthCheck = HealthCheck(
            test = listOf("CMD", "curl", "-f", "http://localhost/"),
            interval = 60000000000L,
            timeout = 30000000000L,
            retries = 5,
            startPeriod = 10000000000L
        )

        assertThat(healthCheck.test).containsExactly("CMD", "curl", "-f", "http://localhost/")
        assertThat(healthCheck.interval).isEqualTo(60000000000L)
        assertThat(healthCheck.timeout).isEqualTo(30000000000L)
        assertThat(healthCheck.retries).isEqualTo(5)
        assertThat(healthCheck.startPeriod).isEqualTo(10000000000L)
    }

    @Test
    fun `HealthCheck should have default values`() {
        val healthCheck = HealthCheck(test = listOf("CMD", "echo", "ok"))

        assertThat(healthCheck.interval).isEqualTo(30000000000L)
        assertThat(healthCheck.timeout).isEqualTo(30000000000L)
        assertThat(healthCheck.retries).isEqualTo(3)
        assertThat(healthCheck.startPeriod).isEqualTo(0)
    }

    // ==================== ContainerCapabilities Tests ====================

    @Test
    fun `ContainerCapabilities should have add and drop lists`() {
        val capabilities = ContainerCapabilities(
            add = listOf("NET_ADMIN", "SYS_TIME"),
            drop = listOf("MKNOD", "SYS_CHROOT")
        )

        assertThat(capabilities.add).containsExactly("NET_ADMIN", "SYS_TIME")
        assertThat(capabilities.drop).containsExactly("MKNOD", "SYS_CHROOT")
    }

    @Test
    fun `ContainerCapabilities should have empty defaults`() {
        val capabilities = ContainerCapabilities()

        assertThat(capabilities.add).isEmpty()
        assertThat(capabilities.drop).isEmpty()
    }

    // ==================== ExecOptions Tests ====================

    @Test
    fun `ExecOptions should have all properties`() {
        val options = ExecOptions(
            attachStdin = true,
            attachStdout = true,
            attachStderr = true,
            detachKeys = "ctrl-p,ctrl-q",
            tty = true,
            env = mapOf("TERM" to "xterm"),
            cwd = "/app",
            privileged = true,
            user = "root"
        )

        assertThat(options.attachStdin).isTrue()
        assertThat(options.attachStdout).isTrue()
        assertThat(options.attachStderr).isTrue()
        assertThat(options.detachKeys).isEqualTo("ctrl-p,ctrl-q")
        assertThat(options.tty).isTrue()
        assertThat(options.env).hasSize(1)
        assertThat(options.cwd).isEqualTo("/app")
        assertThat(options.privileged).isTrue()
        assertThat(options.user).isEqualTo("root")
    }

    @Test
    fun `ExecOptions should have default values`() {
        val options = ExecOptions()

        assertThat(options.attachStdin).isFalse()
        assertThat(options.attachStdout).isTrue()
        assertThat(options.attachStderr).isTrue()
        assertThat(options.detachKeys).isNull()
        assertThat(options.tty).isFalse()
        assertThat(options.env).isEmpty()
        assertThat(options.cwd).isNull()
        assertThat(options.privileged).isFalse()
        assertThat(options.user).isNull()
    }

    // ==================== ExecResult Tests ====================

    @Test
    fun `ExecResult should have all properties`() {
        val result = ExecResult(
            exitCode = 0,
            stdout = "Hello, World!",
            stderr = ""
        )

        assertThat(result.exitCode).isEqualTo(0)
        assertThat(result.stdout).isEqualTo("Hello, World!")
        assertThat(result.stderr).isEmpty()
    }

    // ==================== ContainerLogsOptions Tests ====================

    @Test
    fun `ContainerLogsOptions should have all properties`() {
        val options = ContainerLogsOptions(
            follow = true,
            stdout = true,
            stderr = false,
            since = 1000L,
            until = 2000L,
            timestamps = true,
            tail = "100"
        )

        assertThat(options.follow).isTrue()
        assertThat(options.stdout).isTrue()
        assertThat(options.stderr).isFalse()
        assertThat(options.since).isEqualTo(1000L)
        assertThat(options.until).isEqualTo(2000L)
        assertThat(options.timestamps).isTrue()
        assertThat(options.tail).isEqualTo("100")
    }

    @Test
    fun `ContainerLogsOptions should have default values`() {
        val options = ContainerLogsOptions()

        assertThat(options.follow).isFalse()
        assertThat(options.stdout).isTrue()
        assertThat(options.stderr).isTrue()
        assertThat(options.since).isEqualTo(0)
        assertThat(options.until).isEqualTo(0)
        assertThat(options.timestamps).isFalse()
        assertThat(options.tail).isEqualTo("all")
    }

    // ==================== ImagePullOptions Tests ====================

    @Test
    fun `ImagePullOptions should have all properties`() {
        val auth = RegistryAuth(
            username = "user",
            password = "pass",
            email = "user@example.com",
            serverAddress = "https://registry.example.com"
        )
        val options = ImagePullOptions(
            registry = "registry.example.com",
            platform = "linux/arm64",
            auth = auth
        )

        assertThat(options.registry).isEqualTo("registry.example.com")
        assertThat(options.platform).isEqualTo("linux/arm64")
        assertThat(options.auth).isNotNull()
        assertThat(options.auth?.username).isEqualTo("user")
    }

    @Test
    fun `ImagePullOptions should have null defaults`() {
        val options = ImagePullOptions()

        assertThat(options.registry).isNull()
        assertThat(options.platform).isNull()
        assertThat(options.auth).isNull()
    }

    // ==================== ImageBuildOptions Tests ====================

    @Test
    fun `ImageBuildOptions should have all properties`() {
        val options = ImageBuildOptions(
            dockerfile = "Dockerfile.prod",
            tags = listOf("myapp:latest", "myapp:1.0"),
            buildArgs = mapOf("VERSION" to "1.0"),
            cacheFrom = listOf("myapp:cache"),
            noCache = true,
            pull = true,
            platform = "linux/arm64",
            target = "builder",
            labels = mapOf("maintainer" to "dev@example.com")
        )

        assertThat(options.dockerfile).isEqualTo("Dockerfile.prod")
        assertThat(options.tags).containsExactly("myapp:latest", "myapp:1.0")
        assertThat(options.buildArgs).hasSize(1)
        assertThat(options.cacheFrom).containsExactly("myapp:cache")
        assertThat(options.noCache).isTrue()
        assertThat(options.pull).isTrue()
        assertThat(options.platform).isEqualTo("linux/arm64")
        assertThat(options.target).isEqualTo("builder")
        assertThat(options.labels).hasSize(1)
    }

    @Test
    fun `ImageBuildOptions should have default values`() {
        val options = ImageBuildOptions()

        assertThat(options.dockerfile).isEqualTo("Dockerfile")
        assertThat(options.tags).isEmpty()
        assertThat(options.buildArgs).isEmpty()
        assertThat(options.cacheFrom).isEmpty()
        assertThat(options.noCache).isFalse()
        assertThat(options.pull).isFalse()
        assertThat(options.platform).isNull()
        assertThat(options.target).isNull()
        assertThat(options.labels).isEmpty()
    }

    // ==================== NetworkConfig Tests ====================

    @Test
    fun `NetworkConfig should have all properties`() {
        val config = NetworkConfig(
            name = "my-network",
            driver = "bridge",
            scope = "local",
            internal = true,
            attachable = true,
            ingress = false,
            enableIPv6 = true,
            ipam = IpamConfig(
                driver = "default",
                config = listOf(IpamConfigEntry(subnet = "172.20.0.0/16", gateway = "172.20.0.1"))
            ),
            options = mapOf("com.docker.network.bridge.enable_icc" to "true"),
            labels = mapOf("environment" to "test")
        )

        assertThat(config.name).isEqualTo("my-network")
        assertThat(config.driver).isEqualTo("bridge")
        assertThat(config.scope).isEqualTo("local")
        assertThat(config.internal).isTrue()
        assertThat(config.attachable).isTrue()
        assertThat(config.ingress).isFalse()
        assertThat(config.enableIPv6).isTrue()
        assertThat(config.ipam.driver).isEqualTo("default")
        assertThat(config.ipam.config).hasSize(1)
        assertThat(config.options).hasSize(1)
        assertThat(config.labels).hasSize(1)
    }

    @Test
    fun `NetworkConfig should have default values`() {
        val config = NetworkConfig(name = "my-network")

        assertThat(config.driver).isEqualTo("bridge")
        assertThat(config.scope).isEqualTo("local")
        assertThat(config.internal).isFalse()
        assertThat(config.attachable).isFalse()
        assertThat(config.ingress).isFalse()
        assertThat(config.enableIPv6).isFalse()
        assertThat(config.options).isEmpty()
        assertThat(config.labels).isEmpty()
    }

    // ==================== VolumeConfig Tests ====================

    @Test
    fun `VolumeConfig should have all properties`() {
        val config = VolumeConfig(
            name = "my-volume",
            driver = "local",
            driverOpts = mapOf("type" to "tmpfs"),
            labels = mapOf("app" to "myapp")
        )

        assertThat(config.name).isEqualTo("my-volume")
        assertThat(config.driver).isEqualTo("local")
        assertThat(config.driverOpts).hasSize(1)
        assertThat(config.labels).hasSize(1)
    }

    @Test
    fun `VolumeConfig should have default values`() {
        val config = VolumeConfig()

        assertThat(config.name).isNull()
        assertThat(config.driver).isEqualTo("local")
        assertThat(config.driverOpts).isEmpty()
        assertThat(config.labels).isEmpty()
    }

    // ==================== RegistryAuth Tests ====================

    @Test
    fun `RegistryAuth should have all properties`() {
        val auth = RegistryAuth(
            username = "dockeruser",
            password = "secret123",
            email = "user@example.com",
            serverAddress = "https://registry.hub.docker.com"
        )

        assertThat(auth.username).isEqualTo("dockeruser")
        assertThat(auth.password).isEqualTo("secret123")
        assertThat(auth.email).isEqualTo("user@example.com")
        assertThat(auth.serverAddress).isEqualTo("https://registry.hub.docker.com")
    }

    @Test
    fun `RegistryAuth should have null defaults for optional fields`() {
        val auth = RegistryAuth(username = "user", password = "pass")

        assertThat(auth.email).isNull()
        assertThat(auth.serverAddress).isNull()
    }
}