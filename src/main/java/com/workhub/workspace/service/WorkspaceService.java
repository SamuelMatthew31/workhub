package com.workhub.workspace.service;

import com.workhub.auth.entity.User;
import com.workhub.auth.repository.UserRepository;
import com.workhub.common.exception.ConflictException;
import com.workhub.common.exception.ForbiddenException;
import com.workhub.common.exception.NotFoundException;
import com.workhub.workspace.dto.*;
import com.workhub.workspace.entity.Workspace;
import com.workhub.workspace.entity.WorkspaceMember;
import com.workhub.workspace.entity.WorkspaceRole;
import com.workhub.workspace.repository.WorkspaceMemberRepository;
import com.workhub.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService accessService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            WorkspaceAccessService accessService) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    /**
     * FR-WS-1: Membuat workspace baru dan pembuatnya otomatis menjadi OWNER tunggal
     */
    @Transactional
    public WorkspaceResponse createWorkspace(UUID userId, CreateWorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String baseSlug = toSlug(request.name());
        String slug = generateUniqueSlug(baseSlug);

        Workspace workspace = new Workspace(request.name().trim(), slug);
        workspace = workspaceRepository.save(workspace);

        // Jadikan creator sebagai OWNER tunggal
        WorkspaceMember ownerMember = new WorkspaceMember(workspace, user, WorkspaceRole.OWNER);
        memberRepository.save(ownerMember);

        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                WorkspaceRole.OWNER,
                workspace.getCreatedAt()
        );
    }

    /**
     * WS-2: List seluruh workspace tempat user bergabung
     */
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getUserWorkspaces(UUID userId) {
        return memberRepository.findAllByUserIdWithWorkspace(userId).stream()
                .map(m -> new WorkspaceResponse(
                        m.getWorkspace().getId(),
                        m.getWorkspace().getName(),
                        m.getWorkspace().getSlug(),
                        m.getRole(),
                        m.getWorkspace().getCreatedAt()
                ))
                .toList();
    }

    /**
     * WS-3: Mengundang / Menambahkan member baru ke workspace
     */
    @Transactional
    public WorkspaceMemberResponse inviteMember(UUID currentUserId, UUID workspaceId, InviteMemberRequest request) {
        // Hanya Owner atau Admin yang boleh mengundang (FR-WS-4)
        accessService.requireAdminOrOwner(workspaceId, currentUserId);

        // Tidak boleh mengundang orang langsung sebagai OWNER (hanya ada 1 OWNER via transfer)
        if (request.role() == WorkspaceRole.OWNER) {
            throw new ForbiddenException("Cannot invite a member directly as OWNER. Use ownership transfer.");
        }

        User invitedUser = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new NotFoundException("User with email " + request.email() + " not found"));

        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, invitedUser.getId())) {
            throw new ConflictException("User is already a member of this workspace");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));

        WorkspaceMember newMember = new WorkspaceMember(workspace, invitedUser, request.role());
        newMember = memberRepository.save(newMember);

        return toMemberResponse(newMember);
    }

    /**
     * Mengambil daftar seluruh anggota workspace
     */
    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getWorkspaceMembers(UUID currentUserId, UUID workspaceId) {
        accessService.requireMember(workspaceId, currentUserId);
        return memberRepository.findAllByWorkspaceIdWithUser(workspaceId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    /**
     * FR-WS-4: Ubah role member
     */
    @Transactional
    public WorkspaceMemberResponse updateMemberRole(
            UUID currentUserId, UUID workspaceId, UUID targetMemberId, UpdateMemberRoleRequest request) {

        WorkspaceMember currentMember = accessService.requireAdminOrOwner(workspaceId, currentUserId);
        WorkspaceMember targetMember = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        // Pastikan target member memang berada di workspace ini
        if (!targetMember.getWorkspace().getId().equals(workspaceId)) {
            throw new NotFoundException("Member not found in this workspace");
        }

        // FR-WS-4: Admin TIDAK BOLEH memodifikasi OWNER
        if (targetMember.getRole() == WorkspaceRole.OWNER && currentMember.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Admins cannot modify an Owner's role");
        }

        // Tidak boleh menaikkan role menjadi OWNER lewat method ini
        if (request.role() == WorkspaceRole.OWNER) {
            throw new ForbiddenException("Cannot set role to OWNER. Use transfer ownership.");
        }

        targetMember.setRole(request.role());
        targetMember = memberRepository.save(targetMember);

        return toMemberResponse(targetMember);
    }

    /**
     * FR-WS-4 & WS-5: Hapus member dari workspace
     */
    @Transactional
    public void removeMember(UUID currentUserId, UUID workspaceId, UUID targetMemberId) {
        WorkspaceMember currentMember = accessService.requireAdminOrOwner(workspaceId, currentUserId);
        WorkspaceMember targetMember = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (!targetMember.getWorkspace().getId().equals(workspaceId)) {
            throw new NotFoundException("Member not found in this workspace");
        }

        // Tidak boleh menghapus OWNER
        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenException("Cannot remove the Owner from the workspace");
        }

        memberRepository.delete(targetMember);
    }

    /**
     * FR-WS-3: Transfer kepemilikan workspace (Atomik & Eksplisit)
     */
    @Transactional
    public void transferOwnership(UUID currentUserId, UUID workspaceId, UUID newOwnerMemberId) {
        WorkspaceMember currentOwner = accessService.requireOwner(workspaceId, currentUserId);

        WorkspaceMember targetMember = memberRepository.findById(newOwnerMemberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (!targetMember.getWorkspace().getId().equals(workspaceId)) {
            throw new NotFoundException("Member not found in this workspace");
        }

        // Ubah owner lama menjadi ADMIN
        currentOwner.setRole(WorkspaceRole.ADMIN);
        memberRepository.save(currentOwner);

        // Jadikan target member sebagai OWNER baru
        targetMember.setRole(WorkspaceRole.OWNER);
        memberRepository.save(targetMember);
    }

    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember m) {
        return new WorkspaceMemberResponse(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getEmail(),
                m.getUser().getDisplayName(),
                m.getRole(),
                m.getCreatedAt()
        );
    }

    private String toSlug(String input) {
        String nonLatin = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("[^\\w\\s-]", "");
        return nonLatin.toLowerCase(Locale.ENGLISH).replaceAll("[\\s_-]+", "-").replaceAll("^-+|-+$", "");
    }

    private String generateUniqueSlug(String baseSlug) {
        String slug = baseSlug.isBlank() ? "workspace" : baseSlug;
        if (!workspaceRepository.existsBySlug(slug)) {
            return slug;
        }
        return slug + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
