package com.example.credos_settlement.settlement;

public class RetryableSettlementException extends RuntimeException {

  public RetryableSettlementException(String message) {
    super(message);
  }

  public RetryableSettlementException(String message, Throwable cause) {
    super(message, cause);
  }
}
