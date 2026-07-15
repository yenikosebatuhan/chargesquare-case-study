package com.chargesquare.station;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reservation lifecycle: reserve -> occupy-by-holder, and the guards around it. */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "classpath:station-test-seed.sql")
class ReservationEndpointsTest {

    @Autowired
    MockMvc mvc;

    private static String reserve(long user) {
        return "{\"userId\":" + user + "}";
    }

    private static String occupy(long user) {
        return "{\"userId\":" + user + "}";
    }

    @Test
    void reserveThenOccupyByHolder_transitionsToOccupied() throws Exception {
        mvc.perform(post("/connectors/10/reserve").contentType(MediaType.APPLICATION_JSON).content(reserve(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.reservedBy").value(7));

        mvc.perform(get("/connectors/10")).andExpect(jsonPath("$.status").value("RESERVED"));

        // The holder can occupy the reserved connector.
        mvc.perform(post("/connectors/10/occupy").contentType(MediaType.APPLICATION_JSON).content(occupy(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCUPIED"));
    }

    @Test
    void occupyReservedByAnotherUser_isRejectedWith409() throws Exception {
        mvc.perform(post("/connectors/10/reserve").contentType(MediaType.APPLICATION_JSON).content(reserve(7)))
                .andExpect(status().isOk());

        mvc.perform(post("/connectors/10/occupy").contentType(MediaType.APPLICATION_JSON).content(occupy(99)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONNECTOR_RESERVED"));
    }

    @Test
    void reservingAnAlreadyReservedConnector_isRejectedWith409() throws Exception {
        mvc.perform(post("/connectors/10/reserve").contentType(MediaType.APPLICATION_JSON).content(reserve(7)))
                .andExpect(status().isOk());
        mvc.perform(post("/connectors/10/reserve").contentType(MediaType.APPLICATION_JSON).content(reserve(8)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONNECTOR_RESERVED"));
    }

    @Test
    void cancelReservation_returnsConnectorToAvailable() throws Exception {
        mvc.perform(post("/connectors/10/reserve").contentType(MediaType.APPLICATION_JSON).content(reserve(7)))
                .andExpect(status().isOk());
        mvc.perform(post("/connectors/10/cancel-reservation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }
}
