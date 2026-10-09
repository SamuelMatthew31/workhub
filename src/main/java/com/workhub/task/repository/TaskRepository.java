package com.workhub.task.repository;

import com.workhub.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    List<Task> findAllByProjectIdAndDeletedFalse(UUID projectId);
    Optional<Task> findByIdAndDeletedFalse(UUID id);
    long countByStatusIdAndDeletedFalse(UUID statusId);
}
