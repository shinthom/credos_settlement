package com.example.credos_settlement.transfer;

import com.example.credos_settlement.account.Account;
import com.example.credos_settlement.account.AccountNotFoundException;
import com.example.credos_settlement.account.AccountRepository;
import com.example.credos_settlement.ledger.LedgerEntry;
import com.example.credos_settlement.ledger.LedgerEntryRepository;
import com.example.credos_settlement.outbox.OutboxEvent;
import com.example.credos_settlement.outbox.OutboxEventRepository;
import com.example.credos_settlement.outbox.OutboxEventStatus;
import com.example.credos_settlement.outbox.OutboxEventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
  private static final Logger log = LoggerFactory.getLogger(TransferService.class);

  private final AccountRepository accountRepository;
  private final TransferRepository transferRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
  private final OutboxEventRepository outboxEventRepository;

  public TransferService(
      AccountRepository accountRepository,
      TransferRepository transferRepository,
      LedgerEntryRepository ledgerEntryRepository,
      OutboxEventRepository outboxEventRepository) {
    this.accountRepository = accountRepository;
    this.transferRepository = transferRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.outboxEventRepository = outboxEventRepository;
  }

  @Transactional
  public Transfer transfer(
      String idempotencyKey, Long fromAccountId, Long toAccountId, BigDecimal amount) {
    if (fromAccountId.equals(toAccountId)) {
      throw new IllegalArgumentException("Cannot transfer to the same account");
    }

    UUID transferKey = UUID.randomUUID();
    Instant now = Instant.now();

    int inserted =
        transferRepository.insertIfAbsent(
            idempotencyKey,
            transferKey,
            fromAccountId,
            toAccountId,
            amount,
            TransferStatus.PROCESSING.name(),
            now);

    if (inserted == 0) {
      Transfer existing = transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();

      if (!sameRequest(existing, fromAccountId, toAccountId, amount)) {
        log.warn(
            "Idempotency conflict: idempotencyKey={}, existingTransferKey={}, fromAccountId={}, toAccountId={}, amount={}",
            idempotencyKey,
            existing.getTransferKey(),
            fromAccountId,
            toAccountId,
            amount);

        throw new IdempotencyConflictException();
      }

      log.info(
          "Duplicate transfer request, returning existing transfer: idempotencyKey={}, transferKey={}",
          idempotencyKey,
          existing.getTransferKey());

      return existing;
    }

    Transfer transfer = transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();

    List<Account> accounts =
        accountRepository.findAllByIdForUpdate(List.of(fromAccountId, toAccountId));

    Account from = findAccount(accounts, fromAccountId);
    Account to = findAccount(accounts, toAccountId);

    from.withdraw(amount);
    to.deposit(amount);

    LedgerEntry debitEntry =
        new LedgerEntry(transfer.getTransferKey(), fromAccountId, amount.negate(), now);
    LedgerEntry creditEntry = new LedgerEntry(transfer.getTransferKey(), toAccountId, amount, now);

    ledgerEntryRepository.save(debitEntry);
    ledgerEntryRepository.save(creditEntry);

    OutboxEvent outboxEvent =
        new OutboxEvent(
            UUID.randomUUID(),
            transfer.getTransferKey(),
            OutboxEventType.SETTLEMENT_REQUESTED,
            OutboxEventStatus.PENDING,
            now);
    outboxEventRepository.save(outboxEvent);

    transfer.complete();

    log.info(
        "Transfer completed and settlement requested: transferKey={}, fromAccountId={}, toAccountId={}, amount={}",
        transfer.getTransferKey(),
        fromAccountId,
        toAccountId,
        amount);

    return transfer;
  }

  private boolean sameRequest(
      Transfer transfer, Long fromAccountId, Long toAccountId, BigDecimal amount) {
    return transfer.getFromAccountId().equals(fromAccountId)
        && transfer.getToAccountId().equals(toAccountId)
        && transfer.getAmount().compareTo(amount) == 0;
  }

  private Account findAccount(List<Account> accounts, Long accountId) {
    return accounts.stream()
        .filter(account -> account.getId().equals(accountId))
        .findFirst()
        .orElseThrow(() -> new AccountNotFoundException(accountId));
  }
}
