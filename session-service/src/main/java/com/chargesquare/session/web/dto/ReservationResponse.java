package com.chargesquare.session.web.dto;

import com.chargesquare.session.station.ConnectorView;

import java.time.Instant;

public record ReservationResponse(Long connectorId, Long reservedBy, String status, Instant reservedUntil) {
    public static ReservationResponse from(ConnectorView c) {
        return new ReservationResponse(c.connectorId(), c.reservedBy(), c.status(), c.reservedUntil());
    }
}
