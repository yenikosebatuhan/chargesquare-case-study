package com.chargesquare.session.error;

import org.springframework.http.HttpStatus;

/** Bad or missing credentials at the login endpoint. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message);
    }
}
