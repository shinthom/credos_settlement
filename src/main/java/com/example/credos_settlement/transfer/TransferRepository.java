package com.example.credos_settlement.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

  Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

  Optional<Transfer> findByTransferKey(UUID transferKey);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO transfers (
              idempotency_key,
              transfer_key,
              from_account_id,
              to_account_id,
              amount,
              status,
              created_at
          )
          VALUES (
              :idempotencyKey,
              :transferKey,
              :fromAccountId,
              :toAccountId,
              :amount,
              :status,
              :createdAt
          )
          ON CONFLICT (idempotency_key)
          DO NOTHING
          """,
      nativeQuery = true)
  int insertIfAbsent(
      @Param("idempotencyKey") String idempotencyKey,
      @Param("transferKey") UUID transferKey,
      @Param("fromAccountId") Long fromAccountId,
      @Param("toAccountId") Long toAccountId,
      @Param("amount") BigDecimal amount,
      @Param("status") String status,
      @Param("createdAt") Instant createdAt);
}
