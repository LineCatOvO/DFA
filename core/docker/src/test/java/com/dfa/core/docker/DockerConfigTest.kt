package com.dfa.core.docker

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration

/**
 * DockerConfig 单元测试
 *
 * 测试DockerConfig及其相关配置类的属性和方法。
 */
class DockerConfigTest {

    // ==================== 默认值测试 ====================

    @Test
    fun `default host should be localhost`() {
        assertThat(DockerConfig.DEFAULT_HOST).isEqualTo("localhost")
    }

    @Test
    fun `default unix socket should be correct`() {
        assertThat(DockerConfig.DEFAULT_UNIX_SOCKET).isEqualTo("unix:///var/run/docker.sock")
    }

    @Test
    fun `default windows pipe should be correct`() {
        assertThat(DockerConfig.DEFAULT_WINDOWS_PIPE).isEqualTo("npipe:////./pipe/docker_engine")
    }

    @Test
    fun `default TCP port should be 2375`() {
        assertThat(DockerConfig.DEFAULT_TCP_PORT).isEqualTo(2375)
    }

    @Test
    fun `default TLS port should be 2376`() {
        assertThat(DockerConfig.DEFAULT_TLS_PORT).isEqualTo(2376)
    }

    @Test
    fun `default connection timeout should be 30 seconds`() {
        assertThat(DockerConfig.DEFAULT_CONNECTION_TIMEOUT_SECONDS).isEqualTo(30L)
    }

    @Test
    fun `default read timeout should be 60 seconds`() {
        assertThat(DockerConfig.DEFAULT_READ_TIMEOUT_SECONDS).isEqualTo(60L)
    }

    @Test
    fun `default write timeout should be 60 seconds`() {
        assertThat(DockerConfig.DEFAULT_WRITE_TIMEOUT_SECONDS).isEqualTo(60L)
    }

    @Test
    fun `default max retries should be 3`() {
        assertThat(DockerConfig.DEFAULT_MAX_RETRIES).isEqualTo(3)
    }

    @Test
    fun `default retry delay should be 1000ms`() {
        assertThat(DockerConfig.DEFAULT_RETRY_DELAY_MS).isEqualTo(1000L)
    }

    // ==================== 构造函数测试 ====================

    @Test
    fun `DockerConfig should have default values`() {
        val config = DockerConfig()

        assertThat(config.host).isEqualTo("localhost")
        assertThat(config.dockerHost).isNull()
        assertThat(config.tlsVerify).isFalse()
        assertThat(config.certPath).isNull()
        assertThat(config.connectionTimeout).isEqualTo(Duration.ofSeconds(30))
        assertThat(config.readTimeout).isEqualTo(Duration.ofSeconds(60))
        assertThat(config.writeTimeout).isEqualTo(Duration.ofSeconds(60))
        assertThat(config.maxRetries).isEqualTo(3)
        assertThat(config.retryDelay).isEqualTo(Duration.ofMillis(1000))
        assertThat(config.enableMetrics).isFalse()
        assertThat(config.apiVersion).isNull()
    }

    @Test
    fun `DockerConfig should accept custom values`() {
        val config = DockerConfig(
            host = "custom-host",
            dockerHost = "tcp://custom:2375",
            tlsVerify = true,
            certPath = "/path/to/certs",
            connectionTimeout = Duration.ofSeconds(60),
            readTimeout = Duration.ofSeconds(120),
            writeTimeout = Duration.ofSeconds(90),
            maxRetries = 5,
            retryDelay = Duration.ofMillis(2000),
            enableMetrics = true,
            apiVersion = "1.41"
        )

        assertThat(config.host).isEqualTo("custom-host")
        assertThat(config.dockerHost).isEqualTo("tcp://custom:2375")
        assertThat(config.tlsVerify).isTrue()
        assertThat(config.certPath).isEqualTo("/path/to/certs")
        assertThat(config.connectionTimeout).isEqualTo(Duration.ofSeconds(60))
        assertThat(config.readTimeout).isEqualTo(Duration.ofSeconds(120))
        assertThat(config.writeTimeout).isEqualTo(Duration.ofSeconds(90))
        assertThat(config.maxRetries).isEqualTo(5)
        assertThat(config.retryDelay).isEqualTo(Duration.ofMillis(2000))
        assertThat(config.enableMetrics).isTrue()
        assertThat(config.apiVersion).isEqualTo("1.41")
    }

