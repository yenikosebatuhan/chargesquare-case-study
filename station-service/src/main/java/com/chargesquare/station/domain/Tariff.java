package com.chargesquare.station.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "tariffs")
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Standard / off-peak price. Money is BigDecimal (NUMERIC in the DB) — never a float. */
    @Column(name = "price_per_kwh", nullable = false)
    private BigDecimal pricePerKwh;

    /**
     * Optional time-of-use peak price (stretch goal). When set, it applies during the
     * configured peak window; otherwise {@link #pricePerKwh} applies at all times.
     */
    @Column(name = "price_per_kwh_peak")
    private BigDecimal pricePerKwhPeak;

    @Column(name = "start_fee", nullable = false)
    private BigDecimal startFee;

    @Column(nullable = false, length = 3)
    private String currency;

    protected Tariff() {
    }

    /** The price that applies right now: the peak rate during peak hours, otherwise the base rate. */
    public BigDecimal effectivePrice(boolean peakNow) {
        return (peakNow && pricePerKwhPeak != null) ? pricePerKwhPeak : pricePerKwh;
    }

    public boolean hasPeakPricing() {
        return pricePerKwhPeak != null;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public BigDecimal getPricePerKwhPeak() {
        return pricePerKwhPeak;
    }

    public BigDecimal getStartFee() {
        return startFee;
    }

    public String getCurrency() {
        return currency;
    }
}
