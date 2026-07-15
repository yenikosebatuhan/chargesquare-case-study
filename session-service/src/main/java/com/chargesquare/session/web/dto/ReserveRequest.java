package com.chargesquare.session.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReserveRequest(
        @NotNull(message = "is required") @Positive(message = "must be positive") Long userId,
        @NotNull(message = "is required") @Positive(message = "must be positive") Long connectorId,
        Integer ttlSeconds) {
}
