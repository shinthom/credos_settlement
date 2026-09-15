package com.example.credos_settlement.outbox;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventService {

  private final OutboxEventRepository outboxEventRepository;

  public OutboxEventService(OutboxEventRepository outboxEventRepository) {
    this.outboxEventRepository = outboxEventRepository;
  }

  /**
   * Transaction 1: lock the oldest PENDING event, move it to PROCESSING and commit. The row lock is
   * released on commit, so other workers are never blocked by the external call that follows.
   */
  @Transactional
  public Optional<ClaimedOutboxEvent> claimNext() {
    return outboxEventRepository
        .findNextPendingForUpdate()
        .map(
            event -> {
              event.markProcessing();

              return new ClaimedOutboxEvent(event.getId(), event.getTransferKey());
            });
  }

  /** Transaction 2: move the event from PROCESSING to PROCESSED once settlement has completed. */
  @Transactional
  public void markProcessed(Long eventId) {
    OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();

    event.markProcessed();
  }
}
