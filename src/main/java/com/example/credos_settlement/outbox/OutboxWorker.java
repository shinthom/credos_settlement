package com.example.credos_settlement.outbox;

import com.example.credos_settlement.settlement.PermanentSettlementException;
import com.example.credos_settlement.settlement.RetryableSettlementException;
import com.example.credos_settlement.settlement.SettlementClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

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
    log.info(
        "Processing outbox event: eventId={}, transferKey={}",
        event.eventId(),
        event.transferKey());

    try {
      settlementClient.settle(event.transferKey());
      outboxEventService.markProcessed(event.eventId());

      log.info(
          "Outbox event processed: eventId={}, transferKey={}",
          event.eventId(),
          event.transferKey());
    } catch (RetryableSettlementException e) {
      outboxEventService.handleRetryableFailure(event.eventId(), errorMessage(e));

      log.warn(
          "Retryable settlement failure: eventId={}, transferKey={}, error={}",
          event.eventId(),
          event.transferKey(),
          errorMessage(e));
    } catch (PermanentSettlementException e) {
      outboxEventService.handlePermanentFailure(event.eventId(), errorMessage(e));

      log.error(
          "Permanent settlement failure, event marked as FAILED: eventId={}, transferKey={}, error={}",
          event.eventId(),
          event.transferKey(),
          errorMessage(e));
    } catch (Exception e) {
      outboxEventService.handleRetryableFailure(event.eventId(), errorMessage(e));

      log.error(
          "Unexpected error while processing outbox event: eventId={}, transferKey={}",
          event.eventId(),
          event.transferKey(),
          e);
    }
  }

  private String errorMessage(Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
