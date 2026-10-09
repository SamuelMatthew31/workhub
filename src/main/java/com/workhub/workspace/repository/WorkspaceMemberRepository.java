package com.workhub.workspace.repository;

import com.workhub.workspace.entity.WorkspaceMember;
import com.workhub.workspace.entity.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    @Query("SELECT wm FROM WorkspaceMember wm JOIN FETCH wm.workspace WHERE wm.user.id = :userId")
    List<WorkspaceMember> findAllByUserIdWithWorkspace(@Param("userId") UUID userId);

    @Query("SELECT wm FROM WorkspaceMember wm JOIN FETCH wm.user WHERE wm.workspace.id = :workspaceId")
    List<WorkspaceMember> findAllByWorkspaceIdWithUser(@Param("workspaceId") UUID workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndRole(UUID workspaceId, WorkspaceRole role);
}
