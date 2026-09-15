package com.example.credos_settlement.settlement;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubSettlementClient implements SettlementClient {

  @Override
  public void settle(UUID transferKey) {

    System.out.println("Settlement started: " + transferKey);

    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }

    System.out.println("Settlement completed: " + transferKey);
  }
}
