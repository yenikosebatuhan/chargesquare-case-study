package com.chargesquare.station;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the connector reads and the guarded occupy/release lifecycle. */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "classpath:station-test-seed.sql")
class ConnectorEndpointsTest {

    @Autowired
    MockMvc mvc;

    @Test
    void getConnector_returnsStatusAndTariff() throws Exception {
        mvc.perform(get("/connectors/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.tariff.pricePerKwh").value(8.50))
                .andExpect(jsonPath("$.tariff.startFee").value(2.00));
    }

    @Test
    void getUnknownConnector_returns404WithErrorBody() throws Exception {
        mvc.perform(get("/connectors/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CONNECTOR_NOT_FOUND"));
    }

    @Test
    void occupyThenRelease_flipsStatusBackToAvailable() throws Exception {
        mvc.perform(post("/connectors/10/occupy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCUPIED"));

        mvc.perform(post("/connectors/10/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void occupyingAnOccupiedConnector_isRejectedWith409() throws Exception {
        mvc.perform(post("/connectors/10/occupy")).andExpect(status().isOk());

        mvc.perform(post("/connectors/10/occupy"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONNECTOR_OCCUPIED"));
    }
}
