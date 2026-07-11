package com.chargesquare.session.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TopUpRequest(
        @NotNull(message = "is required") @Positive(message = "must be positive") BigDecimal amount) {
}
