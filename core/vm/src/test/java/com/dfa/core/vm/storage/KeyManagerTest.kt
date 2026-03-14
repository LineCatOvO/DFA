package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.crypto.SecureRandomProvider
import com.dfa.core.vm.storage.models.EncryptionAlgorithm
import com.dfa.core.vm.storage.models.KeyGenerationRequest
import com.dfa.core.vm.storage.models.KeyState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * KeyManager单元测试
 * 
 * 注意：由于KeyManager依赖Android Keystore，部分测试需要在Android环境中运行
 */
class KeyManagerTest {

    private lateinit var keyManager: com.dfa.core.vm.storage.crypto.KeyManager
    private lateinit var secureRandomProvider: SecureRandomProvider

    @Before
    fun setup() {
        secureRandomProvider = SecureRandomProvider()
        keyManager = com.dfa.core.vm.storage.crypto.KeyManager(secureRandomProvider)
    }

    @Test
    fun `deriveKeyFromPassword should generate consistent key for same input`() {
        val password = "testPassword123".toCharArray()
        val salt = secureRandomProvider.generateSalt(32)

        val key1 = keyManager.deriveKeyFromPassword(password, salt)
        val key2 = keyManager.deriveKeyFromPassword(password, salt)

        assertArrayEquals("Same password and salt should produce same key", key1.encoded, key2.encoded)
    }

    @Test
    fun `deriveKeyFromPassword should generate different keys for different passwords`() {
        val salt = secureRandomProvider.generateSalt(32)

        val key1 = keyManager.deriveKeyFromPassword("password1".toCharArray(), salt)
        val key2 = keyManager.deriveKeyFromPassword("password2".toCharArray(), salt)

        assertFalse(
            "Different passwords should produce different keys",
            key1.encoded.contentEquals(key2.encoded)
        )
    }

    @Test
    fun `deriveKeyFromPassword should generate different keys for different salts`() {
        val password = "samePassword".toCharArray()
        val salt1 = secureRandomProvider.generateSalt(32)
        val salt2 = secureRandomProvider.generateSalt(32)

        val key1 = keyManager.deriveKeyFromPassword(password, salt1)
        val key2 = keyManager.deriveKeyFromPassword(password, salt2)

        assertFalse(
            "Different salts should produce different keys",
            key1.encoded.contentEquals(key2.encoded)
        )
    }

    @Test
    fun `generateSalt should produce unique salts`() {
        val salt1 = keyManager.generateSalt()
        val salt2 = keyManager.generateSalt()

        assertFalse("Generated salts should be unique", salt1.contentEquals(salt2))
    }

    @Test
    fun `generateSalt with custom size should produce correct length`() {
        val size = 64
        val salt = keyManager.generateSalt(size)

        assertEquals("Salt should have correct length", size, salt.size)
    }

    @Test
    fun `KeyGenerationRequest validation should work correctly`() {
        val validRequest = KeyGenerationRequest(
            alias = "test_key",
            algorithm = EncryptionAlgorithm.AES_256_GCM,
            keySize = 256
        )
        assertTrue("Valid request should pass validation", validRequest.validate())

        val invalidRequest1 = KeyGenerationRequest(
            alias = "",
            algorithm = EncryptionAlgorithm.AES_256_GCM,
            keySize = 256
        )
        assertFalse("Empty alias should fail validation", invalidRequest1.validate())

        val invalidRequest2 = KeyGenerationRequest(
            alias = "test_key",
            algorithm = EncryptionAlgorithm.AES_256_GCM,
            keySize = 512 // Invalid key size
        )
        assertFalse("Invalid key size should fail validation", invalidRequest2.validate())
    }
}