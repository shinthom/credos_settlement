package com.example.credos_settlement.outbox;

public enum OutboxEventStatus {
  /** No worker has claimed the event yet. */
  PENDING,

  /** A worker has claimed the event and is settling it. */
  PROCESSING,

  /** External settlement has completed. */
  PROCESSED,

  /**
   * External settlement failed and the event will not be retried automatically. Terminal state,
   * unlike a stale PROCESSING event which the recovery worker resets to PENDING.
   */
  FAILED
}
