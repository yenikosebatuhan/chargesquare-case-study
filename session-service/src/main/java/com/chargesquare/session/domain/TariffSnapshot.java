package com.chargesquare.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * The tariff as it stood when the session started. Snapshotting (rather than re-looking-up
 * at stop) means a mid-session price change never alters what this driver is charged.
 */
@Embeddable
public class TariffSnapshot {

    @Column(name = "snapshot_price_per_kwh", nullable = false)
    private BigDecimal pricePerKwh;

    @Column(name = "snapshot_start_fee", nullable = false)
    private BigDecimal startFee;

    @Column(name = "snapshot_currency", nullable = false, length = 3)
    private String currency;

    protected TariffSnapshot() {
    }

    public TariffSnapshot(BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
        this.pricePerKwh = pricePerKwh;
        this.startFee = startFee;
        this.currency = currency;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public BigDecimal getStartFee() {
        return startFee;
    }

    public String getCurrency() {
        return currency;
    }
}
