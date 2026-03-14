package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.QuotaType

/**
 * 配额管理器接口
 *
 * 提供存储配额的管理和监控功能
 */
interface QuotaManager {

    /**
     * 设置配额
     *
     * @param quotaType 配额类型
     * @param limitBytes 限制字节数
     * @return 设置结果
     */
    suspend fun setQuota(quotaType: QuotaType, limitBytes: Long): Result<Unit>

    /**
     * 获取配额限制
     *
     * @param quotaType 配额类型
     * @return 限制字节数
     */
    suspend fun getQuotaLimit(quotaType: QuotaType): Long

    /**
     * 获取当前使用量
     *
     * @param quotaType 配额类型
     * @return 使用字节数
     */
    suspend fun getCurrentUsage(quotaType: QuotaType): Long

    /**
     * 获取剩余配额
     *
     * @param quotaType 配额类型
     * @return 剩余字节数
     */
    suspend fun getRemainingQuota(quotaType: QuotaType): Long

    /**
     * 检查是否有足够配额
     *
     * @param quotaType 配额类型
     * @param requiredBytes 需要的字节数
     * @return 是否有足够配额
     */
    suspend fun hasEnoughQuota(quotaType: QuotaType, requiredBytes: Long): Boolean

    /**
     * 预留配额
     *
     * @param quotaType 配额类型
     * @param bytes 字节数
     * @param reservationId 预留ID
     * @return 预留结果
     */
    suspend fun reserveQuota(
        quotaType: QuotaType,
        bytes: Long,
        reservationId: String
    ): Result<QuotaReservation>

    /**
     * 提交预留
     *
     * @param reservationId 预留ID
     * @return 提交结果
     */
    suspend fun commitReservation(reservationId: String): Result<Unit>

    /**
     * 取消预留
     *
     * @param reservationId 预留ID
     * @return 取消结果
     */
    suspend fun cancelReservation(reservationId: String): Result<Unit>

    /**
     * 更新使用量
     *
     * @param quotaType 配额类型
     * @param deltaBytes 变化字节数（正数增加，负数减少）
     * @return 更新结果
     */
    suspend fun updateUsage(quotaType: QuotaType, deltaBytes: Long): Result<Unit>

    /**
     * 重置使用量
     *
     * @param quotaType 配额类型
     * @return 重置结果
     */
    suspend fun resetUsage(quotaType: QuotaType): Result<Unit>

    /**
     * 获取配额使用率
     *
     * @param quotaType 配额类型
     * @return 使用率（0-100）
     */
    suspend fun getUsagePercent(quotaType: QuotaType): Int

    /**
     * 检查是否超限
     *
     * @param quotaType 配额类型
     * @return 是否超限
     */
    suspend fun isOverLimit(quotaType: QuotaType): Boolean

    /**
     * 获取所有配额状态
     *
     * @return 配额状态列表
     */
    suspend fun getAllQuotaStatus(): List<QuotaStatus>

    /**
     * 设置警告阈值
     *
     * @param quotaType 配额类型
     * @param warningThreshold 警告阈值（百分比）
     * @return 设置结果
     */
    suspend fun setWarningThreshold(quotaType: QuotaType, warningThreshold: Int): Result<Unit>

    /**
     * 检查是否需要警告
     *
     * @param quotaType 配额类型
     * @return 是否需要警告
     */
    suspend fun needsWarning(quotaType: QuotaType): Boolean

    /**
     * 清理过期预留
     *
     * @return 清理的数量
     */
    suspend fun cleanupExpiredReservations(): Int
}

/**
 * 配额类型枚举
 */
enum class QuotaType {
    /** 总存储配额 */
    TOTAL_STORAGE,
    /** 镜像配额 */
    DISK_IMAGES,
    /** 快照配额 */
    SNAPSHOTS,
    /** 加密数据配额 */
    ENCRYPTED_DATA,
    /** 临时文件配额 */
    TEMP_FILES,
    /** 用户数据配额 */
    USER_DATA
}

/**
 * 配额预留
 *
 * @property id 预留ID
 * @property quotaType 配额类型
 * @property bytes 预留字节数
 * @property createdAt 创建时间
 * @property expiresAt 过期时间
 * @property state 预留状态
 */
data class QuotaReservation(
    val id: String,
    val quotaType: QuotaType,
    val bytes: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + DEFAULT_RESERVATION_TIMEOUT,
    val state: ReservationState = ReservationState.PENDING
) {
    /**
     * 是否已过期
     */
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    /**
     * 是否有效
     */
    val isValid: Boolean
        get() = state == ReservationState.PENDING && !isExpired

    companion object {
        const val DEFAULT_RESERVATION_TIMEOUT = 5 * 60 * 1000L // 5分钟
    }
}

/**
 * 预留状态枚举
 */
enum class ReservationState {
    PENDING,
    COMMITTED,
    CANCELLED,
    EXPIRED
}

/**
 * 配额状态
 *
 * @property type 配额类型
 * @property limitBytes 限制字节数
 * @property usedBytes 已使用字节数
 * @property reservedBytes 预留字节数
 * @property availableBytes 可用字节数
 * @property warningThreshold 警告阈值
 */
data class QuotaStatus(
    val type: QuotaType,
    val limitBytes: Long,
    val usedBytes: Long,
    val reservedBytes: Long = 0,
    val availableBytes: Long = limitBytes - usedBytes - reservedBytes,
    val warningThreshold: Int = 80
) {
    /**
     * 使用率（0-100）
     */
    val usagePercent: Int
        get() = if (limitBytes > 0) {
            ((usedBytes * 100) / limitBytes).toInt().coerceIn(0, 100)
        } else 0

    /**
     * 是否超限
     */
    val isOverLimit: Boolean
        get() = usedBytes > limitBytes

    /**
     * 是否需要警告
     */
    val needsWarning: Boolean
        get() = usagePercent >= warningThreshold

    /**
     * 是否有足够配额
     */
    fun hasEnough(bytes: Long): Boolean = availableBytes >= bytes
}