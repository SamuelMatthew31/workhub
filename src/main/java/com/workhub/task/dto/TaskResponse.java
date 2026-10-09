package com.workhub.task.dto;

import com.workhub.task.entity.TaskPriority;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        UUID statusId,
        String statusName,
        String title,
        String description,
        TaskPriority priority,
        Instant dueDate,
        UUID assigneeId,
        UUID creatorId,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}
