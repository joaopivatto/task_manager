package com.joaopivatto.task.service;

import com.joaopivatto.task.dto.TaskRequestDTO;
import com.joaopivatto.task.entity.Task;
import com.joaopivatto.task.entity.TaskUpdate;
import com.joaopivatto.task.enums.TaskStatus;
import com.joaopivatto.task.repository.TaskRepository;
import com.joaopivatto.task.validator.TaskDeadlineValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
    private final EntityManager entityManager;

    public TaskService(TaskRepository taskRepository,
                       CategoryService categoryService,
                       TaskDeadlineValidator deadlineValidator,
                       EntityManager entityManager) {
        this.taskRepository = taskRepository;
        this.categoryService = categoryService;
        this.deadlineValidator = deadlineValidator;
        this.entityManager = entityManager;
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
    public void archiveNow(Long id) {
        Task task = findById(id);
        if (task.getStatus() != TaskStatus.DONE) {
            throw new IllegalStateException("Only DONE tasks can be archived manually");
        }
        task.setArchived(true);
        taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<TaskUpdate> findUpdatesByTaskId(Long taskId) {
        TypedQuery<TaskUpdate> query = entityManager.createQuery(
            "select u from TaskUpdate u where u.task.id = :taskId order by u.createdAt desc, u.id desc",
            TaskUpdate.class
        );
        query.setParameter("taskId", taskId);
        return query.getResultList();
    }

    @Transactional
    public TaskUpdate addUpdateComment(Long taskId, String comment) {
        String normalizedComment = comment == null ? "" : comment.trim();
        if (normalizedComment.isBlank()) {
            throw new IllegalArgumentException("Update comment cannot be empty");
        }

        Task task = findById(taskId);
        TaskUpdate update = new TaskUpdate();
        update.setTask(task);
        update.setComment(normalizedComment);
        entityManager.persist(update);
        return update;
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
