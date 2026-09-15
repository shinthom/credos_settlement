package com.example.credos_settlement.outbox;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OutboxRetryPolicy {

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

      event.scheduleRetry(now.plus(backoff), error);

      return;
    }

    event.markFailed(error);
  }
}