    @Test
    fun `DockerConfig copy should work correctly`() {
        val original = DockerConfig(host = "original", maxRetries = 3)
        val copied = original.copy(host = "copied", maxRetries = 5)

        assertThat(original.host).isEqualTo("original")
        assertThat(original.maxRetries).isEqualTo(3)
        assertThat(copied.host).isEqualTo("copied")
        assertThat(copied.maxRetries).isEqualTo(5)
    }

    // ==================== fromEnvironment 测试 ====================

    @Test
    fun `fromEnvironment should create config with environment values`() {
        // Note: This test uses the actual environment variables
        val config = DockerConfig.fromEnvironment()

        // Just verify it creates a valid config
        assertThat(config).isNotNull()
    }

    // ==================== default 测试 ====================

    @Test
    fun `default should create config with appropriate dockerHost`() {
        val config = DockerConfig.default()

        assertThat(config.dockerHost).isNotNull()
        assertThat(config.host).isEqualTo("localhost")
    }

    // ==================== tcp 测试 ====================

    @Test
    fun `tcp should create TCP config with default values`() {
        val config = DockerConfig.tcp()

        assertThat(config.host).isEqualTo("localhost")
        assertThat(config.dockerHost).isEqualTo("http://localhost:2375")
        assertThat(config.tlsVerify).isFalse()
    }

    @Test
    fun `tcp should create TLS config when tls is true`() {
        val config = DockerConfig.tcp(host = "secure-host", port = 2376, tls = true)

        assertThat(config.host).isEqualTo("secure-host")
        assertThat(config.dockerHost).isEqualTo("https://secure-host:2376")
        assertThat(config.tlsVerify).isTrue()
    }

    @Test
    fun `tcp should accept custom host and port`() {
        val config = DockerConfig.tcp(host = "192.168.1.100", port = 5000)

        assertThat(config.host).isEqualTo("192.168.1.100")
        assertThat(config.dockerHost).isEqualTo("http://192.168.1.100:5000")
    }

    // ==================== unixSocket 测试 ====================

    @Test
    fun `unixSocket should create Unix socket config with default path`() {
        val config = DockerConfig.unixSocket()

        assertThat(config.dockerHost).isEqualTo("unix:///var/run/docker.sock")
    }

    @Test
    fun `unixSocket should accept custom socket path`() {
        val config = DockerConfig.unixSocket("/custom/docker.sock")

        assertThat(config.dockerHost).isEqualTo("/custom/docker.sock")
    }

    // ==================== getEffectiveDockerHost 测试 ====================

    @Test
    fun `getEffectiveDockerHost should return dockerHost when set`() {
        val config = DockerConfig(dockerHost = "tcp://custom:2375")

        assertThat(config.getEffectiveDockerHost()).isEqualTo("tcp://custom:2375")
    }

    @Test
    fun `getEffectiveDockerHost should return default when dockerHost is null`() {
        val config = DockerConfig(dockerHost = null)

        val effectiveHost = config.getEffectiveDockerHost()

        // Should return either Unix socket or Windows pipe based on OS
        assertThat(effectiveHost).isNotNull()
        assertThat(effectiveHost).isNotEmpty()
    }

    // ==================== isTlsEnabled 测试 ====================

    @Test
    fun `isTlsEnabled should return true when tlsVerify is true`() {
        val config = DockerConfig(tlsVerify = true)

        assertThat(config.isTlsEnabled()).isTrue()
    }

    @Test
    fun `isTlsEnabled should return true when dockerHost uses https`() {
        val config = DockerConfig(dockerHost = "https://secure:2376")

        assertThat(config.isTlsEnabled()).isTrue()
    }

