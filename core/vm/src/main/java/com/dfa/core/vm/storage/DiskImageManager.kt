package com.dfa.core.vm.storage

import com.dfa.core.vm.storage.models.ConvertImageProgress
import com.dfa.core.vm.storage.models.CreateDiskImageRequest
import com.dfa.core.vm.storage.models.CreateDiskImageResult
import com.dfa.core.vm.storage.models.CreateSnapshotRequest
import com.dfa.core.vm.storage.models.DiskImageFormat
import com.dfa.core.vm.storage.models.DiskImageInfo
import com.dfa.core.vm.storage.models.ImageValidationResult
import com.dfa.core.vm.storage.models.SnapshotInfo
import kotlinx.coroutines.flow.Flow

/**
 * 磁盘镜像管理器接口
 *
 * 提供磁盘镜像的创建、管理、转换和快照功能
 */
interface DiskImageManager {

    /**
     * 创建磁盘镜像
     *
     * @param request 创建请求
     * @return 创建结果
     */
    suspend fun createImage(request: CreateDiskImageRequest): CreateDiskImageResult

    /**
     * 删除磁盘镜像
     *
     * @param imageId 镜像ID
     * @return 删除结果
     */
    suspend fun deleteImage(imageId: String): Result<Unit>

    /**
     * 获取镜像信息
     *
     * @param imageId 镜像ID
     * @return 镜像信息
     */
    suspend fun getImageInfo(imageId: String): Result<DiskImageInfo>

    /**
     * 获取镜像信息（通过路径）
     *
     * @param path 镜像路径
     * @return 镜像信息
     */
    suspend fun getImageInfoByPath(path: String): Result<DiskImageInfo>

    /**
     * 列出所有镜像
     *
     * @return 镜像列表
     */
    suspend fun listImages(): Result<List<DiskImageInfo>>

    /**
     * 列出指定虚拟机的镜像
     *
     * @param vmId 虚拟机ID
     * @return 镜像列表
     */
    suspend fun listImagesByVm(vmId: String): Result<List<DiskImageInfo>>

    /**
     * 验证镜像
     *
     * @param imageId 镜像ID
     * @return 验证结果
     */
    suspend fun validateImage(imageId: String): Result<ImageValidationResult>

    /**
     * 调整镜像大小
     *
     * @param imageId 镜像ID
     * @param newSizeBytes 新大小
     * @return 更新后的镜像信息
     */
    suspend fun resizeImage(imageId: String, newSizeBytes: Long): Result<DiskImageInfo>

    /**
     * 转换镜像格式
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @param targetFormat 目标格式
     * @return 转换进度流
     */
    fun convertImage(
        sourcePath: String,
        targetPath: String,
        targetFormat: DiskImageFormat
    ): Flow<ConvertImageProgress>

    /**
     * 创建快照
     *
     * @param request 创建请求
     * @return 快照信息
     */
    suspend fun createSnapshot(request: CreateSnapshotRequest): Result<SnapshotInfo>

    /**
     * 删除快照
     *
     * @param snapshotId 快照ID
     * @return 删除结果
     */
    suspend fun deleteSnapshot(snapshotId: String): Result<Unit>

    /**
     * 列出镜像的快照
     *
     * @param imageId 镜像ID
     * @return 快照列表
     */
    suspend fun listSnapshots(imageId: String): Result<List<SnapshotInfo>>

    /**
     * 恢复快照
     *
     * @param snapshotId 快照ID
     * @return 恢复结果
     */
    suspend fun restoreSnapshot(snapshotId: String): Result<DiskImageInfo>

    /**
     * 锁定镜像
     *
     * @param imageId 镜像ID
     * @param vmId 锁定的虚拟机ID
     * @return 锁定结果
     */
    suspend fun lockImage(imageId: String, vmId: String): Result<Unit>

    /**
     * 解锁镜像
     *
     * @param imageId 镜像ID
     * @return 解锁结果
     */
    suspend fun unlockImage(imageId: String): Result<Unit>

    /**
     * 检测镜像格式
     *
     * @param path 镜像路径
     * @return 格式
     */
    suspend fun detectFormat(path: String): Result<DiskImageFormat>

    /**
     * 复制镜像
     *
     * @param sourceId 源镜像ID
     * @param targetName 目标名称
     * @return 新镜像信息
     */
    suspend fun copyImage(sourceId: String, targetName: String): Result<DiskImageInfo>

    /**
     * 导入镜像
     *
     * @param sourcePath 源路径
     * @param targetName 目标名称
     * @return 导入的镜像信息
     */
    suspend fun importImage(sourcePath: String, targetName: String): Result<DiskImageInfo>

    /**
     * 导出镜像
     *
     * @param imageId 镜像ID
     * @param targetPath 目标路径
     * @return 导出结果
     */
    suspend fun exportImage(imageId: String, targetPath: String): Result<Unit>

    /**
     * 获取镜像总大小
     *
     * @return 总字节数
     */
    suspend fun getTotalImageSize(): Long

    /**
     * 清理未使用的镜像
     *
     * @return 清理的数量
     */
    suspend fun cleanupUnusedImages(): Result<Int>
}