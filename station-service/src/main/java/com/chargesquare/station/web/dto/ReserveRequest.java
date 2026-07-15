package com.chargesquare.station.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Reserve a connector for a user for a short time-to-live (defaults applied if omitted). */
public record ReserveRequest(
        @NotNull(message = "is required") @Positive(message = "must be positive") Long userId,
        Integer ttlSeconds) {
}
