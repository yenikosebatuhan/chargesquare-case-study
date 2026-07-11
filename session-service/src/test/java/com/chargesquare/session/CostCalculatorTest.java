package com.chargesquare.session;

import com.chargesquare.session.domain.CostCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** The worked example from the brief plus a rounding case. */
class CostCalculatorTest {

    @Test
    void workedExample_12_5kWh_at_8_50_plus_2_00_startFee_is_108_25() {
        BigDecimal cost = CostCalculator.cost(
                new BigDecimal("12.5"), new BigDecimal("8.50"), new BigDecimal("2.00"));
        assertThat(cost).isEqualByComparingTo("108.25");
    }

    @Test
    void roundsToTwoDecimalsHalfUp() {
        // 3.333 * 1.00 + 0 = 3.333 -> 3.33
        assertThat(CostCalculator.cost(new BigDecimal("3.333"), BigDecimal.ONE, BigDecimal.ZERO))
                .isEqualByComparingTo("3.33");
        // 1.005 * 1.00 + 0 = 1.005 -> 1.01 (HALF_UP)
        assertThat(CostCalculator.cost(new BigDecimal("1.005"), BigDecimal.ONE, BigDecimal.ZERO))
                .isEqualByComparingTo("1.01");
    }

    @Test
    void zeroEnergyStillChargesTheStartFee() {
        assertThat(CostCalculator.cost(BigDecimal.ZERO, new BigDecimal("8.50"), new BigDecimal("2.00")))
                .isEqualByComparingTo("2.00");
    }
}
