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

    @ManyToOne(optional = false)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    protected Connector() {
    }

    /** Flip to OCCUPIED, rejecting the call if it is already taken. */
    public void occupy() {
        if (status == ConnectorStatus.OCCUPIED) {
            throw new IllegalStateException("already OCCUPIED");
        }
        this.status = ConnectorStatus.OCCUPIED;
    }

    /** Return the connector to AVAILABLE (idempotent by design — releasing a free connector is a no-op). */
    public void release() {
        this.status = ConnectorStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == ConnectorStatus.AVAILABLE;
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

    public Tariff getTariff() {
        return tariff;
    }
}
