package com.chargesquare.station.web.dto;

import com.chargesquare.station.domain.Tariff;

import java.math.BigDecimal;

/**
 * Tariff view. {@code pricePerKwh} is the <em>effective</em> price right now (peak or off-peak),
 * which is what a session snapshots at start; the base/peak rates are exposed for transparency.
 */
public record TariffDto(
        Long tariffId,
        BigDecimal pricePerKwh,
        BigDecimal basePricePerKwh,
        BigDecimal peakPricePerKwh,
        Boolean peakNow,
        BigDecimal startFee,
        String currency) {

    public static TariffDto from(Tariff t, boolean peakNow) {
        return new TariffDto(
                t.getId(),
                t.effectivePrice(peakNow),
                t.getPricePerKwh(),
                t.getPricePerKwhPeak(),
                t.hasPeakPricing() ? peakNow : null,
                t.getStartFee(),
                t.getCurrency());
    }
}
