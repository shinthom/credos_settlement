package com.example.credos_settlement.outbox;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
