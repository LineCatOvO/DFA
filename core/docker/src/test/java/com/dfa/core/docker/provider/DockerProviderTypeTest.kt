package com.dfa.core.docker.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * DockerProviderType 单元测试
 *
 * 测试DockerProviderType枚举的属性和方法。
 */
class DockerProviderTypeTest {

    // ==================== 枚举值测试 ====================

    @Test
    fun `should contain all expected enum values`() {
        val expectedTypes = listOf(
            DockerProviderType.QEMU,
            DockerProviderType.AVF,
            DockerProviderType.LOCAL,
            DockerProviderType.UNKNOWN
        )

        assertThat(DockerProviderType.entries.size).isEqualTo(expectedTypes.size)
        expectedTypes.forEach { type ->
            assertThat(DockerProviderType.entries.contains(type)).isTrue()
        }
    }

    // ==================== displayLabel 测试 ====================

    @Test
    fun `QEMU displayLabel should be QEMU`() {
        assertThat(DockerProviderType.QEMU.displayLabel).isEqualTo("QEMU")
    }

    @Test
    fun `AVF displayLabel should be AVF`() {
        assertThat(DockerProviderType.AVF.displayLabel).isEqualTo("AVF")
    }

    @Test
    fun `LOCAL displayLabel should be Local`() {
        assertThat(DockerProviderType.LOCAL.displayLabel).isEqualTo("Local")
    }

    @Test
    fun `UNKNOWN displayLabel should be Unknown`() {
        assertThat(DockerProviderType.UNKNOWN.displayLabel).isEqualTo("Unknown")
    }

    // ==================== description 测试 ====================

    @Test
    fun `QEMU description should describe virtual machine environment`() {
        assertThat(DockerProviderType.QEMU.description).isEqualTo("Docker running in QEMU virtual machine environment")
    }

    @Test
    fun `AVF description should describe Apple Virtualization Framework`() {
        assertThat(DockerProviderType.AVF.description).isEqualTo("Docker running via Apple Virtualization Framework")
    }

    @Test
    fun `LOCAL description should describe host system`() {
        assertThat(DockerProviderType.LOCAL.description).isEqualTo("Docker running directly on the host system")
    }

    @Test
    fun `UNKNOWN description should indicate unknown type`() {
        assertThat(DockerProviderType.UNKNOWN.description).isEqualTo("Unknown or unsupported Docker provider type")
    }

    // ==================== isVirtualized 测试 ====================

    @Test
    fun `QEMU should be virtualized`() {
        assertThat(DockerProviderType.QEMU.isVirtualized()).isTrue()
    }

    @Test
    fun `AVF should be virtualized`() {
        assertThat(DockerProviderType.AVF.isVirtualized()).isTrue()
    }

    @Test
    fun `LOCAL should not be virtualized`() {
        assertThat(DockerProviderType.LOCAL.isVirtualized()).isFalse()
    }

    @Test
    fun `UNKNOWN should not be virtualized`() {
        assertThat(DockerProviderType.UNKNOWN.isVirtualized()).isFalse()
    }

    // ==================== isSupported 测试 ====================

    @Test
    fun `QEMU should be supported`() {
        assertThat(DockerProviderType.QEMU.isSupported()).isTrue()
    }

    @Test
    fun `AVF should be supported`() {
        assertThat(DockerProviderType.AVF.isSupported()).isTrue()
    }

    @Test
    fun `LOCAL should be supported`() {
        assertThat(DockerProviderType.LOCAL.isSupported()).isTrue()
    }

    @Test
    fun `UNKNOWN should not be supported`() {
        assertThat(DockerProviderType.UNKNOWN.isSupported()).isFalse()
    }

    // ==================== fromName 测试 ====================

    @Test
    fun `fromName should return QEMU for exact match`() {
        assertThat(DockerProviderType.fromName("QEMU")).isEqualTo(DockerProviderType.QEMU)
    }

    @Test
    fun `fromName should return QEMU for lowercase`() {
        assertThat(DockerProviderType.fromName("qemu")).isEqualTo(DockerProviderType.QEMU)
    }

    @Test
    fun `fromName should return QEMU for mixed case`() {
        assertThat(DockerProviderType.fromName("Qemu")).isEqualTo(DockerProviderType.QEMU)
    }

    @Test
    fun `fromName should return AVF for exact match`() {
        assertThat(DockerProviderType.fromName("AVF")).isEqualTo(DockerProviderType.AVF)
    }

    @Test
    fun `fromName should return AVF for lowercase`() {
        assertThat(DockerProviderType.fromName("avf")).isEqualTo(DockerProviderType.AVF)
    }

    @Test
    fun `fromName should return LOCAL for exact match`() {
        assertThat(DockerProviderType.fromName("LOCAL")).isEqualTo(DockerProviderType.LOCAL)
    }

    @Test
    fun `fromName should return LOCAL for lowercase`() {
        assertThat(DockerProviderType.fromName("local")).isEqualTo(DockerProviderType.LOCAL)
    }

    @Test
    fun `fromName should return UNKNOWN for invalid name`() {
        assertThat(DockerProviderType.fromName("invalid")).isEqualTo(DockerProviderType.UNKNOWN)
    }

    @Test
    fun `fromName should return UNKNOWN for empty string`() {
        assertThat(DockerProviderType.fromName("")).isEqualTo(DockerProviderType.UNKNOWN)
    }

    @Test
    fun `fromName should return UNKNOWN for null-like string`() {
        assertThat(DockerProviderType.fromName("null")).isEqualTo(DockerProviderType.UNKNOWN)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `all types should have non-empty displayLabel`() {
        DockerProviderType.entries.forEach { type ->
            assertThat(type.displayLabel).isNotEmpty()
        }
    }

    @Test
    fun `all types should have non-empty description`() {
        DockerProviderType.entries.forEach { type ->
            assertThat(type.description).isNotEmpty()
        }
    }

    @Test
    fun `virtualized types count should be 2`() {
        val virtualizedCount = DockerProviderType.entries.count { it.isVirtualized() }
        assertThat(virtualizedCount).isEqualTo(2)
    }

    @Test
    fun `supported types count should be 3`() {
        val supportedCount = DockerProviderType.entries.count { it.isSupported() }
        assertThat(supportedCount).isEqualTo(3)
    }
}