package com.chargesquare.wallet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WalletConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
