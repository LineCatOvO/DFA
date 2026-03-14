package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.EncryptionAlgorithm
import com.dfa.core.vm.storage.models.EncryptionConfig
import com.dfa.core.vm.storage.models.EncryptedData
import com.dfa.core.vm.storage.crypto.SecureRandomProvider
import com.dfa.core.vm.storage.crypto.AesCipher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator

/**
 * AesCipher单元测试
 */
class AesCipherTest {

    private lateinit var aesCipher: AesCipher
    private lateinit var secureRandomProvider: SecureRandomProvider
    private lateinit var testKey: javax.crypto.SecretKey

    @Before
    fun setup() {
        secureRandomProvider = SecureRandomProvider()
        aesCipher = AesCipher(secureRandomProvider)

        // 生成测试密钥
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        testKey = keyGenerator.generateKey()
    }

    @Test
    fun `encrypt and decrypt with AES-256-GCM should return original data`() {
        val config = EncryptionConfig(
            algorithm = EncryptionAlgorithm.AES_256_GCM,
            keySize = 256
        )
        val plaintext = "Hello, World!".toByteArray()

        // 加密
        val encryptResult = aesCipher.encrypt(plaintext, testKey, config)
        assertTrue("Encryption should succeed", encryptResult.success)
        assertNotNull("Encrypted data should not be null", encryptResult.encryptedData)

        // 解密
        val decryptResult = aesCipher.decrypt(encryptResult.encryptedData!!, testKey, config)
        assertTrue("Decryption should succeed", decryptResult.success)
        assertArrayEquals("Decrypted data should match original", plaintext, decryptResult.data)
    }

    @Test
    fun `encrypt with different IVs should produce different ciphertext`() {
        val config = EncryptionConfig(
            algorithm = EncryptionAlgorithm.AES_256_GCM,
            keySize = 256
        )
        val plaintext = "Same message".toByteArray()

        val result1 = aesCipher.encrypt(plaintext, testKey, config)
        val result2 = aesCipher.encrypt(plaintext, testKey, config)

        assertTrue("First encryption should succeed", result1.success)
        assertTrue("Second encryption should succeed", result2.success)

        // 不同的IV应该产生不同的密文
        assertFalse(
            "Ciphertexts should be different",
            result1.encryptedData?.ciphertext?.contentEquals(result2.encryptedData?.ciphertext) ?: false
        )
    }

    @Test
    fun `encrypt with associated data should require same data for decryption`() {
        val config = EncryptionConfig(
            algorithm = EncryptionAlgorithm.AES_256_GCM,
            keySize = 256
        )
        val plaintext = "Secret message".toByteArray()
        val associatedData = "Context data".toByteArray()

        // 使用关联数据加密
        val encryptResult = aesCipher.encrypt(plaintext, testKey, config, associatedData)
        assertTrue("Encryption should succeed", encryptResult.success)

        // 使用相同的关联数据解密
        val decryptResult = aesCipher.decrypt(encryptResult.encryptedData!!, testKey, config, associatedData)
        assertTrue("Decryption with same associated data should succeed", decryptResult.success)
        assertArrayEquals("Decrypted data should match original", plaintext, decryptResult.data)
    }

    @Test
    fun `encrypt empty data should succeed`() {
        val config = EncryptionConfig(
            algorithm = EncryptionAlgorithm.AES_256_GCM,
            keySize = 256
        )
        val emptyData = ByteArray(0)

        val encryptResult = aesCipher.encrypt(emptyData, testKey, config)
        assertTrue("Encryption of empty data should succeed", encryptResult.success)
    }

    @Test
    fun `encrypt large data should succeed`() {
        val config = EncryptionConfig(
            algorithm = EncryptionAlgorithm.AES_256_GCM,
            keySize = 256
        )
        val largeData = ByteArray(1024 * 1024) // 1MB
        secureRandomProvider.fillRandomBytes(largeData)

        val encryptResult = aesCipher.encrypt(largeData, testKey, config)
        assertTrue("Encryption of large data should succeed", encryptResult.success)

        val decryptResult = aesCipher.decrypt(encryptResult.encryptedData!!, testKey, config)
        assertTrue("Decryption of large data should succeed", decryptResult.success)
        assertArrayEquals("Decrypted large data should match original", largeData, decryptResult.data)
    }
}