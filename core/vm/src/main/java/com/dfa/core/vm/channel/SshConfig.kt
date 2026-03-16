package com.dfa.core.vm.channel

import com.dfa.core.vm.communication.ChannelConfig
import com.dfa.core.vm.communication.ChannelType

/**
 * SSH认证方式
 *
 * 使用sealed class表示不同的认证方式
 */
sealed class SshAuthMethod {
    /**
     * 密码认证
     *
     * @property username 用户名
     * @property password 密码
     */
    data class Password(
        val username: String,
        val password: String
    ) : SshAuthMethod()

    /**
     * 密钥认证
     *
     * @property username 用户名
     * @property privateKey 私钥内容
     * @property passphrase 私钥密码（可选）
     */
    data class PublicKey(
        val username: String,
        val privateKey: String,
        val passphrase: String? = null
    ) : SshAuthMethod()

    /**
     * 密钥文件认证
     *
     * @property username 用户名
     * @property privateKeyPath 私钥文件路径
     * @property passphrase 私钥密码（可选）
     */
    data class PublicKeyFile(
        val username: String,
        val privateKeyPath: String,
        val passphrase: String? = null
    ) : SshAuthMethod()

    /**
     * 交互式认证（键盘交互）
     *
     * @property username 用户名
     */
    data class KeyboardInteractive(
        val username: String
    ) : SshAuthMethod()
}

/**
 * SSH重连策略配置
 *
 * @property enableReconnect 是否启用重连
 * @property maxReconnectAttempts 最大重连尝试次数
 * @property reconnectDelayMs 重连延迟（毫秒）
 * @property backoffMultiplier 退避乘数（用于指数退避）
 * @property maxReconnectDelayMs 最大重连延迟（毫秒）
 */
data class SshReconnectConfig(
    val enableReconnect: Boolean = true,
    val maxReconnectAttempts: Int = DEFAULT_MAX_RECONNECT_ATTEMPTS,
    val reconnectDelayMs: Long = DEFAULT_RECONNECT_DELAY_MS,
    val backoffMultiplier: Double = DEFAULT_BACKOFF_MULTIPLIER,
    val maxReconnectDelayMs: Long = DEFAULT_MAX_RECONNECT_DELAY_MS
) {
    companion object {
        const val DEFAULT_MAX_RECONNECT_ATTEMPTS = 5
        const val DEFAULT_RECONNECT_DELAY_MS = 1000L
        const val DEFAULT_BACKOFF_MULTIPLIER = 1.5
        const val DEFAULT_MAX_RECONNECT_DELAY_MS = 30000L
    }
}

/**
 * SSH连接超时配置
 *
 * @property connectionTimeoutMs 连接超时（毫秒）
 * @property readTimeoutMs 读取超时（毫秒）
 * @property writeTimeoutMs 写入超时（毫秒）
 * @property keepAliveIntervalMs 心跳间隔（毫秒）
 * @property keepAliveTimeoutMs 心跳超时（毫秒）
 */
data class SshTimeoutConfig(
    val connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS,
    val readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
    val writeTimeoutMs: Long = DEFAULT_WRITE_TIMEOUT_MS,
    val keepAliveIntervalMs: Long = DEFAULT_KEEPALIVE_INTERVAL_MS,
    val keepAliveTimeoutMs: Long = DEFAULT_KEEPALIVE_TIMEOUT_MS
) {
    companion object {
        const val DEFAULT_CONNECTION_TIMEOUT_MS = 30000L // 30秒
        const val DEFAULT_READ_TIMEOUT_MS = 60000L // 60秒
        const val DEFAULT_WRITE_TIMEOUT_MS = 60000L // 60秒
        const val DEFAULT_KEEPALIVE_INTERVAL_MS = 30000L // 30秒
        const val DEFAULT_KEEPALIVE_TIMEOUT_MS = 10000L // 10秒
    }
}

/**
 * SSH安全配置
 *
 * @property strictHostKeyChecking 是否严格检查主机密钥
 * @property knownHostsPath 已知主机文件路径
 * @property preferredKeyExchangeAlgorithms 首选密钥交换算法
 * @property preferredCipherAlgorithms 首选加密算法
 * @property preferredMacAlgorithms 首选MAC算法
 * @property compressionEnabled 是否启用压缩
 */
