package com.example.credos_settlement.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventResponse(
    Long id,
    UUID transferKey,
    OutboxEventType eventType,
    OutboxEventStatus status,
    int attempts,
    Instant nextAttemptAt,
    String lastError,
    Instant createdAt) {

  public static OutboxEventResponse from(OutboxEvent event) {
    return new OutboxEventResponse(
        event.getId(),
        event.getTransferKey(),
        event.getEventType(),
        event.getStatus(),
        event.getAttempts(),
        event.getNextAttemptAt(),
        event.getLastError(),
        event.getCreatedAt());
  }
}
