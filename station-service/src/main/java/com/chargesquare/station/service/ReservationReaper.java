package com.chargesquare.station.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically frees connectors whose reservation hold has expired, so a stale RESERVED
 * connector never gets stuck. (Stretch goal: stuck-connector / expiry recovery.)
 */
@Component
public class ReservationReaper {

    private final ConnectorService connectorService;

    public ReservationReaper(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @Scheduled(fixedDelayString = "${reservation.reaper.interval-ms:30000}")
    public void sweep() {
        connectorService.releaseExpiredReservations();
    }
}
