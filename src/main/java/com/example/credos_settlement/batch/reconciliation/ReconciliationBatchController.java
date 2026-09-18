package com.example.credos_settlement.batch.reconciliation;

import java.util.Map;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batch/reconciliation")
public class ReconciliationBatchController {

  private final JobOperator jobOperator;
  private final Job reconciliationJob;

  public ReconciliationBatchController(
      JobOperator jobOperator, @Qualifier("reconciliationJob") Job reconciliationJob) {
    this.jobOperator = jobOperator;
    this.reconciliationJob = reconciliationJob;
  }

  @PostMapping("/run")
  public Map<String, Object> run() throws Exception {

    JobParameters jobParameters =
        new JobParametersBuilder().addLong("run.id", System.currentTimeMillis()).toJobParameters();

    JobExecution execution = jobOperator.start(reconciliationJob, jobParameters);

    return Map.of("jobExecutionId", execution.getId(), "status", execution.getStatus().name());
  }
}
