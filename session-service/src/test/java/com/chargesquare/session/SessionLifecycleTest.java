package com.chargesquare.session;

import com.chargesquare.session.station.ConnectorView;
import com.chargesquare.session.station.StationClient;
import com.chargesquare.session.wallet.WalletClient;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives start -> stop end to end with Station and Wallet mocked, asserting cost, the settled
 * balance, the state guards, and the client Idempotency-Key replay.
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

    @MockBean
    WalletClient walletClient;

    private ConnectorView availableConnector() {
        return new ConnectorView(10L, 1L, "CCS2-DC", 60, "AVAILABLE", null, null,
                new ConnectorView.TariffView(5L, new BigDecimal("8.50"), new BigDecimal("2.00"), "TRY"));
    }

    private long startSession() throws Exception {
        when(stationClient.getConnector(10L)).thenReturn(availableConnector());
        String body = mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("sessionId").asLong();
    }

    @Test
    void startThenStop_pricesEnergyAndSettlesWallet() throws Exception {
        long id = startSession();
        verify(stationClient).occupy(10L, 7L);

        // 12.5 kWh * 8.50 + 2.00 = 108.25; wallet returns 391.75
        when(walletClient.debit(eq(7L), eq(new BigDecimal("108.25")), eq("session-" + id)))
                .thenReturn(new WalletClient.DebitResult(7L, new BigDecimal("391.75"), "TRY", false));

        mvc.perform(post("/sessions/" + id + "/stop")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"energyKwh\":12.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.cost").value(108.25))
                .andExpect(jsonPath("$.walletBalanceAfter").value(391.75));
        verify(stationClient).release(10L);

        mvc.perform(get("/sessions/" + id))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.walletBalanceAfter").value(391.75));
    }

    @Test
    void repeatedStopWithSameIdempotencyKey_replaysReceipt_andSettlesOnce() throws Exception {
        long id = startSession();
        when(walletClient.debit(eq(7L), any(), eq("session-" + id)))
                .thenReturn(new WalletClient.DebitResult(7L, new BigDecimal("391.75"), "TRY", false));

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/sessions/" + id + "/stop")
                            .header("Idempotency-Key", "abc-123")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"energyKwh\":12.5}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cost").value(108.25));
        }
        // Settlement happened exactly once despite two identical stop calls.
        verify(walletClient, times(1)).debit(eq(7L), any(), eq("session-" + id));
    }

    @Test
    void stoppingTwiceWithoutKey_isRejectedWith409() throws Exception {
        long id = startSession();
        when(walletClient.debit(anyLong(), any(), any()))
                .thenReturn(new WalletClient.DebitResult(7L, new BigDecimal("390.00"), "TRY", false));

        mvc.perform(post("/sessions/" + id + "/stop")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"energyKwh\":10}"))
                .andExpect(status().isOk());
        mvc.perform(post("/sessions/" + id + "/stop")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"energyKwh\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SESSION_NOT_ACTIVE"));
    }

    @Test
    void startingOnAnOccupiedConnector_isRejectedWith409_andCreatesNoSession() throws Exception {
        ConnectorView occupied = new ConnectorView(10L, 1L, "CCS2-DC", 60, "OCCUPIED", null, null,
                new ConnectorView.TariffView(5L, new BigDecimal("8.50"), new BigDecimal("2.00"), "TRY"));
        when(stationClient.getConnector(10L)).thenReturn(occupied);

        mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONNECTOR_OCCUPIED"));

        Mockito.verify(stationClient, Mockito.never()).occupy(anyLong(), anyLong());
        mvc.perform(get("/users/7/sessions")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void missingUserId_isRejectedWith400() throws Exception {
        mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"connectorId\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