    @Test
    fun `isTlsEnabled should return false when no TLS`() {
        val config = DockerConfig(dockerHost = "http://insecure:2375", tlsVerify = false)

        assertThat(config.isTlsEnabled()).isFalse()
    }

    // ==================== validate 测试 ====================

    @Test
    fun `validate should pass for valid config`() {
        val config = DockerConfig()

        // Should not throw
        config.validate()
    }

    @Test
    fun `validate should throw when maxRetries is negative`() {
        val config = DockerConfig(maxRetries = -1)

        val exception = assertThrows<IllegalArgumentException> {
            config.validate()
        }

        assertThat(exception.message).contains("maxRetries")
    }

    @Test
    fun `validate should throw when connectionTimeout is negative`() {
        val config = DockerConfig(connectionTimeout = Duration.ofSeconds(-1))

        val exception = assertThrows<IllegalArgumentException> {
            config.validate()
        }

        assertThat(exception.message).contains("connectionTimeout")
    }

    @Test
    fun `validate should throw when readTimeout is negative`() {
        val config = DockerConfig(readTimeout = Duration.ofSeconds(-1))

        val exception = assertThrows<IllegalArgumentException> {
            config.validate()
        }

        assertThat(exception.message).contains("readTimeout")
    }

    @Test
    fun `validate should throw when writeTimeout is negative`() {
        val config = DockerConfig(writeTimeout = Duration.ofSeconds(-1))

        val exception = assertThrows<IllegalArgumentException> {
            config.validate()
        }

        assertThat(exception.message).contains("writeTimeout")
    }

    @Test
    fun `validate should throw when retryDelay is negative`() {
        val config = DockerConfig(retryDelay = Duration.ofMillis(-1))

        val exception = assertThrows<IllegalArgumentException> {
            config.validate()
        }

        assertThat(exception.message).contains("retryDelay")
    }

    @Test
    fun `validate should throw when tlsVerify is true but certPath is null`() {
        val config = DockerConfig(tlsVerify = true, certPath = null)

        val exception = assertThrows<DockerConfigException> {
            config.validate()
        }

        assertThat(exception.message).contains("certPath")
    }

    @Test
    fun `validate should pass when tlsVerify is true and certPath is set`() {
        val config = DockerConfig(tlsVerify = true, certPath = "/path/to/certs")

        // Should not throw
        config.validate()
    }

    // ==================== Builder 测试 ====================

    @Test
    fun `Builder should create default config`() {
        val config = DockerConfig.Builder().build()

        assertThat(config.host).isEqualTo("localhost")
        assertThat(config.tlsVerify).isFalse()
    }

    @Test
    fun `Builder should set all properties`() {
        val config = DockerConfig.Builder()
            .host("builder-host")
            .dockerHost("tcp://builder:2375")
            .tlsVerify(true)
            .certPath("/builder/certs")
            .connectionTimeout(Duration.ofSeconds(45))
            .readTimeout(Duration.ofSeconds(90))
            .writeTimeout(Duration.ofSeconds(75))
            .maxRetries(7)
            .retryDelay(Duration.ofMillis(1500))
            .enableMetrics(true)
            .apiVersion("1.42")
            .build()

        assertThat(config.host).isEqualTo("builder-host")
        assertThat(config.dockerHost).isEqualTo("tcp://builder:2375")
        assertThat(config.tlsVerify).isTrue()
        assertThat(config.certPath).isEqualTo("/builder/certs")
        assertThat(config.connectionTimeout).isEqualTo(Duration.ofSeconds(45))
        assertThat(config.readTimeout).isEqualTo(Duration.ofSeconds(90))
        assertThat(config.writeTimeout).isEqualTo(Duration.ofSeconds(75))
        assertThat(config.maxRetries).isEqualTo(7)
        assertThat(config.retryDelay).isEqualTo(Duration.ofMillis(1500))
        assertThat(config.enableMetrics).isTrue()
        assertThat(config.apiVersion).isEqualTo("1.42")
    }

