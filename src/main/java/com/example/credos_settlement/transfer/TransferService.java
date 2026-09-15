package com.example.credos_settlement.transfer;

import com.example.credos_settlement.account.Account;
import com.example.credos_settlement.account.AccountNotFoundException;
import com.example.credos_settlement.account.AccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

  private final AccountRepository accountRepository;
  private final TransferRepository transferRepository;

  public TransferService(
      AccountRepository accountRepository, TransferRepository transferRepository) {
    this.accountRepository = accountRepository;
    this.transferRepository = transferRepository;
  }

  @Transactional
  public Transfer transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
    if (fromAccountId.equals(toAccountId)) {
      throw new IllegalArgumentException("Cannot transfer to the same account");
    }

    List<Account> accounts =
        accountRepository.findAllByIdForUpdate(List.of(fromAccountId, toAccountId));

    Account from = findAccount(accounts, fromAccountId);
    Account to = findAccount(accounts, toAccountId);

    from.withdraw(amount);
    to.deposit(amount);

    Transfer transfer =
        new Transfer(
            UUID.randomUUID(),
            fromAccountId,
            toAccountId,
            amount,
            TransferStatus.COMPLETED,
            Instant.now());

    return transferRepository.save(transfer);
  }

  private Account findAccount(List<Account> accounts, Long accountId) {
    return accounts.stream()
        .filter(account -> account.getId().equals(accountId))
        .findFirst()
        .orElseThrow(() -> new AccountNotFoundException(accountId));
  }
}
