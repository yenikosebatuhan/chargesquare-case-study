package com.chargesquare.session.web;

import com.chargesquare.session.service.SessionService;
import com.chargesquare.session.web.dto.ReservationResponse;
import com.chargesquare.session.web.dto.ReserveRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reserve a connector before starting (stretch goal). ADMIN-gated when secured. */
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final SessionService service;

    public ReservationController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Valid @RequestBody ReserveRequest request) {
        return ReservationResponse.from(
                service.reserve(request.userId(), request.connectorId(), request.ttlSeconds()));
    }
}
