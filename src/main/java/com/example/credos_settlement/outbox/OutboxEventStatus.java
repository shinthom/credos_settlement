package com.example.credos_settlement.outbox;

public enum OutboxEventStatus {
  /** No worker has claimed the event yet. */
  PENDING,

  /** A worker has claimed the event and is settling it. */
  PROCESSING,

  /** External settlement has completed. */
  PROCESSED
}
