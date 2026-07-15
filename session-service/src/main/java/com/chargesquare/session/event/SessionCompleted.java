package com.chargesquare.session.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Emitted once when a session completes. In this slice the wallet is settled synchronously
 * during stop; this event is the single domain signal other consumers (analytics, receipts,
 * a future async settlement) could subscribe to — kept to one hop, no broker.
 */
public record SessionCompleted(
        Long sessionId,
        Long userId,
        Long connectorId,
        BigDecimal energyKwh,
        BigDecimal cost,
        Instant occurredAt) {
}