    @Test
    fun `Builder should create from existing config`() {
        val original = DockerConfig(host = "original", maxRetries = 5)
        val config = DockerConfig.Builder(original).build()

        assertThat(config.host).isEqualTo("original")
        assertThat(config.maxRetries).isEqualTo(5)
    }

    @Test
    fun `toBuilder should create builder from config`() {
        val original = DockerConfig(host = "to-builder", maxRetries = 8)
        val config = original.toBuilder().build()

        assertThat(config.host).isEqualTo("to-builder")
        assertThat(config.maxRetries).isEqualTo(8)
    }

    @Test
    fun `Builder should allow modifying existing config`() {
        val original = DockerConfig(host = "original", maxRetries = 3)
        val config = original.toBuilder()
            .host("modified")
            .maxRetries(10)
            .build()

        assertThat(original.host).isEqualTo("original")
        assertThat(original.maxRetries).isEqualTo(3)
        assertThat(config.host).isEqualTo("modified")
        assertThat(config.maxRetries).isEqualTo(10)
    }

    // ==================== RegistryConfig 测试 ====================

    @Test
    fun `RegistryConfig should have default values`() {
        val registryConfig = RegistryConfig()

        assertThat(registryConfig.registries).isEmpty()
        assertThat(registryConfig.defaultRegistry).isEqualTo("docker.io")
        assertThat(registryConfig.insecureRegistries).isEmpty()
        assertThat(registryConfig.mirrorRegistry).isNull()
    }

    @Test
    fun `RegistryConfig DOCKER_HUB constant should be docker dot io`() {
        assertThat(RegistryConfig.DOCKER_HUB).isEqualTo("docker.io")
    }

    @Test
    fun `RegistryConfig getAuth should return auth for existing registry`() {
        val auth = RegistryAuth("user", "pass")
        val config = RegistryConfig(registries = mapOf("docker.io" to auth))

        assertThat(config.getAuth("docker.io")).isEqualTo(auth)
    }

    @Test
    fun `RegistryConfig getAuth should return null for non-existing registry`() {
        val config = RegistryConfig()

        assertThat(config.getAuth("non-existing")).isNull()
    }

    @Test
    fun `RegistryConfig getAuth should normalize registry URL`() {
        val auth = RegistryAuth("user", "pass")
        val config = RegistryConfig(registries = mapOf("registry.example.com" to auth))

        assertThat(config.getAuth("https://registry.example.com")).isEqualTo(auth)
        assertThat(config.getAuth("http://registry.example.com/")).isEqualTo(auth)
    }

    @Test
    fun `RegistryConfig withAuth should add new auth`() {
        val auth = RegistryAuth("user", "pass")
        val config = RegistryConfig()

        val newConfig = config.withAuth("my-registry", auth)

        assertThat(config.registries).isEmpty()
        assertThat(newConfig.registries).hasSize(1)
        assertThat(newConfig.registries["my-registry"]).isEqualTo(auth)
    }

    @Test
    fun `RegistryConfig withInsecureRegistry should add insecure registry`() {
        val config = RegistryConfig()

        val newConfig = config.withInsecureRegistry("insecure.local")

        assertThat(config.insecureRegistries).isEmpty()
        assertThat(newConfig.insecureRegistries).hasSize(1)
        assertThat(newConfig.insecureRegistries).contains("insecure.local")
    }

    @Test
    fun `RegistryConfig copy should work correctly`() {
        val original = RegistryConfig(defaultRegistry = "original.io")
        val copied = original.copy(defaultRegistry = "copied.io")

        assertThat(original.defaultRegistry).isEqualTo("original.io")
        assertThat(copied.defaultRegistry).isEqualTo("copied.io")
    }

    // ==================== ProxyConfig 测试 ====================

