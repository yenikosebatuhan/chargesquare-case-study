package com.chargesquare.session;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test: the real Session Service (web + JPA + Flyway) against a real
 * Postgres (Testcontainers) and WireMock stand-ins for Station and Wallet. Exercises the full
 * start -> stop path over real HTTP, and proves the Postgres migrations + entity mapping are valid.
 * Skips automatically if Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)   // skips cleanly when no Docker (e.g. some laptops); runs in CI
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StartStopIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static WireMockServer downstream;

    @BeforeAll
    static void startWireMock() {
        downstream = new WireMockServer(options().dynamicPort());
        downstream.start();

        // Station: connector 10 is AVAILABLE with the DC tariff; occupy/release succeed.
        downstream.stubFor(get(urlEqualTo("/connectors/10")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"connectorId":10,"stationId":1,"type":"CCS2-DC","powerKw":60,"status":"AVAILABLE",
                         "tariff":{"tariffId":5,"pricePerKwh":8.50,"startFee":2.00,"currency":"TRY"}}""")));
        downstream.stubFor(post(urlEqualTo("/connectors/10/occupy")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"connectorId\":10,\"status\":\"OCCUPIED\"}")));
        downstream.stubFor(post(urlEqualTo("/connectors/10/release")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"connectorId\":10,\"status\":\"AVAILABLE\"}")));
        // Wallet: debit returns the settled balance.
        downstream.stubFor(post(urlEqualTo("/wallets/7/debit")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"userId\":7,\"balance\":391.75,\"currency\":\"TRY\",\"replayed\":false}")));
    }

    @AfterAll
    static void stopWireMock() {
        if (downstream != null) {
            downstream.stop();
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("security.enabled", () -> "false");
        registry.add("station.service.url", () -> downstream.baseUrl());
        registry.add("wallet.service.url", () -> downstream.baseUrl());
        // Push the reconciliation reaper far out so it never runs during the test.
        registry.add("reconcile.initial-delay-ms", () -> "3600000");
    }

    @Autowired
    TestRestTemplate rest;

    @Test
    void startThenStop_overRealHttpAndPostgres() throws Exception {
        // START
        ResponseEntity<String> start = postJson("/sessions", "{\"userId\":7,\"connectorId\":10}", null);
        assertThat(start.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode startBody = new com.fasterxml.jackson.databind.ObjectMapper().readTree(start.getBody());
        assertThat(startBody.get("status").asText()).isEqualTo("ACTIVE");
        long sessionId = startBody.get("sessionId").asLong();

        // STOP
        ResponseEntity<String> stop = postJson("/sessions/" + sessionId + "/stop", "{\"energyKwh\":12.5}", "key-1");
        assertThat(stop.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode stopBody = new com.fasterxml.jackson.databind.ObjectMapper().readTree(stop.getBody());
        assertThat(stopBody.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(stopBody.get("cost").decimalValue()).isEqualByComparingTo("108.25");
        assertThat(stopBody.get("walletBalanceAfter").decimalValue()).isEqualByComparingTo("391.75");

        // The downstreams were actually called over HTTP.
        downstream.verify(postRequestedFor(urlEqualTo("/connectors/10/occupy")));
        downstream.verify(postRequestedFor(urlEqualTo("/wallets/7/debit")));
        downstream.verify(postRequestedFor(urlEqualTo("/connectors/10/release")));

        // Idempotent replay: same key -> same receipt, no second debit.
        ResponseEntity<String> replay = postJson("/sessions/" + sessionId + "/stop", "{\"energyKwh\":12.5}", "key-1");
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        downstream.verify(1, postRequestedFor(urlEqualTo("/wallets/7/debit")));
    }

    private ResponseEntity<String> postJson(String path, String body, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.add("Idempotency-Key", idempotencyKey);
        }
        return rest.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    }
}
