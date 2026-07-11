package com.chargesquare.session.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Energy may be zero (start fee still applies) but never negative. */
public record StopSessionRequest(
        @NotNull(message = "is required")
        @PositiveOrZero(message = "must not be negative")
        BigDecimal energyKwh) {
}
