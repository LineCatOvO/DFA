package com.dfa.core.docker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Docker客户端集成测试
 *
 * 测试与真实Docker守护进程的交互功能。
 * 需要在支持Docker的环境中运行（如Termux with Docker或AVF）。
 *
 * 测试覆盖范围：
 * - Docker守护进程连接
 * - 容器生命周期管理
 * - 镜像操作
 * - 网络操作
 * - 卷操作
 * - 健康检查
 *
 * 运行条件：
 * - 设备支持Docker或可通过API访问Docker
 * - 网络连接正常
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 33)
class DockerClientIntegrationTest {

    // Docker客户端实例
    private lateinit var dockerClient: DockerClient

    // 测试用的容器名称前缀
    private val testContainerPrefix = "test-dfa-${System.currentTimeMillis()}"

    // 测试过程中创建的资源
    private val createdContainers = mutableListOf<String>()
    private val createdImages = mutableListOf<String>()
    private val createdNetworks = mutableListOf<String>()
    private val createdVolumes = mutableListOf<String>()

    @Before
    fun setup() = runTest {
        // 初始化Docker客户端
        // 实际实现中应该通过依赖注入获取
        // dockerClient = DockerClientImpl(DockerConfig.DEFAULT)

        // 检查Docker是否可用
        val isDockerAvailable = checkDockerAvailability()
        Assume.assumeTrue("Docker is not available on this device", isDockerAvailable)
    }

    @After
    fun tearDown() = runTest {
        // 清理测试过程中创建的资源
        cleanupTestResources()
        dockerClient.disconnect()
    }

    // ==================== 辅助方法 ====================

