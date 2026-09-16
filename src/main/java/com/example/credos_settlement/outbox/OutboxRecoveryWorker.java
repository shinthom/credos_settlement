package com.example.credos_settlement.outbox;

import com.example.credos_settlement.settlement.SettlementClient;
import com.example.credos_settlement.settlement.SettlementStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRecoveryWorker {
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
    try {
      SettlementStatus externalStatus = settlementClient.getStatus(event.transferKey());

      if (externalStatus == SettlementStatus.SETTLED) {
        recoveryService.markRecoveredAsProcessed(event.eventId());
        return;
      }

      recoveryService.scheduleRecoveryRetry(event.eventId(), "Settlement not found externally");
    } catch (Exception e) {
      recoveryService.scheduleRecoveryRetry(event.eventId(), errorMessage(e));
    }
  }

  private String errorMessage(Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
