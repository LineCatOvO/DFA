package com.dfa.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Constants 单元测试
 */
class ConstantsTest {

    @Test
    fun `DEFAULT_TIMEOUT should be 30000 milliseconds`() {
        assertThat(Constants.DEFAULT_TIMEOUT).isEqualTo(30_000L)
    }

    @Test
    fun `CONNECT_TIMEOUT should be 10000 milliseconds`() {
        assertThat(Constants.CONNECT_TIMEOUT).isEqualTo(10_000L)
    }

    @Test
    fun `READ_TIMEOUT should be 30000 milliseconds`() {
        assertThat(Constants.READ_TIMEOUT).isEqualTo(30_000L)
    }

    @Test
    fun `DOCKER_DEFAULT_PORT should be 2375`() {
        assertThat(Constants.DOCKER_DEFAULT_PORT).isEqualTo(2375)
    }

    @Test
    fun `DOCKER_TLS_PORT should be 2376`() {
        assertThat(Constants.DOCKER_TLS_PORT).isEqualTo(2376)
    }

    @Test
    fun `VM_DEFAULT_MEMORY should be 2048 MB`() {
        assertThat(Constants.VM_DEFAULT_MEMORY).isEqualTo(2048)
    }

    @Test
    fun `VM_DEFAULT_CPU should be 2`() {
        assertThat(Constants.VM_DEFAULT_CPU).isEqualTo(2)
    }

    @Test
    fun `timeout constants should have correct relationships`() {
        // CONNECT_TIMEOUT should be less than or equal to DEFAULT_TIMEOUT
        assertThat(Constants.CONNECT_TIMEOUT).isAtMost(Constants.DEFAULT_TIMEOUT)
        
        // READ_TIMEOUT should be less than or equal to DEFAULT_TIMEOUT
        assertThat(Constants.READ_TIMEOUT).isAtMost(Constants.DEFAULT_TIMEOUT)
    }

    @Test
    fun `docker ports should be different`() {
        assertThat(Constants.DOCKER_DEFAULT_PORT).isNotEqualTo(Constants.DOCKER_TLS_PORT)
    }

    @Test
    fun `docker TLS port should be higher than default port`() {
        assertThat(Constants.DOCKER_TLS_PORT).isGreaterThan(Constants.DOCKER_DEFAULT_PORT)
    }

    @Test
    fun `VM defaults should be positive`() {
        assertThat(Constants.VM_DEFAULT_MEMORY).isGreaterThan(0)
        assertThat(Constants.VM_DEFAULT_CPU).isGreaterThan(0)
    }

    @Test
    fun `VM default memory should be at least 1GB`() {
        assertThat(Constants.VM_DEFAULT_MEMORY).isAtLeast(1024)
    }

    @Test
    fun `VM default CPU should be at least 1`() {
        assertThat(Constants.VM_DEFAULT_CPU).isAtLeast(1)
    }

    @Test
    fun `all timeout values should be positive`() {
        assertThat(Constants.DEFAULT_TIMEOUT).isGreaterThan(0L)
        assertThat(Constants.CONNECT_TIMEOUT).isGreaterThan(0L)
        assertThat(Constants.READ_TIMEOUT).isGreaterThan(0L)
    }

    @Test
    fun `all port values should be valid`() {
        // Valid port range is 1-65535
        assertThat(Constants.DOCKER_DEFAULT_PORT).isAtLeast(1)
        assertThat(Constants.DOCKER_DEFAULT_PORT).isAtMost(65535)
        assertThat(Constants.DOCKER_TLS_PORT).isAtLeast(1)
        assertThat(Constants.DOCKER_TLS_PORT).isAtMost(65535)
    }
}