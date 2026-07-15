package com.chargesquare.station.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "ChargeSquare Station Service",
        version = "1.0.0",
        description = "Stations, connectors and tariffs — reads, occupy/release, and reservations."))
public class OpenApiConfig {
}
