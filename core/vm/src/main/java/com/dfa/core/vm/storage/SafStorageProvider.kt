package com.dfa.core.vm.storage

import android.net.Uri
import com.dfa.core.vm.storage.models.StorageInfo
import com.dfa.core.vm.storage.models.StorageType
import kotlinx.coroutines.flow.Flow

/**
 * SAF存储提供者接口
 *
 * 提供Android Storage Access Framework (SAF) 的存储访问功能
 */
interface SafStorageProvider {

    /**
     * 初始化SAF存储
     *
     * @param rootUri SAF根目录URI
     * @return 初始化结果
     */
    suspend fun initialize(rootUri: Uri): Result<StorageInfo>

    /**
     * 检查是否已初始化
     *
     * @return 是否已初始化
     */
    fun isInitialized(): Boolean

    /**
     * 获取存储信息
     *
     * @return 存储信息
     */
    suspend fun getStorageInfo(): Result<StorageInfo>

    /**
     * 列出目录内容
     *
     * @param directoryUri 目录URI
     * @return 文件列表
     */
    suspend fun listDirectory(directoryUri: Uri): Result<List<SafFileInfo>>

    /**
     * 创建文件
     *
     * @param parentUri 父目录URI
     * @param fileName 文件名
     * @param mimeType MIME类型
     * @return 创建的文件URI
     */
    suspend fun createFile(
        parentUri: Uri,
        fileName: String,
        mimeType: String = "application/octet-stream"
    ): Result<Uri>

    /**
     * 创建目录
     *
     * @param parentUri 父目录URI
     * @param directoryName 目录名
     * @return 创建的目录URI
     */
    suspend fun createDirectory(parentUri: Uri, directoryName: String): Result<Uri>

    /**
     * 删除文件或目录
     *
     * @param uri 文件或目录URI
     * @return 删除结果
     */
    suspend fun delete(uri: Uri): Result<Unit>

    /**
     * 读取文件内容
     *
     * @param fileUri 文件URI
     * @return 文件内容
     */
    suspend fun readFile(fileUri: Uri): Result<ByteArray>

    /**
     * 写入文件内容
     *
     * @param fileUri 文件URI
     * @param data 数据
     * @return 写入结果
     */
    suspend fun writeFile(fileUri: Uri, data: ByteArray): Result<Unit>

    /**
     * 追加文件内容
     *
     * @param fileUri 文件URI
     * @param data 数据
     * @return 追加结果
     */
    suspend fun appendFile(fileUri: Uri, data: ByteArray): Result<Unit>

    /**
     * 复制文件
     *
     * @param sourceUri 源文件URI
     * @param targetParentUri 目标父目录URI
     * @param targetName 目标文件名
     * @return 复制的文件URI
     */
    suspend fun copyFile(
        sourceUri: Uri,
        targetParentUri: Uri,
        targetName: String
    ): Result<Uri>

    /**
     * 移动文件
     *
     * @param sourceUri 源文件URI
     * @param targetParentUri 目标父目录URI
     * @param targetName 目标文件名
     * @return 移动的文件URI
     */
    suspend fun moveFile(
        sourceUri: Uri,
        targetParentUri: Uri,
        targetName: String
    ): Result<Uri>

    /**
     * 重命名文件或目录
     *
     * @param uri 文件或目录URI
     * @param newName 新名称
     * @return 重命名后的URI
     */
    suspend fun rename(uri: Uri, newName: String): Result<Uri>

    /**
     * 检查文件或目录是否存在
     *
     * @param uri URI
     * @return 是否存在
     */
    suspend fun exists(uri: Uri): Boolean

    /**
     * 获取文件信息
     *
     * @param fileUri 文件URI
     * @return 文件信息
     */
    suspend fun getFileInfo(fileUri: Uri): Result<SafFileInfo>

    /**
     * 获取文件大小
     *
     * @param fileUri 文件URI
     * @return 文件大小（字节）
     */
    suspend fun getFileSize(fileUri: Uri): Result<Long>

    /**
     * 获取可用空间
     *
     * @return 可用空间（字节）
     */
    suspend fun getAvailableSpace(): Result<Long>

    /**
     * 检查是否有写入权限
     *
     * @param uri URI
     * @return 是否有写入权限
     */
    suspend fun hasWritePermission(uri: Uri): Boolean

    /**
     * 检查是否有读取权限
     *
     * @param uri URI
     * @return 是否有读取权限
     */
    suspend fun hasReadPermission(uri: Uri): Boolean

    /**
     * 请求持久化权限
     *
     * @param uri URI
     * @return 请求结果
     */
    suspend fun requestPersistedPermission(uri: Uri): Result<Unit>

    /**
     * 释放持久化权限
     *
     * @param uri URI
     * @return 释放结果
     */
    suspend fun releasePersistedPermission(uri: Uri): Result<Unit>

    /**
     * 查找文件
     *
     * @param parentUri 父目录URI
     * @param fileName 文件名
     * @return 文件URI，如果不存在则返回null
     */
    suspend fun findFile(parentUri: Uri, fileName: String): Result<Uri?>

    /**
     * 监听文件变化
     *
     * @param uri 文件或目录URI
     * @return 变化事件流
     */
    fun observeChanges(uri: Uri): Flow<SafChangeEvent>

    /**
     * 获取根目录URI
     *
     * @return 根目录URI
     */
    fun getRootUri(): Uri?

    /**
     * 释放资源
     */
    suspend fun release()
}

/**
 * SAF文件信息
 *
 * @property uri 文件URI
 * @property name 文件名
 * @property mimeType MIME类型
 * @property size 文件大小
 * @property lastModified 最后修改时间
 * @property isDirectory 是否是目录
 * @property isFile 是否是文件
 * @property isVirtual 是否是虚拟文件
 */
data class SafFileInfo(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long = 0,
    val lastModified: Long = 0,
    val isDirectory: Boolean = false,
    val isFile: Boolean = true,
    val isVirtual: Boolean = false
) {
    /**
     * 文件扩展名
     */
    val extension: String?
        get() = name.substringAfterLast('.', null)

    /**
     * 是否是镜像文件
     */
    val isImageFile: Boolean
        get() = extension?.lowercase() in listOf("raw", "qcow2", "img", "vmdk", "vdi", "vhd")

    /**
     * 格式化文件大小
     */
    fun getFormattedSize(): String {
        if (size <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val value = size / Math.pow(1024.0, digitGroups.toDouble())

        return String.format("%.1f %s", value, units[digitGroups])
    }
}

/**
 * SAF变更事件
 *
 * @property uri 变更的URI
 * @property eventType 事件类型
 * @property timestamp 时间戳
 */
data class SafChangeEvent(
    val uri: Uri,
    val eventType: SafEventType,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * SAF事件类型枚举
 */
enum class SafEventType {
    /** 文件创建 */
    CREATE,
    /** 文件修改 */
    MODIFY,
    /** 文件删除 */
    DELETE,
    /** 文件移动 */
    MOVE,
    /** 文件重命名 */
    RENAME
}