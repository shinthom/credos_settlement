package com.example.credos_settlement.outbox;

import com.example.credos_settlement.settlement.SettlementClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Relays outbox events in three steps, so no database transaction stays
// open while the external call runs:
//
//   1. Transaction 1: claim the oldest PENDING event, mark it PROCESSING, commit.
//   2. No transaction: call the settlement client (external API, may take seconds).
//   3. Transaction 2: mark the event PROCESSED, commit.
//
// If the process dies between steps, the event stays PROCESSING and needs recovery.
@Component
public class OutboxWorker {

  private final OutboxEventService outboxEventService;
  private final SettlementClient settlementClient;

  public OutboxWorker(OutboxEventService outboxEventService, SettlementClient settlementClient) {
    this.outboxEventService = outboxEventService;
    this.settlementClient = settlementClient;
  }

  @Scheduled(fixedDelay = 1000)
  public void processNext() {
    outboxEventService.claimNext().ifPresent(this::process);
  }

  private void process(ClaimedOutboxEvent event) {
    // Runs outside any transaction: the claim has already committed.
    settlementClient.settle(event.transferKey());

    outboxEventService.markProcessed(event.eventId());
  }
}
