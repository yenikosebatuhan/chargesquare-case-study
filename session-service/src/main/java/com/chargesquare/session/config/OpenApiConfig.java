package com.chargesquare.session.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "ChargeSquare Session Service",
        version = "1.0.0",
        description = "Charging session lifecycle, settlement orchestration, reservations and auth."))
public class OpenApiConfig {
}
