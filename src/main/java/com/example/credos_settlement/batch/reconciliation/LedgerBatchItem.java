package com.example.credos_settlement.batch.reconciliation;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerBatchItem(UUID transferKey, Long accountId, BigDecimal amount) {}
