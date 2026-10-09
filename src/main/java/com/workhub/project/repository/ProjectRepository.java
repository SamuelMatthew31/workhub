package com.workhub.project.repository;

import com.workhub.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findAllByWorkspaceIdAndArchivedFalse(UUID workspaceId);
    Optional<Project> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
