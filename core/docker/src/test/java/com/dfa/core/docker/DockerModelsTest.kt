package com.dfa.core.docker

import com.google.truth.Truth.assertThat
import org.junit.Test

/**
 * DockerModels 单元测试
 */
class DockerModelsTest {

    // ==================== ContainerState Tests ====================

    @Test
    fun `ContainerState should contain all expected states`() {
        val expectedStates = listOf(
            ContainerState.CREATED,
            ContainerState.RUNNING,
            ContainerState.PAUSED,
            ContainerState.RESTARTING,
            ContainerState.REMOVING,
            ContainerState.EXITED,
            ContainerState.DEAD
        )

        assertThat(ContainerState.entries.size).isEqualTo(expectedStates.size)
        expectedStates.forEach { state ->
            assertThat(ContainerState.entries.contains(state)).isTrue()
        }
    }

    @Test
    fun `ContainerState CREATED should be first state`() {
        assertThat(ContainerState.entries[0]).isEqualTo(ContainerState.CREATED)
    }

    @Test
    fun `ContainerState DEAD should be last state`() {
        assertThat(ContainerState.entries.last()).isEqualTo(ContainerState.DEAD)
    }

    // ==================== ContainerInfo Tests ====================

    @Test
    fun `ContainerInfo should have all required properties`() {
        val containerInfo = ContainerInfo(
            id = "container-123",
            name = "test-container",
            image = "nginx:latest",
            state = ContainerState.RUNNING,
            status = "Up 2 hours"
        )

        assertThat(containerInfo.id).isEqualTo("container-123")
        assertThat(containerInfo.name).isEqualTo("test-container")
        assertThat(containerInfo.image).isEqualTo("nginx:latest")
        assertThat(containerInfo.state).isEqualTo(ContainerState.RUNNING)
        assertThat(containerInfo.status).isEqualTo("Up 2 hours")
        assertThat(containerInfo.ports).isEmpty()
    }

    @Test
    fun `ContainerInfo should support port mappings`() {
        val ports = listOf(
            PortMapping(80, 8080),
            PortMapping(443, 8443, "tcp")
        )
        val containerInfo = ContainerInfo(
            id = "container-123",
            name = "test-container",
            image = "nginx:latest",
            state = ContainerState.RUNNING,
            status = "Up 2 hours",
            ports = ports
        )

        assertThat(containerInfo.ports).hasSize(2)
        assertThat(containerInfo.ports[0].containerPort).isEqualTo(80)
        assertThat(containerInfo.ports[0].hostPort).isEqualTo(8080)
    }

    @Test
    fun `ContainerInfo copy should work correctly`() {
        val original = ContainerInfo(
            id = "container-123",
            name = "test-container",
            image = "nginx:latest",
            state = ContainerState.RUNNING,
            status = "Up 2 hours"
        )

        val copied = original.copy(state = ContainerState.STOPPED, status = "Exited (0) 1 minute ago")

        assertThat(copied.id).isEqualTo("container-123")
        assertThat(copied.state).isEqualTo(ContainerState.STOPPED)
        assertThat(original.state).isEqualTo(ContainerState.RUNNING)
    }

    @Test
    fun `ContainerInfo equals should work correctly`() {
        val container1 = ContainerInfo(
            id = "container-123",
            name = "test-container",
            image = "nginx:latest",
            state = ContainerState.RUNNING,
            status = "Up 2 hours"
        )
        val container2 = container1.copy()
        val container3 = container1.copy(name = "different-name")

        assertThat(container1).isEqualTo(container2)
        assertThat(container1).isNotEqualTo(container3)
    }

    @Test
    fun `ContainerInfo toString should contain relevant information`() {
        val containerInfo = ContainerInfo(
            id = "container-123",
            name = "test-container",
            image = "nginx:latest",
            state = ContainerState.RUNNING,
            status = "Up 2 hours"
        )

        val stringRepresentation = containerInfo.toString()

        assertThat(stringRepresentation).contains("container-123")
        assertThat(stringRepresentation).contains("test-container")
        assertThat(stringRepresentation).contains("RUNNING")
    }

    // ==================== PortMapping Tests ====================

    @Test
    fun `PortMapping should have default protocol tcp`() {
        val portMapping = PortMapping(80, 8080)

        assertThat(portMapping.containerPort).isEqualTo(80)
        assertThat(portMapping.hostPort).isEqualTo(8080)
        assertThat(portMapping.protocol).isEqualTo("tcp")
    }

