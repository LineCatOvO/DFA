package com.dfa.core.vm.statemachine

import com.dfa.core.vm.VmEvent
import com.dfa.core.vm.VmState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 虚拟机状态机
 * 
 * 管理虚拟机状态转换，确保状态转换的合法性
 */
@Singleton
class VmStateMachine @Inject constructor() {
    
    private val mutex = Mutex()
    private val _currentState = MutableStateFlow(VmState.CREATED)
    val currentState: StateFlow<VmState> = _currentState.asStateFlow()
    
    /**
     * 状态转换规则
     */
    private val validTransitions: Map<VmState, Set<VmState>> = mapOf(
        VmState.CREATED to setOf(
            VmState.STARTING,
            VmState.ERROR
        ),
        VmState.STARTING to setOf(
            VmState.RUNNING,
            VmState.ERROR,
            VmState.STOPPED
        ),
        VmState.RUNNING to setOf(
            VmState.STOPPING,
            VmState.PAUSED,
            VmState.ERROR,
            VmState.MIGRATING
        ),
        VmState.STOPPING to setOf(
            VmState.STOPPED,
            VmState.ERROR
        ),
        VmState.STOPPED to setOf(
            VmState.STARTING,
            VmState.ERROR
        ),
        VmState.ERROR to setOf(
            VmState.STOPPED,
            VmState.STARTING
        ),
        VmState.PAUSED to setOf(
            VmState.RESUMING,
            VmState.STOPPING,
            VmState.ERROR
        ),
        VmState.RESUMING to setOf(
            VmState.RUNNING,
            VmState.ERROR,
            VmState.PAUSED
        ),
        VmState.MIGRATING to setOf(
            VmState.RUNNING,
            VmState.ERROR,
            VmState.STOPPED
        )
    )
    
    /**
     * 事件到目标状态的映射
     */
    private val eventToTargetState: Map<VmEvent, VmState> = mapOf(
        VmEvent.Start::class to VmState.STARTING,
        VmEvent.Stop::class to VmState.STOPPING,
        VmEvent.Pause::class to VmState.PAUSED,
        VmEvent.Resume::class to VmState.RESUMING,
        VmEvent.Reset::class to VmState.CREATED
    )
    
    /**
     * 处理事件
     * 
     * @param event 虚拟机事件
     * @return 状态转换结果
     */
    suspend fun handleEvent(event: VmEvent): StateTransitionResult {
        return mutex.withLock {
            val currentState = _currentState.value
            val targetState = getTargetState(event)
            
            if (targetState == null) {
                return@withLock StateTransitionResult.InvalidEvent(
                    currentState = currentState,
                    event = event,
                    reason = "Unknown event type"
                )
            }
            
            if (!isValidTransition(currentState, targetState)) {
                return@withLock StateTransitionResult.InvalidTransition(
                    currentState = currentState,
                    targetState = targetState,
                    reason = "Invalid state transition from $currentState to $targetState"
                )
            }
            
            _currentState.value = targetState
            StateTransitionResult.Success(
                previousState = currentState,
                newState = targetState
            )
        }
    }
    
    /**
     * 强制设置状态
     * 
     * @param state 目标状态
     */
    suspend fun setState(state: VmState) {
        mutex.withLock {
            _currentState.value = state
        }
    }
    
    /**
     * 重置状态机
     */
    suspend fun reset() {
        mutex.withLock {
            _currentState.value = VmState.CREATED
        }
    }
    
    /**
     * 获取当前状态
     */
    fun getCurrentState(): VmState = _currentState.value
    
    /**
     * 检查是否可以执行指定事件
     * 
     * @param event 虚拟机事件
     * @return 是否可以执行
     */
    fun canHandleEvent(event: VmEvent): Boolean {
        val currentState = _currentState.value
        val targetState = getTargetState(event) ?: return false
        return isValidTransition(currentState, targetState)
    }
    
    /**
     * 获取可用的下一个状态
     * 
     * @return 可用状态集合
     */
    fun getAvailableNextStates(): Set<VmState> {
        val currentState = _currentState.value
        return validTransitions[currentState] ?: emptySet()
    }
    
    /**
     * 检查状态转换是否合法
     */
    private fun isValidTransition(from: VmState, to: VmState): Boolean {
        return validTransitions[from]?.contains(to) ?: false
    }
    
    /**
     * 获取事件对应的目标状态
     */
    private fun getTargetState(event: VmEvent): VmState? {
        return when (event) {
            is VmEvent.Start -> VmState.STARTING
            is VmEvent.Stop -> VmState.STOPPING
            is VmEvent.Pause -> VmState.PAUSED
            is VmEvent.Resume -> VmState.RESUMING
            is VmEvent.Migrate -> VmState.MIGRATING
            is VmEvent.Error -> VmState.ERROR
            is VmEvent.Reset -> VmState.CREATED
        }
    }
}

/**
 * 状态转换结果
 */
sealed class StateTransitionResult {
    /**
     * 成功转换
     */
    data class Success(
        val previousState: VmState,
        val newState: VmState
    ) : StateTransitionResult()
    
    /**
     * 无效转换
     */
    data class InvalidTransition(
        val currentState: VmState,
        val targetState: VmState,
        val reason: String
    ) : StateTransitionResult()
    
    /**
     * 无效事件
     */
    data class InvalidEvent(
        val currentState: VmState,
        val event: VmEvent,
        val reason: String
    ) : StateTransitionResult()
    
    /**
     * 是否成功
     */
    val isSuccess: Boolean
        get() = this is Success
}