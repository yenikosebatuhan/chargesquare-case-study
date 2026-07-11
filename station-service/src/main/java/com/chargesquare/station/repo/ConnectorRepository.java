package com.chargesquare.station.repo;

import com.chargesquare.station.domain.Connector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConnectorRepository extends JpaRepository<Connector, Long> {
    List<Connector> findByStationIdOrderById(Long stationId);
}