    @Test
    fun `PortMapping should support custom protocol`() {
        val portMapping = PortMapping(53, 5353, "udp")

        assertThat(portMapping.protocol).isEqualTo("udp")
    }

    @Test
    fun `PortMapping copy should work correctly`() {
        val original = PortMapping(80, 8080)
        val copied = original.copy(hostPort = 9090)

        assertThat(copied.containerPort).isEqualTo(80)
        assertThat(copied.hostPort).isEqualTo(9090)
        assertThat(original.hostPort).isEqualTo(8080)
    }

    @Test
    fun `PortMapping equals should work correctly`() {
        val port1 = PortMapping(80, 8080)
        val port2 = PortMapping(80, 8080)
        val port3 = PortMapping(80, 9090)

        assertThat(port1).isEqualTo(port2)
        assertThat(port1).isNotEqualTo(port3)
    }

    @Test
    fun `PortMapping toString should contain port information`() {
        val portMapping = PortMapping(80, 8080, "tcp")

        val stringRepresentation = portMapping.toString()

        assertThat(stringRepresentation).contains("80")
        assertThat(stringRepresentation).contains("8080")
        assertThat(stringRepresentation).contains("tcp")
    }

    // ==================== ImageInfo Tests ====================

    @Test
    fun `ImageInfo should have all required properties`() {
        val imageInfo = ImageInfo(
            id = "sha256:abc123",
            name = "nginx",
            tag = "latest",
            size = 142000000,
            createdAt = "2024-01-15T10:30:00Z"
        )

        assertThat(imageInfo.id).isEqualTo("sha256:abc123")
        assertThat(imageInfo.name).isEqualTo("nginx")
        assertThat(imageInfo.tag).isEqualTo("latest")
        assertThat(imageInfo.size).isEqualTo(142000000)
        assertThat(imageInfo.createdAt).isEqualTo("2024-01-15T10:30:00Z")
    }

    @Test
    fun `ImageInfo copy should work correctly`() {
        val original = ImageInfo(
            id = "sha256:abc123",
            name = "nginx",
            tag = "latest",
            size = 142000000,
            createdAt = "2024-01-15T10:30:00Z"
        )

        val copied = original.copy(tag = "1.25")

        assertThat(copied.name).isEqualTo("nginx")
        assertThat(copied.tag).isEqualTo("1.25")
        assertThat(original.tag).isEqualTo("latest")
    }

    @Test
    fun `ImageInfo equals should work correctly`() {
        val image1 = ImageInfo(
            id = "sha256:abc123",
            name = "nginx",
            tag = "latest",
            size = 142000000,
            createdAt = "2024-01-15T10:30:00Z"
        )
        val image2 = image1.copy()
        val image3 = image1.copy(size = 100000000)

        assertThat(image1).isEqualTo(image2)
        assertThat(image1).isNotEqualTo(image3)
    }

    @Test
    fun `ImageInfo toString should contain relevant information`() {
        val imageInfo = ImageInfo(
            id = "sha256:abc123",
            name = "nginx",
            tag = "latest",
            size = 142000000,
            createdAt = "2024-01-15T10:30:00Z"
        )

        val stringRepresentation = imageInfo.toString()

        assertThat(stringRepresentation).contains("nginx")
        assertThat(stringRepresentation).contains("latest")
        assertThat(stringRepresentation).contains("sha256:abc123")
    }

    @Test
    fun `ImageInfo should support various tag formats`() {
        val imageLatest = ImageInfo("id1", "nginx", "latest", 1000, "2024-01-01")
        val imageVersioned = ImageInfo("id2", "nginx", "1.25.0", 1000, "2024-01-01")
        val imageDigest = ImageInfo("id3", "nginx", "sha256:def456", 1000, "2024-01-01")

        assertThat(imageLatest.tag).isEqualTo("latest")
        assertThat(imageVersioned.tag).isEqualTo("1.25.0")
        assertThat(imageDigest.tag).isEqualTo("sha256:def456")
    }

    @Test
    fun `ImageInfo size should handle large values`() {
        val largeImage = ImageInfo(
            id = "sha256:large",
            name = "large-image",
            tag = "latest",
            size = Long.MAX_VALUE,
            createdAt = "2024-01-01"
        )

        assertThat(largeImage.size).isEqualTo(Long.MAX_VALUE)
    }
}