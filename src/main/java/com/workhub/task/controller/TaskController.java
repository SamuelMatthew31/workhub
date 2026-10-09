package com.workhub.task.controller;

import com.workhub.task.dto.*;
import com.workhub.task.service.TaskService;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Tasks", description = "Task management and event projection")
@SecurityRequirement(name = "BearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Create a task (Emits task.created event)")
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(currentUserId, workspaceId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "List all non-deleted tasks in a project")
    public ResponseEntity<List<TaskResponse>> getProjectTasks(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId) {
        List<TaskResponse> responses = taskService.getProjectTasks(currentUserId, workspaceId, projectId);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/tasks/{taskId}")
    @Operation(summary = "Update a task with optimistic locking (Emits event)")
    public ResponseEntity<TaskResponse> updateTask(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskResponse response = taskService.updateTask(currentUserId, workspaceId, taskId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Soft delete a task (Emits task.deleted event)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId) {
        taskService.deleteTask(currentUserId, workspaceId, taskId);
    }
}
