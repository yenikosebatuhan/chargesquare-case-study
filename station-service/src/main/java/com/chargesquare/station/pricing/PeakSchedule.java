package com.chargesquare.station.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Time-of-use pricing window (stretch goal). Peak hours are config-driven; during the window a
 * tariff's peak price applies, otherwise its base price. The window may wrap past midnight.
 */
@Component
public class PeakSchedule {

    private final int startHour;
    private final int endHour;
    private final ZoneId zone;

    public PeakSchedule(@Value("${pricing.peak.start-hour:17}") int startHour,
                        @Value("${pricing.peak.end-hour:22}") int endHour,
                        @Value("${pricing.peak.zone:Europe/Istanbul}") String zone) {
        this.startHour = startHour;
        this.endHour = endHour;
        this.zone = ZoneId.of(zone);
    }

    public boolean isPeak(Instant when) {
        int hour = when.atZone(zone).getHour();
        if (startHour <= endHour) {
            return hour >= startHour && hour < endHour;
        }
        // window wraps midnight, e.g. 22..6
        return hour >= startHour || hour < endHour;
    }

    public boolean isPeakNow(Clock clock) {
        return isPeak(Instant.now(clock));
    }
}
