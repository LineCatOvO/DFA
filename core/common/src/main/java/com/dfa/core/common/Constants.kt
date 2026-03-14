package com.dfa.core.common

/**
 * 应用常量定义
 */
object Constants {
    // 网络配置
    const val DEFAULT_TIMEOUT = 30_000L
    const val CONNECT_TIMEOUT = 10_000L
    const val READ_TIMEOUT = 30_000L
    
    // Docker相关
    const val DOCKER_DEFAULT_PORT = 2375
    const val DOCKER_TLS_PORT = 2376
    
    // 虚拟机相关
    const val VM_DEFAULT_MEMORY = 2048 // MB
    const val VM_DEFAULT_CPU = 2
}