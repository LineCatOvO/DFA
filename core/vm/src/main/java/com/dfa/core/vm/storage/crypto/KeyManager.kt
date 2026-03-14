package com.dfa.core.vm.storage.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.dfa.core.vm.storage.models.EncryptionAlgorithm
import com.dfa.core.vm.storage.models.EncryptionConfig
import com.dfa.core.vm.storage.models.KeyGenerationRequest
import com.dfa.core.vm.storage.models.KeyInfo
import com.dfa.core.vm.storage.models.KeyState
import com.dfa.core.vm.storage.StorageException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import java.util.Date
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 密钥管理器
 *
 * 提供密钥生成、存储、检索和删除功能
 * 支持Android Keystore和内存密钥存储
 */
@Singleton
class KeyManager @Inject constructor(
    private val secureRandomProvider: SecureRandomProvider
) {

    private val mutex = Mutex()
    private val memoryKeyStore = mutableMapOf<String, SecretKey>()
    private val keyInfoCache = mutableMapOf<String, KeyInfo>()

    private val keyStore: KeyStore? by lazy {
        try {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
                load(null)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 生成密钥
     *
     * @param request 密钥生成请求
     * @return 生成的密钥信息
     */
    suspend fun generateKey(request: KeyGenerationRequest): Result<KeyInfo> = mutex.withLock {
        return try {
            if (!request.validate()) {
                return Result.failure(
                    StorageException.KeyManagementException("Invalid key generation request")
                )
            }

            val keyInfo = if (shouldUseAndroidKeystore()) {
                generateKeystoreKey(request)
            } else {
                generateMemoryKey(request)
            }

            keyInfoCache[request.alias] = keyInfo
            Result.success(keyInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.KeyManagementException(
                    "Failed to generate key: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * 获取密钥
     *
     * @param alias 密钥别名
     * @return 密钥
     */
    suspend fun getKey(alias: String): Result<SecretKey> = mutex.withLock {
        return try {
            val key = if (shouldUseAndroidKeystore()) {
                getKeystoreKey(alias)
            } else {
                getMemoryKey(alias)
            }

            if (key != null) {
                updateKeyLastUsed(alias)
                Result.success(key)
            } else {
                Result.failure(
                    StorageException.KeyManagementException("Key not found: $alias")
                )
            }
        } catch (e: UnrecoverableKeyException) {
            Result.failure(
                StorageException.KeyManagementException("Cannot recover key: $alias")
            )
        } catch (e: Exception) {
            Result.failure(
                StorageException.KeyManagementException(
                    "Failed to get key: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * 检查密钥是否存在
     *
     * @param alias 密钥别名
     * @return 是否存在
     */
    suspend fun hasKey(alias: String): Boolean = mutex.withLock {
        return try {
            if (shouldUseAndroidKeystore()) {
                keyStore?.containsAlias(alias) ?: false
            } else {
                memoryKeyStore.containsKey(alias)
            }
        } catch (e: KeyStoreException) {
            false
        }
    }

    /**
     * 删除密钥
     *
     * @param alias 密钥别名
     * @return 删除结果
     */
    suspend fun deleteKey(alias: String): Result<Unit> = mutex.withLock {
        return try {
            if (shouldUseAndroidKeystore()) {
                keyStore?.deleteEntry(alias)
            } else {
                memoryKeyStore.remove(alias)
            }
            keyInfoCache.remove(alias)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.KeyManagementException(
                    "Failed to delete key: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * 获取密钥信息
     *
     * @param alias 密钥别名
     * @return 密钥信息
     */
    suspend fun getKeyInfo(alias: String): Result<KeyInfo> = mutex.withLock {
        return keyInfoCache[alias]?.let {
            Result.success(it)
        } ?: Result.failure(
            StorageException.KeyManagementException("Key info not found: $alias")
        )
    }

    /**
     * 列出所有密钥
     *
     * @return 密钥别名列表
     */
    suspend fun listKeys(): Result<List<String>> = mutex.withLock {
        return try {
            val aliases = if (shouldUseAndroidKeystore()) {
                keyStore?.aliases()?.toList() ?: emptyList()
            } else {
                memoryKeyStore.keys.toList()
            }
            Result.success(aliases)
        } catch (e: Exception) {
            Result.failure(
                StorageException.KeyManagementException(
                    "Failed to list keys: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * 从密码派生密钥
     *
     * @param password 密码
     * @param salt 盐值
     * @param keySize 密钥大小
     * @return 派生的密钥
     */
    fun deriveKeyFromPassword(
        password: CharArray,
        salt: ByteArray,
        keySize: Int = 256
    ): SecretKey {
        val spec = PBEKeySpec(
            password,
            salt,
            PBKDF2_ITERATIONS,
            keySize
        )
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec)
    }

    /**
     * 生成盐值
     *
     * @param size 盐值大小
     * @return 盐值
     */
    fun generateSalt(size: Int = 32): ByteArray {
        return secureRandomProvider.generateRandomBytes(size)
    }

    // 私有方法

    private fun shouldUseAndroidKeystore(): Boolean {
        return keyStore != null
    }

    private fun generateKeystoreKey(request: KeyGenerationRequest): KeyInfo {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        val blockModes = if (request.algorithm == EncryptionAlgorithm.AES_128_GCM ||
            request.algorithm == EncryptionAlgorithm.AES_256_GCM
        ) {
            KeyProperties.BLOCK_MODE_GCM
        } else {
            KeyProperties.BLOCK_MODE_CBC
        }
        val padding = KeyProperties.ENCRYPTION_PADDING_NONE

        val specBuilder = KeyGenParameterSpec.Builder(request.alias, purposes)
            .setBlockModes(blockModes)
            .setEncryptionPaddings(padding)
            .setKeySize(request.keySize)

        if (request.requireUserAuthentication) {
            specBuilder.setUserAuthenticationRequired(true)
            request.validityDurationSeconds?.let {
                specBuilder.setUserAuthenticationValidityDurationSeconds(it)
            }
        }

        keyGenerator.init(specBuilder.build())
        keyGenerator.generateKey()

        return KeyInfo(
            alias = request.alias,
            algorithm = request.algorithm,
            keySize = request.keySize,
            state = KeyState.ACTIVE,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun generateMemoryKey(request: KeyGenerationRequest): KeyInfo {
        val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES)
        keyGenerator.init(request.keySize, secureRandomProvider.getSecureRandom())
        val key = keyGenerator.generateKey()

        memoryKeyStore[request.alias] = key

        return KeyInfo(
            alias = request.alias,
            algorithm = request.algorithm,
            keySize = request.keySize,
            state = KeyState.ACTIVE,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun getKeystoreKey(alias: String): SecretKey? {
        return try {
            keyStore?.getKey(alias, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }

    private fun getMemoryKey(alias: String): SecretKey? {
        return memoryKeyStore[alias]
    }

    private fun updateKeyLastUsed(alias: String) {
        keyInfoCache[alias]?.let { info ->
            keyInfoCache[alias] = info.copy(lastUsedAt = System.currentTimeMillis())
        }
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALGORITHM_AES = "AES"
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 100000
    }
}