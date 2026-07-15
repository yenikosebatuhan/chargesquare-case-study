package com.chargesquare.station;

import com.chargesquare.station.pricing.PeakSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** Time-of-use window logic, including the midnight-wrap case. */
class PeakScheduleTest {

    private final ZoneId ist = ZoneId.of("Europe/Istanbul");

    private Instant at(int hour) {
        return LocalDateTime.of(2026, 7, 9, hour, 0).atZone(ist).toInstant();
    }

    @Test
    void insideWindowIsPeak_outsideIsOffPeak() {
        PeakSchedule s = new PeakSchedule(17, 22, "Europe/Istanbul");
        assertThat(s.isPeak(at(18))).isTrue();   // inside
        assertThat(s.isPeak(at(17))).isTrue();   // start inclusive
        assertThat(s.isPeak(at(22))).isFalse();  // end exclusive
        assertThat(s.isPeak(at(9))).isFalse();   // morning off-peak
    }

    @Test
    void windowWrappingMidnightIsHandled() {
        PeakSchedule s = new PeakSchedule(22, 6, "Europe/Istanbul");
        assertThat(s.isPeak(at(23))).isTrue();
        assertThat(s.isPeak(at(3))).isTrue();
        assertThat(s.isPeak(at(12))).isFalse();
    }
}
