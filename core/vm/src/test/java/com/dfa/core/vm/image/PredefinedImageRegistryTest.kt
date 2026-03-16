package com.dfa.core.vm.image

import com.google.truth.Truth.assertThat
import org.junit.Test

/**
 * PredefinedImageRegistry 单元测试
 */
class PredefinedImageRegistryTest {

    @Test
    fun `ALL_IMAGES should not be empty`() {
        // When
        val images = PredefinedImageRegistry.ALL_IMAGES

        // Then
        assertThat(images).isNotEmpty()
    }

    @Test
    fun `ALL_IMAGES should contain Debian images`() {
        // When
        val images = PredefinedImageRegistry.ALL_IMAGES

        // Then
        val debianImages = images.filter { it.osType == OsType.DEBIAN }
        assertThat(debianImages).isNotEmpty()
    }

    @Test
    fun `ALL_IMAGES should contain Ubuntu images`() {
        // When
        val images = PredefinedImageRegistry.ALL_IMAGES

        // Then
        val ubuntuImages = images.filter { it.osType == OsType.UBUNTU }
        assertThat(ubuntuImages).isNotEmpty()
    }

    @Test
    fun `ALL_IMAGES should contain ARM64 images`() {
        // When
        val images = PredefinedImageRegistry.ALL_IMAGES

        // Then
        val arm64Images = images.filter { it.architecture == ImageArchitecture.ARM64 }
        assertThat(arm64Images).isNotEmpty()
    }

    @Test
    fun `ALL_IMAGES should contain AMD64 images`() {
        // When
        val images = PredefinedImageRegistry.ALL_IMAGES

        // Then
        val amd64Images = images.filter { it.architecture == ImageArchitecture.AMD64 }
        assertThat(amd64Images).isNotEmpty()
    }

    @Test
    fun `getById should return correct image for valid id`() {
        // When
        val image = PredefinedImageRegistry.getById("debian-12-nocloud-arm64")

        // Then
        assertThat(image).isNotNull()
        assertThat(image?.name).contains("Debian 12")
        assertThat(image?.architecture).isEqualTo(ImageArchitecture.ARM64)
    }

    @Test
    fun `getById should return null for invalid id`() {
        // When
        val image = PredefinedImageRegistry.getById("non-existent-image")

        // Then
        assertThat(image).isNull()
    }

    @Test
    fun `BY_ARCHITECTURE should group images correctly`() {
        // When
        val byArch = PredefinedImageRegistry.BY_ARCHITECTURE

        // Then
        assertThat(byArch).containsKey(ImageArchitecture.ARM64)
        assertThat(byArch).containsKey(ImageArchitecture.AMD64)
        assertThat(byArch[ImageArchitecture.ARM64]).isNotEmpty()
        assertThat(byArch[ImageArchitecture.AMD64]).isNotEmpty()
    }

    @Test
    fun `BY_OS_TYPE should group images correctly`() {
        // When
        val byOsType = PredefinedImageRegistry.BY_OS_TYPE

        // Then
        assertThat(byOsType).containsKey(OsType.DEBIAN)
        assertThat(byOsType).containsKey(OsType.UBUNTU)
        assertThat(byOsType[OsType.DEBIAN]).isNotEmpty()
        assertThat(byOsType[OsType.UBUNTU]).isNotEmpty()
    }

    @Test
    fun `RECOMMENDED_IMAGES should contain images with docker or cloud tags`() {
        // When
        val recommended = PredefinedImageRegistry.RECOMMENDED_IMAGES

        // Then
        assertThat(recommended).isNotEmpty()
        recommended.forEach { image ->
            assertThat(image.isRecommendedForDocker).isTrue()
        }
    }

    @Test
    fun `MINIMAL_IMAGES should contain images with isMinimal true`() {
        // When
        val minimal = PredefinedImageRegistry.MINIMAL_IMAGES

        // Then
        assertThat(minimal).isNotEmpty()
        minimal.forEach { image ->
            assertThat(image.isMinimal).isTrue()
        }
    }

    @Test
    fun `search should find images by name`() {
        // When
        val results = PredefinedImageRegistry.search("Debian")

        // Then
        assertThat(results).isNotEmpty()
        results.forEach { image ->
            val containsDebian = image.name.contains("Debian", ignoreCase = true) ||
                                 image.description.contains("Debian", ignoreCase = true)
            assertThat(containsDebian).isTrue()
        }
    }

    @Test
    fun `search should find images by osType`() {
        // When
        val results = PredefinedImageRegistry.search("ubuntu")

        // Then
        assertThat(results).isNotEmpty()
        results.forEach { image ->
            val containsUbuntu = image.name.contains("Ubuntu", ignoreCase = true) ||
                                 image.description.contains("Ubuntu", ignoreCase = true) ||
                                 image.osType.name.equals("UBUNTU", ignoreCase = true)
            assertThat(containsUbuntu).isTrue()
        }
    }

    @Test
    fun `search should find images by tag`() {
        // When
        val results = PredefinedImageRegistry.search("docker")

        // Then
        assertThat(results).isNotEmpty()
        results.forEach { image ->
            val hasDockerTag = image.tags.any { it.equals("docker", ignoreCase = true) }
            val containsDocker = image.name.contains("docker", ignoreCase = true) ||
                                 image.description.contains("docker", ignoreCase = true)
            assertThat(hasDockerTag || containsDocker).isTrue()
        }
    }

