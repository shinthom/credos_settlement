package com.example.credos_settlement.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxRecoveryService {

  private static final Duration STALE_TIMEOUT = Duration.ofMinutes(5);

  private final OutboxEventRepository outboxEventRepository;
  private final OutboxRetryPolicy retryPolicy;

  public OutboxRecoveryService(
      OutboxEventRepository outboxEventRepository, OutboxRetryPolicy retryPolicy) {
    this.outboxEventRepository = outboxEventRepository;

    this.retryPolicy = retryPolicy;
  }

  @Transactional
  public int recoverStaleEvents() {

    Instant now = Instant.now();

    Instant threshold = now.minus(STALE_TIMEOUT);

    List<OutboxEvent> events = outboxEventRepository.findStaleProcessingForUpdate(threshold);

    for (OutboxEvent event : events) {
      retryPolicy.apply(event, now, "Processing timed out");
    }

    return events.size();
  }
}
