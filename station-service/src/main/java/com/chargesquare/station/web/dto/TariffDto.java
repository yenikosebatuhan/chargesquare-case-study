package com.chargesquare.station.web.dto;

import com.chargesquare.station.domain.Tariff;

import java.math.BigDecimal;

public record TariffDto(Long tariffId, BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
    public static TariffDto from(Tariff t) {
        return new TariffDto(t.getId(), t.getPricePerKwh(), t.getStartFee(), t.getCurrency());
    }
}
