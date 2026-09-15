package com.example.credos_settlement.outbox;

import java.util.UUID;

/**
 * Result of claiming an outbox event, carrying only what the worker needs: which event was claimed
 * and which transfer to settle. Passed instead of the JPA entity so the worker stays detached from
 * persistence.
 */
public record ClaimedOutboxEvent(Long eventId, UUID transferKey) {}
