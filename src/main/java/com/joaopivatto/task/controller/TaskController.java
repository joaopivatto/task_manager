package com.joaopivatto.task.controller;

import com.joaopivatto.task.dto.TaskRequestDTO;
import com.joaopivatto.task.entity.Task;
import com.joaopivatto.task.enums.TaskStatus;
import com.joaopivatto.task.service.CategoryService;
import com.joaopivatto.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping
public class TaskController {

    private final TaskService taskService;
    private final CategoryService categoryService;

    public TaskController(TaskService taskService, CategoryService categoryService) {
        this.taskService = taskService;
        this.categoryService = categoryService;
    }

    @GetMapping("/board")
    public String board(@RequestParam(value = "categoryId", required = false) Long categoryId, Model model) {
        model.addAttribute("board", taskService.getKanbanData(categoryId));
        model.addAttribute("tasks", taskService.findActiveTasks(categoryId));
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        return "board/kanban";
    }

    @GetMapping("/tasks/table")
    public String table(@RequestParam(value = "categoryId", required = false) Long categoryId, Model model) {
        model.addAttribute("board", taskService.getKanbanData(categoryId));
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        return "task/table-view";
    }

    @GetMapping("/tasks/archive")
    public String archive(Model model) {
        model.addAttribute("tasks", taskService.findArchivedTasks());
        return "task/archive";
    }

    @GetMapping("/tasks/new")
    public String newTask(Model model) {
        fillFormModel(model, new TaskRequestDTO(), false, null);
        return "task/form";
    }

    @GetMapping("/tasks/{id}")
    public String editTask(@PathVariable Long id, Model model) {
        Task task = taskService.findById(id);
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setDeadline(task.getDeadline());
        if (task.getCategory() != null) {
            dto.setCategoryId(task.getCategory().getId());
        }

        fillFormModel(model, dto, true, id);
        return "task/form";
    }

    @PostMapping("/tasks")
    public String createTask(@Valid @ModelAttribute("task") TaskRequestDTO taskRequestDTO,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillFormModel(model, taskRequestDTO, false, null);
            return "task/form";
        }

        taskService.create(taskRequestDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Task created successfully");
        return "redirect:/board";
    }

    @PostMapping("/tasks/{id}")
    public String updateTask(@PathVariable Long id,
                             @Valid @ModelAttribute("task") TaskRequestDTO taskRequestDTO,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillFormModel(model, taskRequestDTO, true, id);
            return "task/form";
        }

        taskService.update(id, taskRequestDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Task updated successfully");
        return "redirect:/board";
    }

    @PostMapping("/tasks/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("status") TaskStatus status,
                               @RequestParam(value = "categoryId", required = false) Long categoryId,
                               RedirectAttributes redirectAttributes) {
        taskService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Status updated successfully");
        if (categoryId != null) {
            return "redirect:/board?categoryId=" + categoryId;
        }
        return "redirect:/board";
    }

    @PostMapping("/tasks/{id}/status/drag")
    public ResponseEntity<Map<String, String>> updateStatusByDragAndDrop(@PathVariable Long id,
                                                                          @RequestParam("status") TaskStatus status) {
        try {
            taskService.updateStatus(id, status);
            return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id,
                             @RequestParam(value = "categoryId", required = false) Long categoryId,
                             RedirectAttributes redirectAttributes) {
        taskService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Task deleted successfully");
        if (categoryId != null) {
            return "redirect:/board?categoryId=" + categoryId;
        }
        return "redirect:/board";
    }

    private void fillFormModel(Model model, TaskRequestDTO dto, boolean editing, Long taskId) {
        model.addAttribute("task", dto);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("editing", editing);
        model.addAttribute("taskId", taskId);

        model.addAttribute("pageTitle", editing ? "Edit Task" : "New Task");
        model.addAttribute("formHeading", editing ? "Edit task" : "Create task");
        model.addAttribute("submitLabel", editing ? "Save changes" : "Create task");
        model.addAttribute("formAction", editing ? "/tasks/" + taskId : "/tasks");
    }
}
