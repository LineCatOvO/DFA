package com.dfa.core.vm.statemachine

import com.dfa.core.vm.VmEvent
import com.dfa.core.vm.VmState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * VmStateMachine 单元测试
 */
class VmStateMachineTest {
    
    private lateinit var stateMachine: VmStateMachine
    
    @Before
    fun setup() {
        stateMachine = VmStateMachine()
    }
    
    @Test
    fun `initial state should be CREATED`() = runTest {
        assertEquals(VmState.CREATED, stateMachine.getCurrentState())
    }
    
    @Test
    fun `handleEvent Start should transition from CREATED to STARTING`() = runTest {
        val result = stateMachine.handleEvent(VmEvent.Start(com.dfa.core.vm.VmConfig("test", "Test")))
        
        assertTrue(result.isSuccess)
        assertTrue(result is StateTransitionResult.Success)
        val successResult = result as StateTransitionResult.Success
        assertEquals(VmState.CREATED, successResult.previousState)
        assertEquals(VmState.STARTING, successResult.newState)
        assertEquals(VmState.STARTING, stateMachine.getCurrentState())
    }
    
    @Test
    fun `handleEvent Stop should fail from CREATED state`() = runTest {
        val result = stateMachine.handleEvent(VmEvent.Stop)
        
        assertFalse(result.isSuccess)
        assertTrue(result is StateTransitionResult.InvalidTransition)
    }
    
    @Test
    fun `handleEvent Pause should fail from CREATED state`() = runTest {
        val result = stateMachine.handleEvent(VmEvent.Pause)
        
        assertFalse(result.isSuccess)
        assertTrue(result is StateTransitionResult.InvalidTransition)
    }
    
    @Test
    fun `valid state transitions should succeed`() = runTest {
        // CREATED -> STARTING
        var result = stateMachine.handleEvent(VmEvent.Start(com.dfa.core.vm.VmConfig("test", "Test")))
        assertTrue(result.isSuccess)
        assertEquals(VmState.STARTING, stateMachine.getCurrentState())
        
        // STARTING -> RUNNING
        stateMachine.setState(VmState.RUNNING)
        assertEquals(VmState.RUNNING, stateMachine.getCurrentState())
        
        // RUNNING -> STOPPING
        result = stateMachine.handleEvent(VmEvent.Stop)
        assertTrue(result.isSuccess)
        assertEquals(VmState.STOPPING, stateMachine.getCurrentState())
        
        // STOPPING -> STOPPED
        stateMachine.setState(VmState.STOPPED)
        assertEquals(VmState.STOPPED, stateMachine.getCurrentState())
    }
    
    @Test
    fun `canHandleEvent should return correct value`() = runTest {
        // From CREATED state
        assertTrue(stateMachine.canHandleEvent(VmEvent.Start(com.dfa.core.vm.VmConfig("test", "Test"))))
        assertFalse(stateMachine.canHandleEvent(VmEvent.Stop))
        assertFalse(stateMachine.canHandleEvent(VmEvent.Pause))
        
        // Change to RUNNING state
        stateMachine.setState(VmState.RUNNING)
        assertFalse(stateMachine.canHandleEvent(VmEvent.Start(com.dfa.core.vm.VmConfig("test", "Test"))))
        assertTrue(stateMachine.canHandleEvent(VmEvent.Stop))
        assertTrue(stateMachine.canHandleEvent(VmEvent.Pause))
    }
    
    @Test
    fun `getAvailableNextStates should return valid transitions`() = runTest {
        // From CREATED state
        var nextStates = stateMachine.getAvailableNextStates()
        assertTrue(nextStates.contains(VmState.STARTING))
        assertTrue(nextStates.contains(VmState.ERROR))
        assertFalse(nextStates.contains(VmState.RUNNING))
        
        // From RUNNING state
        stateMachine.setState(VmState.RUNNING)
        nextStates = stateMachine.getAvailableNextStates()
        assertTrue(nextStates.contains(VmState.STOPPING))
        assertTrue(nextStates.contains(VmState.PAUSED))
        assertTrue(nextStates.contains(VmState.ERROR))
        assertFalse(nextStates.contains(VmState.CREATED))
    }
    
    @Test
    fun `reset should return to CREATED state`() = runTest {
        stateMachine.setState(VmState.RUNNING)
        assertEquals(VmState.RUNNING, stateMachine.getCurrentState())
        
        stateMachine.reset()
        assertEquals(VmState.CREATED, stateMachine.getCurrentState())
    }
    
    @Test
    fun `setState should update current state`() = runTest {
        stateMachine.setState(VmState.RUNNING)
        assertEquals(VmState.RUNNING, stateMachine.getCurrentState())
        
        stateMachine.setState(VmState.PAUSED)
        assertEquals(VmState.PAUSED, stateMachine.getCurrentState())
        
        stateMachine.setState(VmState.ERROR)
        assertEquals(VmState.ERROR, stateMachine.getCurrentState())
    }
    
    @Test
    fun `Error event should transition to ERROR state`() = runTest {
        stateMachine.setState(VmState.RUNNING)
        val result = stateMachine.handleEvent(VmEvent.Error(com.dfa.core.vm.VmError.UnknownError("Test error")))
        
        assertTrue(result.isSuccess)
        assertEquals(VmState.ERROR, stateMachine.getCurrentState())
    }
    
    @Test
    fun `Reset event should transition to CREATED state`() = runTest {
        stateMachine.setState(VmState.ERROR)
        val result = stateMachine.handleEvent(VmEvent.Reset)
        
        assertTrue(result.isSuccess)
        assertEquals(VmState.CREATED, stateMachine.getCurrentState())
    }
    
    @Test
    fun `Pause and Resume transitions should work correctly`() = runTest {
        // Setup: RUNNING state
        stateMachine.setState(VmState.RUNNING)
        
        // RUNNING -> PAUSED
        var result = stateMachine.handleEvent(VmEvent.Pause)
        assertTrue(result.isSuccess)
        assertEquals(VmState.PAUSED, stateMachine.getCurrentState())
        
        // PAUSED -> RESUMING
        result = stateMachine.handleEvent(VmEvent.Resume)
        assertTrue(result.isSuccess)
        assertEquals(VmState.RESUMING, stateMachine.getCurrentState())
    }
    
    @Test
    fun `Migrate event should work from RUNNING state`() = runTest {
        stateMachine.setState(VmState.RUNNING)
        val result = stateMachine.handleEvent(VmEvent.Migrate("target-host"))
        
        assertTrue(result.isSuccess)
        assertEquals(VmState.MIGRATING, stateMachine.getCurrentState())
    }
}