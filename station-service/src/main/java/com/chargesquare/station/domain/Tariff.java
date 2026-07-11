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

    /** Money is BigDecimal (NUMERIC in the DB) — never a float. */
    @Column(name = "price_per_kwh", nullable = false)
    private BigDecimal pricePerKwh;

    @Column(name = "start_fee", nullable = false)
    private BigDecimal startFee;

    @Column(nullable = false, length = 3)
    private String currency;

    protected Tariff() {
    }

    public Long getId() {
        return id;
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
