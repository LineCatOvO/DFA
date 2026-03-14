package com.dfa.core.vm.di

import com.dfa.core.vm.VmManager
import com.dfa.core.vm.VmManagerImpl
import com.dfa.core.vm.avf.AvfVmAdapter
import com.dfa.core.vm.avf.AvfVmAdapterImpl
import com.dfa.core.vm.repository.VmRepository
import com.dfa.core.vm.repository.VmRepositoryImpl
import dagger.Binds
import dagger.Module
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
}