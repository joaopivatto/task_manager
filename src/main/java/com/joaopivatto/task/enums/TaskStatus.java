package com.joaopivatto.task.enums;

public enum TaskStatus {
    TO_DO,
    DOING,
    DONE,
    STUCK;

    public String getDisplayName() {
        return switch (this) {
            case TO_DO -> "TO DO";
            case DOING -> "DOING";
            case DONE -> "DONE";
            case STUCK -> "STUCK";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case TO_DO -> "status-badge status-todo";
            case DOING -> "status-badge status-doing";
            case DONE -> "status-badge status-done";
            case STUCK -> "status-badge status-stuck";
        };
    }

    public String getColumnClass() {
        return switch (this) {
            case TO_DO -> "status-column status-column-todo";
            case DOING -> "status-column status-column-doing";
            case DONE -> "status-column status-column-done";
            case STUCK -> "status-column status-column-stuck";
        };
    }
}
