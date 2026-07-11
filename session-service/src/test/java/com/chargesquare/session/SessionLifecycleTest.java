package com.chargesquare.session;

import com.chargesquare.session.station.ConnectorView;
import com.chargesquare.session.station.StationClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the start -> stop lifecycle end to end (Station Service mocked), asserting the
 * cost, the wallet settlement, and the state guards (occupied-start, stop-twice).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "classpath:session-test-seed.sql")
class SessionLifecycleTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockBean
    StationClient stationClient;

    private ConnectorView availableConnector() {
        return new ConnectorView(10L, 1L, "CCS2-DC", 60, "AVAILABLE",
                new ConnectorView.TariffView(5L, new BigDecimal("8.50"), new BigDecimal("2.00"), "TRY"));
    }

    @Test
    void startThenStop_pricesEnergyAndSettlesWallet() throws Exception {
        when(stationClient.getConnector(10L)).thenReturn(availableConnector());

        // START
        String startBody = mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.tariffSnapshot.pricePerKwh").value(8.50))
                .andReturn().getResponse().getContentAsString();
        long sessionId = json.readTree(startBody).get("sessionId").asLong();
        verify(stationClient).occupy(10L);

        // STOP: 12.5 kWh * 8.50 + 2.00 = 108.25; wallet 500.00 -> 391.75
        mvc.perform(post("/sessions/" + sessionId + "/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyKwh\":12.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.cost").value(108.25))
                .andExpect(jsonPath("$.walletBalanceAfter").value(391.75));
        verify(stationClient).release(10L);

        // GET reflects the completed state
        mvc.perform(get("/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.cost").value(108.25));
    }

    @Test
    void stoppingTwice_isRejectedWith409_andDoesNotDoubleCharge() throws Exception {
        when(stationClient.getConnector(10L)).thenReturn(availableConnector());

        String startBody = mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sessionId = json.readTree(startBody).get("sessionId").asLong();

        JsonNode firstStop = json.readTree(mvc.perform(post("/sessions/" + sessionId + "/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyKwh\":10}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        double balanceAfterFirst = firstStop.get("walletBalanceAfter").asDouble();

        // Second stop -> 409, no state change
        mvc.perform(post("/sessions/" + sessionId + "/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyKwh\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SESSION_NOT_ACTIVE"));

        // Balance unchanged after the rejected second stop
        mvc.perform(get("/sessions/" + sessionId))
                .andExpect(jsonPath("$.walletBalanceAfter").value(balanceAfterFirst));
    }

    @Test
    void startingOnAnOccupiedConnector_isRejectedWith409_andCreatesNoSession() throws Exception {
        ConnectorView occupied = new ConnectorView(10L, 1L, "CCS2-DC", 60, "OCCUPIED",
                new ConnectorView.TariffView(5L, new BigDecimal("8.50"), new BigDecimal("2.00"), "TRY"));
        when(stationClient.getConnector(10L)).thenReturn(occupied);

        mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONNECTOR_OCCUPIED"));

        // Never tried to occupy, and no session exists for the user
        Mockito.verify(stationClient, Mockito.never()).occupy(anyLong());
        mvc.perform(get("/users/7/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void missingUserId_isRejectedWith400() throws Exception {
        mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"connectorId\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
