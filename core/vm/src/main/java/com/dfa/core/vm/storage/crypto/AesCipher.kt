package com.dfa.core.vm.storage.crypto

import com.dfa.core.vm.storage.models.EncryptionAlgorithm
import com.dfa.core.vm.storage.models.EncryptionConfig
import com.dfa.core.vm.storage.models.EncryptedData
import com.dfa.core.vm.storage.models.EncryptionResult
import com.dfa.core.vm.storage.models.DecryptionResult
import com.dfa.core.vm.storage.StorageException
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES加密器
 *
 * 提供AES-GCM和AES-CBC加密解密功能
 */
@Singleton
class AesCipher @Inject constructor(
    private val secureRandomProvider: SecureRandomProvider
) {

    /**
     * 加密数据
     *
     * @param plaintext 明文
     * @param key 密钥
     * @param config 加密配置
     * @param associatedData 关联数据（用于AEAD模式）
     * @return 加密结果
     */
    fun encrypt(
        plaintext: ByteArray,
        key: SecretKey,
        config: EncryptionConfig,
        associatedData: ByteArray? = null
    ): EncryptionResult {
        return try {
            val cipher = createCipher(config.algorithm)
            val iv = generateIv(config.ivSize)

            initCipherForEncryption(cipher, key, iv, config, associatedData)
            val ciphertext = cipher.doFinal(plaintext)

            val encryptedData = when (config.algorithm) {
                EncryptionAlgorithm.AES_128_GCM,
                EncryptionAlgorithm.AES_256_GCM -> {
                    // GCM模式，标签包含在密文中
                    val tagSize = config.tagSize / 8
                    val tagStart = ciphertext.size - tagSize
                    EncryptedData(
                        ciphertext = ciphertext.copyOfRange(0, tagStart),
                        iv = iv,
                        tag = ciphertext.copyOfRange(tagStart, ciphertext.size),
                        algorithm = config.algorithm
                    )
                }
                else -> {
                    EncryptedData(
                        ciphertext = ciphertext,
                        iv = iv,
                        algorithm = config.algorithm
                    )
                }
            }

            EncryptionResult.success(encryptedData)
        } catch (e: Exception) {
            EncryptionResult.failure("Encryption failed: ${e.message}")
        }
    }

    /**
     * 解密数据
     *
     * @param encryptedData 加密数据
     * @param key 密钥
     * @param config 加密配置
     * @param associatedData 关联数据（用于AEAD模式）
     * @return 解密结果
     */
    fun decrypt(
        encryptedData: EncryptedData,
        key: SecretKey,
        config: EncryptionConfig,
        associatedData: ByteArray? = null
    ): DecryptionResult {
        return try {
            val cipher = createCipher(encryptedData.algorithm)

            initCipherForDecryption(cipher, key, encryptedData, config, associatedData)

            val plaintext = when (encryptedData.algorithm) {
                EncryptionAlgorithm.AES_128_GCM,
                EncryptionAlgorithm.AES_256_GCM -> {
                    // GCM模式，需要将标签附加到密文
                    val ciphertextWithTag = encryptedData.ciphertext + (encryptedData.tag ?: byteArrayOf())
                    cipher.doFinal(ciphertextWithTag)
                }
                else -> {
                    cipher.doFinal(encryptedData.ciphertext)
                }
            }

            DecryptionResult.success(plaintext)
        } catch (e: Exception) {
            DecryptionResult.failure("Decryption failed: ${e.message}")
        }
    }

    /**
     * 创建Cipher实例
     */
    private fun createCipher(algorithm: EncryptionAlgorithm): Cipher {
        val transformation = when (algorithm) {
            EncryptionAlgorithm.AES_128_GCM,
            EncryptionAlgorithm.AES_256_GCM -> AES_GCM_TRANSFORMATION
            EncryptionAlgorithm.AES_128_CBC,
            EncryptionAlgorithm.AES_256_CBC -> AES_CBC_TRANSFORMATION
            EncryptionAlgorithm.CHACHA20_POLY1305 -> throw StorageException.EncryptionException(
                "ChaCha20-Poly1305 not supported in this implementation"
            )
        }
        return Cipher.getInstance(transformation)
    }

    /**
     * 初始化加密模式的Cipher
     */
    private fun initCipherForEncryption(
        cipher: Cipher,
        key: SecretKey,
        iv: ByteArray,
        config: EncryptionConfig,
        associatedData: ByteArray?
    ) {
        when (config.algorithm) {
            EncryptionAlgorithm.AES_128_GCM,
            EncryptionAlgorithm.AES_256_GCM -> {
                val spec = GCMParameterSpec(config.tagSize, iv)
                cipher.init(Cipher.ENCRYPT_MODE, key, spec)
                associatedData?.let { cipher.updateAAD(it) }
            }
            EncryptionAlgorithm.AES_128_CBC,
            EncryptionAlgorithm.AES_256_CBC -> {
                val spec = IvParameterSpec(iv)
                cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            }
            EncryptionAlgorithm.CHACHA20_POLY1305 -> {
                throw StorageException.EncryptionException(
                    "ChaCha20-Poly1305 not supported"
                )
            }
        }
    }

    /**
     * 初始化解密模式的Cipher
     */
    private fun initCipherForDecryption(
        cipher: Cipher,
        key: SecretKey,
        encryptedData: EncryptedData,
        config: EncryptionConfig,
        associatedData: ByteArray?
    ) {
        when (encryptedData.algorithm) {
            EncryptionAlgorithm.AES_128_GCM,
            EncryptionAlgorithm.AES_256_GCM -> {
                val spec = GCMParameterSpec(config.tagSize, encryptedData.iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
                associatedData?.let { cipher.updateAAD(it) }
            }
            EncryptionAlgorithm.AES_128_CBC,
            EncryptionAlgorithm.AES_256_CBC -> {
                val spec = IvParameterSpec(encryptedData.iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
            }
            EncryptionAlgorithm.CHACHA20_POLY1305 -> {
                throw StorageException.DecryptionException(
                    "ChaCha20-Poly1305 not supported"
                )
            }
        }
    }

    /**
     * 生成初始化向量
     */
    private fun generateIv(size: Int): ByteArray {
        return secureRandomProvider.generateRandomBytes(size)
    }

    companion object {
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_CBC_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    }
}