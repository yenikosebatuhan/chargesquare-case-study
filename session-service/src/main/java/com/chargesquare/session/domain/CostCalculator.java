package com.chargesquare.session.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The one piece of money maths in the system: cost = energy * pricePerKwh + startFee,
 * rounded to 2 decimals. All BigDecimal — never a float — with an explicit rounding mode.
 */
public final class CostCalculator {

    private CostCalculator() {
    }

    public static BigDecimal cost(BigDecimal energyKwh, BigDecimal pricePerKwh, BigDecimal startFee) {
        BigDecimal energyCost = energyKwh.multiply(pricePerKwh);
        return energyCost.add(startFee).setScale(2, RoundingMode.HALF_UP);
    }
}
