package com.workhub.workspace.dto;

import com.workhub.workspace.entity.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID id,
        UUID userId,
        String email,
        String displayName,
        WorkspaceRole role,
        Instant joinedAt
) {}
