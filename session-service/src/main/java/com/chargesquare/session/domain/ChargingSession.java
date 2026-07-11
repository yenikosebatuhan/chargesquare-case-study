package com.chargesquare.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** The charging session aggregate — owns its own start/stop lifecycle and guards. */
@Entity
@Table(name = "sessions")
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "connector_id", nullable = false)
    private Long connectorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "energy_kwh")
    private BigDecimal energyKwh;

    @Column(name = "cost")
    private BigDecimal cost;

    @Embedded
    private TariffSnapshot tariffSnapshot;

    protected ChargingSession() {
    }

    private ChargingSession(Long userId, Long connectorId, TariffSnapshot snapshot, Instant startedAt) {
        this.userId = userId;
        this.connectorId = connectorId;
        this.tariffSnapshot = snapshot;
        this.startedAt = startedAt;
        this.status = SessionStatus.ACTIVE;
    }

    /** Begin a session in ACTIVE state with the tariff snapshotted. */
    public static ChargingSession start(Long userId, Long connectorId, TariffSnapshot snapshot, Instant startedAt) {
        return new ChargingSession(userId, connectorId, snapshot, startedAt);
    }

    /**
     * Price the reported energy against the snapshot, record the receipt and mark COMPLETED.
     * Guards against stopping a session that is not ACTIVE (covers the stop-twice case).
     *
     * @return the computed cost that should be settled against the wallet
     */
    public BigDecimal complete(BigDecimal energyKwh, Instant endedAt) {
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("session " + id + " is not ACTIVE");
        }
        this.energyKwh = energyKwh;
        this.cost = CostCalculator.cost(
                energyKwh, tariffSnapshot.getPricePerKwh(), tariffSnapshot.getStartFee());
        this.endedAt = endedAt;
        this.status = SessionStatus.COMPLETED;
        return this.cost;
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getConnectorId() {
        return connectorId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public BigDecimal getEnergyKwh() {
        return energyKwh;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public TariffSnapshot getTariffSnapshot() {
        return tariffSnapshot;
    }
}
