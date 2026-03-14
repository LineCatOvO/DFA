package com.dfa.core.vm.storage.crypto

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安全随机数提供者
 *
 * 提供加密安全的随机数生成功能
 */
@Singleton
class SecureRandomProvider @Inject constructor() {

    private val secureRandom: SecureRandom by lazy {
        SecureRandom.getInstanceStrong() ?: SecureRandom()
    }

    /**
     * 生成指定长度的随机字节数组
     *
     * @param size 字节数
     * @return 随机字节数组
     */
    fun generateRandomBytes(size: Int): ByteArray {
        require(size > 0) { "Size must be positive" }
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * 生成随机整数
     *
     * @param bound 上界（不包含）
     * @return 随机整数
     */
    fun generateRandomInt(bound: Int): Int {
        require(bound > 0) { "Bound must be positive" }
        return secureRandom.nextInt(bound)
    }

    /**
     * 生成随机长整数
     *
     * @return 随机长整数
     */
    fun generateRandomLong(): Long {
        return secureRandom.nextLong()
    }

    /**
     * 生成随机布尔值
     *
     * @return 随机布尔值
     */
    fun generateRandomBoolean(): Boolean {
        return secureRandom.nextBoolean()
    }

    /**
     * 填充字节数组
     *
     * @param bytes 待填充的字节数组
     */
    fun fillRandomBytes(bytes: ByteArray) {
        secureRandom.nextBytes(bytes)
    }

    /**
     * 生成随机字符串
     *
     * @param length 字符串长度
     * @param charset 字符集
     * @return 随机字符串
     */
    fun generateRandomString(
        length: Int,
        charset: String = ALPHANUMERIC_CHARSET
    ): String {
        require(length > 0) { "Length must be positive" }
        require(charset.isNotEmpty()) { "Charset must not be empty" }

        val result = StringBuilder(length)
        for (i in 0 until length) {
            val index = secureRandom.nextInt(charset.length)
            result.append(charset[index])
        }
        return result.toString()
    }

    /**
     * 生成随机十六进制字符串
     *
     * @param length 字符串长度
     * @return 随机十六进制字符串
     */
    fun generateRandomHexString(length: Int): String {
        return generateRandomString(length, HEX_CHARSET)
    }

    /**
     * 生成随机Base64字符串
     *
     * @param byteCount 字节数
     * @return Base64编码的随机字符串
     */
    fun generateRandomBase64(byteCount: Int): String {
        val bytes = generateRandomBytes(byteCount)
        return android.util.Base64.encodeToString(
            bytes,
            android.util.Base64.NO_WRAP
        )
    }

    /**
     * 获取底层SecureRandom实例
     *
     * @return SecureRandom实例
     */
    fun getSecureRandom(): SecureRandom {
        return secureRandom
    }

    companion object {
        private const val ALPHANUMERIC_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private const val HEX_CHARSET = "0123456789abcdef"
    }
}