package com.chargesquare.station.web.dto;

import com.chargesquare.station.domain.Connector;

public record ConnectorResponse(
        Long connectorId,
        Long stationId,
        String type,
        int powerKw,
        String status,
        TariffDto tariff) {

    public static ConnectorResponse from(Connector c) {
        return new ConnectorResponse(
                c.getId(),
                c.getStationId(),
                c.getType(),
                c.getPowerKw(),
                c.getStatus().name(),
                TariffDto.from(c.getTariff()));
    }
}
