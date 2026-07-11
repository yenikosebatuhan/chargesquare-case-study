package com.chargesquare.session.error;

import org.springframework.http.HttpStatus;

/**
 * A required downstream dependency (Station Service) could not be reached or errored.
 * We fail fast with 502 rather than retry or fall back — see DESIGN.md.
 */
public class UpstreamException extends ApiException {
    public UpstreamException(String message) {
        super(HttpStatus.BAD_GATEWAY, "DEPENDENCY_UNAVAILABLE", message);
    }
}
