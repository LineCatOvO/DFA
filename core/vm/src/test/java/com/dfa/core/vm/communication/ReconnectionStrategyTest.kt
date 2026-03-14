package com.dfa.core.vm.communication

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ReconnectionStrategy单元测试
 */
class ReconnectionStrategyTest {

    private lateinit var exponentialBackoff: ExponentialBackoffStrategy
    private lateinit var fixedDelay: FixedDelayStrategy
    private lateinit var linearBackoff: LinearBackoffStrategy

    @Before
    fun setup() {
        exponentialBackoff = ExponentialBackoffStrategy(ReconnectionConfig())
        fixedDelay = FixedDelayStrategy()
        linearBackoff = LinearBackoffStrategy(ReconnectionConfig())
    }

    @Test
    fun `ExponentialBackoffStrategy shouldReconnect returns true for transient errors`() {
        val error = CommunicationError.ConnectionError("Connection failed")
        assertTrue(exponentialBackoff.shouldReconnect(0, error))
        assertTrue(exponentialBackoff.shouldReconnect(2, error))
    }

    @Test
    fun `ExponentialBackoffStrategy shouldReconnect returns false for permission errors`() {
        val error = CommunicationError.PermissionError("Permission denied")
        assertFalse(exponentialBackoff.shouldReconnect(0, error))
    }

    @Test
    fun `ExponentialBackoffStrategy shouldReconnect returns false after max attempts`() {
        val error = CommunicationError.ConnectionError("Connection failed")
        val maxAttempts = exponentialBackoff.getMaxAttempts()
        assertFalse(exponentialBackoff.shouldReconnect(maxAttempts, error))
    }

    @Test
    fun `ExponentialBackoffStrategy getReconnectDelay increases exponentially`() {
        val delay0 = exponentialBackoff.getReconnectDelay(0)
        val delay1 = exponentialBackoff.getReconnectDelay(1)
        val delay2 = exponentialBackoff.getReconnectDelay(2)

        assertTrue(delay1 > delay0)
        assertTrue(delay2 > delay1)
    }

    @Test
    fun `ExponentialBackoffStrategy getReconnectDelay respects max delay`() {
        val config = ReconnectionConfig(
            initialDelayMs = 1000L,
            maxDelayMs = 5000L,
            multiplier = 10.0
        )
        val strategy = ExponentialBackoffStrategy(config)

        val delay = strategy.getReconnectDelay(10)
        assertTrue(delay <= config.maxDelayMs)
    }

    @Test
    fun `FixedDelayStrategy returns constant delay`() {
        val delay0 = fixedDelay.getReconnectDelay(0)
        val delay1 = fixedDelay.getReconnectDelay(1)
        val delay2 = fixedDelay.getReconnectDelay(2)

        assertEquals(delay0, delay1)
        assertEquals(delay1, delay2)
    }

    @Test
    fun `LinearBackoffStrategy getReconnectDelay increases linearly`() {
        val config = ReconnectionConfig(initialDelayMs = 1000L)
        val strategy = LinearBackoffStrategy(config)

        val delay0 = strategy.getReconnectDelay(0)
        val delay1 = strategy.getReconnectDelay(1)
        val delay2 = strategy.getReconnectDelay(2)

        assertEquals(1000L, delay0)
        assertEquals(2000L, delay1)
        assertEquals(3000L, delay2)
    }

    @Test
    fun `ReconnectionStrategyFactory creates correct strategy types`() {
        val factory = ReconnectionStrategyFactory()

        val exponential = factory.create(ReconnectionStrategyType.EXPONENTIAL_BACKOFF)
        assertTrue(exponential is ExponentialBackoffStrategy)

        val fixed = factory.create(ReconnectionStrategyType.FIXED_DELAY)
        assertTrue(fixed is FixedDelayStrategy)

        val linear = factory.create(ReconnectionStrategyType.LINEAR_BACKOFF)
        assertTrue(linear is LinearBackoffStrategy)

        val adaptive = factory.create(ReconnectionStrategyType.ADAPTIVE)
        assertTrue(adaptive is AdaptiveReconnectionStrategy)
    }

    @Test
    fun `ReconnectionConfig validates correctly`() {
        val validConfig = ReconnectionConfig()
        assertTrue(validConfig.validate())

        val invalidConfig = ReconnectionConfig(
            maxAttempts = 0,
            initialDelayMs = 0
        )
        assertFalse(invalidConfig.validate())
    }
}