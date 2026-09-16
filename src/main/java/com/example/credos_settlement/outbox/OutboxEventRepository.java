package com.example.credos_settlement.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

  // Claims the oldest PENDING event that is due for an attempt:
  //
  //   PENDING + next_attempt_at IS NULL   -> never attempted, run immediately
  //   PENDING + next_attempt_at <= now    -> backoff has elapsed, eligible to run
  //   PENDING + next_attempt_at >  now    -> still backing off, left untouched
  //
  // SKIP LOCKED lets concurrent workers claim different rows without blocking each other.
  @Query(
      value =
          """
          SELECT *
          FROM outbox_events
          WHERE status = 'PENDING'
            AND (
                next_attempt_at IS NULL
                OR next_attempt_at <= :now
            )
          ORDER BY created_at
          LIMIT 1
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  Optional<OutboxEvent> findNextPendingForUpdate(@Param("now") Instant now);

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

  @Query(
      value =
          """
          SELECT *
          FROM outbox_events
          WHERE status = 'PROCESSING'
            AND processing_started_at < :threshold
          ORDER BY processing_started_at
          LIMIT 100
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  List<OutboxEvent> findStaleProcessingForUpdate(@Param("threshold") Instant threshold);

  Optional<OutboxEvent> findByTransferKeyAndEventType(UUID transferKey, OutboxEventType eventType);
}
