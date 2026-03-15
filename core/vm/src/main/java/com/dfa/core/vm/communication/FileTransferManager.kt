package com.dfa.core.vm.communication

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 文件传输管理器接口
 */
interface FileTransferManager {
    /**
     * 活跃传输会话流
     */
    val activeSessions: StateFlow<List<FileTransferSession>>

    /**
     * 上传文件
     *
     * @param channelId 通道ID
     * @param file 要上传的文件
     * @param config 传输配置
     * @return 传输会话
     */
    suspend fun upload(
        channelId: String,
        file: File,
        config: FileTransferConfig = FileTransferConfig()
    ): Result<FileTransferSession>

    /**
     * 下载文件
     *
     * @param channelId 通道ID
     * @param remotePath 远程文件路径
     * @param localFile 本地保存路径
     * @param config 传输配置
     * @return 传输会话
     */
    suspend fun download(
        channelId: String,
        remotePath: String,
        localFile: File,
        config: FileTransferConfig = FileTransferConfig()
    ): Result<FileTransferSession>

    /**
     * 获取传输会话
     *
     * @param sessionId 会话ID
     * @return 传输会话
     */
    fun getSession(sessionId: String): FileTransferSession?

    /**
     * 取消传输
     *
     * @param sessionId 会话ID
     * @return 取消结果
     */
    suspend fun cancelTransfer(sessionId: String): Result<Unit>

    /**
     * 取消所有传输
     */
    suspend fun cancelAllTransfers()

    /**
     * 获取传输统计
     *
     * @return 传输统计信息
     */
    fun getStatistics(): TransferStatistics
}

/**
 * 传输统计信息
 */
data class TransferStatistics(
    val totalUploads: Int = 0,
    val totalDownloads: Int = 0,
    val totalBytesUploaded: Long = 0,
    val totalBytesDownloaded: Long = 0,
    val activeUploads: Int = 0,
    val activeDownloads: Int = 0,
    val failedTransfers: Int = 0
)

/**
 * 文件传输管理器实现
 */
class FileTransferManagerImpl(
    private val communicationManager: CommunicationManager
) : FileTransferManager {

    private val sessions = ConcurrentHashMap<String, FileTransferSessionImpl>()
    private val _activeSessions = MutableStateFlow<List<FileTransferSession>>(emptyList())
    private val _statistics = MutableStateFlow(TransferStatistics())

    private val scope = CoroutineScope(Dispatchers.IO)

    override val activeSessions: StateFlow<List<FileTransferSession>> = _activeSessions.asStateFlow()

    override suspend fun upload(
        channelId: String,
        file: File,
        config: FileTransferConfig
    ): Result<FileTransferSession> {
        if (!file.exists()) {
            return Result.failure(TransferError.StorageError("File not found: ${file.path}"))
        }

        if (file.length() > config.maxFileSize) {
            return Result.failure(TransferError.SizeLimitError("File size exceeds limit"))
        }

        val channel = communicationManager.getChannel(channelId)
            ?: return Result.failure(TransferError.NetworkError("Channel not found: $channelId"))

        val session = FileTransferSessionImpl(
            sessionId = UUID.randomUUID().toString(),
            fileName = file.name,
            fileSize = file.length(),
            direction = TransferDirection.UPLOAD,
            file = file,
            channel = channel,
            config = config,
            scope = scope
        )

        sessions[session.sessionId] = session
        updateActiveSessions()

        session.start()

        return Result.success(session)
    }

    override suspend fun download(
        channelId: String,
        remotePath: String,
        localFile: File,
        config: FileTransferConfig
    ): Result<FileTransferSession> {
        val channel = communicationManager.getChannel(channelId)
            ?: return Result.failure(TransferError.NetworkError("Channel not found: $channelId"))

        // 确保父目录存在
        localFile.parentFile?.mkdirs()

        val session = FileTransferSessionImpl(
            sessionId = UUID.randomUUID().toString(),
            fileName = localFile.name,
            fileSize = 0, // 未知大小，需要从远程获取
            direction = TransferDirection.DOWNLOAD,
            file = localFile,
            channel = channel,
            config = config,
            scope = scope,
            remotePath = remotePath
        )

        sessions[session.sessionId] = session
        updateActiveSessions()

        session.start()

        return Result.success(session)
    }

    override fun getSession(sessionId: String): FileTransferSession? {
        return sessions[sessionId]
    }

    override suspend fun cancelTransfer(sessionId: String): Result<Unit> {
        val session = sessions[sessionId] ?: return Result.failure(
            TransferError.UnknownError("Session not found: $sessionId")
        )
        return session.cancel()
    }

    override suspend fun cancelAllTransfers() {
        sessions.values.forEach { session ->
            session.cancel()
        }
    }

    override fun getStatistics(): TransferStatistics = _statistics.value

    private fun updateActiveSessions() {
        _activeSessions.value = sessions.values.toList()
        updateStatistics()
    }

    private fun updateStatistics() {
        val activeList = sessions.values
        _statistics.value = TransferStatistics(
            activeUploads = activeList.count { it.direction == TransferDirection.UPLOAD && it.state.value == TransferState.IN_PROGRESS },
            activeDownloads = activeList.count { it.direction == TransferDirection.DOWNLOAD && it.state.value == TransferState.IN_PROGRESS }
        )
    }

    private fun removeSession(sessionId: String) {
        sessions.remove(sessionId)
        updateActiveSessions()
    }
}

