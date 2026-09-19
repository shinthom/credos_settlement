package com.example.credos_settlement.batch.reconciliation;

import com.example.credos_settlement.transfer.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferBatchItem(
    Long id,
    UUID transferKey,
    Long fromAccountId,
    Long toAccountId,
    BigDecimal amount,
    TransferStatus status,
    Instant createdAt) {}
