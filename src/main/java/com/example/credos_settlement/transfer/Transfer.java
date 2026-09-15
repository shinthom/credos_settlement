package com.example.credos_settlement.transfer;

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
@Table(name = "transfers")
public class Transfer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
  private String idempotencyKey;

  @Column(name = "transfer_key", nullable = false, unique = true)
  private UUID transferKey;

  @Column(name = "from_account_id", nullable = false)
  private Long fromAccountId;

  @Column(name = "to_account_id", nullable = false)
  private Long toAccountId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransferStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Transfer() {}

  public Transfer(
      String idempotencyKey,
      UUID transferKey,
      Long fromAccountId,
      Long toAccountId,
      BigDecimal amount,
      TransferStatus status,
      Instant createdAt) {
    this.idempotencyKey = idempotencyKey;
    this.transferKey = transferKey;
    this.fromAccountId = fromAccountId;
    this.toAccountId = toAccountId;
    this.amount = amount;
    this.status = status;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public UUID getTransferKey() {
    return transferKey;
  }

  public Long getFromAccountId() {
    return fromAccountId;
  }

  public Long getToAccountId() {
    return toAccountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public TransferStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void complete() {
    this.status = TransferStatus.COMPLETED;
  }
}
