package com.dfa.core.vm.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.provider.DocumentsContract
import com.dfa.core.vm.storage.models.StorageInfo
import com.dfa.core.vm.storage.models.StorageState
import com.dfa.core.vm.storage.models.StorageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SAF存储提供者实现
 *
 * 提供Android Storage Access Framework (SAF) 的存储访问功能
 */
@Singleton
class SafStorageProviderImpl @Inject constructor(
    private val context: Context
) : SafStorageProvider {

    private val mutex = Mutex()
    private var rootUri: Uri? = null
    private var isReady = false

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    override suspend fun initialize(rootUri: Uri): Result<StorageInfo> = mutex.withLock {
        return try {
            // 验证URI权限
            val hasRead = hasReadPermissionInternal(rootUri)
            val hasWrite = hasWritePermissionInternal(rootUri)

            if (!hasRead) {
                return Result.failure(
                    StorageException.SafStorageException(
                        "No read permission for URI: $rootUri",
                        rootUri.toString()
                    )
                )
            }

            this.rootUri = rootUri
            this.isReady = true

            // 获取存储信息
            val storageInfo = getStorageInfoInternal(rootUri)

            Result.success(storageInfo)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to initialize SAF storage: ${e.message}",
                    rootUri.toString(),
                    e
                )
            )
        }
    }

    override fun isInitialized(): Boolean = isReady

    override suspend fun getStorageInfo(): Result<StorageInfo> {
        val uri = rootUri ?: return Result.failure(
            StorageException.SafStorageException("SAF storage not initialized")
        )
        return try {
            Result.success(getStorageInfoInternal(uri))
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to get storage info: ${e.message}",
                    uri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun listDirectory(directoryUri: Uri): Result<List<SafFileInfo>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                directoryUri,
                DocumentsContract.getTreeDocumentId(directoryUri)
            )

            val files = mutableListOf<SafFileInfo>()

            contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_FLAGS
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val flagsColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)

                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn)
                    val name = cursor.getString(nameColumn)
                    val mimeType = cursor.getString(mimeColumn)
                    val size = cursor.getLong(sizeColumn)
                    val lastModified = cursor.getLong(modifiedColumn)
                    val flags = cursor.getInt(flagsColumn)

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)
                    val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR

                    files.add(
                        SafFileInfo(
                            uri = documentUri,
                            name = name,
                            mimeType = mimeType,
                            size = size,
                            lastModified = lastModified,
                            isDirectory = isDirectory,
                            isFile = !isDirectory,
                            isVirtual = flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
                        )
                    )
                }
            }

            Result.success(files)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to list directory: ${e.message}",
                    directoryUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun createFile(
        parentUri: Uri,
        fileName: String,
        mimeType: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val parentDocumentId = DocumentsContract.getTreeDocumentId(parentUri)
            val fileUri = DocumentsContract.createDocument(
                contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(parentUri, parentDocumentId),
                mimeType,
                fileName
            )

            if (fileUri != null) {
                Result.success(fileUri)
            } else {
                Result.failure(
                    StorageException.SafStorageException(
                        "Failed to create file: $fileName",
                        parentUri.toString()
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to create file: ${e.message}",
                    parentUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun createDirectory(parentUri: Uri, directoryName: String): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val parentDocumentId = DocumentsContract.getTreeDocumentId(parentUri)
            val dirUri = DocumentsContract.createDocument(
                contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(parentUri, parentDocumentId),
                DocumentsContract.Document.MIME_TYPE_DIR,
                directoryName
            )

            if (dirUri != null) {
                Result.success(dirUri)
            } else {
                Result.failure(
                    StorageException.SafStorageException(
                        "Failed to create directory: $directoryName",
                        parentUri.toString()
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to create directory: ${e.message}",
                    parentUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun delete(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val deleted = DocumentsContract.deleteDocument(contentResolver, uri)

            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(
                    StorageException.SafStorageException(
                        "Failed to delete: $uri",
                        uri.toString()
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to delete: ${e.message}",
                    uri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun readFile(fileUri: Uri): Result<ByteArray> = withContext(Dispatchers.IO) {
        return@withContext try {
            val bytes = contentResolver.openInputStream(fileUri)?.use { input ->
                input.readBytes()
            } ?: throw StorageException.SafStorageException(
                "Failed to open file for reading",
                fileUri.toString()
            )

            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to read file: ${e.message}",
                    fileUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun writeFile(fileUri: Uri, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.openOutputStream(fileUri, "wt")?.use { output ->
                output.write(data)
            } ?: throw StorageException.SafStorageException(
                "Failed to open file for writing",
                fileUri.toString()
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to write file: ${e.message}",
                    fileUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun appendFile(fileUri: Uri, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.openOutputStream(fileUri, "wa")?.use { output ->
                output.write(data)
            } ?: throw StorageException.SafStorageException(
                "Failed to open file for appending",
                fileUri.toString()
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to append file: ${e.message}",
                    fileUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun copyFile(
        sourceUri: Uri,
        targetParentUri: Uri,
        targetName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 读取源文件
            val data = readFile(sourceUri).getOrThrow()

            // 创建目标文件
            val targetUri = createFile(targetParentUri, targetName).getOrThrow()

            // 写入数据
            writeFile(targetUri, data).getOrThrow()

            Result.success(targetUri)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to copy file: ${e.message}",
                    sourceUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun moveFile(
        sourceUri: Uri,
        targetParentUri: Uri,
        targetName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 复制文件
            val targetUri = copyFile(sourceUri, targetParentUri, targetName).getOrThrow()

            // 删除源文件
            delete(sourceUri).getOrThrow()

            Result.success(targetUri)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to move file: ${e.message}",
                    sourceUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun rename(uri: Uri, newName: String): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val renamedUri = DocumentsContract.renameDocument(contentResolver, uri, newName)

            if (renamedUri != null) {
                Result.success(renamedUri)
            } else {
                Result.failure(
                    StorageException.SafStorageException(
                        "Failed to rename: $uri",
                        uri.toString()
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to rename: ${e.message}",
                    uri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun exists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getFileInfo(fileUri: Uri): Result<SafFileInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.query(
                fileUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_FLAGS
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@withContext Result.failure(
                        StorageException.SafStorageException(
                            "File not found: $fileUri",
                            fileUri.toString()
                        )
                    )
                }

                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val flagsColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)

                val name = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeColumn)
                val size = cursor.getLong(sizeColumn)
                val lastModified = cursor.getLong(modifiedColumn)
                val flags = cursor.getInt(flagsColumn)
                val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR

                Result.success(
                    SafFileInfo(
                        uri = fileUri,
                        name = name,
                        mimeType = mimeType,
                        size = size,
                        lastModified = lastModified,
                        isDirectory = isDirectory,
                        isFile = !isDirectory,
                        isVirtual = flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
                    )
                )
            } ?: Result.failure(
                StorageException.SafStorageException(
                    "Failed to query file info",
                    fileUri.toString()
                )
            )
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to get file info: ${e.message}",
                    fileUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun getFileSize(fileUri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        return@withContext try {
            val info = getFileInfo(fileUri).getOrThrow()
            Result.success(info.size)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to get file size: ${e.message}",
                    fileUri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun getAvailableSpace(): Result<Long> = withContext(Dispatchers.IO) {
        return@withContext try {
            val uri = rootUri ?: return@withContext Result.failure(
                StorageException.SafStorageException("SAF storage not initialized")
            )

            // SAF不直接提供可用空间信息，返回一个估计值
            // 实际应用中可能需要使用其他方法获取
            Result.success(Long.MAX_VALUE)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to get available space: ${e.message}"
                )
            )
        }
    }

    override suspend fun hasWritePermission(uri: Uri): Boolean = hasWritePermissionInternal(uri)

    override suspend fun hasReadPermission(uri: Uri): Boolean = hasReadPermissionInternal(uri)

    override suspend fun requestPersistedPermission(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to request persisted permission: ${e.message}",
                    uri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun releasePersistedPermission(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(uri, takeFlags)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to release persisted permission: ${e.message}",
                    uri.toString(),
                    e
                )
            )
        }
    }

    override suspend fun findFile(parentUri: Uri, fileName: String): Result<Uri?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val files = listDirectory(parentUri).getOrThrow()
            val file = files.find { it.name == fileName }
            Result.success(file?.uri)
        } catch (e: Exception) {
            Result.failure(
                StorageException.SafStorageException(
                    "Failed to find file: ${e.message}",
                    parentUri.toString(),
                    e
                )
            )
        }
    }

    override fun observeChanges(uri: Uri): Flow<SafChangeEvent> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(
                    SafChangeEvent(
                        uri = uri,
                        eventType = SafEventType.MODIFY
                    )
                )
            }
        }

        contentResolver.registerContentObserver(uri, true, observer)

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    override fun getRootUri(): Uri? = rootUri

    override suspend fun release() = mutex.withLock {
        rootUri = null
        isReady = false
    }

    // 私有方法

    private fun hasReadPermissionInternal(uri: Uri): Boolean {
        return try {
            val persistedUris = contentResolver.persistedUriPermissions
            persistedUris.any { it.uri == uri && it.isReadPermission }
        } catch (e: Exception) {
            false
        }
    }

    private fun hasWritePermissionInternal(uri: Uri): Boolean {
        return try {
            val persistedUris = contentResolver.persistedUriPermissions
            persistedUris.any { it.uri == uri && it.isWritePermission }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun getStorageInfoInternal(uri: Uri): StorageInfo = withContext(Dispatchers.IO) {
        val documentId = DocumentsContract.getTreeDocumentId(uri)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)

        var name = "SAF Storage"
        var totalBytes = 0L
        var usedBytes = 0L

        contentResolver.query(
            documentUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                name = cursor.getString(nameColumn)
            }
        }

        StorageInfo(
            path = uri.toString(),
            type = StorageType.SAF,
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            availableBytes = totalBytes - usedBytes,
            state = StorageState.READY
        )
    }
}