package com.workhub.workspace.dto;

import com.workhub.workspace.entity.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String slug,
        WorkspaceRole currentUserRole,
        Instant createdAt
) {}
