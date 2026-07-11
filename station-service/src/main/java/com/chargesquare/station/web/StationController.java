package com.chargesquare.station.web;

import com.chargesquare.station.service.ConnectorService;
import com.chargesquare.station.web.dto.ConnectorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stations")
public class StationController {

    private final ConnectorService service;

    public StationController(ConnectorService service) {
        this.service = service;
    }

    /** List a station's connectors with status + tariff. */
    @GetMapping("/{id}/connectors")
    public List<ConnectorResponse> listConnectors(@PathVariable Long id) {
        return service.listStationConnectors(id).stream()
                .map(ConnectorResponse::from)
                .toList();
    }
}
