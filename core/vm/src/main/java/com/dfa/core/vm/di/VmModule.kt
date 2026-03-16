package com.dfa.core.vm.di

import android.content.Context
import com.dfa.core.vm.VmManager
import com.dfa.core.vm.VmManagerImpl
import com.dfa.core.vm.channel.SshChannel
import com.dfa.core.vm.channel.SshChannelImpl
import com.dfa.core.vm.channel.SocketChannel
import com.dfa.core.vm.channel.SocketChannelImpl
import com.dfa.core.vm.channel.SocketChannelFactory
import com.dfa.core.vm.communication.CommunicationErrorHandler
import com.dfa.core.vm.communication.CommunicationErrorHandlerImpl
import com.dfa.core.vm.communication.CommunicationManager
import com.dfa.core.vm.communication.CommunicationManagerImpl
import com.dfa.core.vm.communication.FileTransferManager
import com.dfa.core.vm.communication.FileTransferManagerImpl
import com.dfa.core.vm.image.ImageCache
import com.dfa.core.vm.image.ImageCacheImpl
import com.dfa.core.vm.image.ImageDownloader
import com.dfa.core.vm.image.ImageDownloaderConfig
import com.dfa.core.vm.image.ImageDownloaderImpl
import com.dfa.core.vm.image.ImageManager
import com.dfa.core.vm.image.ImageManagerImpl
import com.dfa.core.vm.image.PredefinedImageProvider
import com.dfa.core.vm.image.PredefinedImageProviderImpl
import com.dfa.core.vm.protocol.CodecConfig
import com.dfa.core.vm.protocol.MessageCodec
import com.dfa.core.vm.protocol.MessageCodecImpl
import com.dfa.core.vm.qemu.QemuMonitor
import com.dfa.core.vm.qemu.QemuMonitorImpl
import com.dfa.core.vm.qemu.QemuProcessManager
import com.dfa.core.vm.qemu.QemuProcessManagerImpl
import com.dfa.core.vm.termux.TermuxConfig
import com.dfa.core.vm.qemu.QemuVmAdapter
import com.dfa.core.vm.qemu.QemuVmAdapterImpl
import com.dfa.core.vm.repository.VmRepository
import com.dfa.core.vm.repository.VmRepositoryImpl
import com.dfa.core.vm.storage.DiskImageManager
import com.dfa.core.vm.storage.DiskImageManagerImpl
import com.dfa.core.vm.storage.EncryptionManager
import com.dfa.core.vm.storage.EncryptionManagerImpl
import com.dfa.core.vm.storage.PersistenceManager
import com.dfa.core.vm.storage.PersistenceManagerImpl
import com.dfa.core.vm.storage.QuotaManager
import com.dfa.core.vm.storage.QuotaManagerImpl
import com.dfa.core.vm.storage.SafStorageProvider
import com.dfa.core.vm.storage.SafStorageProviderImpl
import com.dfa.core.vm.storage.StorageConfigProvider
import com.dfa.core.vm.storage.StorageManager
import com.dfa.core.vm.storage.StorageManagerImpl
import com.dfa.core.vm.storage.crypto.AesCipher
import com.dfa.core.vm.storage.crypto.KeyManager
import com.dfa.core.vm.storage.crypto.SecureRandomProvider
import com.dfa.core.vm.storage.image.ImageFormatDetector
import com.dfa.core.vm.storage.image.Qcow2Handler
import com.dfa.core.vm.storage.image.RawImageHandler
import com.dfa.core.vm.termux.TermuxBridge
import com.dfa.core.vm.termux.TermuxBridgeImpl
import com.dfa.core.vm.termux.TermuxEnvironmentChecker
import com.dfa.core.vm.termux.TermuxEnvironmentCheckerImpl
import com.dfa.core.vm.termux.TermuxPackageManager
import com.dfa.core.vm.termux.TermuxPackageManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 虚拟机模块的Hilt依赖注入配置
 * 
 * 支持QEMU虚拟机和Termux环境集成
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VmModule {

    /**
     * 绑定VmManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindVmManager(impl: VmManagerImpl): VmManager

    /**
     * 绑定VmRepository接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindVmRepository(impl: VmRepositoryImpl): VmRepository

    /**
     * 绑定QemuVmAdapter接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindQemuVmAdapter(impl: QemuVmAdapterImpl): QemuVmAdapter

    /**
     * 绑定QemuProcessManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindQemuProcessManager(impl: QemuProcessManagerImpl): QemuProcessManager

    /**
     * 绑定QemuMonitor接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindQemuMonitor(impl: QemuMonitorImpl): QemuMonitor

    /**
     * 绑定SshChannel接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindSshChannel(impl: SshChannelImpl): SshChannel

    /**
     * 绑定SocketChannel接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindSocketChannel(impl: SocketChannelImpl): SocketChannel

    /**
     * 绑定TermuxBridge接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindTermuxBridge(impl: TermuxBridgeImpl): TermuxBridge

    /**
     * 绑定TermuxPackageManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindTermuxPackageManager(impl: TermuxPackageManagerImpl): TermuxPackageManager

    /**
     * 绑定TermuxEnvironmentChecker接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindTermuxEnvironmentChecker(impl: TermuxEnvironmentCheckerImpl): TermuxEnvironmentChecker

    /**
     * 绑定ImageDownloader接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindImageDownloader(impl: ImageDownloaderImpl): ImageDownloader

    /**
     * 绑定ImageCache接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindImageCache(impl: ImageCacheImpl): ImageCache

    /**
     * 绑定ImageManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindImageManager(impl: ImageManagerImpl): ImageManager

    /**
     * 绑定PredefinedImageProvider接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindPredefinedImageProvider(impl: PredefinedImageProviderImpl): PredefinedImageProvider

    /**
     * 绑定CommunicationManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindCommunicationManager(impl: CommunicationManagerImpl): CommunicationManager

    /**
     * 绑定CommunicationErrorHandler接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindCommunicationErrorHandler(impl: CommunicationErrorHandlerImpl): CommunicationErrorHandler

    /**
     * 绑定MessageCodec接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindMessageCodec(impl: MessageCodecImpl): MessageCodec

    /**
     * 绑定StorageManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindStorageManager(impl: StorageManagerImpl): StorageManager

    /**
     * 绑定DiskImageManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindDiskImageManager(impl: DiskImageManagerImpl): DiskImageManager

    /**
     * 绑定EncryptionManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindEncryptionManager(impl: EncryptionManagerImpl): EncryptionManager

    /**
     * 绑定QuotaManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindQuotaManager(impl: QuotaManagerImpl): QuotaManager

    /**
     * 绑定PersistenceManager接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindPersistenceManager(impl: PersistenceManagerImpl): PersistenceManager

    /**
     * 绑定SafStorageProvider接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindSafStorageProvider(impl: SafStorageProviderImpl): SafStorageProvider

    /**
     * 绑定StorageConfigProvider接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindStorageConfigProvider(impl: StorageManagerImpl): StorageConfigProvider

    companion object {
        /**
         * 提供ImageDownloaderConfig配置
         */
        @Provides
        @Singleton
        fun provideImageDownloaderConfig(): ImageDownloaderConfig {
            return ImageDownloaderConfig()
        }

        /**
         * 提供CodecConfig配置
         */
        @Provides
        @Singleton
        fun provideCodecConfig(): CodecConfig {
            return CodecConfig()
        }

        /**
         * 提供FileTransferManager实例
         */
        @Provides
        @Singleton
        fun provideFileTransferManager(
            communicationManager: CommunicationManager
        ): FileTransferManager {
            return FileTransferManagerImpl(communicationManager)
        }

        /**
         * 提供SecureRandomProvider实例
         */
        @Provides
        @Singleton
        fun provideSecureRandomProvider(): SecureRandomProvider {
            return SecureRandomProvider()
        }

        /**
         * 提供AesCipher实例
         */
        @Provides
        @Singleton
        fun provideAesCipher(
            secureRandomProvider: SecureRandomProvider
        ): AesCipher {
            return AesCipher(secureRandomProvider)
        }

        /**
         * 提供KeyManager实例
         */
        @Provides
        @Singleton
        fun provideKeyManager(
            secureRandomProvider: SecureRandomProvider
        ): KeyManager {
            return KeyManager(secureRandomProvider)
        }

        /**
         * 提供ImageFormatDetector实例
         */
        @Provides
        @Singleton
        fun provideImageFormatDetector(): ImageFormatDetector {
            return ImageFormatDetector()
        }

        /**
         * 提供Qcow2Handler实例
         */
        @Provides
        @Singleton
        fun provideQcow2Handler(): Qcow2Handler {
            return Qcow2Handler()
        }

        /**
         * 提供RawImageHandler实例
         */
        @Provides
        @Singleton
        fun provideRawImageHandler(): RawImageHandler {
            return RawImageHandler()
        }

        /**
         * 提供DiskImageManagerImpl实例
         */
        @Provides
        @Singleton
        fun provideDiskImageManagerImpl(
            qcow2Handler: Qcow2Handler,
            rawImageHandler: RawImageHandler,
            imageFormatDetector: ImageFormatDetector,
            storageConfigProvider: StorageConfigProvider
        ): DiskImageManagerImpl {
            return DiskImageManagerImpl(
                qcow2Handler = qcow2Handler,
                rawImageHandler = rawImageHandler,
                imageFormatDetector = imageFormatDetector,
                storageConfig = storageConfigProvider
            )
        }

        /**
         * 提供EncryptionManagerImpl实例
         */
        @Provides
        @Singleton
        fun provideEncryptionManagerImpl(
            keyManager: KeyManager,
            aesCipher: AesCipher
        ): EncryptionManagerImpl {
            return EncryptionManagerImpl(
                keyManager = keyManager,
                aesCipher = aesCipher
            )
        }

        /**
         * 提供PersistenceManagerImpl实例
         */
        @Provides
        @Singleton
        fun providePersistenceManagerImpl(
            encryptionManager: EncryptionManager
        ): PersistenceManagerImpl {
            return PersistenceManagerImpl(encryptionManager)
        }

        /**
         * 提供SafStorageProviderImpl实例
         */
        @Provides
        @Singleton
        fun provideSafStorageProviderImpl(
            @ApplicationContext context: Context
        ): SafStorageProviderImpl {
            return SafStorageProviderImpl(context)
        }

        /**
         * 提供StorageManagerImpl实例
         */
        @Provides
        @Singleton
        fun provideStorageManagerImpl(
            diskImageManager: DiskImageManager,
            encryptionManager: EncryptionManager,
            quotaManager: QuotaManager,
            persistenceManager: PersistenceManager,
            safStorageProvider: SafStorageProvider
        ): StorageManagerImpl {
            return StorageManagerImpl(
                diskImageManager = diskImageManager,
                encryptionManager = encryptionManager,
                quotaManager = quotaManager,
                persistenceManager = persistenceManager,
                safStorageProvider = safStorageProvider
            )
        }

        /**
         * 提供QemuVmAdapterImpl实例
         */
        @Provides
        @Singleton
        fun provideQemuVmAdapterImpl(
            processManager: QemuProcessManager
        ): QemuVmAdapterImpl {
            return QemuVmAdapterImpl(
                processManager = processManager
            )
        }

        /**
         * 提供TermuxBridgeImpl实例
         */
        @Provides
        @Singleton
        fun provideTermuxBridgeImpl(): TermuxBridgeImpl {
            return TermuxBridgeImpl(TermuxConfig.DEFAULT)
        }

        /**
         * 提供TermuxPackageManagerImpl实例
         */
        @Provides
        @Singleton
        fun provideTermuxPackageManagerImpl(
            termuxBridge: TermuxBridge
        ): TermuxPackageManagerImpl {
            return TermuxPackageManagerImpl(termuxBridge)
        }

        /**
         * 提供TermuxEnvironmentCheckerImpl实例
         */
        @Provides
        @Singleton
        fun provideTermuxEnvironmentCheckerImpl(
            @ApplicationContext context: Context,
            termuxBridge: TermuxBridge
        ): TermuxEnvironmentCheckerImpl {
            return TermuxEnvironmentCheckerImpl(
                context = context,
                termuxBridge = termuxBridge
            )
        }

        /**
         * 提供VmManagerImpl实例
         */
        @Provides
        @Singleton
        fun provideVmManagerImpl(
            stateMachine: com.dfa.core.vm.statemachine.VmStateMachine,
            qemuAdapter: QemuVmAdapter,
            repository: VmRepository,
            termuxBridge: TermuxBridge
        ): VmManagerImpl {
            return VmManagerImpl(
                stateMachine = stateMachine,
                qemuAdapter = qemuAdapter,
                repository = repository,
                termuxBridge = termuxBridge
            )
        }
    }
}