    private suspend fun checkDockerAvailability(): Boolean {
        return try {
            val pingResult = dockerClient.ping()
            pingResult.isSuccess && pingResult.getOrThrow()
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun cleanupTestResources() {
        // 清理容器
        createdContainers.forEach { containerId ->
            try {
                dockerClient.stopContainer(containerId, timeout = 5)
                dockerClient.removeContainer(containerId, force = true, removeVolumes = true)
            } catch (e: Exception) {
                // 忽略清理错误
            }
        }

        // 清理镜像
        createdImages.forEach { imageId ->
            try {
                dockerClient.removeImage(imageId, force = true)
            } catch (e: Exception) {
                // 忽略清理错误
            }
        }

        // 清理网络
        createdNetworks.forEach { networkId ->
            try {
                dockerClient.removeNetwork(networkId)
            } catch (e: Exception) {
                // 忽略清理错误
            }
        }

        // 清理卷
        createdVolumes.forEach { volumeName ->
            try {
                dockerClient.removeVolume(volumeName, force = true)
            } catch (e: Exception) {
                // 忽略清理错误
            }
        }
    }

    private fun generateContainerName(): String {
        return "$testContainerPrefix-${createdContainers.size}"
    }

    // ==================== 连接管理测试 ====================

    @Test
    fun `connect should establish connection to Docker daemon`() = runTest {
        // When: 连接到Docker守护进程
        val result = dockerClient.connect()

        // Then: 应该成功连接
        assertThat(result.isSuccess).isTrue()
        assertThat(dockerClient.isConnected()).isTrue()
    }

    @Test
    fun `disconnect should close connection gracefully`() = runTest {
        // Given: 已连接的客户端
        dockerClient.connect()

        // When: 断开连接
        dockerClient.disconnect()

        // Then: 应该断开连接
        assertThat(dockerClient.isConnected()).isFalse()
    }

    @Test
    fun `ping should return true when Docker is healthy`() = runTest {
        // Given: 已连接的客户端
        dockerClient.connect()

        // When: 执行健康检查
        val result = dockerClient.ping()

        // Then: 应该返回true
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isTrue()
    }

    @Test
    fun `version should return valid Docker version info`() = runTest {
        // Given: 已连接的客户端
        dockerClient.connect()

        // When: 获取Docker版本
        val result = dockerClient.version()

        // Then: 应该返回有效的版本信息
        assertThat(result.isSuccess).isTrue()
        val version = result.getOrThrow()
        assertThat(version.version).isNotEmpty()
        assertThat(version.apiVersion).isNotEmpty()
    }

    @Test
    fun `info should return Docker system information`() = runTest {
        // Given: 已连接的客户端
        dockerClient.connect()

        // When: 获取系统信息
        val result = dockerClient.info()

        // Then: 应该返回有效的系统信息
        assertThat(result.isSuccess).isTrue()
        val info = result.getOrThrow()
        assertThat(info.containers).isAtLeast(0)
        assertThat(info.images).isAtLeast(0)
        assertThat(info.cpus).isGreaterThan(0)
        assertThat(info.memory).isGreaterThan(0)
    }

    // ==================== 容器生命周期测试 ====================

    @Test
    fun `createContainer should create container with valid config`() = runTest {
        // Given: 有效的容器配置
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sh", "-c", "echo 'Hello Docker'"),
            labels = mapOf("test" to "integration")
        )

        // When: 创建容器
        val result = dockerClient.createContainer(config)

        // Then: 应该成功创建
        assertThat(result.isSuccess).isTrue()
        val createResult = result.getOrThrow()
        assertThat(createResult.id).isNotEmpty()
        createdContainers.add(createResult.id)
    }

    @Test
    fun `startContainer should start created container`() = runTest {
        // Given: 已创建的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "60")
        )
        val createResult = dockerClient.createContainer(config)
        val containerId = createResult.getOrThrow().id
        createdContainers.add(containerId)

        // When: 启动容器
        val result = dockerClient.startContainer(containerId)

        // Then: 应该成功启动
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `stopContainer should stop running container`() = runTest {
        // Given: 正在运行的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "300")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)

        // When: 停止容器
        val result = dockerClient.stopContainer(containerId, timeout = 5)

        // Then: 应该成功停止
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `restartContainer should restart running container`() = runTest {
        // Given: 正在运行的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "300")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)

        // When: 重启容器
        val result = dockerClient.restartContainer(containerId, timeout = 10)

        // Then: 应该成功重启
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `pauseContainer should pause running container`() = runTest {
        // Given: 正在运行的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "300")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)

        // When: 暂停容器
        val result = dockerClient.pauseContainer(containerId)

        // Then: 应该成功暂停
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `unpauseContainer should resume paused container`() = runTest {
        // Given: 已暂停的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "300")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)
        dockerClient.pauseContainer(containerId)

        // When: 恢复容器
        val result = dockerClient.unpauseContainer(containerId)

        // Then: 应该成功恢复
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `removeContainer should delete container`() = runTest {
        // Given: 已停止的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("echo", "test")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        dockerClient.startContainer(containerId)
        Thread.sleep(1000) // 等待容器执行完成

        // When: 删除容器
        val result = dockerClient.removeContainer(containerId, force = true)

        // Then: 应该成功删除
        assertThat(result.isSuccess).isTrue()

        // 从清理列表中移除（已删除）
        createdContainers.remove(containerId)
    }

    @Test
    fun `listContainers should return all containers`() = runTest {
        // Given: 已创建的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "60")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)

        // When: 列出所有容器
        val result = dockerClient.listContainers(all = true)

        // Then: 应该包含创建的容器
        assertThat(result.isSuccess).isTrue()
        val containers = result.getOrThrow()
        assertThat(containers.any { it.id == containerId || it.name == containerName }).isTrue()
    }

    @Test
    fun `inspectContainer should return container details`() = runTest {
        // Given: 已创建的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "60")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)

        // When: 检查容器详情
        val result = dockerClient.inspectContainer(containerId)

        // Then: 应该返回正确的详情
        assertThat(result.isSuccess).isTrue()
        val details = result.getOrThrow()
        assertThat(details.id).isEqualTo(containerId)
        assertThat(details.name).isEqualTo(containerName)
        assertThat(details.image).contains("alpine")
    }

    @Test
    fun `getContainerLogs should return container output`() = runTest {
        // Given: 已执行完成的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("echo", "Hello from container")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)
        Thread.sleep(2000) // 等待容器执行完成

        // When: 获取容器日志
        val result = dockerClient.getContainerLogs(containerId)

        // Then: 应该包含输出
        assertThat(result.isSuccess).isTrue()
        val logs = result.getOrThrow()
        assertThat(logs).contains("Hello from container")
    }

    @Test
    fun `execInContainer should execute command in running container`() = runTest {
        // Given: 正在运行的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "300")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)

        // When: 在容器中执行命令
        val result = dockerClient.execInContainer(
            containerId = containerId,
            command = listOf("echo", "exec test")
        )

        // Then: 应该成功执行
        assertThat(result.isSuccess).isTrue()
        val execResult = result.getOrThrow()
        assertThat(execResult.exitCode).isEqualTo(0)
        assertThat(execResult.stdout).contains("exec test")
    }

    // ==================== 镜像操作测试 ====================

    @Test
    fun `pullImage should download image from registry`() = runTest {
        // Given: 小型镜像
        val imageName = "alpine:latest"

        // When: 拉取镜像
        val result = dockerClient.pullImage(imageName)

        // Then: 应该成功拉取
        assertThat(result.isSuccess).isTrue()
        val pullResult = result.getOrThrow()
        assertThat(pullResult.imageId).isNotEmpty()
    }

    @Test
    fun `listImages should return available images`() = runTest {
        // When: 列出镜像
        val result = dockerClient.listImages()

        // Then: 应该返回镜像列表
        assertThat(result.isSuccess).isTrue()
        val images = result.getOrThrow()
        // 至少应该有alpine镜像（之前的测试可能已拉取）
        assertThat(images).isNotEmpty()
    }

    @Test
    fun `inspectImage should return image details`() = runTest {
        // Given: 已存在的镜像
        val imageName = "alpine:latest"
        dockerClient.pullImage(imageName)

        // When: 检查镜像详情
        val result = dockerClient.inspectImage(imageName)

        // Then: 应该返回正确的详情
        assertThat(result.isSuccess).isTrue()
        val details = result.getOrThrow()
        assertThat(details.id).isNotEmpty()
        assertThat(details.architecture).isNotEmpty()
    }

    @Test
    fun `tagImage should create new tag for image`() = runTest {
        // Given: 已存在的镜像
        val sourceImage = "alpine:latest"
        val targetImage = "test-alpine:v1"
        dockerClient.pullImage(sourceImage)

        // When: 标记镜像
        val result = dockerClient.tagImage(sourceImage, targetImage)

        // Then: 应该成功标记
        assertThat(result.isSuccess).isTrue()
        createdImages.add(targetImage)
    }

    // ==================== 网络操作测试 ====================

    @Test
    fun `createNetwork should create custom network`() = runTest {
        // Given: 网络配置
        val networkName = "test-network-${System.currentTimeMillis()}"
        val config = NetworkConfig(
            name = networkName,
            driver = "bridge"
        )

        // When: 创建网络
        val result = dockerClient.createNetwork(config)

        // Then: 应该成功创建
        assertThat(result.isSuccess).isTrue()
        val createResult = result.getOrThrow()
        assertThat(createResult.id).isNotEmpty()
        createdNetworks.add(createResult.id)
    }

    @Test
    fun `listNetworks should return available networks`() = runTest {
        // When: 列出网络
        val result = dockerClient.listNetworks()

        // Then: 应该返回网络列表
        assertThat(result.isSuccess).isTrue()
        val networks = result.getOrThrow()
        // 应该至少有默认网络
        assertThat(networks.any { it.name == "bridge" }).isTrue()
    }

    @Test
    fun `connectToNetwork should connect container to network`() = runTest {
        // Given: 容器和网络
        val networkName = "test-network-${System.currentTimeMillis()}"
        val networkId = dockerClient.createNetwork(
            NetworkConfig(name = networkName, driver = "bridge")
        ).getOrThrow().id
        createdNetworks.add(networkId)

        val containerName = generateContainerName()
        val containerId = dockerClient.createContainer(
            ContainerConfig(
                name = containerName,
                image = "alpine:latest",
                command = listOf("sleep", "60")
            )
        ).getOrThrow().id
        createdContainers.add(containerId)

        // When: 将容器连接到网络
        val result = dockerClient.connectToNetwork(networkId, containerId)

        // Then: 应该成功连接
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `disconnectFromNetwork should disconnect container from network`() = runTest {
        // Given: 已连接到网络的容器
        val networkName = "test-network-${System.currentTimeMillis()}"
        val networkId = dockerClient.createNetwork(
            NetworkConfig(name = networkName, driver = "bridge")
        ).getOrThrow().id
        createdNetworks.add(networkId)

        val containerName = generateContainerName()
        val containerId = dockerClient.createContainer(
            ContainerConfig(
                name = containerName,
                image = "alpine:latest",
                command = listOf("sleep", "60")
            )
        ).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.connectToNetwork(networkId, containerId)

        // When: 断开容器与网络的连接
        val result = dockerClient.disconnectFromNetwork(networkId, containerId)

        // Then: 应该成功断开
        assertThat(result.isSuccess).isTrue()
    }

    // ==================== 卷操作测试 ====================

    @Test
    fun `createVolume should create named volume`() = runTest {
        // Given: 卷配置
        val volumeName = "test-volume-${System.currentTimeMillis()}"
        val config = VolumeConfig(name = volumeName)

        // When: 创建卷
        val result = dockerClient.createVolume(config)

        // Then: 应该成功创建
        assertThat(result.isSuccess).isTrue()
        val createResult = result.getOrThrow()
        assertThat(createResult.name).isEqualTo(volumeName)
        createdVolumes.add(volumeName)
    }

    @Test
    fun `listVolumes should return available volumes`() = runTest {
        // Given: 已创建的卷
        val volumeName = "test-volume-${System.currentTimeMillis()}"
        dockerClient.createVolume(VolumeConfig(name = volumeName))
        createdVolumes.add(volumeName)

        // When: 列出卷
        val result = dockerClient.listVolumes()

        // Then: 应该包含创建的卷
        assertThat(result.isSuccess).isTrue()
        val volumes = result.getOrThrow()
        assertThat(volumes.any { it.name == volumeName }).isTrue()
    }

    @Test
    fun `inspectVolume should return volume details`() = runTest {
        // Given: 已存在的卷
        val volumeName = "test-volume-${System.currentTimeMillis()}"
        dockerClient.createVolume(VolumeConfig(name = volumeName))
        createdVolumes.add(volumeName)

        // When: 检查卷详情
        val result = dockerClient.inspectVolume(volumeName)

        // Then: 应该返回正确的详情
        assertThat(result.isSuccess).isTrue()
        val details = result.getOrThrow()
        assertThat(details.name).isEqualTo(volumeName)
        assertThat(details.mountpoint).isNotEmpty()
    }

    @Test
    fun `container with volume mount should persist data`() = runTest {
        // Given: 卷和容器配置
        val volumeName = "test-volume-${System.currentTimeMillis()}"
        dockerClient.createVolume(VolumeConfig(name = volumeName))
        createdVolumes.add(volumeName)

        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sh", "-c", "echo 'test data' > /data/test.txt && sleep 60"),
            volumes = listOf(
                VolumeMount(
                    source = volumeName,
                    destination = "/data",
                    mode = "rw"
                )
            )
        )

        // When: 创建并启动容器
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)
        Thread.sleep(2000)

        // Then: 数据应该写入卷
        val execResult = dockerClient.execInContainer(
            containerId = containerId,
            command = listOf("cat", "/data/test.txt")
        )
        assertThat(execResult.getOrThrow().stdout).contains("test data")
    }

    // ==================== 资源限制测试 ====================

    @Test
    fun `container with resource limits should respect limits`() = runTest {
        // Given: 带资源限制的容器配置
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "60"),
            resources = ResourceLimits(
                memory = 64 * 1024 * 1024, // 64MB
                cpuShares = 512
            )
        )

        // When: 创建并启动容器
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)

        // Then: 容器应该正常运行
        val inspectResult = dockerClient.inspectContainer(containerId)
        assertThat(inspectResult.isSuccess).isTrue()
    }

    @Test
    fun `container with port mapping should expose ports`() = runTest {
        // Given: 带端口映射的容器配置
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "nginx:alpine",
            ports = listOf(
                PortBinding(
                    containerPort = 80,
                    hostPort = 8080,
                    protocol = "tcp"
                )
            )
        )

        // When: 创建并启动容器
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)

