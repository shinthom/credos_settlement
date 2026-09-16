package com.example.credos_settlement.reconciliation;

import com.example.credos_settlement.outbox.OutboxEvent;
import com.example.credos_settlement.outbox.OutboxEventRepository;
import com.example.credos_settlement.outbox.OutboxEventStatus;
import com.example.credos_settlement.outbox.OutboxEventType;
import com.example.credos_settlement.settlement.SettlementClient;
import com.example.credos_settlement.settlement.SettlementStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalReconciliationService {

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
