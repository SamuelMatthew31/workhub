package com.workhub.notification.service;

import com.workhub.common.exception.NotFoundException;
import com.workhub.notification.dto.NotificationResponse;
import com.workhub.notification.entity.Notification;
import com.workhub.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * FR-ACT-2 & FR-ACT-3: Simpan notifikasi baru dari event.
     * Wajib berjalan di transaksi yang sama (Propagation.MANDATORY).
     * Jika actorId == recipientUserId, tidak membuat notifikasi (user tidak menotifikasi dirinya sendiri).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createNotification(
            UUID recipientUserId,
            UUID workspaceId,
            UUID actorId,
            UUID eventId,
            String title,
            String message,
            String entityType,
            UUID entityId
    ) {
        // FR-ACT-3: Aturan proteksi self-notification
        if (recipientUserId == null || recipientUserId.equals(actorId)) {
            return;
        }

        Notification notification = new Notification(
                recipientUserId,
                workspaceId,
                actorId,
                eventId,
                title,
                message,
                entityType,
                entityId
        );
        notificationRepository.save(notification);
    }

    /**
     * Ambil seluruh notifikasi milik user yang sedang login
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Tandai 1 notifikasi sebagai sudah dibaca
     */
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Tandai semua notifikasi milik user sebagai sudah dibaca
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getWorkspaceId(),
                n.getActorId(),
                n.getEventId(),
                n.getTitle(),
                n.getMessage(),
                n.getEntityType(),
                n.getEntityId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
