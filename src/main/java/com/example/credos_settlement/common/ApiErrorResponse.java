package com.example.credos_settlement.common;

import java.time.Instant;

public record ApiErrorResponse(int status, String code, String message, Instant timestamp) {}
