package com.dfa.core.vm.di

import com.dfa.core.vm.VmManager
import com.dfa.core.vm.VmManagerImpl
import com.dfa.core.vm.avf.AvfVmAdapter
import com.dfa.core.vm.avf.AvfVmAdapterImpl
import com.dfa.core.vm.channel.VirtIOChannel
import com.dfa.core.vm.channel.VirtIOChannelImpl
import com.dfa.core.vm.channel.VsockChannel
import com.dfa.core.vm.channel.VsockChannelImpl
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
import com.dfa.core.vm.protocol.CodecConfig
import com.dfa.core.vm.protocol.MessageCodec
import com.dfa.core.vm.protocol.MessageCodecImpl
import com.dfa.core.vm.repository.VmRepository
import com.dfa.core.vm.repository.VmRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 虚拟机模块的Hilt依赖注入配置
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
     * 绑定AvfVmAdapter接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindAvfVmAdapter(impl: AvfVmAdapterImpl): AvfVmAdapter

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
     * 绑定VirtIOChannel接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindVirtIOChannel(impl: VirtIOChannelImpl): VirtIOChannel

    /**
     * 绑定VsockChannel接口到实现
     */
    @Binds
    @Singleton
    abstract fun bindVsockChannel(impl: VsockChannelImpl): VsockChannel

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
    }
}