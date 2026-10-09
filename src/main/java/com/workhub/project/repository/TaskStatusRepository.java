package com.workhub.project.repository;

import com.workhub.project.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskStatusRepository extends JpaRepository<TaskStatus, UUID> {
    List<TaskStatus> findAllByProjectIdOrderByPositionAsc(UUID projectId);
    Optional<TaskStatus> findByIdAndProjectId(UUID id, UUID projectId);
    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}
