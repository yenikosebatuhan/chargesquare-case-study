package com.chargesquare.station.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling   // enables the reservation-expiry reaper
public class StationConfig {

    /** A single injectable UTC clock so pricing and expiry logic are deterministic and testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
