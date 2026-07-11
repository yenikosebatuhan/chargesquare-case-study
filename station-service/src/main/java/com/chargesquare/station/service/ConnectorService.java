package com.chargesquare.station.service;

import com.chargesquare.station.domain.Connector;
import com.chargesquare.station.error.ConflictException;
import com.chargesquare.station.error.NotFoundException;
import com.chargesquare.station.repo.ConnectorRepository;
import com.chargesquare.station.repo.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reads plus the single occupy/release status update — deliberately simple. */
@Service
public class ConnectorService {

    private static final Logger log = LoggerFactory.getLogger(ConnectorService.class);

    private final ConnectorRepository connectors;
    private final StationRepository stations;

    public ConnectorService(ConnectorRepository connectors, StationRepository stations) {
        this.connectors = connectors;
        this.stations = stations;
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

    @Transactional
    public Connector occupy(Long id) {
        Connector connector = getConnector(id);
        if (!connector.isAvailable()) {
            throw new ConflictException(
                    "CONNECTOR_OCCUPIED", "Connector " + id + " is already OCCUPIED");
        }
        connector.occupy();
        log.info("connector {} occupied", id);
        return connector;
    }

    @Transactional
    public Connector release(Long id) {
        Connector connector = getConnector(id);
        connector.release();
        log.info("connector {} released", id);
        return connector;
    }
}
