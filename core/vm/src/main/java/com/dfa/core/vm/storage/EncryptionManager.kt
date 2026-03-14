package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.DecryptionRequest
import com.dfa.core.vm.storage.models.DecryptionResult
import com.dfa.core.vm.storage.models.EncryptionConfig
import com.dfa.core.vm.storage.models.EncryptionRequest
import com.dfa.core.vm.storage.models.EncryptionResult
import com.dfa.core.vm.storage.models.KeyGenerationRequest
import com.dfa.core.vm.storage.models.KeyInfo
import javax.crypto.SecretKey

/**
 * 加密管理器接口
 *
 * 提供数据加密、解密和密钥管理功能
 */
interface EncryptionManager {

    /**
     * 初始化加密管理器
     *
     * @param config 加密配置
     * @return 初始化结果
     */
    suspend fun initialize(config: EncryptionConfig): Result<Unit>

    /**
     * 加密数据
     *
     * @param request 加密请求
     * @return 加密结果
     */
    suspend fun encrypt(request: EncryptionRequest): EncryptionResult

    /**
     * 解密数据
     *
     * @param request 解密请求
     * @return 解密结果
     */
    suspend fun decrypt(request: DecryptionRequest): DecryptionResult

    /**
     * 加密字节数组
     *
     * @param data 待加密数据
     * @param associatedData 关联数据
     * @return 加密结果
     */
    suspend fun encryptData(
        data: ByteArray,
        associatedData: ByteArray? = null
    ): EncryptionResult

    /**
     * 解密字节数组
     *
     * @param encryptedData 加密数据
     * @param associatedData 关联数据
     * @return 解密结果
     */
    suspend fun decryptData(
        encryptedData: ByteArray,
        associatedData: ByteArray? = null
    ): DecryptionResult

    /**
     * 生成密钥
     *
     * @param request 密钥生成请求
     * @return 密钥信息
     */
    suspend fun generateKey(request: KeyGenerationRequest): Result<KeyInfo>

    /**
     * 检查密钥是否存在
     *
     * @param alias 密钥别名
     * @return 是否存在
     */
    suspend fun hasKey(alias: String): Boolean

    /**
     * 删除密钥
     *
     * @param alias 密钥别名
     * @return 删除结果
     */
    suspend fun deleteKey(alias: String): Result<Unit>

    /**
     * 获取密钥信息
     *
     * @param alias 密钥别名
     * @return 密钥信息
     */
    suspend fun getKeyInfo(alias: String): Result<KeyInfo>

    /**
     * 检查是否已初始化
     *
     * @return 是否已初始化
     */
    fun isInitialized(): Boolean

    /**
     * 获取当前配置
     *
     * @return 当前加密配置
     */
    fun getConfig(): EncryptionConfig?

    /**
     * 释放资源
     */
    suspend fun release()
}