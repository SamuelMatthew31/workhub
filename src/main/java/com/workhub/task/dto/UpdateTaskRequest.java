package com.workhub.task.dto;

import com.workhub.task.entity.TaskPriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateTaskRequest(
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        String description,

        UUID statusId,

        TaskPriority priority,

        Instant dueDate,

        UUID assigneeId,

        @NotNull(message = "Version is required for optimistic locking")
        Long version
) {}
