package com.workhub.task.service;

import com.workhub.auth.entity.User;
import com.workhub.auth.repository.UserRepository;
import com.workhub.common.exception.ConflictException;
import com.workhub.common.exception.NotFoundException;
import com.workhub.common.exception.UnprocessableEntityException;
import com.workhub.event.service.EventStoreService;
import com.workhub.project.entity.Project;
import com.workhub.project.entity.TaskStatus;
import com.workhub.project.repository.ProjectRepository;
import com.workhub.project.repository.TaskStatusRepository;
import com.workhub.task.dto.*;
import com.workhub.task.entity.Task;
import com.workhub.task.repository.TaskRepository;
import com.workhub.workspace.repository.WorkspaceMemberRepository;
import com.workhub.workspace.service.WorkspaceAccessService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskStatusRepository statusRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceAccessService accessService;
    private final EventStoreService eventStoreService;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            TaskStatusRepository statusRepository,
            UserRepository userRepository,
            WorkspaceMemberRepository memberRepository,
            WorkspaceAccessService accessService,
            EventStoreService eventStoreService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.statusRepository = statusRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.accessService = accessService;
        this.eventStoreService = eventStoreService;
    }

    /**
     * FR-TASK-4: Buat task baru + catat event task.created dalam satu transaksi
     */
    @Transactional
    public TaskResponse createTask(UUID currentUserId, UUID workspaceId, UUID projectId, CreateTaskRequest request) {
        accessService.requireMember(workspaceId, currentUserId);

        Project project = projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        TaskStatus status = statusRepository.findByIdAndProjectId(request.statusId(), projectId)
                .orElseThrow(() -> new NotFoundException("Status not found in this project"));

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Creator not found"));

        User assignee = null;
        if (request.assigneeId() != null) {
            // FR-TASK-3: Assignee harus merupakan member workspace ini. Jika bukan, throw 422
            if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.assigneeId())) {
                throw new UnprocessableEntityException("Assignee must be a member of the workspace");
            }
            assignee = userRepository.findById(request.assigneeId()).orElse(null);
        }

        Task task = new Task(
                project,
                status,
                request.title().trim(),
                request.description(),
                request.priority(),
                request.dueDate(),
                assignee,
                creator
        );
        task = taskRepository.save(task);

        // Catat ke Event Store
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", task.getTitle());
        payload.put("statusId", status.getId().toString());
        payload.put("statusName", status.getName());
        payload.put("projectId", project.getId().toString());
        if (assignee != null) payload.put("assigneeId", assignee.getId().toString());

        eventStoreService.recordEvent(
                workspaceId,
                currentUserId,
                "task.created",
                "task",
                task.getId(),
                payload
        );

        return toTaskResponse(task);
    }

    /**
     * FR-TASK-7: Update task dengan Optimistic Locking
     */
    @Transactional
    public TaskResponse updateTask(
            UUID currentUserId, UUID workspaceId, UUID taskId, UpdateTaskRequest request) {
        accessService.requireMember(workspaceId, currentUserId);

        Task task = taskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (!task.getProject().getWorkspace().getId().equals(workspaceId)) {
            throw new NotFoundException("Task not found in this workspace");
        }

        // FR-TASK-7: Pengecekan versi stale secara manual
        if (!task.getVersion().equals(request.version())) {
            throw new ConflictException("Stale version conflict. Task has been updated by someone else.");
        }

        Map<String, Object> eventPayload = new HashMap<>();
        String eventType = "task.updated";

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
            eventPayload.put("title", task.getTitle());
        }

        if (request.description() != null) {
            task.setDescription(request.description());
        }

        if (request.priority() != null) {
            task.setPriority(request.priority());
            eventPayload.put("priority", task.getPriority().name());
        }

        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        // Perubahan Status
        if (request.statusId() != null && !request.statusId().equals(task.getStatus().getId())) {
            TaskStatus newStatus = statusRepository.findByIdAndProjectId(request.statusId(), task.getProject().getId())
                    .orElseThrow(() -> new NotFoundException("Target status not found"));
            String oldStatusName = task.getStatus().getName();
            task.setStatus(newStatus);
            eventType = "task.status_changed";
            eventPayload.put("oldStatusName", oldStatusName);
            eventPayload.put("newStatusName", newStatus.getName());
            eventPayload.put("newStatusId", newStatus.getId().toString());
        }

        // Perubahan Assignee
        if (request.assigneeId() != null && (task.getAssignee() == null || !request.assigneeId().equals(task.getAssignee().getId()))) {
            if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.assigneeId())) {
                throw new UnprocessableEntityException("Assignee must be a member of the workspace");
            }
            User newAssignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new NotFoundException("Assignee not found"));
            task.setAssignee(newAssignee);
            eventPayload.put("assigneeId", newAssignee.getId().toString());
            eventType = "task.assigned";
        }

        task.setUpdatedAt(Instant.now());

        try {
            task = taskRepository.saveAndFlush(task);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Stale version conflict. Task has been updated concurrently.");
        }

        // Catat Event
        eventStoreService.recordEvent(
                workspaceId,
                currentUserId,
                eventType,
                "task",
                task.getId(),
                eventPayload
        );

        return toTaskResponse(task);
    }

    /**
     * List seluruh task dalam project
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getProjectTasks(UUID currentUserId, UUID workspaceId, UUID projectId) {
        accessService.requireMember(workspaceId, currentUserId);
        return taskRepository.findAllByProjectIdAndDeletedFalse(projectId).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    /**
     * ADR-007: Soft Delete Task
     */
    @Transactional
    public void deleteTask(UUID currentUserId, UUID workspaceId, UUID taskId) {
        accessService.requireMember(workspaceId, currentUserId);

        Task task = taskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (!task.getProject().getWorkspace().getId().equals(workspaceId)) {
            throw new NotFoundException("Task not found in this workspace");
        }

        task.setDeleted(true);
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);

        eventStoreService.recordEvent(
                workspaceId,
                currentUserId,
                "task.deleted",
                "task",
                task.getId(),
                Map.of("title", task.getTitle())
        );
    }

    private TaskResponse toTaskResponse(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getProject().getId(),
                t.getStatus().getId(),
                t.getStatus().getName(),
                t.getTitle(),
                t.getDescription(),
                t.getPriority(),
                t.getDueDate(),
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getCreator().getId(),
                t.getVersion(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