    @Test
    fun `ProxyConfig should have default values`() {
        val proxyConfig = ProxyConfig()

        assertThat(proxyConfig.httpProxy).isNull()
        assertThat(proxyConfig.httpsProxy).isNull()
        assertThat(proxyConfig.noProxy).isEmpty()
    }

    @Test
    fun `ProxyConfig should accept custom values`() {
        val proxyConfig = ProxyConfig(
            httpProxy = "http://proxy:8080",
            httpsProxy = "https://proxy:8443",
            noProxy = listOf("localhost", "internal.local")
        )

        assertThat(proxyConfig.httpProxy).isEqualTo("http://proxy:8080")
        assertThat(proxyConfig.httpsProxy).isEqualTo("https://proxy:8443")
        assertThat(proxyConfig.noProxy).hasSize(2)
    }

    @Test
    fun `ProxyConfig fromEnvironment should create config from env`() {
        // Note: This test uses the actual environment variables
        val config = ProxyConfig.fromEnvironment()

        assertThat(config).isNotNull()
    }

    @Test
    fun `ProxyConfig shouldUseProxy should return false when no proxy configured`() {
        val proxyConfig = ProxyConfig()

        assertThat(proxyConfig.shouldUseProxy("http://example.com")).isFalse()
    }

    @Test
    fun `ProxyConfig shouldUseProxy should return true for non-excluded hosts`() {
        val proxyConfig = ProxyConfig(
            httpProxy = "http://proxy:8080",
            noProxy = listOf("localhost")
        )

        assertThat(proxyConfig.shouldUseProxy("http://example.com")).isTrue()
    }

    @Test
    fun `ProxyConfig shouldUseProxy should return false for excluded hosts`() {
        val proxyConfig = ProxyConfig(
            httpProxy = "http://proxy:8080",
            noProxy = listOf("localhost", "internal.local")
        )

        assertThat(proxyConfig.shouldUseProxy("http://localhost")).isFalse()
        assertThat(proxyConfig.shouldUseProxy("http://internal.local")).isFalse()
    }

    @Test
    fun `ProxyConfig shouldUseProxy should support wildcard`() {
        val proxyConfig = ProxyConfig(
            httpProxy = "http://proxy:8080",
            noProxy = listOf("*")
        )

        assertThat(proxyConfig.shouldUseProxy("http://any.host")).isFalse()
    }

    @Test
    fun `ProxyConfig shouldUseProxy should support subdomain matching`() {
        // Note: Source code uses host.endsWith(".$it"), so noProxy entries should NOT have leading dot
        val proxyConfig = ProxyConfig(
            httpProxy = "http://proxy:8080",
            noProxy = listOf("example.com")
        )

        // With "example.com" in noProxy, subdomains like "api.example.com" match via host.endsWith(".example.com")
        assertThat(proxyConfig.shouldUseProxy("http://api.example.com")).isFalse()
        assertThat(proxyConfig.shouldUseProxy("http://www.example.com")).isFalse()
        // Exact match for "example.com"
        assertThat(proxyConfig.shouldUseProxy("http://example.com")).isFalse()
        // Other domains should use proxy
        assertThat(proxyConfig.shouldUseProxy("http://other.com")).isTrue()
    }

    @Test
    fun `ProxyConfig copy should work correctly`() {
        val original = ProxyConfig(httpProxy = "http://original:8080")
        val copied = original.copy(httpProxy = "http://copied:8080")

        assertThat(original.httpProxy).isEqualTo("http://original:8080")
        assertThat(copied.httpProxy).isEqualTo("http://copied:8080")
    }

    // ==================== LogConfig 测试 ====================

    @Test
    fun `LogConfig should have default values`() {
        val logConfig = LogConfig()

        assertThat(logConfig.enableLogging).isTrue()
        assertThat(logConfig.logLevel).isEqualTo(LogConfig.LogLevel.INFO)
        assertThat(logConfig.logRequests).isTrue()
        assertThat(logConfig.logResponses).isFalse()
        assertThat(logConfig.maxLogLength).isEqualTo(1000)
    }

