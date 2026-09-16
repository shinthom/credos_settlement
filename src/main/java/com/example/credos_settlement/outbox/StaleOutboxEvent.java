package com.example.credos_settlement.outbox;

import java.util.UUID;

public record StaleOutboxEvent(Long eventId, UUID transferKey) {}
