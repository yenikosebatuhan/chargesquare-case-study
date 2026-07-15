package com.chargesquare.wallet.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Debit request from Session Service at stop. The key (session id) makes it idempotent. */
public record DebitRequest(
        @NotNull(message = "is required") @PositiveOrZero(message = "must not be negative") BigDecimal amount,
        @NotBlank(message = "is required") String idempotencyKey) {
}
