package com.workhub.event.service;

import com.workhub.event.entity.EventRecord;
import com.workhub.event.repository.EventRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class EventStoreService {

    private final EventRecordRepository eventRecordRepository;

    public EventStoreService(EventRecordRepository eventRecordRepository) {
        this.eventRecordRepository = eventRecordRepository;
    }

    /**
     * ADR-006: Menuliskan event secara sinkron di transaksi database yang sama (MANDATORY).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public EventRecord recordEvent(
            UUID workspaceId,
            UUID actorId,
            String eventType,
            String entityType,
            UUID entityId,
            Map<String, Object> payload
    ) {
        EventRecord event = new EventRecord(
                workspaceId,
                actorId,
                eventType,
                entityType,
                entityId,
                payload
        );
        return eventRecordRepository.save(event);
    }
}
