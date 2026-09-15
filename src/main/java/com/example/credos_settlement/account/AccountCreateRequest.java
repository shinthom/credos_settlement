package com.example.credos_settlement.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AccountCreateRequest(
    @NotBlank String name, @NotNull @PositiveOrZero BigDecimal balance) {}
