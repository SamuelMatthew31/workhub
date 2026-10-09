package com.workhub.workspace.dto;

import com.workhub.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        WorkspaceRole role
) {}
