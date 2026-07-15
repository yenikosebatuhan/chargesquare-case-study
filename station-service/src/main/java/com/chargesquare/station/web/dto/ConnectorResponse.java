package com.chargesquare.station.web.dto;

import com.chargesquare.station.domain.Connector;

import java.time.Instant;

public record ConnectorResponse(
        Long connectorId,
        Long stationId,
        String type,
        int powerKw,
        String status,
        Long reservedBy,
        Instant reservedUntil,
        TariffDto tariff) {

    public static ConnectorResponse from(Connector c, boolean peakNow) {
        return new ConnectorResponse(
                c.getId(),
                c.getStationId(),
                c.getType(),
                c.getPowerKw(),
                c.getStatus().name(),
                c.getReservedBy(),
                c.getReservedUntil(),
                TariffDto.from(c.getTariff(), peakNow));
    }
}
