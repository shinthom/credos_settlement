package com.example.credos_settlement.outbox;

public enum OutboxEventStatus {
  PENDING, // not yet settled
  PROCESSED // settled
}
