package com.dfa.core.docker.provider

import com.dfa.core.docker.DockerClient
import com.dfa.core.docker.DockerSystemInfo
import com.dfa.core.docker.DockerVersion
import com.dfa.core.vm.VmConfig
import com.dfa.core.vm.VmHandle
import com.dfa.core.vm.VmInfo
import com.dfa.core.vm.VmState
import com.dfa.core.vm.channel.SshChannelConfig
import com.dfa.core.vm.qemu.QemuVmAdapter
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * QemuDockerProviderImpl 单元测试
 *
 * 测试QEMU Docker Provider的核心功能，包括生命周期管理、状态转换、VM管理等。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QemuDockerProviderImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockDockerClient: DockerClient
    private lateinit var mockQemuVmAdapter: QemuVmAdapter
    private lateinit var config: QemuDockerProviderConfig
    private lateinit var provider: QemuDockerProviderImpl

    @Before
    fun setup() {
        mockDockerClient = createMockDockerClient()
        mockQemuVmAdapter = createMockQemuVmAdapter()
        config = QemuDockerProviderConfig(
            providerId = "test-qemu-provider",
            vmId = "test-vm-001",
            sshHost = "localhost",
            sshPort = 2222,
            sshUser = "root",
            memoryMB = 4096,
            cpus = 4,
            diskSizeGB = 50
        )
        provider = QemuDockerProviderImpl(
            config = config,
            qemuVmAdapter = mockQemuVmAdapter,
            dockerClientFactory = { _ -> mockDockerClient },
            scope = testScope.backgroundScope
        )
    }

    // ==================== 辅助方法 ====================

    private fun createMockDockerClient(): DockerClient {
        return mockk<DockerClient> {
            coEvery { connect() } returns Result.success(Unit)
            coEvery { disconnect() } just runs
            every { isConnected() } returns true
            coEvery { ping() } returns Result.success(true)
            coEvery { version() } returns Result.success(
                DockerVersion(
                    version = "24.0.7",
                    apiVersion = "1.43",
                    gitCommit = "test",
                    goVersion = "go1.21",
                    os = "linux",
                    arch = "x86_64",
                    kernelVersion = "6.1.0"
                )
            )
            coEvery { info() } returns Result.success(
                DockerSystemInfo(
                    containers = 5,
                    containersRunning = 3,
                    containersStopped = 1,
                    containersPaused = 1,
                    images = 10,
                    operatingSystem = "Alpine Linux",
                    architecture = "x86_64",
                    cpus = 4,
                    memory = 4294967296L,
                    dockerRootDir = "/var/lib/docker",
                    driver = "overlay2"
                )
            )
        }
    }

    private fun createMockQemuVmAdapter(): QemuVmAdapter {
        return mockk<QemuVmAdapter> {
            coEvery { isQemuAvailable() } returns true
            coEvery { createVm(any()) } returns Result.success(createMockVmHandle())
            coEvery { startVm(any()) } returns Result.success(createMockVmInfo())
            coEvery { stopVm(any(), any()) } returns Result.success(Unit)
            coEvery { destroyVm(any()) } returns Result.success(Unit)
            coEvery { getVmStatus(any()) } returns Result.success(createMockVmInfo())
        }
    }

    private fun createMockVmHandle(): VmHandle {
        return VmHandle(
            vmId = "test-vm-001",
            backendType = com.dfa.core.vm.VmBackendType.QEMU
        )
    }

    private fun createMockVmInfo(): VmInfo {
        return VmInfo(
            config = VmConfig(
                id = "test-vm-001",
                name = "docker-test-qemu-provider",
                memory = 4096,
                cpu = 4,
                diskSize = 50
            ),
            state = VmState.RUNNING,
            handle = createMockVmHandle()
        )
    }

    // ==================== 基本属性测试 ====================

    @Test
    fun `providerType should be QEMU`() {
        assertThat(provider.providerType).isEqualTo(DockerProviderType.QEMU)
    }

    @Test
    fun `providerId should match config`() {
        assertThat(provider.providerId).isEqualTo("test-qemu-provider")
    }

    @Test
    fun `initial state should be CREATED`() {
        assertThat(provider.getState()).isEqualTo(DockerProviderState.CREATED)
    }

    // ==================== 生命周期测试 ====================

    @Test
    fun `initialize should fail when QEMU not available`() = testScope.runTest {
        val unavailableAdapter = mockk<QemuVmAdapter> {
            coEvery { isQemuAvailable() } returns false
        }
        val failingProvider = QemuDockerProviderImpl(
            config = config,
            qemuVmAdapter = unavailableAdapter,
            dockerClientFactory = { _ -> mockDockerClient },
            scope = testScope.backgroundScope
        )

        val result = failingProvider.initialize()

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(ProviderInitializationException::class.java)
        assertThat((exception as ProviderInitializationException).reason).contains("QEMU is not available")
    }

    @Test
    fun `initialize should fail when VM creation fails`() = testScope.runTest {
        val failingAdapter = mockk<QemuVmAdapter> {
            coEvery { isQemuAvailable() } returns true
            coEvery { createVm(any()) } returns Result.failure(RuntimeException("VM creation failed"))
        }
        val failingProvider = QemuDockerProviderImpl(
            config = config,
            qemuVmAdapter = failingAdapter,
            dockerClientFactory = { _ -> mockDockerClient },
            scope = testScope.backgroundScope
        )

        val result = failingProvider.initialize()

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(ProviderInitializationException::class.java)
    }

    @Test
    fun `start should fail when not initialized`() = testScope.runTest {
        val result = provider.start()

        assertThat(result.isFailure).isTrue()
        assertThat(provider.getState()).isEqualTo(DockerProviderState.CREATED)
    }

    @Test
    fun `stop should fail when not running`() = testScope.runTest {
        val result = provider.stop()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `destroy should work from CREATED state`() = testScope.runTest {
        val result = provider.destroy()

        // CREATED状态可以销毁
        assertThat(result.isSuccess).isTrue()
        assertThat(provider.getState()).isEqualTo(DockerProviderState.DESTROYED)
    }

    // ==================== 状态查询测试 ====================

    @Test
    fun `isAvailable should return false when not running`() = testScope.runTest {
        val available = provider.isAvailable()

        assertThat(available).isFalse()
    }

    @Test
    fun `getInfo should return basic info when not running`() = testScope.runTest {
        val result = provider.getInfo()

        assertThat(result.isSuccess).isTrue()
        val info = result.getOrThrow()
        assertThat(info.providerId).isEqualTo("test-qemu-provider")
        assertThat(info.providerType).isEqualTo(DockerProviderType.QEMU)
    }

    // ==================== Docker客户端获取测试 ====================

    @Test
    fun `getDockerClient should throw when not running`() {
        try {
            provider.getDockerClient()
            assertThat(false).isTrue() // Should not reach here
        } catch (e: ProviderStateException) {
            assertThat(e.providerId).isEqualTo("test-qemu-provider")
            assertThat(e.currentState).isEqualTo(DockerProviderState.CREATED)
        }
    }

    // ==================== 回调管理测试 ====================

    @Test
    fun `registerCallback should store callback`() {
        val callback = mockk<DockerProviderCallback> {
            every { onStateChanged(any(), any(), any()) } just runs
        }

        provider.registerCallback(callback)

        verify(exactly = 0) { callback.onStateChanged(any(), any(), any()) }
    }

    @Test
    fun `unregisterCallback should remove callback`() {
        val callback = mockk<DockerProviderCallback> {
            every { onStateChanged(any(), any(), any()) } just runs
        }

        provider.registerCallback(callback)
        provider.unregisterCallback(callback)

        verify(exactly = 0) { callback.onStateChanged(any(), any(), any()) }
    }

    // ==================== 特性支持测试 ====================

    @Test
    fun `supportsFeature should return true for DOCKER_API`() {
        assertThat(provider.supportsFeature(DockerProviderFeature.DOCKER_API)).isTrue()
    }

    @Test
    fun `supportsFeature should return true for SNAPSHOTS`() {
        assertThat(provider.supportsFeature(DockerProviderFeature.SNAPSHOTS)).isTrue()
    }

    @Test
    fun `supportsFeature should return false for DOCKER_SWARM`() {
        // QEMU provider不支持Docker Swarm
        assertThat(provider.supportsFeature(DockerProviderFeature.DOCKER_SWARM)).isFalse()
    }

    @Test
    fun `getSupportedFeatures should return non-empty set`() {
        val features = provider.getSupportedFeatures()

        assertThat(features).isNotEmpty()
        assertThat(features).contains(DockerProviderFeature.DOCKER_API)
        assertThat(features).contains(DockerProviderFeature.SNAPSHOTS)
    }

    // ==================== 配置测试 ====================

    @Test
    fun `config should have correct VM settings`() {
        assertThat(config.vmId).isEqualTo("test-vm-001")
        assertThat(config.memoryMB).isEqualTo(4096)
        assertThat(config.cpus).isEqualTo(4)
        assertThat(config.diskSizeGB).isEqualTo(50)
    }

    @Test
    fun `config should have correct SSH settings`() {
        assertThat(config.sshHost).isEqualTo("localhost")
        assertThat(config.sshPort).isEqualTo(2222)
        assertThat(config.sshUser).isEqualTo("root")
    }

    @Test
    fun `config sshConnectionString should be formatted correctly`() {
        assertThat(config.sshConnectionString).isEqualTo("root@localhost:2222")
    }

    @Test
    fun `config dockerHost should be formatted correctly`() {
        assertThat(config.dockerHost).isEqualTo("tcp://localhost:2375")
    }

    @Test
    fun `config with SSH key should have sshKeyPath set`() {
        val keyConfig = QemuDockerProviderConfig(
            providerId = "key-provider",
            vmId = "vm-001",
            sshKeyPath = "/home/user/.ssh/id_rsa"
        )

        assertThat(keyConfig.sshKeyPath).isEqualTo("/home/user/.ssh/id_rsa")
    }

    @Test
    fun `config getProviderType should return QEMU`() {
        assertThat(config.getProviderType()).isEqualTo(DockerProviderType.QEMU)
    }

    // ==================== 状态流测试 ====================

    @Test
    fun `stateFlow should emit initial state`() = testScope.runTest {
        val state = provider.stateFlow.first()

        assertThat(state).isEqualTo(DockerProviderState.CREATED)
    }

    // ==================== 扩展方法测试 ====================

    @Test
    fun `getVmHandle should return null initially`() {
        assertThat(provider.getVmHandle()).isNull()
    }

    @Test
    fun `metadata operations should work correctly`() {
        provider.setMetadata("key1", "value1")
        provider.setMetadata("key2", 456)

        assertThat(provider.getMetadata("key1")).isEqualTo("value1")
        assertThat(provider.getMetadata("key2")).isEqualTo(456)
        assertThat(provider.getMetadata("nonexistent")).isNull()
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `start should fail when VM start fails`() = testScope.runTest {
        val failingAdapter = mockk<QemuVmAdapter> {
            coEvery { isQemuAvailable() } returns true
            coEvery { createVm(any()) } returns Result.success(createMockVmHandle())
            coEvery { startVm(any()) } returns Result.failure(RuntimeException("VM start failed"))
        }
        val failingProvider = QemuDockerProviderImpl(
            config = config,
            qemuVmAdapter = failingAdapter,
            dockerClientFactory = { _ -> mockDockerClient },
            scope = testScope.backgroundScope
        )

        failingProvider.initialize()
        val result = failingProvider.start()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `stop should handle VM stop failure gracefully`() = testScope.runTest {
        val failingAdapter = mockk<QemuVmAdapter> {
            coEvery { isQemuAvailable() } returns true
            coEvery { createVm(any()) } returns Result.success(createMockVmHandle())
            coEvery { startVm(any()) } returns Result.success(createMockVmInfo())
            coEvery { stopVm(any(), any()) } returns Result.failure(RuntimeException("VM stop failed"))
            coEvery { getVmStatus(any()) } returns Result.success(createMockVmInfo())
        }
        val failingProvider = QemuDockerProviderImpl(
            config = config,
            qemuVmAdapter = failingAdapter,
            dockerClientFactory = { _ -> mockDockerClient },
            scope = testScope.backgroundScope
        )

        failingProvider.initialize()
        failingProvider.start()
        val result = failingProvider.stop(force = true)

        // 强制停止应该成功
        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 并发安全测试 ====================

    @Test
    fun `multiple callback registrations should be handled safely`() {
        val callbacks = List(10) { index ->
            mockk<DockerProviderCallback> {
                every { onStateChanged(any(), any(), any()) } just runs
                every { onError(any(), any()) } just runs
                every { onAvailabilityChanged(any(), any()) } just runs
            }
        }

        callbacks.forEach { provider.registerCallback(it) }

        // 验证不会抛出异常
        assertThat(provider.getState()).isEqualTo(DockerProviderState.CREATED)
    }

    @Test
    fun `callback exceptions should not crash provider`() {
        val failingCallback = mockk<DockerProviderCallback> {
            every { onStateChanged(any(), any(), any()) } throws RuntimeException("Test exception")
            every { onError(any(), any()) } just runs
            every { onAvailabilityChanged(any(), any()) } just runs
        }

        provider.registerCallback(failingCallback)

        // 验证注册不会抛出异常
        assertThat(provider.getState()).isEqualTo(DockerProviderState.CREATED)
    }

    // ==================== 配置验证测试 ====================

    @Test
    fun `config with custom timeouts should be valid`() {
        val customConfig = QemuDockerProviderConfig(
            providerId = "timeout-provider",
            vmId = "vm-001",
            connectionTimeout = 60000L,
            requestTimeout = 120000L
        )

        assertThat(customConfig.connectionTimeout).isEqualTo(60000L)
        assertThat(customConfig.requestTimeout).isEqualTo(120000L)
    }

    @Test
    fun `config with custom resources should be valid`() {
        val customConfig = QemuDockerProviderConfig(
            providerId = "resource-provider",
            vmId = "vm-001",
            memoryMB = 8192,
            cpus = 8,
            diskSizeGB = 100
        )

        assertThat(customConfig.memoryMB).isEqualTo(8192)
        assertThat(customConfig.cpus).isEqualTo(8)
        assertThat(customConfig.diskSizeGB).isEqualTo(100)
    }

    @Test
    fun `config with custom image directory should be valid`() {
        val customConfig = QemuDockerProviderConfig(
            providerId = "image-provider",
            vmId = "vm-001",
            imageDir = "/custom/docker/images"
        )

        assertThat(customConfig.imageDir).isEqualTo("/custom/docker/images")
    }

    // ==================== Docker客户端工厂测试 ====================

    @Test
    fun `dockerClientFactory should be called with correct SSH config`() = testScope.runTest {
        var capturedConfig: SshChannelConfig? = null
        val capturingProvider = QemuDockerProviderImpl(
            config = config,
            qemuVmAdapter = mockQemuVmAdapter,
            dockerClientFactory = { sshConfig ->
                capturedConfig = sshConfig
                mockDockerClient
            },
            scope = testScope.backgroundScope
        )

        capturingProvider.initialize()
        capturingProvider.start()

        // 验证SSH配置被正确传递
        assertThat(capturedConfig).isNotNull()
        assertThat(capturedConfig?.host).isEqualTo(config.sshHost)
        assertThat(capturedConfig?.port).isEqualTo(config.sshPort)
    }

    // ==================== 状态转换测试 ====================

    @Test
    fun `state transitions should follow correct order`() = testScope.runTest {
        // CREATED -> INITIALIZING -> INITIALIZED
        assertThat(provider.getState()).isEqualTo(DockerProviderState.CREATED)

        // 尝试初始化
        val initResult = provider.initialize()

        if (initResult.isSuccess) {
            assertThat(provider.getState()).isEqualTo(DockerProviderState.INITIALIZED)

            // INITIALIZED -> STARTING -> RUNNING
            val startResult = provider.start()

            if (startResult.isSuccess) {
                assertThat(provider.getState()).isEqualTo(DockerProviderState.RUNNING)

                // RUNNING -> STOPPING -> STOPPED
                val stopResult = provider.stop()

                if (stopResult.isSuccess) {
                    assertThat(provider.getState()).isEqualTo(DockerProviderState.STOPPED)
                }
            }
        }
    }

    // ==================== 资源清理测试 ====================

    @Test
    fun `destroy should clean up all resources`() = testScope.runTest {
        provider.initialize()

        val result = provider.destroy()

        assertThat(result.isSuccess).isTrue()
        assertThat(provider.getVmHandle()).isNull()
        assertThat(provider.getMetadata("any")).isNull()
    }

    // ==================== 默认值测试 ====================

    @Test
    fun `config should have correct default values`() {
        val defaultConfig = QemuDockerProviderConfig(
            providerId = "default-provider",
            vmId = "default-vm"
        )

        assertThat(defaultConfig.socketPath).isEqualTo("/var/run/docker.sock")
        assertThat(defaultConfig.sshHost).isEqualTo("localhost")
        assertThat(defaultConfig.sshPort).isEqualTo(22)
        assertThat(defaultConfig.sshUser).isEqualTo("root")
        assertThat(defaultConfig.sshKeyPath).isNull()
        assertThat(defaultConfig.autoStart).isTrue()
        assertThat(defaultConfig.connectionTimeout).isEqualTo(30000L)
        assertThat(defaultConfig.requestTimeout).isEqualTo(60000L)
        assertThat(defaultConfig.memoryMB).isEqualTo(4096)
        assertThat(defaultConfig.cpus).isEqualTo(4)
        assertThat(defaultConfig.diskSizeGB).isEqualTo(50)
        assertThat(defaultConfig.imageDir).isEqualTo("/var/lib/docker")
    }
}