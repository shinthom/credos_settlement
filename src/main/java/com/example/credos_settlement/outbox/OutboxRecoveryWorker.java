package com.example.credos_settlement.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRecoveryWorker {

  private final OutboxRecoveryService recoveryService;

  public OutboxRecoveryWorker(OutboxRecoveryService recoveryService) {
    this.recoveryService = recoveryService;
  }

  @Scheduled(fixedDelay = 60000)
  public void recover() {
    int recovered = recoveryService.recoverStaleEvents();

    if (recovered > 0) {
      System.out.println("Recovered stale outbox events: " + recovered);
    }
  }
}
