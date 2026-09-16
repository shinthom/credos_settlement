package com.example.credos_settlement.reconciliation;

import com.example.credos_settlement.outbox.OutboxEventStatus;
import com.example.credos_settlement.settlement.SettlementStatus;
import java.util.UUID;

public record ExternalReconciliationResult(
    UUID transferKey,
    OutboxEventStatus internalStatus,
    SettlementStatus externalStatus,
    ExternalReconciliationStatus status) {}
