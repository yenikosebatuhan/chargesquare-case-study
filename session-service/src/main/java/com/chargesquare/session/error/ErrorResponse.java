package com.chargesquare.session.error;

/** The small, consistent JSON error body used across every endpoint. */
public record ErrorResponse(String error, String message) {
}
