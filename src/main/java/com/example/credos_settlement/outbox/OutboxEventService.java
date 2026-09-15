package com.example.credos_settlement.outbox;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventService {

  private final OutboxEventRepository outboxEventRepository;
  private final OutboxRetryPolicy retryPolicy;

  public OutboxEventService(
      OutboxEventRepository outboxEventRepository, OutboxRetryPolicy retryPolicy) {
    this.outboxEventRepository = outboxEventRepository;

    this.retryPolicy = retryPolicy;
  }

  /**
   * Transaction 1: lock the oldest PENDING event, move it to PROCESSING and commit. The row lock is
   * released on commit, so other workers are never blocked by the external call that follows.
   */
  @Transactional
  public Optional<ClaimedOutboxEvent> claimNext() {

    Instant now = Instant.now();

    return outboxEventRepository
        .findNextPendingForUpdate(now)
        .map(
            event -> {
              event.markProcessing(now);

              return new ClaimedOutboxEvent(event.getId(), event.getTransferKey());
            });
  }

  /** Transaction 2: move the event from PROCESSING to PROCESSED once settlement has completed. */
  @Transactional
  public void markProcessed(Long eventId) {
    OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();

    event.markProcessed();
  }

  @Transactional
  public void handleRetryableFailure(Long eventId, String error) {
    OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();

    retryPolicy.apply(event, Instant.now(), error);
  }

  @Transactional
  public void handlePermanentFailure(Long eventId, String error) {
    OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();

    event.markFailed(error);
  }
}
