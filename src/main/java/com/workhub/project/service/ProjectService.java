package com.workhub.project.service;

import com.workhub.common.exception.ConflictException;
import com.workhub.common.exception.NotFoundException;
import com.workhub.event.service.EventStoreService;
import com.workhub.project.dto.*;
import com.workhub.project.entity.Project;
import com.workhub.project.entity.TaskStatus;
import com.workhub.project.repository.ProjectRepository;
import com.workhub.project.repository.TaskStatusRepository;
import com.workhub.workspace.entity.Workspace;
import com.workhub.workspace.repository.WorkspaceRepository;
import com.workhub.workspace.service.WorkspaceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskStatusRepository statusRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAccessService accessService;
    private final EventStoreService eventStoreService;

    public ProjectService(
            ProjectRepository projectRepository,
            TaskStatusRepository statusRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceAccessService accessService,
            EventStoreService eventStoreService) {
        this.projectRepository = projectRepository;
        this.statusRepository = statusRepository;
        this.workspaceRepository = workspaceRepository;
        this.accessService = accessService;
        this.eventStoreService = eventStoreService;
    }

    /**
     * PROJ-1 & FR-TASK-1: Buat project baru + otomatis buat status default (Backlog, In Progress, Done)
     */
    @Transactional
    public ProjectResponse createProject(UUID currentUserId, UUID workspaceId, CreateProjectRequest request) {
        accessService.requireMember(workspaceId, currentUserId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));

        Project project = new Project(workspace, request.name().trim(), request.description());
        project = projectRepository.save(project);

        // FR-TASK-1: Buat status default bawaan
        TaskStatus backlog = statusRepository.save(new TaskStatus(project, "Backlog", 0));
        TaskStatus inProgress = statusRepository.save(new TaskStatus(project, "In Progress", 1));
        TaskStatus done = statusRepository.save(new TaskStatus(project, "Done", 2));

        // Catat ke Event Store
        eventStoreService.recordEvent(
                workspaceId,
                currentUserId,
                "project.created",
                "project",
                project.getId(),
                Map.of("name", project.getName())
        );

        return toProjectResponse(project, List.of(backlog, inProgress, done));
    }

    /**
     * List seluruh project aktif di dalam workspace
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getWorkspaceProjects(UUID currentUserId, UUID workspaceId) {
        accessService.requireMember(workspaceId, currentUserId);

        return projectRepository.findAllByWorkspaceIdAndArchivedFalse(workspaceId).stream()
                .map(p -> toProjectResponse(p, statusRepository.findAllByProjectIdOrderByPositionAsc(p.getId())))
                .toList();
    }

    /**
     * PROJ-3: Tambah status baru ke project
     */
    @Transactional
    public ProjectResponse.StatusDto addStatus(
            UUID currentUserId, UUID workspaceId, UUID projectId, CreateStatusRequest request) {
        accessService.requireAdminOrOwner(workspaceId, currentUserId);

        Project project = projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        if (statusRepository.existsByProjectIdAndNameIgnoreCase(projectId, request.name().trim())) {
            throw new ConflictException("Status name already exists in this project");
        }

        List<TaskStatus> existingStatuses = statusRepository.findAllByProjectIdOrderByPositionAsc(projectId);
        int nextPosition = existingStatuses.size();

        TaskStatus newStatus = statusRepository.save(new TaskStatus(project, request.name().trim(), nextPosition));

        return new ProjectResponse.StatusDto(newStatus.getId(), newStatus.getName(), newStatus.getPosition());
    }

    private ProjectResponse toProjectResponse(Project p, List<TaskStatus> statuses) {
        List<ProjectResponse.StatusDto> statusDtos = statuses.stream()
                .map(s -> new ProjectResponse.StatusDto(s.getId(), s.getName(), s.getPosition()))
                .toList();

        return new ProjectResponse(
                p.getId(),
                p.getWorkspace().getId(),
                p.getName(),
                p.getDescription(),
                p.isArchived(),
                statusDtos,
                p.getCreatedAt()
        );
    }
}
