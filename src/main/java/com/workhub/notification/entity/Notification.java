package com.workhub.notification.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Notification() {}

    public Notification(UUID userId, UUID workspaceId, UUID actorId, UUID eventId,
                        String title, String message, String entityType, UUID entityId) {
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.actorId = actorId;
        this.eventId = eventId;
        this.title = title;
        this.message = message;
        this.entityType = entityType;
        this.entityId = entityId;
        this.read = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getActorId() { return actorId; }
    public UUID getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
}
