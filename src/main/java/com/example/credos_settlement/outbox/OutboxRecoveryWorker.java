package com.example.credos_settlement.outbox;

import com.example.credos_settlement.settlement.SettlementClient;
import com.example.credos_settlement.settlement.SettlementStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRecoveryWorker {
  private static final Logger log = LoggerFactory.getLogger(OutboxRecoveryWorker.class);

  private final OutboxRecoveryService recoveryService;
  private final SettlementClient settlementClient;

  public OutboxRecoveryWorker(
      OutboxRecoveryService recoveryService, SettlementClient settlementClient) {
    this.recoveryService = recoveryService;
    this.settlementClient = settlementClient;
  }

  @Scheduled(fixedDelay = 60000)
  public void recover() {
    recoveryService.claimStale().ifPresent(this::recover);
  }

  private void recover(StaleOutboxEvent event) {
    log.info(
        "Recovering stale outbox event: eventId={}, transferKey={}",
        event.eventId(),
        event.transferKey());

    try {
      SettlementStatus externalStatus = settlementClient.getStatus(event.transferKey());

      if (externalStatus == SettlementStatus.SETTLED) {
        recoveryService.markRecoveredAsProcessed(event.eventId());

        log.info(
            "Stale outbox event already settled externally, marked as processed: eventId={}, transferKey={}",
            event.eventId(),
            event.transferKey());
        return;
      }

      recoveryService.scheduleRecoveryRetry(event.eventId(), "Settlement not found externally");

      log.warn(
          "Settlement not found externally, scheduled retry: eventId={}, transferKey={}, externalStatus={}",
          event.eventId(),
          event.transferKey(),
          externalStatus);
    } catch (Exception e) {
      recoveryService.scheduleRecoveryRetry(event.eventId(), errorMessage(e));

      log.error(
          "Failed to recover stale outbox event, scheduled retry: eventId={}, transferKey={}",
          event.eventId(),
          event.transferKey(),
          e);
    }
  }

  private String errorMessage(Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
