package com.workhub.task.dto;

import com.workhub.task.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        String description,

        @NotNull(message = "Status ID is required")
        UUID statusId,

        TaskPriority priority,

        Instant dueDate,

        UUID assigneeId
) {}
