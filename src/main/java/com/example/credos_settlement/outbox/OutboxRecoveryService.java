package com.example.credos_settlement.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
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
  public Optional<StaleOutboxEvent> claimStale() {

    Instant now = Instant.now();

    Instant threshold = now.minus(STALE_TIMEOUT);

    return outboxEventRepository
        .findNextStaleProcessingForUpdate(threshold)
        .map(
            event -> {
              event.refreshProcessingStartedAt(now);

              return new StaleOutboxEvent(event.getId(), event.getTransferKey());
            });
  }

  @Transactional
  public void markRecoveredAsProcessed(Long eventId) {
    OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();

    event.markProcessed();
  }

  @Transactional
  public void scheduleRecoveryRetry(Long eventId, String error) {
    OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();

    retryPolicy.apply(event, Instant.now(), error);
  }
}
