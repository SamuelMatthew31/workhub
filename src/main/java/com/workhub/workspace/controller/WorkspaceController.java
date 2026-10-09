package com.workhub.workspace.controller;

import com.workhub.workspace.dto.*;
import com.workhub.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces", description = "Workspace management and multi-tenancy access control")
@SecurityRequirement(name = "BearerAuth")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @Operation(summary = "Create a new workspace (Creator becomes sole OWNER)")
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @AuthenticationPrincipal UUID currentUserId,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse response = workspaceService.createWorkspace(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List workspaces the current user belongs to")
    public ResponseEntity<List<WorkspaceResponse>> getUserWorkspaces(
            @AuthenticationPrincipal UUID currentUserId) {
        List<WorkspaceResponse> responses = workspaceService.getUserWorkspaces(currentUserId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{workspaceId}/members")
    @Operation(summary = "Invite/add a user to the workspace (Owner/Admin only)")
    public ResponseEntity<WorkspaceMemberResponse> inviteMember(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request) {
        WorkspaceMemberResponse response = workspaceService.inviteMember(currentUserId, workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "Get all members of a workspace")
    public ResponseEntity<List<WorkspaceMemberResponse>> getWorkspaceMembers(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId) {
        List<WorkspaceMemberResponse> responses = workspaceService.getWorkspaceMembers(currentUserId, workspaceId);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{workspaceId}/members/{memberId}")
    @Operation(summary = "Update member role (Owner/Admin only; Admins cannot modify Owner)")
    public ResponseEntity<WorkspaceMemberResponse> updateMemberRole(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        WorkspaceMemberResponse response = workspaceService.updateMemberRole(currentUserId, workspaceId, memberId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @Operation(summary = "Remove a member from the workspace")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId) {
        workspaceService.removeMember(currentUserId, workspaceId, memberId);
    }

    @PostMapping("/{workspaceId}/transfer-ownership")
    @Operation(summary = "Transfer workspace ownership to another member (Owner only)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferOwnership(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody TransferOwnershipRequest request) {
        workspaceService.transferOwnership(currentUserId, workspaceId, request.newOwnerMemberId());
    }
}
