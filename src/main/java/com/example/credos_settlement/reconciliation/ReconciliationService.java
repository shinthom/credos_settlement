package com.example.credos_settlement.reconciliation;

import com.example.credos_settlement.ledger.LedgerEntry;
import com.example.credos_settlement.ledger.LedgerEntryRepository;
import com.example.credos_settlement.transfer.Transfer;
import com.example.credos_settlement.transfer.TransferRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {

  private final TransferRepository transferRepository;
  private final LedgerEntryRepository ledgerEntryRepository;

  public ReconciliationService(
      TransferRepository transferRepository, LedgerEntryRepository ledgerEntryRepository) {
    this.transferRepository = transferRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
  }

  @Transactional(readOnly = true)
  public TransferReconciliationResult reconcile(UUID transferKey) {
    Transfer transfer = transferRepository.findByTransferKey(transferKey).orElseThrow();

    List<LedgerEntry> entries = ledgerEntryRepository.findAllByTransferKey(transferKey);

    BigDecimal debitAmount = sumForAccount(entries, transfer.getFromAccountId());

    BigDecimal creditAmount = sumForAccount(entries, transfer.getToAccountId());

    BigDecimal ledgerSum =
        entries.stream().map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    boolean matched =
        entries.size() == 2
            && debitAmount.compareTo(transfer.getAmount().negate()) == 0
            && creditAmount.compareTo(transfer.getAmount()) == 0
            && ledgerSum.compareTo(BigDecimal.ZERO) == 0;

    return new TransferReconciliationResult(
        transfer.getTransferKey(),
        matched ? ReconciliationStatus.MATCHED : ReconciliationStatus.MISMATCH,
        transfer.getAmount(),
        debitAmount,
        creditAmount,
        ledgerSum,
        entries.size());
  }

  private BigDecimal sumForAccount(List<LedgerEntry> entries, Long accountId) {
    return entries.stream()
        .filter(entry -> entry.getAccountId().equals(accountId))
        .map(LedgerEntry::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
