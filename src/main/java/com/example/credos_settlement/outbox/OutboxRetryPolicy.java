package com.example.credos_settlement.outbox;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OutboxRetryPolicy {
  private static final Logger log = LoggerFactory.getLogger(OutboxRetryPolicy.class);

  private static final int MAX_ATTEMPTS = 5;

  public boolean canRetry(int attempts) {
    return attempts < MAX_ATTEMPTS;
  }

  public Duration calculateBackoff(int attempts) {
    long seconds = 1;

    for (int i = 1; i < attempts; i++) {
      seconds *= 2;
    }

    return Duration.ofSeconds(seconds);
  }

  public void apply(OutboxEvent event, Instant now, String error) {
    if (canRetry(event.getAttempts())) {

      Duration backoff = calculateBackoff(event.getAttempts());
      Instant nextAttemptAt = now.plus(backoff);

      event.scheduleRetry(nextAttemptAt, error);

      log.info(
          "Outbox event retry scheduled: eventId={}, attempts={}/{}, backoff={}, nextAttemptAt={}",
          event.getId(),
          event.getAttempts(),
          MAX_ATTEMPTS,
          backoff,
          nextAttemptAt);

      return;
    }

    event.markFailed(error);

    log.error(
        "Outbox event exhausted retries, marked as FAILED: eventId={}, transferKey={}, attempts={}, error={}",
        event.getId(),
        event.getTransferKey(),
        event.getAttempts(),
        error);
  }
}
