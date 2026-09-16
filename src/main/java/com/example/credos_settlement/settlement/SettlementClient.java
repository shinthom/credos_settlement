package com.example.credos_settlement.settlement;

import java.util.UUID;

public interface SettlementClient {

  void settle(UUID transferKey);

  SettlementStatus getStatus(UUID transferKey);
}
