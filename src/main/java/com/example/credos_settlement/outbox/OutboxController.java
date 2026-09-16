package com.example.credos_settlement.outbox;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/outbox")
public class OutboxController {
  private final OutboxEventService outboxEventService;

  public OutboxController(OutboxEventService outboxEventService) {
    this.outboxEventService = outboxEventService;
  }

  @GetMapping("/failed")
  public List<OutboxEventResponse> getFailedEvents() {
    return outboxEventService.getFailedEvents();
  }
}
