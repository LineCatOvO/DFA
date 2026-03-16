package com.dfa.core.vm.image

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * PredefinedImageProviderImpl 单元测试
 */
class PredefinedImageProviderImplTest {

    private lateinit var provider: PredefinedImageProvider

    @Before
    fun setUp() {
        provider = PredefinedImageProviderImpl()
    }

    @Test
    fun `getAllImages should return all predefined images`() {
        // When
        val images = provider.getAllImages()

        // Then
        assertThat(images).isNotEmpty()
        assertThat(images).hasSize(PredefinedImageRegistry.ALL_IMAGES.size)
    }

    @Test
    fun `getImagesByArchitecture should return ARM64 images`() {
        // When
        val images = provider.getImagesByArchitecture(ImageArchitecture.ARM64)

        // Then
        assertThat(images).isNotEmpty()
        images.forEach { image ->
            assertThat(image.architecture).isEqualTo(ImageArchitecture.ARM64)
        }
    }

    @Test
    fun `getImagesByArchitecture should return AMD64 images`() {
        // When
        val images = provider.getImagesByArchitecture(ImageArchitecture.AMD64)

        // Then
        assertThat(images).isNotEmpty()
        images.forEach { image ->
            assertThat(image.architecture).isEqualTo(ImageArchitecture.AMD64)
        }
    }

    @Test
    fun `getImagesByArchitecture should return empty list for unused architecture`() {
        // When
        val images = provider.getImagesByArchitecture(ImageArchitecture.X86)

        // Then
        assertThat(images).isEmpty()
    }

    @Test
    fun `getImagesByOsType should return Debian images`() {
        // When
        val images = provider.getImagesByOsType(OsType.DEBIAN)

        // Then
        assertThat(images).isNotEmpty()
        images.forEach { image ->
            assertThat(image.osType).isEqualTo(OsType.DEBIAN)
        }
    }

    @Test
    fun `getImagesByOsType should return Ubuntu images`() {
        // When
        val images = provider.getImagesByOsType(OsType.UBUNTU)

        // Then
        assertThat(images).isNotEmpty()
        images.forEach { image ->
            assertThat(image.osType).isEqualTo(OsType.UBUNTU)
        }
    }

    @Test
    fun `getImageById should return correct image`() {
        // When
        val image = provider.getImageById("debian-12-nocloud-arm64")

        // Then
        assertThat(image).isNotNull()
        assertThat(image?.id).isEqualTo("debian-12-nocloud-arm64")
    }

    @Test
    fun `getImageById should return null for non-existent id`() {
        // When
        val image = provider.getImageById("non-existent-image-id")

        // Then
        assertThat(image).isNull()
    }

    @Test
    fun `searchImages should find images by name`() {
        // When
        val results = provider.searchImages("Debian")

        // Then
        assertThat(results).isNotEmpty()
    }

    @Test
    fun `searchImages should find images by description`() {
        // When
        val results = provider.searchImages("cloud")

        // Then
        assertThat(results).isNotEmpty()
    }

    @Test
    fun `searchImages should find images by tag`() {
        // When
        val results = provider.searchImages("docker")

        // Then
        assertThat(results).isNotEmpty()
    }

    @Test
    fun `searchImages should return empty list for non-matching query`() {
        // When
        val results = provider.searchImages("nonexistentos12345")

        // Then
        assertThat(results).isEmpty()
    }

    @Test
    fun `getRecommendedImages should return images with docker or cloud tags`() {
        // When
        val images = provider.getRecommendedImages()

        // Then
        assertThat(images).isNotEmpty()
        images.forEach { image ->
            assertThat(image.isRecommendedForDocker).isTrue()
        }
    }

    @Test
    fun `getMinimalImages should return minimal images`() {
        // When
        val images = provider.getMinimalImages()

        // Then
        assertThat(images).isNotEmpty()
        images.forEach { image ->
            assertThat(image.isMinimal).isTrue()
        }
    }

    @Test
    fun `getDefaultImage should return Debian 12 NoCloud ARM64`() {
        // When
        val image = provider.getDefaultImage()

        // Then
        assertThat(image.id).isEqualTo("debian-12-nocloud-arm64")
        assertThat(image.osType).isEqualTo(OsType.DEBIAN)
        assertThat(image.architecture).isEqualTo(ImageArchitecture.ARM64)
    }

    @Test
    fun `getImagesByTag should return images with specified tag`() {
        // When
        val images = provider.getImagesByTag("docker")

        // Then
        assertThat(images).isNotEmpty()
        images.forEach { image ->
            val hasDockerTag = image.tags.any { it.equals("docker", ignoreCase = true) }
            assertThat(hasDockerTag).isTrue()
        }
    }

    @Test
    fun `getImagesByTag should be case insensitive`() {
        // When
        val images = provider.getImagesByTag("DOCKER")

        // Then
        assertThat(images).isNotEmpty()
    }

    @Test
    fun `getImagesByTag should return empty list for non-existent tag`() {
        // When
        val images = provider.getImagesByTag("nonexistenttag12345")

        // Then
        assertThat(images).isEmpty()
    }

    @Test
    fun `getAvailableArchitectures should return distinct architectures`() {
        // When
        val architectures = provider.getAvailableArchitectures()

        // Then
        assertThat(architectures).isNotEmpty()
        assertThat(architectures).contains(ImageArchitecture.ARM64)
        assertThat(architectures).contains(ImageArchitecture.AMD64)
        // Verify distinct
        assertThat(architectures.toSet()).hasSize(architectures.size)
    }

    @Test
    fun `getAvailableOsTypes should return distinct os types`() {
        // When
        val osTypes = provider.getAvailableOsTypes()

        // Then
        assertThat(osTypes).isNotEmpty()
        assertThat(osTypes).contains(OsType.DEBIAN)
        assertThat(osTypes).contains(OsType.UBUNTU)
        // Verify distinct
        assertThat(osTypes.toSet()).hasSize(osTypes.size)
    }

    @Test
    fun `provider should return consistent results across multiple calls`() {
        // When
        val firstCall = provider.getAllImages()
        val secondCall = provider.getAllImages()

        // Then
        assertThat(firstCall).isEqualTo(secondCall)
    }

    @Test
    fun `search should be case insensitive`() {
        // When
        val lowerCase = provider.searchImages("debian")
        val upperCase = provider.searchImages("DEBIAN")
        val mixedCase = provider.searchImages("DeBiAn")

        // Then
        assertThat(lowerCase).isNotEmpty()
        assertThat(upperCase).isNotEmpty()
        assertThat(mixedCase).isNotEmpty()
        assertThat(lowerCase).hasSize(upperCase.size)
        assertThat(lowerCase).hasSize(mixedCase.size)
    }
}