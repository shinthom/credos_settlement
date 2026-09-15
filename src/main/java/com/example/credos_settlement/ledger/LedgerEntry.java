package com.example.credos_settlement.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "transfer_key", nullable = false)
  private UUID transferKey;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected LedgerEntry() {}

  public LedgerEntry(UUID transferKey, Long accountId, BigDecimal amount, Instant createdAt) {
    this.transferKey = transferKey;
    this.accountId = accountId;
    this.amount = amount;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public UUID getTransferKey() {
    return transferKey;
  }

  public Long getAccountId() {
    return accountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
