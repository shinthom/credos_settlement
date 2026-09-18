package com.example.credos_settlement.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_results")
public class ReconciliationResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "transfer_key", nullable = false, unique = true)
  private UUID transferKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReconciliationStatus status;

  @Column(name = "transfer_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal transferAmount;

  @Column(name = "debit_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal debitAmount;

  @Column(name = "credit_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal creditAmount;

  @Column(name = "ledger_sum", nullable = false, precision = 19, scale = 2)
  private BigDecimal ledgerSum;

  @Column(name = "ledger_entry_count", nullable = false)
  private int ledgerEntryCount;

  @Column(name = "checked_at", nullable = false)
  private Instant checkedAt;

  protected ReconciliationResult() {}

  public ReconciliationResult(
      UUID transferKey,
      ReconciliationStatus status,
      BigDecimal transferAmount,
      BigDecimal debitAmount,
      BigDecimal creditAmount,
      BigDecimal ledgerSum,
      int ledgerEntryCount,
      Instant checkedAt) {
    this.transferKey = transferKey;
    this.status = status;
    this.transferAmount = transferAmount;
    this.debitAmount = debitAmount;
    this.creditAmount = creditAmount;
    this.ledgerSum = ledgerSum;
    this.ledgerEntryCount = ledgerEntryCount;
    this.checkedAt = checkedAt;
  }

  public Long getId() {
    return id;
  }

  public UUID getTransferKey() {
    return transferKey;
  }

  public ReconciliationStatus getStatus() {
    return status;
  }

  public BigDecimal getTransferAmount() {
    return transferAmount;
  }

  public BigDecimal getDebitAmount() {
    return debitAmount;
  }

  public BigDecimal getCreditAmount() {
    return creditAmount;
  }

  public BigDecimal getLedgerSum() {
    return ledgerSum;
  }

  public int getLedgerEntryCount() {
    return ledgerEntryCount;
  }

  public Instant getCheckedAt() {
    return checkedAt;
  }
}
