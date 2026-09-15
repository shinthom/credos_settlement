package com.example.credos_settlement.outbox;

import java.time.Duration;
import java.time.Instant;
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
  public int recoverStaleEvents() {
    Instant threshold = Instant.now().minus(STALE_TIMEOUT);

    return outboxEventRepository.recoverStaleProcessing(threshold);
  }
}
