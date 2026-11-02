package com.workhub.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID workspaceId,
        UUID actorId,
        UUID eventId,
        String title,
        String message,
        String entityType,
        UUID entityId,
        boolean isRead,
        Instant createdAt
) {}
