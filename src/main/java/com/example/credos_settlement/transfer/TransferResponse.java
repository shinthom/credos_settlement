package com.example.credos_settlement.transfer;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(UUID transferKey, TransferStatus status, BigDecimal amount) {

  public static TransferResponse from(Transfer transfer) {
    return new TransferResponse(
        transfer.getTransferKey(), transfer.getStatus(), transfer.getAmount());
  }
}
