package com.joaopivatto.task.repository;

import com.joaopivatto.task.entity.Task;
import com.joaopivatto.task.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByArchivedFalseOrderByDeadlineAscIdAsc();

    List<Task> findByArchivedFalseAndCategoryIdOrderByDeadlineAscIdAsc(Long categoryId);

    List<Task> findByArchivedTrueOrderByCompletedAtDescIdDesc();

    List<Task> findByStatusAndArchivedFalseOrderByDeadlineAscIdAsc(TaskStatus status);

    List<Task> findByStatusAndArchivedFalseAndCategoryIdOrderByDeadlineAscIdAsc(TaskStatus status, Long categoryId);

    List<Task> findByArchivedFalseAndStatusAndCompletedAtBefore(TaskStatus status, LocalDateTime threshold);
}
