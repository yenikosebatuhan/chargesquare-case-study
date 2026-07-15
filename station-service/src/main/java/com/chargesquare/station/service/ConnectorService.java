package com.chargesquare.station.service;

import com.chargesquare.station.domain.Connector;
import com.chargesquare.station.domain.ConnectorStatus;
import com.chargesquare.station.error.ConflictException;
import com.chargesquare.station.error.NotFoundException;
import com.chargesquare.station.pricing.PeakSchedule;
import com.chargesquare.station.repo.ConnectorRepository;
import com.chargesquare.station.repo.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Reads, occupy/release, plus the reservation hold and time-of-use pricing helpers. */
@Service
public class ConnectorService {

    private static final Logger log = LoggerFactory.getLogger(ConnectorService.class);

    private final ConnectorRepository connectors;
    private final StationRepository stations;
    private final PeakSchedule peakSchedule;
    private final Clock clock;

    public ConnectorService(ConnectorRepository connectors, StationRepository stations,
                            PeakSchedule peakSchedule, Clock clock) {
        this.connectors = connectors;
        this.stations = stations;
        this.peakSchedule = peakSchedule;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Connector getConnector(Long id) {
        return connectors.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "CONNECTOR_NOT_FOUND", "Connector " + id + " does not exist"));
    }

    @Transactional(readOnly = true)
    public List<Connector> listStationConnectors(Long stationId) {
        if (!stations.existsById(stationId)) {
            throw new NotFoundException("STATION_NOT_FOUND", "Station " + stationId + " does not exist");
        }
        return connectors.findByStationIdOrderById(stationId);
    }

    /** True if the time-of-use peak window is active right now. */
    public boolean isPeakNow() {
        return peakSchedule.isPeakNow(clock);
    }

    /**
     * Occupy a connector. AVAILABLE connectors occupy freely; a RESERVED connector may only be
     * occupied by the user holding a live reservation.
     */
    @Transactional
    public Connector occupy(Long id, Long userId) {
        Connector connector = getConnector(id);
        Instant now = Instant.now(clock);

        if (connector.getStatus() == ConnectorStatus.OCCUPIED) {
            throw new ConflictException("CONNECTOR_OCCUPIED", "Connector " + id + " is already OCCUPIED");
        }
        if (connector.getStatus() == ConnectorStatus.RESERVED) {
            boolean mine = userId != null && userId.equals(connector.getReservedBy());
            if (!mine || connector.isReservationExpired(now)) {
                throw new ConflictException("CONNECTOR_RESERVED",
                        "Connector " + id + " is reserved by another user");
            }
        }
        connector.occupy(userId, now);
        log.info("connector {} occupied (user {})", id, userId);
        return connector;
    }

    @Transactional
    public Connector release(Long id) {
        Connector connector = getConnector(id);
        connector.release();
        log.info("connector {} released", id);
        return connector;
    }

    /** Place a short-lived reservation on an AVAILABLE connector (stretch goal). */
    @Transactional
    public Connector reserve(Long id, Long userId, Duration ttl) {
        Connector connector = getConnector(id);
        if (connector.getStatus() == ConnectorStatus.OCCUPIED) {
            throw new ConflictException("CONNECTOR_OCCUPIED", "Connector " + id + " is OCCUPIED");
        }
        if (connector.getStatus() == ConnectorStatus.RESERVED) {
            throw new ConflictException("CONNECTOR_RESERVED", "Connector " + id + " is already RESERVED");
        }
        Instant until = Instant.now(clock).plus(ttl);
        connector.reserve(userId, until);
        log.info("connector {} reserved by user {} until {}", id, userId, until);
        return connector;
    }

    @Transactional
    public Connector cancelReservation(Long id) {
        Connector connector = getConnector(id);
        if (connector.getStatus() != ConnectorStatus.RESERVED) {
            throw new ConflictException("CONNECTOR_NOT_RESERVED", "Connector " + id + " is not RESERVED");
        }
        connector.release();
        log.info("connector {} reservation cancelled", id);
        return connector;
    }

    /** Reaper: release reservations whose hold has expired. Returns how many were freed. */
    @Transactional
    public int releaseExpiredReservations() {
        List<Connector> expired = connectors.findByStatusAndReservedUntilBefore(
                ConnectorStatus.RESERVED, Instant.now(clock));
        expired.forEach(Connector::expireReservation);
        if (!expired.isEmpty()) {
            log.info("reaper released {} expired reservation(s): {}",
                    expired.size(), expired.stream().map(Connector::getId).toList());
        }
        return expired.size();
    }
}
