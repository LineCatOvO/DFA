package com.dfa.core.vm.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配额管理器实现
 *
 * 提供存储配额的管理和监控功能
 */
@Singleton
class QuotaManagerImpl @Inject constructor() : QuotaManager {

    private val mutex = Mutex()
    private val quotaLimits = ConcurrentHashMap<QuotaType, Long>()
    private val quotaUsage = ConcurrentHashMap<QuotaType, Long>()
    private val reservations = ConcurrentHashMap<String, QuotaReservation>()
    private val warningThresholds = ConcurrentHashMap<QuotaType, Int>()

    init {
        // 初始化默认配额限制
        quotaLimits[QuotaType.TOTAL_STORAGE] = 10L * 1024 * 1024 * 1024 // 10GB
        quotaLimits[QuotaType.DISK_IMAGES] = 5L * 1024 * 1024 * 1024 // 5GB
        quotaLimits[QuotaType.SNAPSHOTS] = 2L * 1024 * 1024 * 1024 // 2GB
        quotaLimits[QuotaType.ENCRYPTED_DATA] = 1L * 1024 * 1024 * 1024 // 1GB
        quotaLimits[QuotaType.TEMP_FILES] = 500L * 1024 * 1024 // 500MB
        quotaLimits[QuotaType.USER_DATA] = 2L * 1024 * 1024 * 1024 // 2GB

        // 初始化默认警告阈值
        QuotaType.entries.forEach { type ->
            warningThresholds[type] = 80
        }
    }

