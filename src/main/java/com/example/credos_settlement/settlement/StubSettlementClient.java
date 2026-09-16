package com.example.credos_settlement.settlement;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class StubSettlementClient implements SettlementClient {

  private final Set<UUID> settledTransfers = ConcurrentHashMap.newKeySet();

  @Override
  public void settle(UUID transferKey) {

    settledTransfers.add(transferKey);

    System.out.println("Settled transfer: " + transferKey);
  }

  @Override
  public SettlementStatus getStatus(UUID transferKey) {
    if (settledTransfers.contains(transferKey)) {
      return SettlementStatus.SETTLED;
    }

    return SettlementStatus.NOT_FOUND;
  }
}
