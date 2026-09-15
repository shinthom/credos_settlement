package com.example.credos_settlement.reconciliation;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferReconciliationResult(
    UUID transferKey,
    ReconciliationStatus status,
    BigDecimal transferAmount,
    BigDecimal debitAmount,
    BigDecimal creditAmount,
    BigDecimal ledgerSum,
    int ledgerEntryCount) {}