    override suspend fun setQuota(quotaType: QuotaType, limitBytes: Long): Result<Unit> = mutex.withLock {
        return try {
            if (limitBytes < 0) {
                return Result.failure(
                    StorageException.QuotaExceededException(
                        "Quota limit cannot be negative",
                        0, 0, 0
                    )
                )
            }

            quotaLimits[quotaType] = limitBytes
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to set quota: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun getQuotaLimit(quotaType: QuotaType): Long {
        return quotaLimits[quotaType] ?: 0L
    }

    override suspend fun getCurrentUsage(quotaType: QuotaType): Long {
        return quotaUsage[quotaType] ?: 0L
    }

    override suspend fun getRemainingQuota(quotaType: QuotaType): Long {
        val limit = quotaLimits[quotaType] ?: 0L
        val usage = quotaUsage[quotaType] ?: 0L
        val reserved = getReservedBytes(quotaType)
        return (limit - usage - reserved).coerceAtLeast(0)
    }

    override suspend fun hasEnoughQuota(quotaType: QuotaType, requiredBytes: Long): Boolean {
        return getRemainingQuota(quotaType) >= requiredBytes
    }

    override suspend fun reserveQuota(
        quotaType: QuotaType,
        bytes: Long,
        reservationId: String
    ): Result<QuotaReservation> = mutex.withLock {
        return try {
            if (bytes < 0) {
                return Result.failure(
                    StorageException.QuotaExceededException(
                        "Reservation bytes cannot be negative",
                        0, 0, 0
                    )
                )
            }

            if (!hasEnoughQuota(quotaType, bytes)) {
                val limit = getQuotaLimit(quotaType)
                val usage = getCurrentUsage(quotaType)
                return Result.failure(
                    StorageException.QuotaExceededException(
                        "Insufficient quota for reservation",
                        limit,
                        usage,
                        bytes
                    )
                )
            }

            val reservation = QuotaReservation(
                id = reservationId,
                quotaType = quotaType,
                bytes = bytes
            )

            reservations[reservationId] = reservation

            Result.success(reservation)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to reserve quota: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun commitReservation(reservationId: String): Result<Unit> = mutex.withLock {
        return try {
            val reservation = reservations[reservationId]
                ?: return Result.failure(
                    StorageException.PersistenceException("Reservation not found: $reservationId")
                )

            if (!reservation.isValid) {
                return Result.failure(
                    StorageException.PersistenceException("Reservation is not valid: $reservationId")
                )
            }

            // 更新使用量
            val currentUsage = quotaUsage[reservation.quotaType] ?: 0L
            quotaUsage[reservation.quotaType] = currentUsage + reservation.bytes

            // 标记预留为已提交
            reservations[reservationId] = reservation.copy(state = ReservationState.COMMITTED)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to commit reservation: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun cancelReservation(reservationId: String): Result<Unit> = mutex.withLock {
        return try {
            val reservation = reservations[reservationId]
                ?: return Result.failure(
                    StorageException.PersistenceException("Reservation not found: $reservationId")
                )

            // 标记预留为已取消
            reservations[reservationId] = reservation.copy(state = ReservationState.CANCELLED)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to cancel reservation: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun updateUsage(quotaType: QuotaType, deltaBytes: Long): Result<Unit> = mutex.withLock {
        return try {
            val currentUsage = quotaUsage[quotaType] ?: 0L
            val newUsage = (currentUsage + deltaBytes).coerceAtLeast(0)

            // 检查是否超限
            val limit = quotaLimits[quotaType] ?: 0L
            if (newUsage > limit) {
                return Result.failure(
                    StorageException.QuotaExceededException(
                        "Quota exceeded after update",
                        limit,
                        newUsage,
                        deltaBytes
                    )
                )
            }

            quotaUsage[quotaType] = newUsage
            Result.success(Unit)
        } catch (e: StorageException.QuotaExceededException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to update usage: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun resetUsage(quotaType: QuotaType): Result<Unit> = mutex.withLock {
        return try {
            quotaUsage[quotaType] = 0L
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to reset usage: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun getUsagePercent(quotaType: QuotaType): Int {
        val limit = quotaLimits[quotaType] ?: 0L
        val usage = quotaUsage[quotaType] ?: 0L

        return if (limit > 0) {
            ((usage * 100) / limit).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    override suspend fun isOverLimit(quotaType: QuotaType): Boolean {
        val limit = quotaLimits[quotaType] ?: 0L
        val usage = quotaUsage[quotaType] ?: 0L
        return usage > limit
    }

    override suspend fun getAllQuotaStatus(): List<QuotaStatus> {
        return QuotaType.entries.map { type ->
            QuotaStatus(
                type = type,
                limitBytes = quotaLimits[type] ?: 0L,
                usedBytes = quotaUsage[type] ?: 0L,
                reservedBytes = getReservedBytes(type),
                warningThreshold = warningThresholds[type] ?: 80
            )
        }
    }

    override suspend fun setWarningThreshold(quotaType: QuotaType, warningThreshold: Int): Result<Unit> = mutex.withLock {
        return try {
            if (warningThreshold !in 0..100) {
                return Result.failure(
                    StorageException.PersistenceException("Warning threshold must be between 0 and 100")
                )
            }

            warningThresholds[quotaType] = warningThreshold
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.PersistenceException(
                    "Failed to set warning threshold: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun needsWarning(quotaType: QuotaType): Boolean {
        val usagePercent = getUsagePercent(quotaType)
        val threshold = warningThresholds[quotaType] ?: 80
        return usagePercent >= threshold
    }

    override suspend fun cleanupExpiredReservations(): Int = mutex.withLock {
        var count = 0
        val toRemove = mutableListOf<String>()

        for ((id, reservation) in reservations) {
            if (reservation.isExpired && reservation.state == ReservationState.PENDING) {
                reservations[id] = reservation.copy(state = ReservationState.EXPIRED)
                toRemove.add(id)
                count++
            }
        }

        // 移除过期的预留
        toRemove.forEach { reservations.remove(it) }

        return count
    }

    // 私有方法

    private fun getReservedBytes(quotaType: QuotaType): Long {
        return reservations.values
            .filter { it.quotaType == quotaType && it.state == ReservationState.PENDING }
            .sumOf { it.bytes }
    }
}