    @Test
    fun `LogConfig should accept custom values`() {
        val logConfig = LogConfig(
            enableLogging = false,
            logLevel = LogConfig.LogLevel.DEBUG,
            logRequests = false,
            logResponses = true,
            maxLogLength = 5000
        )

        assertThat(logConfig.enableLogging).isFalse()
        assertThat(logConfig.logLevel).isEqualTo(LogConfig.LogLevel.DEBUG)
        assertThat(logConfig.logRequests).isFalse()
        assertThat(logConfig.logResponses).isTrue()
        assertThat(logConfig.maxLogLength).isEqualTo(5000)
    }

    @Test
    fun `LogConfig LogLevel should contain all expected levels`() {
        val expectedLevels = listOf(
            LogConfig.LogLevel.TRACE,
            LogConfig.LogLevel.DEBUG,
            LogConfig.LogLevel.INFO,
            LogConfig.LogLevel.WARN,
            LogConfig.LogLevel.ERROR,
            LogConfig.LogLevel.NONE
        )

        assertThat(LogConfig.LogLevel.entries.size).isEqualTo(expectedLevels.size)
        expectedLevels.forEach { level ->
            assertThat(LogConfig.LogLevel.entries.contains(level)).isTrue()
        }
    }

    @Test
    fun `LogConfig copy should work correctly`() {
        val original = LogConfig(logLevel = LogConfig.LogLevel.INFO)
        val copied = original.copy(logLevel = LogConfig.LogLevel.DEBUG)

        assertThat(original.logLevel).isEqualTo(LogConfig.LogLevel.INFO)
        assertThat(copied.logLevel).isEqualTo(LogConfig.LogLevel.DEBUG)
    }

    // ==================== ConnectionPoolConfig 测试 ====================

    @Test
    fun `ConnectionPoolConfig should have default values`() {
        val poolConfig = ConnectionPoolConfig()

        assertThat(poolConfig.maxConnections).isEqualTo(100)
        assertThat(poolConfig.keepAliveDuration).isEqualTo(Duration.ofMinutes(5))
        assertThat(poolConfig.connectionIdleTimeout).isEqualTo(Duration.ofMinutes(10))
    }

    @Test
    fun `ConnectionPoolConfig should accept custom values`() {
        val poolConfig = ConnectionPoolConfig(
            maxConnections = 50,
            keepAliveDuration = Duration.ofMinutes(3),
            connectionIdleTimeout = Duration.ofMinutes(5)
        )

        assertThat(poolConfig.maxConnections).isEqualTo(50)
        assertThat(poolConfig.keepAliveDuration).isEqualTo(Duration.ofMinutes(3))
        assertThat(poolConfig.connectionIdleTimeout).isEqualTo(Duration.ofMinutes(5))
    }

    @Test
    fun `ConnectionPoolConfig should throw when maxConnections is zero`() {
        val exception = assertThrows<IllegalArgumentException> {
            ConnectionPoolConfig(maxConnections = 0)
        }

        assertThat(exception.message).contains("maxConnections")
    }

    @Test
    fun `ConnectionPoolConfig should throw when maxConnections is negative`() {
        val exception = assertThrows<IllegalArgumentException> {
            ConnectionPoolConfig(maxConnections = -1)
        }

        assertThat(exception.message).contains("maxConnections")
    }

    @Test
    fun `ConnectionPoolConfig copy should work correctly`() {
        val original = ConnectionPoolConfig(maxConnections = 100)
        val copied = original.copy(maxConnections = 50)

        assertThat(original.maxConnections).isEqualTo(100)
        assertThat(copied.maxConnections).isEqualTo(50)
    }

    // ==================== 辅助函数 ====================

    /**
     * 断言抛出异常的辅助函数
     */
    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
        try {
            block()
            throw AssertionError("Expected ${T::class.simpleName} to be thrown")
        } catch (e: Throwable) {
            if (e is T) return e
            throw AssertionError("Expected ${T::class.simpleName} but got ${e::class.simpleName}")
        }
    }
}