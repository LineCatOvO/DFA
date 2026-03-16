package com.dfa.core.docker.provider

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * DockerProviderManager 单元测试
 *
 * 测试DockerProviderManager的Provider注册、注销、切换和查询功能。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DockerProviderManagerTest {

    private lateinit var manager: DockerProviderManagerImpl
    private lateinit var mockProvider: DockerProvider
    private lateinit var mockProvider2: DockerProvider
    private lateinit var mockFactory: DockerProviderFactory

    @Before
    fun setup() {
        manager = DockerProviderManagerImpl()
        
        // 创建Mock Provider
        mockProvider = createMockProvider("provider-1", DockerProviderType.LOCAL)
        mockProvider2 = createMockProvider("provider-2", DockerProviderType.QEMU)
        
        // 创建Mock Factory
        mockFactory = mockk<DockerProviderFactory> {
            every { supportedProviderType } returns DockerProviderType.LOCAL
            every { getPriority() } returns 0
            every { validateConfig(any()) } returns emptyList()
            coEvery { isProviderAvailable() } returns true
            coEvery { create(any()) } returns Result.success(mockProvider)
        }
        
        // 注册工厂
        DockerProviderFactoryRegistry.register(mockFactory)
    }

    // ==================== 辅助方法 ====================

    private fun createMockProvider(
        providerId: String,
        type: DockerProviderType,
        state: DockerProviderState = DockerProviderState.CREATED
    ): DockerProvider {
        return mockk<DockerProvider> {
            every { this@mockk.providerId } returns providerId
            every { this@mockk.providerType } returns type
            every { getState() } returns state
            every { registerCallback(any()) } just runs
            every { unregisterCallback(any()) } just runs
            coEvery { initialize() } returns Result.success(Unit)
            coEvery { start() } returns Result.success(Unit)
            coEvery { stop(any()) } returns Result.success(Unit)
            coEvery { destroy() } returns Result.success(Unit)
            coEvery { isAvailable() } returns true
            coEvery { getInfo() } returns Result.success(
                DockerProviderInfo(
                    providerId = providerId,
                    providerType = type,
                    state = state
                )
            )
            every { getSupportedFeatures() } returns emptySet()
            every { supportsFeature(any()) } returns false
        }
    }

    // ==================== Provider注册测试 ====================

    @Test
    fun `registerProvider should add provider to manager`() = runTest {
        val result = manager.registerProvider(mockProvider)

        assertThat(result.isSuccess).isTrue()
        assertThat(manager.hasProvider("provider-1")).isTrue()
        assertThat(manager.providers).hasSize(1)
    }

    @Test
    fun `registerProvider should fail when provider already exists`() = runTest {
        manager.registerProvider(mockProvider)
        
        val duplicateProvider = createMockProvider("provider-1", DockerProviderType.LOCAL)
        val result = manager.registerProvider(duplicateProvider)

        assertThat(result.isFailure).isTrue()
        assertThat(manager.providers).hasSize(1)
    }

    @Test
    fun `registerProvider should update provider count`() = runTest {
        assertThat(manager.providerCountFlow.first()).isEqualTo(0)

        manager.registerProvider(mockProvider)
        assertThat(manager.providerCountFlow.first()).isEqualTo(1)

        manager.registerProvider(mockProvider2)
        assertThat(manager.providerCountFlow.first()).isEqualTo(2)
    }

    // ==================== Provider注销测试 ====================

    @Test
    fun `unregisterProvider should remove provider from manager`() = runTest {
        manager.registerProvider(mockProvider)
        
        val result = manager.unregisterProvider("provider-1")

        assertThat(result.isSuccess).isTrue()
        assertThat(manager.hasProvider("provider-1")).isFalse()
        assertThat(manager.providers).isEmpty()
    }

    @Test
    fun `unregisterProvider should fail when provider not found`() = runTest {
        val result = manager.unregisterProvider("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `unregisterProvider with destroy should call destroy on provider`() = runTest {
        manager.registerProvider(mockProvider)
        
        val result = manager.unregisterProvider("provider-1", destroy = true)

        assertThat(result.isSuccess).isTrue()
        coVerify { mockProvider.destroy() }
    }

    @Test
    fun `unregisterProvider should clear active provider when unregistering active`() = runTest {
        val runningProvider = createMockProvider(
            "provider-1",
            DockerProviderType.LOCAL,
            DockerProviderState.RUNNING
        )
        manager.registerProvider(runningProvider)
        manager.setActiveProvider("provider-1")
        
        val result = manager.unregisterProvider("provider-1")

        assertThat(result.isSuccess).isTrue()
        assertThat(manager.activeProvider).isNull()
    }

    // ==================== Provider查询测试 ====================

    @Test
    fun `getProvider should return provider when exists`() = runTest {
        manager.registerProvider(mockProvider)
        
        val provider = manager.getProvider("provider-1")

        assertThat(provider).isNotNull()
        assertThat(provider?.providerId).isEqualTo("provider-1")
    }

    @Test
    fun `getProvider should return null when not exists`() = runTest {
        val provider = manager.getProvider("non-existent")

        assertThat(provider).isNull()
    }

    @Test
    fun `listProviders should return all registered providers`() = runTest {
        manager.registerProvider(mockProvider)
        manager.registerProvider(mockProvider2)
        
        val providers = manager.listProviders()

        assertThat(providers).hasSize(2)
        assertThat(providers.map { it.providerId }).containsExactly("provider-1", "provider-2")
    }

    @Test
    fun `listProvidersByType should filter by type`() = runTest {
        manager.registerProvider(mockProvider) // LOCAL
        manager.registerProvider(mockProvider2) // QEMU
        
        val localProviders = manager.listProvidersByType(DockerProviderType.LOCAL)
        val qemuProviders = manager.listProvidersByType(DockerProviderType.QEMU)

        assertThat(localProviders).hasSize(1)
        assertThat(localProviders.first().providerId).isEqualTo("provider-1")
        assertThat(qemuProviders).hasSize(1)
        assertThat(qemuProviders.first().providerId).isEqualTo("provider-2")
    }

    @Test
    fun `hasProvider should return correct result`() = runTest {
        assertThat(manager.hasProvider("provider-1")).isFalse()
        
        manager.registerProvider(mockProvider)
        
        assertThat(manager.hasProvider("provider-1")).isTrue()
        assertThat(manager.hasProvider("non-existent")).isFalse()
    }

    @Test
    fun `listAvailableProviders should return only running and available providers`() = runTest {
        val runningProvider = createMockProvider(
            "running-1",
            DockerProviderType.LOCAL,
            DockerProviderState.RUNNING
        )
        val stoppedProvider = createMockProvider(
            "stopped-1",
            DockerProviderType.LOCAL,
            DockerProviderState.STOPPED
        )
        
        manager.registerProvider(runningProvider)
        manager.registerProvider(stoppedProvider)
        
        val availableProviders = manager.listAvailableProviders()

        assertThat(availableProviders).hasSize(1)
        assertThat(availableProviders.first().providerId).isEqualTo("running-1")
    }

    // ==================== Provider切换测试 ====================

    @Test
    fun `setActiveProvider should set active provider`() = runTest {
        val runningProvider = createMockProvider(
            "provider-1",
            DockerProviderType.LOCAL,
            DockerProviderState.RUNNING
        )
        manager.registerProvider(runningProvider)
        
        val result = manager.setActiveProvider("provider-1")

        assertThat(result.isSuccess).isTrue()
        assertThat(manager.activeProvider?.providerId).isEqualTo("provider-1")
    }

    @Test
    fun `setActiveProvider should fail when provider not found`() = runTest {
        val result = manager.setActiveProvider("non-existent")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `setActiveProvider should start provider if not running`() = runTest {
        val createdProvider = createMockProvider(
            "provider-1",
            DockerProviderType.LOCAL,
            DockerProviderState.INITIALIZED
        )
        every { createdProvider.getState() } returns DockerProviderState.INITIALIZED
        every { createdProvider.getState().canStart() } returns true
        coEvery { createdProvider.start() } returns Result.success(Unit)
        every { createdProvider.getState() } returns DockerProviderState.RUNNING
        
        manager.registerProvider(createdProvider)
        
        val result = manager.setActiveProvider("provider-1")

        assertThat(result.isSuccess).isTrue()
        coVerify { createdProvider.start() }
    }

    @Test
    fun `getActiveProvider should return null initially`() = runTest {
        assertThat(manager.activeProvider).isNull()
    }

    @Test
    fun `hasActiveProvider should return correct result`() = runTest {
        assertThat(manager.hasActiveProvider()).isFalse()
        
        val runningProvider = createMockProvider(
            "provider-1",
            DockerProviderType.LOCAL,
            DockerProviderState.RUNNING
        )
        manager.registerProvider(runningProvider)
        manager.setActiveProvider("provider-1")
        
        assertThat(manager.hasActiveProvider()).isTrue()
    }

    // ==================== 管理器状态测试 ====================

    @Test
    fun `getManagerState should return correct state`() = runTest {
        var state = manager.getManagerState()
        
        assertThat(state.totalProviders).isEqualTo(0)
        assertThat(state.activeProviderId).isNull()
        assertThat(state.hasProviders).isFalse()
        assertThat(state.hasActiveProvider).isFalse()

        val runningProvider = createMockProvider(
            "provider-1",
            DockerProviderType.LOCAL,
            DockerProviderState.RUNNING
        )
        manager.registerProvider(runningProvider)
        manager.setActiveProvider("provider-1")
        
        state = manager.getManagerState()
        assertThat(state.totalProviders).isEqualTo(1)
        assertThat(state.activeProviderId).isEqualTo("provider-1")
        assertThat(state.hasProviders).isTrue()
        assertThat(state.hasActiveProvider).isTrue()
    }

    @Test
    fun `getManagerState summary should contain correct information`() = runTest {
        val runningProvider = createMockProvider(
            "provider-1",
            DockerProviderType.LOCAL,
            DockerProviderState.RUNNING
        )
        manager.registerProvider(runningProvider)
        manager.setActiveProvider("provider-1")
        
        val summary = manager.getManagerState().summary

        assertThat(summary).contains("total=1")
        assertThat(summary).contains("active=provider-1")
        assertThat(summary).contains("RUNNING")
    }

    // ==================== 批量操作测试 ====================

    @Test
    fun `initializeAll should initialize all providers`() = runTest {
        val createdProvider1 = createMockProvider(
            "provider-1",
            DockerProviderType.LOCAL,
            DockerProviderState.CREATED
        )
        val createdProvider2 = createMockProvider(
            "provider-2",
            DockerProviderType.LOCAL,
            DockerProviderState.CREATED
        )
        
        manager.registerProvider(createdProvider1)
        manager.registerProvider(createdProvider2)
        
        val results = manager.initializeAll()

        assertThat(results).hasSize(2)
        results.values.forEach { result ->
            assertThat(result.isSuccess).isTrue()
        }
    }

    @Test
    fun `stopAll should stop all running providers`() = runTest {
        val runningProvider1 = createMockProvider(
            "provider-1",
            DockerProviderType.LOCAL,
            DockerProviderState.RUNNING
        )
        val runningProvider2 = createMockProvider(
            "provider-2",
            DockerProviderType.LOCAL,
            DockerProviderState.RUNNING
        )
        
        manager.registerProvider(runningProvider1)
        manager.registerProvider(runningProvider2)
        
        val results = manager.stopAll()

        assertThat(results).hasSize(2)
        results.values.forEach { result ->
            assertThat(result.isSuccess).isTrue()
        }
    }

    @Test
    fun `destroyAll should destroy all providers and clear manager`() = runTest {
        manager.registerProvider(mockProvider)
        manager.registerProvider(mockProvider2)
        
        val results = manager.destroyAll()

        assertThat(results).hasSize(2)
        assertThat(manager.providers).isEmpty()
        assertThat(manager.activeProvider).isNull()
    }

    // ==================== 回调测试 ====================

    @Test
    fun `registerCallback should receive provider registered events`() = runTest {
        val callback = mockk<DockerProviderManagerCallback> {
            every { onProviderRegistered(any()) } just runs
        }
        
        manager.registerCallback(callback)
        manager.registerProvider(mockProvider)

        verify { callback.onProviderRegistered(mockProvider) }
    }

    @Test
    fun `unregisterCallback should stop receiving events`() = runTest {
        val callback = mockk<DockerProviderManagerCallback> {
            every { onProviderRegistered(any()) } just runs
        }
        
        manager.registerCallback(callback)
        manager.unregisterCallback(callback)
        manager.registerProvider(mockProvider)

        verify(exactly = 0) { callback.onProviderRegistered(any()) }
    }

    // ==================== Provider创建测试 ====================

    @Test
    fun `createProvider should create and register provider`() = runTest {
        val config = LocalDockerProviderConfig(providerId = "new-provider")
        
        val result = manager.createProvider(config)

        assertThat(result.isSuccess).isTrue()
        assertThat(manager.hasProvider("new-provider")).isTrue()
    }

    @Test
    fun `createProvider with autoActivate should set as active`() = runTest {
        val runningProvider = createMockProvider(
            "new-provider",
            DockerProviderType.LOCAL,
            DockerProviderState.RUNNING
        )
        coEvery { mockFactory.create(any()) } returns Result.success(runningProvider)
        every { runningProvider.getState() } returns DockerProviderState.RUNNING
        
        val config = LocalDockerProviderConfig(providerId = "new-provider")
        
        val result = manager.createProvider(config, autoActivate = true)

        assertThat(result.isSuccess).isTrue()
        assertThat(manager.activeProvider?.providerId).isEqualTo("new-provider")
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `manager should handle empty state correctly`() = runTest {
        assertThat(manager.providers).isEmpty()
        assertThat(manager.activeProvider).isNull()
        assertThat(manager.hasActiveProvider()).isFalse()
        assertThat(manager.listProviders()).isEmpty()
    }

    @Test
    fun `manager should handle multiple providers of same type`() = runTest {
        val provider1 = createMockProvider("local-1", DockerProviderType.LOCAL)
        val provider2 = createMockProvider("local-2", DockerProviderType.LOCAL)
        
        manager.registerProvider(provider1)
        manager.registerProvider(provider2)
        
        val localProviders = manager.listProvidersByType(DockerProviderType.LOCAL)

        assertThat(localProviders).hasSize(2)
    }
}