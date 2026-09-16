package com.example.credos_settlement.settlement;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StubSettlementClient implements SettlementClient {
  private static final Logger log = LoggerFactory.getLogger(StubSettlementClient.class);

  private final Set<UUID> settledTransfers = ConcurrentHashMap.newKeySet();

  @Override
  public void settle(UUID transferKey) {

    settledTransfers.add(transferKey);

    log.info("Stub settlement completed: transferKey={}", transferKey);
  }

  @Override
  public SettlementStatus getStatus(UUID transferKey) {
    if (settledTransfers.contains(transferKey)) {
      return SettlementStatus.SETTLED;
    }

    return SettlementStatus.NOT_FOUND;
  }
}
