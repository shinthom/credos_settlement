package com.example.credos_settlement.outbox;

import com.example.credos_settlement.settlement.SettlementClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxWorker {

  private final OutboxEventRepository outboxEventRepository;
  private final SettlementClient settlementClient;

  public OutboxWorker(
      OutboxEventRepository outboxEventRepository, SettlementClient settlementClient) {
    this.outboxEventRepository = outboxEventRepository;
    this.settlementClient = settlementClient;
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void processNext() {
    outboxEventRepository.findNextPendingForUpdate().ifPresent(this::process);
  }

  private void process(OutboxEvent event) {
    settlementClient.settle(event.getTransferKey());

    event.markProcessed();
  }
}