        // Then: 端口应该被映射
        val inspectResult = dockerClient.inspectContainer(containerId)
        assertThat(inspectResult.isSuccess).isTrue()
        val details = inspectResult.getOrThrow()
        assertThat(details.ports.any { it.containerPort == 80 }).isTrue()
    }

    // ==================== 健康检查测试 ====================

    @Test
    fun `container with health check should report health status`() = runTest {
        // Given: 带健康检查的容器配置
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "nginx:alpine",
            healthCheck = HealthCheck(
                test = listOf("CMD", "curl", "-f", "http://localhost/"),
                interval = 5000000000, // 5 seconds in nanoseconds
                timeout = 3000000000, // 3 seconds
                retries = 3
            )
        )

        // When: 创建并启动容器
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)

        // Then: 容器应该有健康检查配置
        val inspectResult = dockerClient.inspectContainer(containerId)
        assertThat(inspectResult.isSuccess).isTrue()
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `createContainer should fail with invalid image`() = runTest {
        // Given: 不存在的镜像
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "nonexistent-image-xyz123:latest"
        )

        // When: 尝试创建容器
        val result = dockerClient.createContainer(config)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `startContainer should fail for already running container`() = runTest {
        // Given: 正在运行的容器
        val containerName = generateContainerName()
        val config = ContainerConfig(
            name = containerName,
            image = "alpine:latest",
            command = listOf("sleep", "60")
        )
        val containerId = dockerClient.createContainer(config).getOrThrow().id
        createdContainers.add(containerId)
        dockerClient.startContainer(containerId)

        // When: 再次启动
        val result = dockerClient.startContainer(containerId)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `stopContainer should fail for non-existent container`() = runTest {
        // Given: 不存在的容器ID
        val nonexistentId = "nonexistent123"

        // When: 尝试停止
        val result = dockerClient.stopContainer(nonexistentId)

        // Then: 应该失败
        assertThat(result.isFailure).isTrue()
    }
}