    @Test
    fun `search should return empty list for non-matching query`() {
        // When
        val results = PredefinedImageRegistry.search("nonexistentos12345")

        // Then
        assertThat(results).isEmpty()
    }

    @Test
    fun `DEFAULT_IMAGE should be Debian 12 NoCloud ARM64`() {
        // When
        val default = PredefinedImageRegistry.DEFAULT_IMAGE

        // Then
        assertThat(default.id).isEqualTo("debian-12-nocloud-arm64")
        assertThat(default.osType).isEqualTo(OsType.DEBIAN)
        assertThat(default.architecture).isEqualTo(ImageArchitecture.ARM64)
    }

    @Test
    fun `PredefinedImageSource formattedSize should format bytes correctly`() {
        // Given
        val image = PredefinedImageSource(
            id = "test",
            name = "Test",
            description = "Test image",
            url = "https://example.com/test.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            estimatedSizeBytes = 1024L * 1024L * 1024L, // 1GB
            loginAccount = "root"
        )

        // When
        val formatted = image.formattedSize

        // Then
        assertThat(formatted).isEqualTo("1.00 GB")
    }

    @Test
    fun `PredefinedImageSource formattedSize should format MB correctly`() {
        // Given
        val image = PredefinedImageSource(
            id = "test",
            name = "Test",
            description = "Test image",
            url = "https://example.com/test.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            estimatedSizeBytes = 300L * 1024L * 1024L, // 300MB
            loginAccount = "root"
        )

        // When
        val formatted = image.formattedSize

        // Then
        assertThat(formatted).isEqualTo("300 MB")
    }

    @Test
    fun `PredefinedImageSource isArm should return true for ARM64`() {
        // Given
        val image = PredefinedImageSource(
            id = "test",
            name = "Test",
            description = "Test image",
            url = "https://example.com/test.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            loginAccount = "root"
        )

        // When & Then
        assertThat(image.isArm).isTrue()
    }

    @Test
    fun `PredefinedImageSource isArm should return true for ARMV7`() {
        // Given
        val image = PredefinedImageSource(
            id = "test",
            name = "Test",
            description = "Test image",
            url = "https://example.com/test.qcow2",
            architecture = ImageArchitecture.ARMV7,
            osType = OsType.DEBIAN,
            osVersion = "12",
            loginAccount = "root"
        )

        // When & Then
        assertThat(image.isArm).isTrue()
    }

    @Test
    fun `PredefinedImageSource isArm should return false for AMD64`() {
        // Given
        val image = PredefinedImageSource(
            id = "test",
            name = "Test",
            description = "Test image",
            url = "https://example.com/test.qcow2",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            loginAccount = "root"
        )

        // When & Then
        assertThat(image.isArm).isFalse()
    }

    @Test
    fun `PredefinedImageSource isRecommendedForDocker should return true when docker tag present`() {
        // Given
        val image = PredefinedImageSource(
            id = "test",
            name = "Test",
            description = "Test image",
            url = "https://example.com/test.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            loginAccount = "root",
            tags = setOf("docker", "cloud")
        )

        // When & Then
        assertThat(image.isRecommendedForDocker).isTrue()
    }

    @Test
    fun `PredefinedImageSource isRecommendedForDocker should return true when cloud tag present`() {
        // Given
        val image = PredefinedImageSource(
            id = "test",
            name = "Test",
            description = "Test image",
            url = "https://example.com/test.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            loginAccount = "root",
            tags = setOf("cloud")
        )

        // When & Then
        assertThat(image.isRecommendedForDocker).isTrue()
    }

    @Test
    fun `PredefinedImageSource isRecommendedForDocker should return false when no relevant tags`() {
        // Given
        val image = PredefinedImageSource(
            id = "test",
            name = "Test",
            description = "Test image",
            url = "https://example.com/test.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            loginAccount = "root",
            tags = setOf("minimal", "test")
        )

        // When & Then
        assertThat(image.isRecommendedForDocker).isFalse()
    }

    @Test
    fun `Cirros images should have login credentials`() {
        // When
        val cirrosImages = PredefinedImageRegistry.ALL_IMAGES.filter { it.osType == OsType.CIRROS }

        // Then
        assertThat(cirrosImages).isNotEmpty()
        cirrosImages.forEach { image ->
            assertThat(image.loginAccount).isEqualTo("cirros")
            assertThat(image.loginPassword).isEqualTo("gocubsgo")
        }
    }

    @Test
    fun `All images should have valid URL`() {
        // When
        val images = PredefinedImageRegistry.ALL_IMAGES

        // Then
        images.forEach { image ->
            assertThat(image.url).startsWith("https://")
            assertThat(image.url).isNotEmpty()
        }
    }

    @Test
    fun `All images should have non-empty id and name`() {
        // When
        val images = PredefinedImageRegistry.ALL_IMAGES

        // Then
        images.forEach { image ->
            assertThat(image.id).isNotEmpty()
            assertThat(image.name).isNotEmpty()
        }
    }
}