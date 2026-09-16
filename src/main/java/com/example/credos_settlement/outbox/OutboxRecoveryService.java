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

  public OutboxRecoveryService(OutboxEventRepository outboxEventRepository) {
    this.outboxEventRepository = outboxEventRepository;
  }

  @Transactional
  public List<StaleOutboxEvent> claimStaleEvents() {
    Instant now = Instant.now();
    Instant threshold = now.minus(STALE_TIMEOUT);

    List<OutboxEvent> events = outboxEventRepository.findStaleProcessingForUpdate(threshold);

    return events.stream()
        .map(
            event -> {
              event.refreshProcessingStartedAt(now);
              return new StaleOutboxEvent(event.getId(), event.getTransferKey());
            })
        .toList();
  }
}
