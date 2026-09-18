package com.example.credos_settlement.batch.reconciliation;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ReconciliationBatchConfig {

  private static final Logger log = LoggerFactory.getLogger(ReconciliationBatchConfig.class);

  @Bean
  @StepScope
  public ListItemReader<String> reconciliationReader() {
    return new ListItemReader<>(
        List.of("transfer-1", "transfer-2", "transfer-3", "transfer-4", "transfer-5"));
  }

  @Bean
  public ItemProcessor<String, String> reconciliationProcessor() {
    return item -> "checked: " + item;
  }

  @Bean
  public ItemWriter<String> reconciliationWriter() {
    return chunk -> {
      for (String item : chunk) {
        log.info("Reconciliation result: {}", item);
      }
    };
  }

  @Bean
  public Step reconciliationStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ListItemReader<String> reconciliationReader,
      ItemProcessor<String, String> reconciliationProcessor,
      ItemWriter<String> reconciliationWriter) {
    return new StepBuilder("reconciliationStep", jobRepository)
        .<String, String>chunk(2)
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
}
