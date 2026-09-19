package com.example.credos_settlement.batch.reconciliation;

import com.example.credos_settlement.ledger.LedgerEntry;
import com.example.credos_settlement.ledger.LedgerEntryRepository;
import com.example.credos_settlement.reconciliation.ReconciliationResult;
import com.example.credos_settlement.reconciliation.ReconciliationStatus;
import com.example.credos_settlement.transfer.TransferStatus;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.support.PostgresPagingQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ReconciliationBatchConfig {

  private static final int CHUNK_SIZE = 1000;

  @Bean
  @StepScope
  public JdbcPagingItemReader<TransferBatchItem> reconciliationReader(DataSource dataSource)
      throws Exception {

    PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();

    queryProvider.setSelectClause(
        """
        SELECT
            id,
            transfer_key,
            from_account_id,
            to_account_id,
            amount,
            status,
            created_at
        """);

    queryProvider.setFromClause("FROM transfers");

    queryProvider.setSortKeys(Map.of("id", Order.ASCENDING));

    queryProvider.init(dataSource);

    JdbcPagingItemReader<TransferBatchItem> reader =
        new JdbcPagingItemReader<>(dataSource, queryProvider);

    reader.setName("reconciliationReader");

    reader.setPageSize(CHUNK_SIZE);
    reader.setFetchSize(CHUNK_SIZE);

    reader.setRowMapper(
        (rs, rowNum) ->
            new TransferBatchItem(
                rs.getLong("id"),
                rs.getObject("transfer_key", java.util.UUID.class),
                rs.getLong("from_account_id"),
                rs.getLong("to_account_id"),
                rs.getBigDecimal("amount"),
                TransferStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant()));

    return reader;
  }

  @Bean
  public ItemProcessor<TransferBatchItem, ReconciliationResult> reconciliationProcessor(
      LedgerEntryRepository ledgerEntryRepository) {

    return transfer -> {
      List<LedgerEntry> entries =
          ledgerEntryRepository.findAllByTransferKey(transfer.transferKey());

      BigDecimal debitAmount = sumForAccount(entries, transfer.fromAccountId());
      BigDecimal creditAmount = sumForAccount(entries, transfer.toAccountId());
      BigDecimal ledgerSum =
          entries.stream().map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

      boolean matched =
          entries.size() == 2
              && debitAmount.compareTo(transfer.amount().negate()) == 0
              && creditAmount.compareTo(transfer.amount()) == 0
              && ledgerSum.compareTo(BigDecimal.ZERO) == 0;

      return new ReconciliationResult(
          transfer.transferKey(),
          matched ? ReconciliationStatus.MATCHED : ReconciliationStatus.MISMATCH,
          transfer.amount(),
          debitAmount,
          creditAmount,
          ledgerSum,
          entries.size(),
          Instant.now());
    };
  }

  @Bean
  public ItemWriter<ReconciliationResult> reconciliationWriter(DataSource dataSource) {
    NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

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
            status = EXCLUDED.status,
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

    return chunk -> {
      List<SqlParameterSource> parameters = new ArrayList<>();
      for (ReconciliationResult result : chunk) {
        parameters.add(
            new MapSqlParameterSource()
                .addValue("transferKey", result.getTransferKey())
                .addValue("status", result.getStatus().name())
                .addValue("transferAmount", result.getTransferAmount())
                .addValue("debitAmount", result.getDebitAmount())
                .addValue("creditAmount", result.getCreditAmount())
                .addValue("ledgerSum", result.getLedgerSum())
                .addValue("ledgerEntryCount", result.getLedgerEntryCount())
                .addValue("checkedAt", Timestamp.from(result.getCheckedAt())));
      }

      jdbcTemplate.batchUpdate(sql, parameters.toArray(SqlParameterSource[]::new));
    };
  }

  @Bean
  public Step reconciliationStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      JdbcPagingItemReader<TransferBatchItem> reconciliationReader,
      ItemProcessor<TransferBatchItem, ReconciliationResult> reconciliationProcessor,
      ItemWriter<ReconciliationResult> reconciliationWriter) {

    return new StepBuilder("reconciliationStep", jobRepository)
        .<TransferBatchItem, ReconciliationResult>chunk(CHUNK_SIZE)
        .reader(reconciliationReader)
        .processor(reconciliationProcessor)
        .writer(reconciliationWriter)
        .transactionManager(transactionManager)
        .build();
  }

  @Bean
  public Job reconciliationJob(JobRepository jobRepository, Step reconciliationStep) {
    return new JobBuilder("reconciliationJob", jobRepository).start(reconciliationStep).build();
  }

  private static BigDecimal sumForAccount(List<LedgerEntry> entries, Long accountId) {
    return entries.stream()
        .filter(entry -> entry.getAccountId().equals(accountId))
        .map(LedgerEntry::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
