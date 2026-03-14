package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.crypto.AesCipher
import com.dfa.core.vm.storage.crypto.KeyManager
import com.dfa.core.vm.storage.models.DecryptionRequest
import com.dfa.core.vm.storage.models.DecryptionResult
import com.dfa.core.vm.storage.models.EncryptedData
import com.dfa.core.vm.storage.models.EncryptionConfig
import com.dfa.core.vm.storage.models.EncryptionRequest
import com.dfa.core.vm.storage.models.EncryptionResult
import com.dfa.core.vm.storage.models.KeyGenerationRequest
import com.dfa.core.vm.storage.models.KeyInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 加密管理器实现
 *
 * 协调密钥管理和加密解密操作
 */
@Singleton
class EncryptionManagerImpl @Inject constructor(
    private val keyManager: KeyManager,
    private val aesCipher: AesCipher
) : EncryptionManager {

    private val mutex = Mutex()
    private var config: EncryptionConfig? = null
    private var isReady = false

    override suspend fun initialize(config: EncryptionConfig): Result<Unit> = mutex.withLock {
        return try {
            if (!config.validate()) {
                return Result.failure(
                    StorageException.EncryptionException("Invalid encryption configuration")
                )
            }

            // 检查或生成密钥
            val hasKey = keyManager.hasKey(config.keyAlias)
            if (!hasKey) {
                val keyRequest = KeyGenerationRequest(
                    alias = config.keyAlias,
                    algorithm = config.algorithm,
                    keySize = config.keySize
                )
                val keyResult = keyManager.generateKey(keyRequest)
                if (keyResult.isFailure) {
                    return Result.failure(
                        StorageException.EncryptionException(
                            "Failed to generate encryption key: ${keyResult.exceptionOrNull()?.message}"
                        )
                    )
                }
            }

            this.config = config
            this.isReady = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.EncryptionException(
                    "Initialization failed: ${e.message}",
                    e
                )
            )
        }
    }

    override suspend fun encrypt(request: EncryptionRequest): EncryptionResult {
        if (!isReady) {
            return EncryptionResult.failure("Encryption manager not initialized")
        }

        val currentConfig = config ?: return EncryptionResult.failure("No configuration")

        return try {
            val keyResult = keyManager.getKey(currentConfig.keyAlias)
            if (keyResult.isFailure) {
                return EncryptionResult.failure(
                    "Failed to get encryption key: ${keyResult.exceptionOrNull()?.message}"
                )
            }

            val key = keyResult.getOrThrow()
            aesCipher.encrypt(
                plaintext = request.data,
                key = key,
                config = currentConfig,
                associatedData = request.associatedData
            )
        } catch (e: Exception) {
            EncryptionResult.failure("Encryption failed: ${e.message}")
        }
    }

    override suspend fun decrypt(request: DecryptionRequest): DecryptionResult {
        if (!isReady) {
            return DecryptionResult.failure("Encryption manager not initialized")
        }

        val currentConfig = config ?: return DecryptionResult.failure("No configuration")

        return try {
            val keyResult = keyManager.getKey(currentConfig.keyAlias)
            if (keyResult.isFailure) {
                return DecryptionResult.failure(
                    "Failed to get decryption key: ${keyResult.exceptionOrNull()?.message}"
                )
            }

            val key = keyResult.getOrThrow()
            aesCipher.decrypt(
                encryptedData = request.encryptedData,
                key = key,
                config = currentConfig,
                associatedData = request.associatedData
            )
        } catch (e: Exception) {
            DecryptionResult.failure("Decryption failed: ${e.message}")
        }
    }

    override suspend fun encryptData(
        data: ByteArray,
        associatedData: ByteArray?
    ): EncryptionResult {
        val request = EncryptionRequest(
            data = data,
            config = config ?: EncryptionConfig(),
            associatedData = associatedData
        )
        return encrypt(request)
    }

    override suspend fun decryptData(
        encryptedData: ByteArray,
        associatedData: ByteArray?
    ): DecryptionResult {
        val data = try {
            EncryptedData.fromByteArray(encryptedData)
        } catch (e: Exception) {
            return DecryptionResult.failure("Invalid encrypted data format")
        }

        val request = DecryptionRequest(
            encryptedData = data,
            config = config ?: EncryptionConfig(),
            associatedData = associatedData
        )
        return decrypt(request)
    }

    override suspend fun generateKey(request: KeyGenerationRequest): Result<KeyInfo> {
        return keyManager.generateKey(request)
    }

    override suspend fun hasKey(alias: String): Boolean {
        return keyManager.hasKey(alias)
    }

    override suspend fun deleteKey(alias: String): Result<Unit> {
        return keyManager.deleteKey(alias)
    }

    override suspend fun getKeyInfo(alias: String): Result<KeyInfo> {
        return keyManager.getKeyInfo(alias)
    }

    override fun isInitialized(): Boolean = isReady

    override fun getConfig(): EncryptionConfig? = config

    override suspend fun release() = mutex.withLock {
        config = null
        isReady = false
    }
}