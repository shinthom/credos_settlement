package com.example.credos_settlement.settlement;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubSettlementClient implements SettlementClient {

  @Override
  public void settle(UUID transferKey) {
    System.out.println("Settling transfer: " + transferKey);
  }
}
