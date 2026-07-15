package com.chargesquare.wallet;

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

/** Proves the debit is idempotent: a replay with the same key never double-charges. */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "classpath:wallet-test-seed.sql")
class WalletDebitTest {

    @Autowired
    MockMvc mvc;

    private static String debit(String amount, String key) {
        return "{\"amount\":" + amount + ",\"idempotencyKey\":\"" + key + "\"}";
    }

    @Test
    void debit_thenReplayWithSameKey_doesNotDoubleCharge() throws Exception {
        // First debit: 500.00 - 108.25 = 391.75
        mvc.perform(post("/wallets/7/debit").contentType(MediaType.APPLICATION_JSON).content(debit("108.25", "session-100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(391.75))
                .andExpect(jsonPath("$.replayed").value(false));

        // Replay with the SAME key: balance unchanged, flagged as replayed
        mvc.perform(post("/wallets/7/debit").contentType(MediaType.APPLICATION_JSON).content(debit("108.25", "session-100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(391.75))
                .andExpect(jsonPath("$.replayed").value(true));

        // Confirm via read
        mvc.perform(get("/wallets/7")).andExpect(jsonPath("$.balance").value(391.75));
    }

    @Test
    void differentKeys_debitIndependently_andBalanceMayGoNegative() throws Exception {
        mvc.perform(post("/wallets/7/debit").contentType(MediaType.APPLICATION_JSON).content(debit("300", "s1")))
                .andExpect(jsonPath("$.balance").value(200.00));
        mvc.perform(post("/wallets/7/debit").contentType(MediaType.APPLICATION_JSON).content(debit("250", "s2")))
                .andExpect(jsonPath("$.balance").value(-50.00));   // allowed to go negative
    }

    @Test
    void topUp_increasesBalance() throws Exception {
        mvc.perform(post("/wallets/7/topup").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(600.00));
    }

    @Test
    void unknownWallet_returns404() throws Exception {
        mvc.perform(get("/wallets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WALLET_NOT_FOUND"));
    }
}
