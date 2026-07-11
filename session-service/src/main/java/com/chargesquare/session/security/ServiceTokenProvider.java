package com.chargesquare.session.security;

import com.chargesquare.session.domain.Role;
import org.springframework.stereotype.Component;

/**
 * Mints the short-lived ADMIN token Session Service presents on the internal occupy/release
 * calls, so those service-to-service requests satisfy Station Service's ADMIN requirement.
 */
@Component
public class ServiceTokenProvider {

    private final JwtService jwtService;

    public ServiceTokenProvider(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String bearerToken() {
        return jwtService.issue("service:session-service", Role.ADMIN.name());
    }
}
