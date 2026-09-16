package com.example.credos_settlement.common;

import com.example.credos_settlement.account.AccountNotFoundException;
import com.example.credos_settlement.account.InsufficientBalanceException;
import com.example.credos_settlement.transfer.IdempotencyConflictException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleAccountNotFound(AccountNotFoundException e) {

    return build(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", e.getMessage());
  }

  @ExceptionHandler(InsufficientBalanceException.class)
  public ResponseEntity<ApiErrorResponse> handleInsufficientBalance(
      InsufficientBalanceException e) {

    return build(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", e.getMessage());
  }

  @ExceptionHandler(IdempotencyConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleIdempotencyConflict(
      IdempotencyConflictException e) {

    return build(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e) {

    return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {

    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Invalid request");

    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
  }

  private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message) {
    ApiErrorResponse body = new ApiErrorResponse(status.value(), code, message, Instant.now());

    return ResponseEntity.status(status).body(body);
  }
}
