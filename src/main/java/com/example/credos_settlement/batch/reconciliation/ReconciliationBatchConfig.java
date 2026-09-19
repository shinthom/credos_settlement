package com.example.credos_settlement.batch.reconciliation;

import com.example.credos_settlement.transfer.TransferStatus;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.support.PostgresPagingQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
  public Step reconciliationStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      JdbcPagingItemReader<TransferBatchItem> reconciliationReader,
      ReconciliationBulkWriter reconciliationBulkWriter) {

    return new StepBuilder("reconciliationStep", jobRepository)
        .<TransferBatchItem, TransferBatchItem>chunk(CHUNK_SIZE)
        .reader(reconciliationReader)
        .writer(reconciliationBulkWriter)
        .transactionManager(transactionManager)
        .build();
  }

  @Bean
  public Job reconciliationJob(JobRepository jobRepository, Step reconciliationStep) {

    return new JobBuilder("reconciliationJob", jobRepository).start(reconciliationStep).build();
  }
}
