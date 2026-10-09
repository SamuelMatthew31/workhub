package com.workhub.workspace.service;

import com.workhub.common.exception.ForbiddenException;
import com.workhub.common.exception.NotFoundException;
import com.workhub.workspace.entity.WorkspaceMember;
import com.workhub.workspace.entity.WorkspaceRole;
import com.workhub.workspace.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WorkspaceAccessService {

    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceAccessService(WorkspaceMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * FR-WS-2 & FR-WS-5: Validasi keanggotaan workspace.
     * Jika bukan member, return 404 Not Found (Bukan 403) agar tidak bocor eksistensi data.
     */
    public WorkspaceMember requireMember(UUID workspaceId, UUID userId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
    }

    /**
     * Validasi apakah user memiliki minimal role ADMIN (Owner atau Admin).
     */
    public WorkspaceMember requireAdminOrOwner(UUID workspaceId, UUID userId) {
        WorkspaceMember member = requireMember(workspaceId, userId);
        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.ADMIN) {
            throw new ForbiddenException("Only workspace Admins or Owners can perform this action");
        }
        return member;
    }

    /**
     * FR-WS-3: Validasi apakah user adalah OWNER tunggal workspace.
     */
    public WorkspaceMember requireOwner(UUID workspaceId, UUID userId) {
        WorkspaceMember member = requireMember(workspaceId, userId);
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Only workspace Owner can perform this action");
        }
        return member;
    }
}
