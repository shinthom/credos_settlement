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

  public ReconciliationController(ReconciliationService reconciliationService) {
    this.reconciliationService = reconciliationService;
  }

  @GetMapping("/transfers/{transferKey}")
  public TransferReconciliationResult reconcile(@PathVariable UUID transferKey) {
    return reconciliationService.reconcile(transferKey);
  }
}
