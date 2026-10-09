package com.workhub.event.repository;

import com.workhub.event.entity.EventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRecordRepository extends JpaRepository<EventRecord, UUID> {

    // Untuk Activity Feed Workspace (Cursor Pagination via sequence_number)
    @Query("SELECT e FROM EventRecord e WHERE e.workspaceId = :workspaceId AND (:beforeSeq IS NULL OR e.sequenceNumber < :beforeSeq) ORDER BY e.sequenceNumber DESC")
    List<EventRecord> findWorkspaceActivityFeed(
            @Param("workspaceId") UUID workspaceId,
            @Param("beforeSeq") Long beforeSeq,
            org.springframework.data.domain.Pageable pageable
    );

    // Untuk Entity History (misal melihat riwayat 1 task spesifik - FR-TASK-8)
    List<EventRecord> findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, UUID entityId);
}
