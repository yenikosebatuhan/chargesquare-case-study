package com.chargesquare.session.station;

import java.math.BigDecimal;

/** What Session Service reads back from Station Service's GET /connectors/{id}. */
public record ConnectorView(
        Long connectorId,
        Long stationId,
        String type,
        Integer powerKw,
        String status,
        TariffView tariff) {

    public boolean isAvailable() {
        return "AVAILABLE".equals(status);
    }

    public record TariffView(Long tariffId, BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
    }
}
