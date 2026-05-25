package com.joaopivatto.task.service;

import com.joaopivatto.task.dto.TaskRequestDTO;
import com.joaopivatto.task.entity.Task;
import com.joaopivatto.task.enums.TaskStatus;
import com.joaopivatto.task.repository.TaskRepository;
import com.joaopivatto.task.validator.TaskDeadlineValidator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryService categoryService;
    private final TaskDeadlineValidator deadlineValidator;

    public TaskService(TaskRepository taskRepository,
                       CategoryService categoryService,
                       TaskDeadlineValidator deadlineValidator) {
        this.taskRepository = taskRepository;
        this.categoryService = categoryService;
        this.deadlineValidator = deadlineValidator;
    }

    @Transactional(readOnly = true)
    public Map<TaskStatus, List<Task>> getKanbanData(Long categoryId) {
        Map<TaskStatus, List<Task>> board = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            if (categoryId == null) {
                board.put(status, taskRepository.findByStatusAndArchivedFalseOrderByDeadlineAscIdAsc(status));
            } else {
                board.put(status, taskRepository.findByStatusAndArchivedFalseAndCategoryIdOrderByDeadlineAscIdAsc(status, categoryId));
            }
        }
        return board;
    }

    @Transactional(readOnly = true)
    public List<Task> findActiveTasks(Long categoryId) {
        if (categoryId == null) {
            return taskRepository.findByArchivedFalseOrderByDeadlineAscIdAsc();
        }
        return taskRepository.findByArchivedFalseAndCategoryIdOrderByDeadlineAscIdAsc(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Task> findArchivedTasks() {
        return taskRepository.findByArchivedTrueOrderByCompletedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Task findById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found for id: " + id));
    }

    @Transactional
    public Task create(TaskRequestDTO dto) {
        deadlineValidator.validateForCreate(dto);

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDeadline(dto.getDeadline());
        task.setArchived(false);
        task.setCategory(categoryService.findById(dto.getCategoryId()));

        TaskStatus requestedStatus = dto.getStatus() == null ? TaskStatus.TO_DO : dto.getStatus();
        task.setStatus(TaskStatus.TO_DO);
        if (requestedStatus != TaskStatus.TO_DO) {
            task.updateStatus(requestedStatus);
        }

        return taskRepository.save(task);
    }

    @Transactional
    public Task update(Long id, TaskRequestDTO dto) {
        Task task = findById(id);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDeadline(dto.getDeadline());
        task.setCategory(categoryService.findById(dto.getCategoryId()));

        TaskStatus requestedStatus = dto.getStatus();
        if (requestedStatus != null && requestedStatus != task.getStatus()) {
            task.updateStatus(requestedStatus);
        }

        return taskRepository.save(task);
    }

    @Transactional
    public void updateStatus(Long id, TaskStatus newStatus) {
        Task task = findById(id);
        task.updateStatus(newStatus);
        taskRepository.save(task);
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.deleteById(id);
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void archiveOldTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(14);
        List<Task> oldDoneTasks = taskRepository.findByArchivedFalseAndStatusAndCompletedAtBefore(TaskStatus.DONE, threshold);
        for (Task task : oldDoneTasks) {
            task.setArchived(true);
        }
        taskRepository.saveAll(oldDoneTasks);
    }
}
