package com.workhub.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "sequence_number", insertable = false, updatable = false)
    private Long sequenceNumber;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public EventRecord() {}

    public EventRecord(UUID workspaceId, UUID actorId, String eventType, String entityType, UUID entityId, Map<String, Object> payload) {
        this.workspaceId = workspaceId;
        this.actorId = actorId;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.payload = payload;
        this.schemaVersion = 1;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getActorId() { return actorId; }
    public String getEventType() { return eventType; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public Long getSequenceNumber() { return sequenceNumber; }
    public int getSchemaVersion() { return schemaVersion; }
    public Map<String, Object> getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}
