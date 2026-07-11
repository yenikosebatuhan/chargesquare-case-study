package com.chargesquare.station.web;

import com.chargesquare.station.service.ConnectorService;
import com.chargesquare.station.web.dto.ConnectorResponse;
import com.chargesquare.station.web.dto.ConnectorStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/connectors")
public class ConnectorController {

    private final ConnectorService service;

    public ConnectorController(ConnectorService service) {
        this.service = service;
    }

    /** Read a single connector's status + tariff. Session Service calls this at start. */
    @GetMapping("/{id}")
    public ConnectorResponse getConnector(@PathVariable Long id) {
        return ConnectorResponse.from(service.getConnector(id));
    }

    /** Internal: flip AVAILABLE -> OCCUPIED. Called only by Session Service (ADMIN when secured). */
    @PostMapping("/{id}/occupy")
    public ConnectorStatusResponse occupy(@PathVariable Long id) {
        return ConnectorStatusResponse.from(service.occupy(id));
    }

    /** Internal: mirror of occupy — sets status back to AVAILABLE. */
    @PostMapping("/{id}/release")
    public ConnectorStatusResponse release(@PathVariable Long id) {
        return ConnectorStatusResponse.from(service.release(id));
    }
}
