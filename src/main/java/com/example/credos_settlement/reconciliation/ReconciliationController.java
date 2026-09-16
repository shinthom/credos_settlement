package com.example.credos_settlement.reconciliation;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reconciliations")
public class ReconciliationController {

  private final ReconciliationService reconciliationService;
  private final ExternalReconciliationService externalReconciliationService;

  public ReconciliationController(
      ReconciliationService reconciliationService,
      ExternalReconciliationService externalReconciliationService) {
    this.reconciliationService = reconciliationService;

    this.externalReconciliationService = externalReconciliationService;
  }

  @GetMapping("/external/transfers/{transferKey}")
  public ExternalReconciliationResult reconcileExternal(@PathVariable UUID transferKey) {
    return externalReconciliationService.reconcile(transferKey);
  }
}
