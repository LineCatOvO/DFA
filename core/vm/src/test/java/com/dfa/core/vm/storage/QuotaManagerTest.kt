package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.QuotaType
import com.dfa.core.vm.storage.models.QuotaStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * QuotaManager单元测试
 */
class QuotaManagerTest {

    private lateinit var quotaManager: QuotaManagerImpl

    @Before
    fun setup() {
        quotaManager = QuotaManagerImpl()
    }

    @Test
    fun `setQuota should update quota limit`() = runBlocking {
        val limit = 1024L * 1024 * 1024 // 1GB
        val result = quotaManager.setQuota(QuotaType.TOTAL_STORAGE, limit)

        assertTrue("Set quota should succeed", result.isSuccess)
        assertEquals("Quota limit should be updated", limit, quotaManager.getQuotaLimit(QuotaType.TOTAL_STORAGE))
    }

    @Test
    fun `getCurrentUsage should return zero initially`() = runBlocking {
        val usage = quotaManager.getCurrentUsage(QuotaType.TOTAL_STORAGE)
        assertEquals("Initial usage should be zero", 0L, usage)
    }

    @Test
    fun `updateUsage should increase usage`() = runBlocking {
        val delta = 1024L * 1024 // 1MB
        quotaManager.updateUsage(QuotaType.TOTAL_STORAGE, delta)

        val usage = quotaManager.getCurrentUsage(QuotaType.TOTAL_STORAGE)
        assertEquals("Usage should be increased", delta, usage)
    }

    @Test
    fun `updateUsage with negative delta should decrease usage`() = runBlocking {
        // First increase
        quotaManager.updateUsage(QuotaType.TOTAL_STORAGE, 1024L * 1024)
        // Then decrease
        quotaManager.updateUsage(QuotaType.TOTAL_STORAGE, -512L * 1024)

        val usage = quotaManager.getCurrentUsage(QuotaType.TOTAL_STORAGE)
        assertEquals("Usage should be decreased", 512L * 1024, usage)
    }

    @Test
    fun `hasEnoughQuota should return true when enough space`() = runBlocking {
        quotaManager.setQuota(QuotaType.TOTAL_STORAGE, 1024L * 1024 * 1024) // 1GB

        val hasEnough = quotaManager.hasEnoughQuota(QuotaType.TOTAL_STORAGE, 512L * 1024 * 1024) // 512MB
        assertTrue("Should have enough quota", hasEnough)
    }

    @Test
    fun `hasEnoughQuota should return false when not enough space`() = runBlocking {
        quotaManager.setQuota(QuotaType.TOTAL_STORAGE, 512L * 1024 * 1024) // 512MB
        quotaManager.updateUsage(QuotaType.TOTAL_STORAGE, 256L * 1024 * 1024) // Use 256MB

        val hasEnough = quotaManager.hasEnoughQuota(QuotaType.TOTAL_STORAGE, 512L * 1024 * 1024) // Need 512MB
        assertFalse("Should not have enough quota", hasEnough)
    }

    @Test
    fun `reserveQuota should succeed when enough space`() = runBlocking {
        quotaManager.setQuota(QuotaType.TOTAL_STORAGE, 1024L * 1024 * 1024) // 1GB

        val result = quotaManager.reserveQuota(
            QuotaType.TOTAL_STORAGE,
            512L * 1024 * 1024,
            "reservation_1"
        )

        assertTrue("Reservation should succeed", result.isSuccess)
        assertNotNull("Reservation should be returned", result.getOrNull())
    }

    @Test
    fun `commitReservation should update usage`() = runBlocking {
        quotaManager.setQuota(QuotaType.TOTAL_STORAGE, 1024L * 1024 * 1024)
        val reserveResult = quotaManager.reserveQuota(
            QuotaType.TOTAL_STORAGE,
            512L * 1024 * 1024,
            "reservation_2"
        )

        val commitResult = quotaManager.commitReservation("reservation_2")
        assertTrue("Commit should succeed", commitResult.isSuccess)

        val usage = quotaManager.getCurrentUsage(QuotaType.TOTAL_STORAGE)
        assertEquals("Usage should reflect committed reservation", 512L * 1024 * 1024, usage)
    }

    @Test
    fun `cancelReservation should not affect usage`() = runBlocking {
        quotaManager.setQuota(QuotaType.TOTAL_STORAGE, 1024L * 1024 * 1024)
        quotaManager.reserveQuota(
            QuotaType.TOTAL_STORAGE,
            512L * 1024 * 1024,
            "reservation_3"
        )

        val cancelResult = quotaManager.cancelReservation("reservation_3")
        assertTrue("Cancel should succeed", cancelResult.isSuccess)

        val usage = quotaManager.getCurrentUsage(QuotaType.TOTAL_STORAGE)
        assertEquals("Usage should remain zero", 0L, usage)
    }

    @Test
    fun `getUsagePercent should return correct percentage`() = runBlocking {
        quotaManager.setQuota(QuotaType.TOTAL_STORAGE, 1000L)
        quotaManager.updateUsage(QuotaType.TOTAL_STORAGE, 500L)

        val percent = quotaManager.getUsagePercent(QuotaType.TOTAL_STORAGE)
        assertEquals("Usage percent should be 50", 50, percent)
    }

    @Test
    fun `isOverLimit should return true when exceeded`() = runBlocking {
        quotaManager.setQuota(QuotaType.TOTAL_STORAGE, 1000L)
        quotaManager.updateUsage(QuotaType.TOTAL_STORAGE, 1500L)

        assertTrue("Should be over limit", quotaManager.isOverLimit(QuotaType.TOTAL_STORAGE))
    }

    @Test
    fun `setWarningThreshold should update threshold`() = runBlocking {
        val result = quotaManager.setWarningThreshold(QuotaType.TOTAL_STORAGE, 90)
        assertTrue("Set threshold should succeed", result.isSuccess)
        assertTrue("Should need warning at 90%", quotaManager.needsWarning(QuotaType.TOTAL_STORAGE).not())

        quotaManager.setQuota(QuotaType.TOTAL_STORAGE, 1000L)
        quotaManager.updateUsage(QuotaType.TOTAL_STORAGE, 950L)

        assertTrue("Should need warning when usage exceeds threshold", quotaManager.needsWarning(QuotaType.TOTAL_STORAGE))
    }

    @Test
    fun `getAllQuotaStatus should return all quota types`() = runBlocking {
        val statuses = quotaManager.getAllQuotaStatus()

        assertTrue("Should return all quota types", statuses.isNotEmpty())
        assertTrue("Should contain TOTAL_STORAGE", statuses.any { it.type == QuotaType.TOTAL_STORAGE })
    }

    @Test
    fun `resetUsage should set usage to zero`() = runBlocking {
        quotaManager.setQuota(QuotaType.TOTAL_STORAGE, 1000L)
        quotaManager.updateUsage(QuotaType.TOTAL_STORAGE, 500L)

        quotaManager.resetUsage(QuotaType.TOTAL_STORAGE)

        assertEquals("Usage should be reset to zero", 0L, quotaManager.getCurrentUsage(QuotaType.TOTAL_STORAGE))
    }
}