package com.chargesquare.session.station;

import java.math.BigDecimal;
import java.time.Instant;

/** What Session Service reads back from Station Service's GET /connectors/{id}. */
public record ConnectorView(
        Long connectorId,
        Long stationId,
        String type,
        Integer powerKw,
        String status,
        Long reservedBy,
        Instant reservedUntil,
        TariffView tariff) {

    public boolean isAvailable() {
        return "AVAILABLE".equals(status);
    }

    /** A connector can be started by a user if it is free, or reserved by that same user. */
    public boolean isStartable(Long userId) {
        if (isAvailable()) {
            return true;
        }
        return "RESERVED".equals(status) && userId != null && userId.equals(reservedBy);
    }

    public record TariffView(Long tariffId, BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
    }
}
