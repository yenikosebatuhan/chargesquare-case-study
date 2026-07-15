package com.chargesquare.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Maps a client Idempotency-Key to the session it stopped, so a replay returns the same receipt. */
@Entity
@Table(name = "stop_idempotency")
public class StopIdempotency {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StopIdempotency() {
    }

    public StopIdempotency(String idempotencyKey, Long sessionId, Instant createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.sessionId = sessionId;
        this.createdAt = createdAt;
    }

    public Long getSessionId() {
        return sessionId;
    }
}
