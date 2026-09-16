package com.example.credos_settlement.reconciliation;

import com.example.credos_settlement.outbox.OutboxEvent;
import com.example.credos_settlement.outbox.OutboxEventRepository;
import com.example.credos_settlement.outbox.OutboxEventStatus;
import com.example.credos_settlement.outbox.OutboxEventType;
import com.example.credos_settlement.settlement.SettlementClient;
import com.example.credos_settlement.settlement.SettlementStatus;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalReconciliationService {
  private static final Logger log = LoggerFactory.getLogger(ExternalReconciliationService.class);

  private final OutboxEventRepository outboxEventRepository;
  private final SettlementClient settlementClient;

  public ExternalReconciliationService(
      OutboxEventRepository outboxEventRepository, SettlementClient settlementClient) {
    this.outboxEventRepository = outboxEventRepository;

    this.settlementClient = settlementClient;
  }

  @Transactional(readOnly = true)
  public ExternalReconciliationResult reconcile(UUID transferKey) {
    OutboxEvent event =
        outboxEventRepository
            .findByTransferKeyAndEventType(transferKey, OutboxEventType.SETTLEMENT_REQUESTED)
            .orElseThrow();

    SettlementStatus externalStatus = settlementClient.getStatus(transferKey);

    ExternalReconciliationStatus status = determineStatus(event.getStatus(), externalStatus);

    if (status == ExternalReconciliationStatus.MATCHED) {
      log.info("External reconciliation matched: transferKey={}", transferKey);
    } else {
      log.warn(
          "External reconciliation mismatch: transferKey={}, internalStatus={}, externalStatus={}, result={}",
          transferKey,
          event.getStatus(),
          externalStatus,
          status);
    }

    return new ExternalReconciliationResult(transferKey, event.getStatus(), externalStatus, status);
  }

  private ExternalReconciliationStatus determineStatus(
      OutboxEventStatus internalStatus, SettlementStatus externalStatus) {
    if (internalStatus == OutboxEventStatus.PROCESSED
        && externalStatus == SettlementStatus.SETTLED) {
      return ExternalReconciliationStatus.MATCHED;
    }

    if (internalStatus == OutboxEventStatus.PROCESSING
        && externalStatus == SettlementStatus.SETTLED) {
      return ExternalReconciliationStatus.EXTERNAL_AHEAD;
    }

    if (internalStatus == OutboxEventStatus.PROCESSED
        && externalStatus == SettlementStatus.NOT_FOUND) {
      return ExternalReconciliationStatus.INTERNAL_AHEAD;
    }

    return ExternalReconciliationStatus.PENDING;
  }
}