/**
 * 文件传输会话实现
 */
internal class FileTransferSessionImpl(
    override val sessionId: String,
    override val fileName: String,
    override val fileSize: Long,
    override val direction: TransferDirection,
    private val file: File,
    private val channel: CommunicationChannel,
    private val config: FileTransferConfig,
    private val scope: CoroutineScope,
    private val remotePath: String? = null
) : FileTransferSession {

    private val _state = MutableStateFlow(TransferState.PENDING)
    private val _progress = MutableStateFlow(TransferProgress(totalBytes = fileSize))

    private var transferJob: Job? = null
    private var startTime: Long = 0
    private var bytesTransferred: Long = 0
    private var isPaused = false

    override val state: StateFlow<TransferState> = _state.asStateFlow()
    override val progress: StateFlow<TransferProgress> = _progress.asStateFlow()

    fun start() {
        transferJob = scope.launch {
            try {
                startTime = System.currentTimeMillis()
                _state.value = TransferState.IN_PROGRESS

                when (direction) {
                    TransferDirection.UPLOAD -> performUpload()
                    TransferDirection.DOWNLOAD -> performDownload()
                }

                if (_state.value == TransferState.IN_PROGRESS) {
                    _state.value = TransferState.COMPLETED
                }
            } catch (e: Exception) {
                _state.value = TransferState.FAILED
            }
        }
    }

    override suspend fun pause(): Result<Unit> {
        if (_state.value != TransferState.IN_PROGRESS) {
            return Result.failure(TransferError.UnknownError("Cannot pause: not in progress"))
        }
        isPaused = true
        _state.value = TransferState.PAUSED
        return Result.success(Unit)
    }

    override suspend fun resume(): Result<Unit> {
        if (_state.value != TransferState.PAUSED) {
            return Result.failure(TransferError.UnknownError("Cannot resume: not paused"))
        }
        isPaused = false
        _state.value = TransferState.IN_PROGRESS
        return Result.success(Unit)
    }

    override suspend fun cancel(): Result<Unit> {
        transferJob?.cancel()
        _state.value = TransferState.CANCELLED
        return Result.success(Unit)
    }

    override suspend fun awaitCompletion(): Result<TransferResult> {
        transferJob?.join()

        return when (_state.value) {
            TransferState.COMPLETED -> Result.success(
                TransferResult.Success(
                    sessionId = sessionId,
                    fileName = fileName,
                    bytesTransferred = bytesTransferred,
                    durationMs = System.currentTimeMillis() - startTime
                )
            )
            TransferState.CANCELLED -> Result.success(
                TransferResult.Cancelled(sessionId, bytesTransferred)
            )
            else -> Result.failure(TransferError.UnknownError("Transfer failed"))
        }
    }

    private suspend fun performUpload() {
        val buffer = ByteArray(config.chunkSize)
        var retries = 0

        FileInputStream(file).use { input ->
            var bytesRead: Int
            var offset: Long = 0

            while (input.read(buffer).also { bytesRead = it } != -1) {
                // 等待恢复
                while (isPaused && _state.value == TransferState.PAUSED) {
                    delay(100)
                }

                if (_state.value == TransferState.CANCELLED) {
                    return
                }

                val chunk = if (bytesRead < buffer.size) buffer.copyOf(bytesRead) else buffer

                // 发送数据块
                val result = withTimeout(config.timeoutMs) {
                    channel.send(chunk)
                }

                result.fold(
                    onSuccess = {
                        offset += bytesRead
                        bytesTransferred = offset
                        updateProgress()
                        retries = 0
                    },
                    onFailure = {
                        if (retries < config.maxRetries) {
                            retries++
                            // 重试
                        } else {
                            throw TransferError.NetworkError("Max retries exceeded")
                        }
                    }
                )
            }
        }
    }

    private suspend fun performDownload() {
        // 创建文件
        file.createNewFile()
        val raf = RandomAccessFile(file, "rw")
        raf.setLength(fileSize)

        try {
            // 接收数据并写入文件
            // 注意：实际实现需要与远程端协商传输协议
            channel.receiveData.collect { data ->
                while (isPaused && _state.value == TransferState.PAUSED) {
                    delay(100)
                }

                if (_state.value == TransferState.CANCELLED) {
                    return@collect
                }

                raf.seek(bytesTransferred)
                raf.write(data)
                bytesTransferred += data.size
                updateProgress()

                if (fileSize > 0 && bytesTransferred >= fileSize) {
                    return@collect
                }
            }
        } finally {
            raf.close()
        }
    }

    private fun updateProgress() {
        val elapsed = System.currentTimeMillis() - startTime
        _progress.value = TransferProgress.calculate(
            bytesTransferred = bytesTransferred,
            totalBytes = fileSize,
            elapsedTimeMs = elapsed
        )
    }

    private fun calculateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
}