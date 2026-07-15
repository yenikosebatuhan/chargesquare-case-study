package com.chargesquare.session.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling   // enables the stuck-connector reconciliation reaper
public class AppConfig {

    /** A single injectable clock (UTC) so session timestamps are deterministic and testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
