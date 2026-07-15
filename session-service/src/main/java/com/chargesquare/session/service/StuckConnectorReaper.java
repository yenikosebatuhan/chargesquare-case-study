package com.chargesquare.session.service;

import com.chargesquare.session.station.ConnectorView;
import com.chargesquare.session.station.StationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Recovers stuck connectors (the partial-failure case from DESIGN.md): a connector left OCCUPIED
 * with no ACTIVE session behind it — e.g. occupy succeeded but session creation failed. Periodically
 * reconciles Station's OCCUPIED connectors against Session's ACTIVE sessions and releases orphans.
 */
@Component
public class StuckConnectorReaper {

    private static final Logger log = LoggerFactory.getLogger(StuckConnectorReaper.class);

    private final StationClient station;
    private final SessionService sessions;
    private final List<Long> stationIds;

    public StuckConnectorReaper(StationClient station, SessionService sessions,
                                @Value("${reconcile.station-ids:1}") List<Long> stationIds) {
        this.station = station;
        this.sessions = sessions;
        this.stationIds = stationIds;
    }

    @Scheduled(fixedDelayString = "${reconcile.interval-ms:60000}", initialDelayString = "${reconcile.initial-delay-ms:60000}")
    public void reconcile() {
        for (Long stationId : stationIds) {
            try {
                sweepStation(stationId);
            } catch (Exception ex) {
                // Best-effort: a reaper must never crash the app. Log and try again next tick.
                log.warn("reconcile sweep for station {} skipped: {}", stationId, ex.getMessage());
            }
        }
    }

    private void sweepStation(Long stationId) {
        for (ConnectorView c : station.listConnectors(stationId)) {
            if ("OCCUPIED".equals(c.status()) && !sessions.hasActiveSessionOnConnector(c.connectorId())) {
                log.warn("stuck connector {} is OCCUPIED with no ACTIVE session — releasing", c.connectorId());
                station.release(c.connectorId());
            }
        }
    }
}
