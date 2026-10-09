package com.workhub.project.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String description,
        boolean isArchived,
        List<StatusDto> statuses,
        Instant createdAt
) {
    public record StatusDto(UUID id, String name, int position) {}
}
