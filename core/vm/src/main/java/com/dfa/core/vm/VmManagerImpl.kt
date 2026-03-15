package com.dfa.core.vm

import com.dfa.core.vm.qemu.QemuVmAdapter
import com.dfa.core.vm.qemu.QemuVmCallback
import com.dfa.core.vm.repository.VmRepository
import com.dfa.core.vm.statemachine.StateTransitionResult
import com.dfa.core.vm.statemachine.VmStateMachine
import com.dfa.core.vm.termux.TermuxBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 虚拟机管理器实现
 *
 * 协调状态机、QEMU适配器和仓库，提供完整的虚拟机生命周期管理
 * 支持Termux环境集成
 */
@Singleton
class VmManagerImpl @Inject constructor(
    private val stateMachine: VmStateMachine,
    private val qemuAdapter: QemuVmAdapter,
    private val repository: VmRepository,
    private val termuxBridge: TermuxBridge
) : VmManager, QemuVmCallback {

    private val mutex = Mutex()

    private val _vmState = MutableStateFlow(VmState.CREATED)
    override val vmState: StateFlow<VmState> = _vmState.asStateFlow()

    private val _vmInfo = MutableStateFlow<VmInfo?>(null)
    override val vmInfo: StateFlow<VmInfo?> = _vmInfo.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var currentConfig: VmConfig? = null
    private var currentHandle: QemuVmHandle? = null

    init {
        qemuAdapter.registerCallback(this)
    }

    override suspend fun initialize(config: VmConfig): Result<VmInfo> {
        return mutex.withLock {
            try {
                // 验证配置
                if (!config.resources.validate()) {
                    return@withLock Result.failure(
                        VmError.ConfigurationError("Invalid VM configuration")
                    )
                }

                // 检查Termux环境可用性
                if (!termuxBridge.isTermuxAvailable()) {
                    return@withLock Result.failure(
                        VmError.ResourceError("Termux environment is not available")
                    )
                }

                // 检查QEMU可用性
                if (!qemuAdapter.isQemuAvailable()) {
                    return@withLock Result.failure(
                        VmError.ResourceError("QEMU is not available in Termux")
                    )
                }

                // 检查配置支持
                if (!qemuAdapter.isConfigSupported(config)) {
                    return@withLock Result.failure(
                        VmError.ConfigurationError("Configuration not supported: $config")
                    )
                }

                // 创建虚拟机
                val handleResult = qemuAdapter.createVm(config)
                if (handleResult.isFailure) {
                    return@withLock Result.failure(
                        handleResult.exceptionOrNull() ?: VmError.UnknownError("Failed to create VM")
                    )
                }

                val handle = handleResult.getOrThrow()
                currentConfig = config
                currentHandle = handle

                // 保存到仓库
                repository.saveVmConfig(config)
                repository.saveVmHandle(config.id, handle)

                // 初始化状态机
                stateMachine.reset()

                // 创建初始VmInfo
                val vmInfo = VmInfo(
                    config = config,
                    state = VmState.CREATED,
                    handle = handle
                )

                repository.saveVmInfo(vmInfo)
                _vmInfo.value = vmInfo
                _vmState.value = VmState.CREATED
                _isInitialized.value = true

                Result.success(vmInfo)
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Initialization failed: ${e.message}", e))
            }
        }
    }

    override suspend fun start(): Result<VmInfo> {
        return executeOperation(VmManager.VmOperation.START) {
            val handle = currentHandle ?: return@executeOperation Result.failure(
                VmError.ConfigurationError("VM not initialized")
            )

            val result = qemuAdapter.startVm(handle)
            if (result.isSuccess) {
                Result.success(result.getOrThrow())
            } else {
                Result.failure(
                    result.exceptionOrNull() ?: VmError.UnknownError("Start failed")
                )
            }
        }
    }

    override suspend fun stop(force: Boolean): Result<VmInfo> {
        return executeOperation(VmManager.VmOperation.STOP) {
            val handle = currentHandle ?: return@executeOperation Result.failure(
                VmError.ConfigurationError("VM not initialized")
            )

            val result = qemuAdapter.stopVm(handle, force)
            if (result.isSuccess) {
                val info = getCurrentInfo() ?: return@executeOperation Result.failure(
                    VmError.UnknownError("Failed to get VM info after stop")
                )
                Result.success(info)
            } else {
                Result.failure(
                    result.exceptionOrNull() ?: VmError.UnknownError("Stop failed")
                )
            }
        }
    }

    override suspend fun pause(): Result<VmInfo> {
        return executeOperation(VmManager.VmOperation.PAUSE) {
            val handle = currentHandle ?: return@executeOperation Result.failure(
                VmError.ConfigurationError("VM not initialized")
            )

            val result = qemuAdapter.pauseVm(handle)
            if (result.isSuccess) {
                val info = getCurrentInfo() ?: return@executeOperation Result.failure(
                    VmError.UnknownError("Failed to get VM info after pause")
                )
                Result.success(info)
            } else {
                Result.failure(
                    result.exceptionOrNull() ?: VmError.UnknownError("Pause failed")
                )
            }
        }
    }

    override suspend fun resume(): Result<VmInfo> {
        return executeOperation(VmManager.VmOperation.RESUME) {
            val handle = currentHandle ?: return@executeOperation Result.failure(
                VmError.ConfigurationError("VM not initialized")
            )

            val result = qemuAdapter.resumeVm(handle)
            if (result.isSuccess) {
                val info = getCurrentInfo() ?: return@executeOperation Result.failure(
                    VmError.UnknownError("Failed to get VM info after resume")
                )
                Result.success(info)
            } else {
                Result.failure(
                    result.exceptionOrNull() ?: VmError.UnknownError("Resume failed")
                )
            }
        }
    }

    override suspend fun reset(): Result<VmInfo> {
        return mutex.withLock {
            try {
                val config = currentConfig
                val handle = currentHandle

                // 销毁现有虚拟机
                if (handle != null) {
                    qemuAdapter.destroyVm(handle)
                }

                // 重置状态机
                stateMachine.reset()

                // 重新初始化
                if (config != null) {
                    initialize(config)
                } else {
                    Result.failure(VmError.ConfigurationError("No configuration to reset"))
                }
            } catch (e: Exception) {
                Result.failure(VmError.UnknownError("Reset failed: ${e.message}", e))
            }
        }
    }

    override fun getCurrentState(): VmState = _vmState.value

    override fun getCurrentInfo(): VmInfo? = _vmInfo.value

    override fun canPerformOperation(operation: VmManager.VmOperation): Boolean {
        val currentState = _vmState.value
        return when (operation) {
            VmManager.VmOperation.START -> currentState == VmState.CREATED || currentState == VmState.STOPPED
            VmManager.VmOperation.STOP -> currentState == VmState.RUNNING || currentState == VmState.PAUSED
            VmManager.VmOperation.PAUSE -> currentState == VmState.RUNNING
            VmManager.VmOperation.RESUME -> currentState == VmState.PAUSED
            VmManager.VmOperation.RESET -> true
            VmManager.VmOperation.MIGRATE -> currentState == VmState.RUNNING
        }
    }

    override suspend fun release() {
        mutex.withLock {
            currentHandle?.let { handle ->
                qemuAdapter.destroyVm(handle)
            }

            currentConfig?.id?.let { vmId ->
                repository.deleteVmInfo(vmId)
                repository.deleteVmHandle(vmId)
            }

            currentConfig = null
            currentHandle = null
            _vmInfo.value = null
            _vmState.value = VmState.CREATED
            _isInitialized.value = false

            stateMachine.reset()
        }
    }

    // QemuVmCallback 实现
    override fun onStateChanged(newState: VmState) {
        _vmState.value = newState

        currentConfig?.let { config ->
            val updatedInfo = _vmInfo.value?.copy(state = newState) ?: VmInfo(
                config = config,
                state = newState,
                handle = currentHandle
            )
            _vmInfo.value = updatedInfo
        }
    }

    override fun onError(error: VmError) {
        _vmState.value = VmState.ERROR

        currentConfig?.let { config ->
            val updatedInfo = VmInfo(
                config = config,
                state = VmState.ERROR,
                handle = currentHandle,
                errorMessage = error.message
            )
            _vmInfo.value = updatedInfo
        }
    }

    override fun onVmStarted(ipAddress: String) {
        currentConfig?.let { config ->
            val updatedInfo = VmInfo(
                config = config,
                state = VmState.RUNNING,
                ipAddress = ipAddress,
                handle = currentHandle
            )
            _vmInfo.value = updatedInfo
        }
    }

    override fun onVmStopped() {
        currentConfig?.let { config ->
            val updatedInfo = VmInfo(
                config = config,
                state = VmState.STOPPED,
                handle = currentHandle
            )
            _vmInfo.value = updatedInfo
        }
    }

    override fun onVmDestroyed() {
        currentConfig = null
        currentHandle = null
        _vmInfo.value = null
        _vmState.value = VmState.CREATED
        _isInitialized.value = false
    }

    /**
     * 执行操作的通用方法
     */
    private suspend fun executeOperation(
        operation: VmManager.VmOperation,
        action: suspend () -> Result<VmInfo>
    ): Result<VmInfo> {
        return mutex.withLock {
            if (!_isInitialized.value) {
                return@withLock Result.failure(
                    VmError.ConfigurationError("VM not initialized")
                )
            }

            if (!canPerformOperation(operation)) {
                return@withLock Result.failure(
                    VmError.ResourceError("Cannot perform $operation in current state: ${_vmState.value}")
                )
            }

            // 状态转换
            val event = when (operation) {
                VmManager.VmOperation.START -> VmEvent.Start(currentConfig!!)
                VmManager.VmOperation.STOP -> VmEvent.Stop
                VmManager.VmOperation.PAUSE -> VmEvent.Pause
                VmManager.VmOperation.RESUME -> VmEvent.Resume
                VmManager.VmOperation.RESET -> VmEvent.Reset
                VmManager.VmOperation.MIGRATE -> return@withLock Result.failure(
                    VmError.ResourceError("Migration not implemented")
                )
            }

            val transitionResult = stateMachine.handleEvent(event)
            if (!transitionResult.isSuccess) {
                return@withLock Result.failure(
                    VmError.ResourceError("Invalid state transition: $transitionResult")
                )
            }

            // 执行操作
            val result = action()

            // 更新状态
            if (result.isSuccess) {
                val vmInfo = result.getOrThrow()
                repository.saveVmInfo(vmInfo)
                _vmInfo.value = vmInfo
                _vmState.value = vmInfo.state
            }

            result
        }
    }
}