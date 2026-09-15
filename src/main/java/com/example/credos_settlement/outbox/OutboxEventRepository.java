package com.example.credos_settlement.outbox;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

  @Query(
      value =
          """
          SELECT *
          FROM outbox_events
          WHERE status = 'PENDING'
          ORDER BY created_at
          LIMIT 1
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  Optional<OutboxEvent> findNextPendingForUpdate();

  @Modifying
  @Query(
      value =
          """
          UPDATE outbox_events
          SET status = 'PENDING',
              processing_started_at = NULL
          WHERE status = 'PROCESSING'
            AND processing_started_at < :threshold
          """,
      nativeQuery = true)
  int recoverStaleProcessing(@Param("threshold") Instant threshold);
}
