package com.example.credos_settlement.account;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private final AccountRepository accountRepository;

  public AccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Transactional
  public Account createAccount(String name, BigDecimal balance) {
    Account account = new Account(name, balance, Instant.now());

    return accountRepository.save(account);
  }

  @Transactional(readOnly = true)
  public Account getAccount(Long id) {
    return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
  }
}
