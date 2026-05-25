package com.joaopivatto.task.entity;

import com.joaopivatto.task.enums.TaskStatus;
import com.joaopivatto.task.state.TaskStatusStateMachine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TO_DO;

    private LocalDate deadline;

    private LocalDateTime completedAt;

    private boolean archived;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public void updateStatus(TaskStatus newStatus) {
        if (!TaskStatusStateMachine.isTransitionValid(this.status, newStatus)) {
            throw new IllegalStateException("Invalid status transition: " + this.status + " -> " + newStatus);
        }

        this.status = newStatus;
        if (newStatus == TaskStatus.DONE) {
            this.completedAt = LocalDateTime.now();
        } else {
            this.completedAt = null;
        }
    }
}