data class SshSecurityConfig(
    val strictHostKeyChecking: Boolean = true,
    val knownHostsPath: String? = null,
    val preferredKeyExchangeAlgorithms: List<String> = emptyList(),
    val preferredCipherAlgorithms: List<String> = emptyList(),
    val preferredMacAlgorithms: List<String> = emptyList(),
    val compressionEnabled: Boolean = false
) {
    companion object {
        // 默认密钥交换算法
        val DEFAULT_KEY_EXCHANGE_ALGORITHMS = listOf(
            "ecdh-sha2-nistp256",
            "ecdh-sha2-nistp384",
            "ecdh-sha2-nistp521",
            "diffie-hellman-group-exchange-sha256",
            "diffie-hellman-group14-sha256"
        )

        // 默认加密算法
        val DEFAULT_CIPHER_ALGORITHMS = listOf(
            "aes256-gcm@openssh.com",
            "aes128-gcm@openssh.com",
            "aes256-ctr",
            "aes192-ctr",
            "aes128-ctr"
        )

        // 默认MAC算法
        val DEFAULT_MAC_ALGORITHMS = listOf(
            "hmac-sha2-256-etm@openssh.com",
            "hmac-sha2-512-etm@openssh.com",
            "hmac-sha2-256",
            "hmac-sha2-512"
        )
    }
}

/**
 * SSH通道配置
 *
 * @property host SSH服务器主机地址
 * @property port SSH服务器端口
 * @property authMethod 认证方式
 * @property timeoutConfig 超时配置
 * @property reconnectConfig 重连配置
 * @property securityConfig 安全配置
 * @property terminalType 终端类型（用于PTY分配）
 * @property environment 环境变量
 */
class SshChannelConfig(
    override val type: ChannelType = ChannelType.SSH,
    override val port: Int = DEFAULT_SSH_PORT,
    override val path: String? = null,
    override val bufferSize: Int = ChannelConfig.DEFAULT_BUFFER_SIZE,
    override val timeoutMs: Long = ChannelConfig.DEFAULT_TIMEOUT_MS,
    override val enableReconnect: Boolean = true,
    override val maxReconnectAttempts: Int = ChannelConfig.MAX_RECONNECT_ATTEMPTS,
    override val reconnectDelayMs: Long = ChannelConfig.RECONNECT_DELAY_MS,
    val host: String,
    val authMethod: SshAuthMethod,
    val timeoutConfig: SshTimeoutConfig = SshTimeoutConfig(),
    val reconnectConfig: SshReconnectConfig = SshReconnectConfig(),
    val securityConfig: SshSecurityConfig = SshSecurityConfig(),
    val terminalType: String = DEFAULT_TERMINAL_TYPE,
    val environment: Map<String, String> = emptyMap()
) : ChannelConfig(
    type = type,
    port = port,
    path = path,
    bufferSize = bufferSize,
    timeoutMs = timeoutMs,
    enableReconnect = enableReconnect,
    maxReconnectAttempts = maxReconnectAttempts,
    reconnectDelayMs = reconnectDelayMs
) {
    companion object {
        const val DEFAULT_SSH_PORT = 22
        const val DEFAULT_TERMINAL_TYPE = "xterm-256color"
    }

    /**
     * 获取用户名
     */
    val username: String
        get() = when (authMethod) {
            is SshAuthMethod.Password -> authMethod.username
            is SshAuthMethod.PublicKey -> authMethod.username
            is SshAuthMethod.PublicKeyFile -> authMethod.username
            is SshAuthMethod.KeyboardInteractive -> authMethod.username
        }

    /**
     * 验证配置有效性
     */
    override fun validate(): Boolean {
        if (host.isBlank()) return false
        if (port <= 0 || port > 65535) return false
        return when (authMethod) {
            is SshAuthMethod.Password -> {
                authMethod.username.isNotBlank() && authMethod.password.isNotEmpty()
            }
            is SshAuthMethod.PublicKey -> {
                authMethod.username.isNotBlank() && authMethod.privateKey.isNotBlank()
            }
            is SshAuthMethod.PublicKeyFile -> {
                authMethod.username.isNotBlank() && authMethod.privateKeyPath.isNotBlank()
            }
            is SshAuthMethod.KeyboardInteractive -> {
                authMethod.username.isNotBlank()
            }
        }
    }
}