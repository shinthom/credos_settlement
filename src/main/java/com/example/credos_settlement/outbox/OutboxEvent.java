package com.example.credos_settlement.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_key", nullable = false, unique = true)
  private UUID eventKey;

  @Column(name = "transfer_key", nullable = false)
  private UUID transferKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false)
  private OutboxEventType eventType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OutboxEventStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected OutboxEvent() {}

  public OutboxEvent(
      UUID eventKey,
      UUID transferKey,
      OutboxEventType eventType,
      OutboxEventStatus status,
      Instant createdAt) {
    this.eventKey = eventKey;
    this.transferKey = transferKey;
    this.eventType = eventType;
    this.status = status;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public UUID getEventKey() {
    return eventKey;
  }

  public UUID getTransferKey() {
    return transferKey;
  }

  public OutboxEventType getEventType() {
    return eventType;
  }

  public OutboxEventStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void markProcessed() {
    this.status = OutboxEventStatus.PROCESSED;
  }
}
