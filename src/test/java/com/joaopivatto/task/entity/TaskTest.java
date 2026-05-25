package com.joaopivatto.task.entity;

import com.joaopivatto.task.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskTest {

    @Test
    void shouldSetCompletedAtWhenMovedToDone() {
        Task task = new Task();
        task.setStatus(TaskStatus.DOING);

        task.updateStatus(TaskStatus.DONE);

        assertNotNull(task.getCompletedAt());
    }

    @Test
    void shouldClearCompletedAtWhenLeavingDone() {
        Task task = new Task();
        task.setStatus(TaskStatus.DOING);
        task.updateStatus(TaskStatus.DONE);

        task.updateStatus(TaskStatus.DOING);

        assertNull(task.getCompletedAt());
    }

    @Test
    void shouldThrowForInvalidTransition() {
        Task task = new Task();
        task.setStatus(TaskStatus.TO_DO);

        assertThrows(IllegalStateException.class, () -> task.updateStatus(TaskStatus.DONE));
    }
}

