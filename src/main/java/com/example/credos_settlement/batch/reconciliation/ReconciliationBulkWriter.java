package com.example.credos_settlement.batch.reconciliation;

import com.example.credos_settlement.reconciliation.ReconciliationStatus;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationBulkWriter implements ItemWriter<TransferBatchItem> {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public ReconciliationBulkWriter(DataSource dataSource) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  @Override
  public void write(Chunk<? extends TransferBatchItem> chunk) {

    if (chunk.isEmpty()) {
      return;
    }

    List<? extends TransferBatchItem> transfers = chunk.getItems();

    List<UUID> transferKeys = transfers.stream().map(TransferBatchItem::transferKey).toList();

    List<LedgerBatchItem> ledgerEntries = findLedgerEntries(transferKeys);

    Map<UUID, List<LedgerBatchItem>> ledgerByTransferKey =
        ledgerEntries.stream().collect(Collectors.groupingBy(LedgerBatchItem::transferKey));

    List<SqlParameterSource> parameters = new ArrayList<>();

    for (TransferBatchItem transfer : transfers) {

      List<LedgerBatchItem> entries =
          ledgerByTransferKey.getOrDefault(transfer.transferKey(), List.of());

      parameters.add(reconcile(transfer, entries));
    }

    writeResults(parameters);
  }

  private List<LedgerBatchItem> findLedgerEntries(List<UUID> transferKeys) {

    String sql =
        """
        SELECT
            transfer_key,
            account_id,
            amount
        FROM ledger_entries
        WHERE transfer_key IN (:transferKeys)
        """;

    MapSqlParameterSource parameters =
        new MapSqlParameterSource().addValue("transferKeys", transferKeys);

    return jdbcTemplate.query(
        sql,
        parameters,
        (rs, rowNum) ->
            new LedgerBatchItem(
                rs.getObject("transfer_key", UUID.class),
                rs.getLong("account_id"),
                rs.getBigDecimal("amount")));
  }

  private SqlParameterSource reconcile(TransferBatchItem transfer, List<LedgerBatchItem> entries) {

    BigDecimal debitAmount = sumForAccount(entries, transfer.fromAccountId());

    BigDecimal creditAmount = sumForAccount(entries, transfer.toAccountId());

    BigDecimal ledgerSum =
        entries.stream().map(LedgerBatchItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

    boolean matched =
        entries.size() == 2
            && debitAmount.compareTo(transfer.amount().negate()) == 0
            && creditAmount.compareTo(transfer.amount()) == 0
            && ledgerSum.compareTo(BigDecimal.ZERO) == 0;

    return new MapSqlParameterSource()
        .addValue("transferKey", transfer.transferKey())
        .addValue(
            "status",
            matched ? ReconciliationStatus.MATCHED.name() : ReconciliationStatus.MISMATCH.name())
        .addValue("transferAmount", transfer.amount())
        .addValue("debitAmount", debitAmount)
        .addValue("creditAmount", creditAmount)
        .addValue("ledgerSum", ledgerSum)
        .addValue("ledgerEntryCount", entries.size())
        .addValue("checkedAt", Timestamp.from(Instant.now()));
  }

  private BigDecimal sumForAccount(List<LedgerBatchItem> entries, Long accountId) {

    return entries.stream()
        .filter(entry -> entry.accountId().equals(accountId))
        .map(LedgerBatchItem::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void writeResults(List<SqlParameterSource> parameters) {

    String sql =
        """
        INSERT INTO reconciliation_results (
            transfer_key,
            status,
            transfer_amount,
            debit_amount,
            credit_amount,
            ledger_sum,
            ledger_entry_count,
            checked_at
        )
        VALUES (
            :transferKey,
            :status,
            :transferAmount,
            :debitAmount,
            :creditAmount,
            :ledgerSum,
            :ledgerEntryCount,
            :checkedAt
        )
        ON CONFLICT (transfer_key)
        DO UPDATE SET
            status =
                EXCLUDED.status,
            transfer_amount =
                EXCLUDED.transfer_amount,
            debit_amount =
                EXCLUDED.debit_amount,
            credit_amount =
                EXCLUDED.credit_amount,
            ledger_sum =
                EXCLUDED.ledger_sum,
            ledger_entry_count =
                EXCLUDED.ledger_entry_count,
            checked_at =
                EXCLUDED.checked_at
        """;

    jdbcTemplate.batchUpdate(sql, parameters.toArray(SqlParameterSource[]::new));
  }
}
