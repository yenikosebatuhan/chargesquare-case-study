package com.chargesquare.station.web.dto;

import com.chargesquare.station.domain.Connector;

/** Minimal body returned by the internal occupy/release calls. */
public record ConnectorStatusResponse(Long connectorId, String status) {
    public static ConnectorStatusResponse from(Connector c) {
        return new ConnectorStatusResponse(c.getId(), c.getStatus().name());
    }
}
