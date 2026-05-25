package com.joaopivatto.task.validator;

import com.joaopivatto.task.dto.TaskRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TaskDeadlineValidator {

    public void validateForCreate(TaskRequestDTO dto) {
        LocalDate deadline = dto.getDeadline();
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline cannot be in the past");
        }
    }
}

