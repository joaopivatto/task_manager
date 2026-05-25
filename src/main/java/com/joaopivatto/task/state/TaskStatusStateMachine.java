package com.joaopivatto.task.state;

import com.joaopivatto.task.enums.TaskStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TaskStatusStateMachine {

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        TRANSITIONS.put(TaskStatus.TO_DO, EnumSet.of(TaskStatus.DOING, TaskStatus.STUCK));
        TRANSITIONS.put(TaskStatus.DOING, EnumSet.of(TaskStatus.DONE, TaskStatus.STUCK, TaskStatus.TO_DO));
        TRANSITIONS.put(TaskStatus.STUCK, EnumSet.of(TaskStatus.DOING, TaskStatus.TO_DO));
        TRANSITIONS.put(TaskStatus.DONE, EnumSet.of(TaskStatus.DOING));
    }

    private TaskStatusStateMachine() {
    }

    public static boolean isTransitionValid(TaskStatus current, TaskStatus next) {
        if (current == null || next == null) {
            return false;
        }
        if (current == next) {
            return true;
        }
        return TRANSITIONS.getOrDefault(current, EnumSet.noneOf(TaskStatus.class)).contains(next);
    }
}

