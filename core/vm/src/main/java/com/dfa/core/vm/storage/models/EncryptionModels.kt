package com.dfa.core.vm.storage.models

/**
 * 加密算法枚举
 */
enum class EncryptionAlgorithm {
    /** AES-128-GCM */
    AES_128_GCM,
    /** AES-256-GCM */
    AES_256_GCM,
    /** AES-128-CBC */
    AES_128_CBC,
    /** AES-256-CBC */
    AES_256_CBC,
    /** ChaCha20-Poly1305 */
    CHACHA20_POLY1305
}

/**
 * 密钥状态枚举
 */
enum class KeyState {
    /** 已创建 */
    CREATED,
    /** 已激活 */
    ACTIVE,
    /** 已停用 */
    DEACTIVATED,
    /** 已过期 */
    EXPIRED,
    /** 已删除 */
    DELETED
}

/**
 * 加密配置
 *
 * @property algorithm 加密算法
 * @property keyAlias 密钥别名
 * @property keySize 密钥大小（位）
 * @property ivSize 初始化向量大小（字节）
 * @property tagSize 认证标签大小（位）
 * @property useAndroidKeystore 是否使用Android Keystore
 */
data class EncryptionConfig(
    val algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_256_GCM,
    val keyAlias: String = "dfa_storage_key",
    val keySize: Int = 256,
    val ivSize: Int = 12,
    val tagSize: Int = 128,
    val useAndroidKeystore: Boolean = true
) {
    fun validate(): Boolean {
        return keyAlias.isNotEmpty() &&
                keySize in listOf(128, 192, 256) &&
                ivSize > 0 &&
                tagSize in listOf(96, 128)
    }

    /**
     * 获取密钥字节数
     */
    val keySizeBytes: Int
        get() = keySize / 8
}

/**
 * 密钥信息
 *
 * @property alias 密钥别名
 * @property algorithm 算法
 * @property keySize 密钥大小
 * @property state 密钥状态
 * @property createdAt 创建时间
 * @property lastUsedAt 最后使用时间
 * @property expiresAt 过期时间
 */
data class KeyInfo(
    val alias: String,
    val algorithm: EncryptionAlgorithm,
    val keySize: Int,
    val state: KeyState = KeyState.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val expiresAt: Long? = null
) {
    /**
     * 是否有效
     */
    val isValid: Boolean
        get() = state == KeyState.ACTIVE && !isExpired

    /**
     * 是否过期
     */
    val isExpired: Boolean
        get() = expiresAt != null && System.currentTimeMillis() > expiresAt

    /**
     * 是否可用
     */
    val isUsable: Boolean
        get() = isValid
}

/**
 * 加密数据
 *
 * @property ciphertext 密文
 * @property iv 初始化向量
 * @property tag 认证标签
 * @property algorithm 使用的算法
 * @property version 版本号
 */
data class EncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val tag: ByteArray? = null,
    val algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_256_GCM,
    val version: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedData

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (tag != null) {
            if (other.tag == null) return false
            if (!tag.contentEquals(other.tag)) return false
        } else if (other.tag != null) return false
        if (algorithm != other.algorithm) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + (tag?.contentHashCode() ?: 0)
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + version
        return result
    }

    /**
     * 序列化为字节数组
     */
    fun toByteArray(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        output.write(version)
        output.write(algorithm.ordinal)
        output.write(iv.size)
        output.write(iv)
        if (tag != null) {
            output.write(tag.size)
            output.write(tag)
        } else {
            output.write(0)
        }
        output.write(ciphertext)
        return output.toByteArray()
    }

    companion object {
        /**
         * 从字节数组反序列化
         */
        fun fromByteArray(data: ByteArray): EncryptedData {
            val input = java.io.ByteArrayInputStream(data)
            val version = input.read()
            val algorithmOrdinal = input.read()
            val algorithm = EncryptionAlgorithm.entries[algorithmOrdinal]
            val ivSize = input.read()
            val iv = ByteArray(ivSize)
            input.read(iv)
            val tagSize = input.read()
            val tag = if (tagSize > 0) {
                ByteArray(tagSize).also { input.read(it) }
            } else null
            val ciphertext = input.readAllBytes()
            return EncryptedData(
                ciphertext = ciphertext,
                iv = iv,
                tag = tag,
                algorithm = algorithm,
                version = version
            )
        }
    }
}

/**
 * 加密请求
 *
 * @property data 待加密数据
 * @property config 加密配置
 * @property associatedData 关联数据（用于AEAD）
 */
data class EncryptionRequest(
    val data: ByteArray,
    val config: EncryptionConfig = EncryptionConfig(),
    val associatedData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptionRequest

        if (!data.contentEquals(other.data)) return false
        if (config != other.config) return false
        if (associatedData != null) {
            if (other.associatedData == null) return false
            if (!associatedData.contentEquals(other.associatedData)) return false
        } else if (other.associatedData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + (associatedData?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 解密请求
 *
 * @property encryptedData 加密数据
 * @property config 加密配置
 * @property associatedData 关联数据（用于AEAD）
 */
data class DecryptionRequest(
    val encryptedData: EncryptedData,
    val config: EncryptionConfig = EncryptionConfig(),
    val associatedData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DecryptionRequest

        if (encryptedData != other.encryptedData) return false
        if (config != other.config) return false
        if (associatedData != null) {
            if (other.associatedData == null) return false
            if (!associatedData.contentEquals(other.associatedData)) return false
        } else if (other.associatedData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedData.hashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + (associatedData?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 加密操作结果
 *
 * @property success 是否成功
 * @property encryptedData 加密后的数据
 * @property errorMessage 错误信息
 */
data class EncryptionResult(
    val success: Boolean,
    val encryptedData: EncryptedData? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun success(encryptedData: EncryptedData): EncryptionResult {
            return EncryptionResult(
                success = true,
                encryptedData = encryptedData
            )
        }

        fun failure(error: String): EncryptionResult {
            return EncryptionResult(
                success = false,
                errorMessage = error
            )
        }
    }
}

/**
 * 解密操作结果
 *
 * @property success 是否成功
 * @property data 解密后的数据
 * @property errorMessage 错误信息
 */
data class DecryptionResult(
    val success: Boolean,
    val data: ByteArray? = null,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DecryptionResult

        if (success != other.success) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun success(data: ByteArray): DecryptionResult {
            return DecryptionResult(
                success = true,
                data = data
            )
        }

        fun failure(error: String): DecryptionResult {
            return DecryptionResult(
                success = false,
                errorMessage = error
            )
        }
    }
}

/**
 * 密钥生成请求
 *
 * @property alias 密钥别名
 * @property algorithm 加密算法
 * @property keySize 密钥大小
 * @property requireUserAuthentication 是否需要用户认证
 * @property validityDurationSeconds 有效期（秒）
 */
data class KeyGenerationRequest(
    val alias: String,
    val algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_256_GCM,
    val keySize: Int = 256,
    val requireUserAuthentication: Boolean = false,
    val validityDurationSeconds: Int? = null
) {
    fun validate(): Boolean {
        return alias.isNotEmpty() &&
                keySize in listOf(128, 192, 256)
    }
}