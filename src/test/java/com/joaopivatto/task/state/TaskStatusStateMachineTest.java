package com.joaopivatto.task.state;

import com.joaopivatto.task.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskStatusStateMachineTest {

    @Test
    void shouldAllowDefinedTransition() {
        assertTrue(TaskStatusStateMachine.isTransitionValid(TaskStatus.TO_DO, TaskStatus.DOING));
    }

    @Test
    void shouldRejectInvalidTransition() {
        assertFalse(TaskStatusStateMachine.isTransitionValid(TaskStatus.TO_DO, TaskStatus.DONE));
    }
}

