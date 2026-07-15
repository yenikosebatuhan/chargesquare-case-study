package com.chargesquare.station.repo;

import com.chargesquare.station.domain.Connector;
import com.chargesquare.station.domain.ConnectorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ConnectorRepository extends JpaRepository<Connector, Long> {
    List<Connector> findByStationIdOrderById(Long stationId);

    List<Connector> findByStatusAndReservedUntilBefore(ConnectorStatus status, Instant cutoff);
}
