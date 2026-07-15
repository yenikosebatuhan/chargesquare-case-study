package com.chargesquare.station.web.dto;

import com.chargesquare.station.domain.Connector;

import java.time.Instant;

/** Minimal body returned by the internal occupy/release/reserve calls. */
public record ConnectorStatusResponse(Long connectorId, String status, Long reservedBy, Instant reservedUntil) {
    public static ConnectorStatusResponse from(Connector c) {
        return new ConnectorStatusResponse(c.getId(), c.getStatus().name(), c.getReservedBy(), c.getReservedUntil());
    }
}
