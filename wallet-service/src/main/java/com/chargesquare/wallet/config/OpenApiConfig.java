package com.chargesquare.wallet.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "ChargeSquare Wallet Service",
        version = "1.0.0",
        description = "Per-user balances with an idempotent debit at session stop."))
public class OpenApiConfig {
}
