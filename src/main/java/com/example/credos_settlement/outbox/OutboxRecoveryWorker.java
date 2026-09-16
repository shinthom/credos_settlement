package com.example.credos_settlement.outbox;

import com.example.credos_settlement.settlement.PermanentSettlementException;
import com.example.credos_settlement.settlement.RetryableSettlementException;
import com.example.credos_settlement.settlement.SettlementClient;
import com.example.credos_settlement.settlement.SettlementStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRecoveryWorker {

  private final OutboxRecoveryService recoveryService;
  private final OutboxEventService outboxEventService;
  private final SettlementClient settlementClient;

  public OutboxRecoveryWorker(
      OutboxRecoveryService recoveryService,
      OutboxEventService outboxEventService,
      SettlementClient settlementClient) {
    this.recoveryService = recoveryService;
    this.outboxEventService = outboxEventService;
    this.settlementClient = settlementClient;
  }

  @Scheduled(fixedDelay = 60000)
  public void recover() {
    recoveryService.claimStaleEvents().forEach(this::recover);
  }

  private void recover(StaleOutboxEvent event) {
    try {
      SettlementStatus externalStatus = settlementClient.getStatus(event.transferKey());

      if (externalStatus == SettlementStatus.SETTLED) {
        outboxEventService.markProcessed(event.eventId());

        return;
      }

      outboxEventService.handleRetryableFailure(
          event.eventId(), "Processing timed out and " + "external settlement was not found");

    } catch (RetryableSettlementException e) {

      outboxEventService.handleRetryableFailure(event.eventId(), errorMessage(e));

    } catch (PermanentSettlementException e) {

      outboxEventService.handlePermanentFailure(event.eventId(), errorMessage(e));

    } catch (Exception e) {

      outboxEventService.handleRetryableFailure(event.eventId(), errorMessage(e));
    }
  }

  private String errorMessage(Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
