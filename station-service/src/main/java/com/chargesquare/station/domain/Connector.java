package com.chargesquare.station.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "connectors")
public class Connector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(nullable = false)
    private String type;

    @Column(name = "power_kw", nullable = false)
    private int powerKw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectorStatus status;

    /** Reservation hold (stretch goal): who holds it and until when. Null unless RESERVED. */
    @Column(name = "reserved_by")
    private Long reservedBy;

    @Column(name = "reserved_until")
    private Instant reservedUntil;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    protected Connector() {
    }

    /**
     * Place a short-lived reservation on an AVAILABLE connector for one user.
     * Throws if the connector is not free.
     */
    public void reserve(Long userId, Instant until) {
        if (status != ConnectorStatus.AVAILABLE) {
            throw new IllegalStateException("not AVAILABLE");
        }
        this.status = ConnectorStatus.RESERVED;
        this.reservedBy = userId;
        this.reservedUntil = until;
    }

    /**
     * Flip to OCCUPIED. A RESERVED connector may only be occupied by the user who holds a
     * live reservation; an expired or foreign reservation is rejected.
     *
     * @param userId the user starting the session (may be null for a plain occupy)
     * @param now    current time, to check reservation expiry
     */
    public void occupy(Long userId, Instant now) {
        switch (status) {
            case AVAILABLE -> { /* ok */ }
            case RESERVED -> {
                boolean expired = reservedUntil != null && reservedUntil.isBefore(now);
                boolean mine = userId != null && userId.equals(reservedBy);
                if (expired || !mine) {
                    throw new IllegalStateException("reserved by another user or expired");
                }
            }
            case OCCUPIED -> throw new IllegalStateException("already OCCUPIED");
        }
        this.status = ConnectorStatus.OCCUPIED;
        clearReservation();
    }

    /** Return the connector to AVAILABLE and drop any reservation. */
    public void release() {
        this.status = ConnectorStatus.AVAILABLE;
        clearReservation();
    }

    /** Expire a reservation back to AVAILABLE (used by the reaper). */
    public void expireReservation() {
        if (status == ConnectorStatus.RESERVED) {
            release();
        }
    }

    private void clearReservation() {
        this.reservedBy = null;
        this.reservedUntil = null;
    }

    public boolean isAvailable() {
        return status == ConnectorStatus.AVAILABLE;
    }

    public boolean isReservationExpired(Instant now) {
        return status == ConnectorStatus.RESERVED && reservedUntil != null && reservedUntil.isBefore(now);
    }

    public Long getId() {
        return id;
    }

    public Long getStationId() {
        return stationId;
    }

    public String getType() {
        return type;
    }

    public int getPowerKw() {
        return powerKw;
    }

    public ConnectorStatus getStatus() {
        return status;
    }

    public Long getReservedBy() {
        return reservedBy;
    }

    public Instant getReservedUntil() {
        return reservedUntil;
    }

    public Tariff getTariff() {
        return tariff;
    }
}
