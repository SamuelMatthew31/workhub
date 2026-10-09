package com.workhub.project.controller;

import com.workhub.project.dto.*;
import com.workhub.project.service.ProjectService;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects")
@Tag(name = "Projects", description = "Project and custom status management")
@SecurityRequirement(name = "BearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create a project with default statuses (Backlog, In Progress, Done)")
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(currentUserId, workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all active projects in a workspace")
    public ResponseEntity<List<ProjectResponse>> getWorkspaceProjects(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId) {
        List<ProjectResponse> responses = projectService.getWorkspaceProjects(currentUserId, workspaceId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{projectId}/statuses")
    @Operation(summary = "Add a custom status to a project (Admin/Owner only)")
    public ResponseEntity<ProjectResponse.StatusDto> addStatus(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateStatusRequest request) {
        ProjectResponse.StatusDto response = projectService.addStatus(currentUserId, workspaceId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
