package com.chargesquare.session.web;

import com.chargesquare.session.service.SessionService;
import com.chargesquare.session.service.SessionService.SessionWithBalance;
import com.chargesquare.session.service.SessionService.StopResult;
import com.chargesquare.session.web.dto.SessionResponse;
import com.chargesquare.session.web.dto.StartSessionRequest;
import com.chargesquare.session.web.dto.StopSessionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    /** START: create an ACTIVE session on an AVAILABLE connector. */
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse start(@Valid @RequestBody StartSessionRequest request) {
        return SessionResponse.started(service.start(request.userId(), request.connectorId()));
    }

    /** STOP + BILL + SETTLE: price the energy, settle the wallet, free the connector. */
    @PostMapping("/sessions/{id}/stop")
    public SessionResponse stop(@PathVariable Long id, @Valid @RequestBody StopSessionRequest request) {
        StopResult result = service.stop(id, request.energyKwh());
        return SessionResponse.detail(result.session(), result.walletBalanceAfter());
    }

    @GetMapping("/sessions/{id}")
    public SessionResponse getSession(@PathVariable Long id) {
        SessionWithBalance r = service.getSession(id);
        return SessionResponse.detail(r.session(), r.walletBalance());
    }

    @GetMapping("/users/{userId}/sessions")
    public List<SessionResponse> userSessions(@PathVariable Long userId) {
        return service.listUserSessions(userId).stream()
                .map(r -> SessionResponse.detail(r.session(), r.walletBalance()))
                .toList();
    }
}
