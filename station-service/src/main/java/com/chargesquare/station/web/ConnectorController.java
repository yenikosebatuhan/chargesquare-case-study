package com.chargesquare.station.web;

import com.chargesquare.station.service.ConnectorService;
import com.chargesquare.station.web.dto.ConnectorResponse;
import com.chargesquare.station.web.dto.ConnectorStatusResponse;
import com.chargesquare.station.web.dto.OccupyRequest;
import com.chargesquare.station.web.dto.ReserveRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/connectors")
public class ConnectorController {

    private static final int DEFAULT_RESERVE_TTL_SECONDS = 300;

    private final ConnectorService service;

    public ConnectorController(ConnectorService service) {
        this.service = service;
    }

    /** Read a single connector's status + tariff (effective price reflects peak/off-peak now). */
    @GetMapping("/{id}")
    public ConnectorResponse getConnector(@PathVariable Long id) {
        return ConnectorResponse.from(service.getConnector(id), service.isPeakNow());
    }

    /** Internal: flip to OCCUPIED. Honours a reservation held by the same user (ADMIN when secured). */
    @PostMapping("/{id}/occupy")
    public ConnectorStatusResponse occupy(@PathVariable Long id,
                                          @RequestBody(required = false) OccupyRequest request) {
        Long userId = request != null ? request.userId() : null;
        return ConnectorStatusResponse.from(service.occupy(id, userId));
    }

    /** Internal: mirror of occupy — sets status back to AVAILABLE. */
    @PostMapping("/{id}/release")
    public ConnectorStatusResponse release(@PathVariable Long id) {
        return ConnectorStatusResponse.from(service.release(id));
    }

    /** Reserve an AVAILABLE connector for a user for a short TTL (stretch goal). */
    @PostMapping("/{id}/reserve")
    public ConnectorStatusResponse reserve(@PathVariable Long id, @Valid @RequestBody ReserveRequest request) {
        int ttl = request.ttlSeconds() != null ? request.ttlSeconds() : DEFAULT_RESERVE_TTL_SECONDS;
        return ConnectorStatusResponse.from(service.reserve(id, request.userId(), Duration.ofSeconds(ttl)));
    }

    /** Cancel a reservation, returning the connector to AVAILABLE. */
    @PostMapping("/{id}/cancel-reservation")
    public ConnectorStatusResponse cancelReservation(@PathVariable Long id) {
        return ConnectorStatusResponse.from(service.cancelReservation(id));
    }
}
