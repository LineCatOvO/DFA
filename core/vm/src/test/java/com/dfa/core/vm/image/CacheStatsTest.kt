package com.dfa.core.vm.image

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * CacheStats 单元测试
 */
class CacheStatsTest {

    @Test
    fun `CacheStats should calculate average size correctly`() {
        // Given
        val stats = CacheStats(
            totalImages = 5,
            totalSize = 5000
        )

        // When
        val averageSize = stats.averageSize

        // Then
        assertThat(averageSize).isEqualTo(1000L)
    }

    @Test
    fun `CacheStats averageSize should be 0 when no images`() {
        // Given
        val stats = CacheStats(
            totalImages = 0,
            totalSize = 0
        )

        // When
        val averageSize = stats.averageSize

        // Then
        assertThat(averageSize).isEqualTo(0L)
    }

    @Test
    fun `CacheStats should contain all properties`() {
        // Given
        val stats = CacheStats(
            totalImages = 10,
            totalSize = 100000,
            oldestAccessTime = 1000L,
            newestAccessTime = 2000L
        )

        // When & Then
        assertThat(stats.totalImages).isEqualTo(10)
        assertThat(stats.totalSize).isEqualTo(100000L)
        assertThat(stats.oldestAccessTime).isEqualTo(1000L)
        assertThat(stats.newestAccessTime).isEqualTo(2000L)
        assertThat(stats.averageSize).isEqualTo(10000L)
    }
}