package com.chargesquare.session.web;

import com.chargesquare.session.service.SessionService;
import com.chargesquare.session.web.dto.SessionResponse;
import com.chargesquare.session.web.dto.StartSessionRequest;
import com.chargesquare.session.web.dto.StopSessionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    /** START: create an ACTIVE session on an available (or self-reserved) connector. */
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse start(@Valid @RequestBody StartSessionRequest request) {
        return SessionResponse.started(service.start(request.userId(), request.connectorId()));
    }

    /**
     * STOP + BILL + SETTLE. An optional {@code Idempotency-Key} header makes a repeated stop a
     * no-op that replays the original receipt instead of erroring or double-charging.
     */
    @PostMapping("/sessions/{id}/stop")
    public SessionResponse stop(@PathVariable Long id,
                                @Valid @RequestBody StopSessionRequest request,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return SessionResponse.detail(service.stop(id, request.energyKwh(), idempotencyKey));
    }

    @GetMapping("/sessions/{id}")
    public SessionResponse getSession(@PathVariable Long id) {
        return SessionResponse.detail(service.getSession(id));
    }

    @GetMapping("/users/{userId}/sessions")
    public List<SessionResponse> userSessions(@PathVariable Long userId) {
        return service.listUserSessions(userId).stream().map(SessionResponse::detail).toList();
    }
}
