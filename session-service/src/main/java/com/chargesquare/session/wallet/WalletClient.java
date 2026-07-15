package com.chargesquare.session.wallet;

import com.chargesquare.session.error.NotFoundException;
import com.chargesquare.session.error.UpstreamException;
import com.chargesquare.session.security.ServiceTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * The second real synchronous REST boundary: Session Service settles the wallet by calling the
 * Wallet Service. The debit is idempotent (keyed by the session id), so a retried stop that
 * re-issues the debit never double-charges.
 */
@Component
public class WalletClient {

    private static final Logger log = LoggerFactory.getLogger(WalletClient.class);

    private final RestClient restClient;
    private final ServiceTokenProvider tokenProvider;

    public WalletClient(@Value("${wallet.service.url}") String baseUrl,
                        ServiceTokenProvider tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.tokenProvider = tokenProvider;
        log.info("Wallet Service base URL = {}", baseUrl);
    }

    /** Idempotently debit a wallet; returns the balance after settlement. */
    public DebitResult debit(Long userId, BigDecimal amount, String idempotencyKey) {
        try {
            return restClient.post()
                    .uri("/wallets/{id}/debit", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.bearerToken())
                    .body(new DebitRequest(amount, idempotencyKey))
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new NotFoundException("WALLET_NOT_FOUND", "No wallet for user " + userId);
                    })
                    .body(DebitResult.class);
        } catch (ResourceAccessException ex) {
            log.error("Wallet Service unreachable: {}", ex.getMessage());
            throw new UpstreamException("Wallet Service is unreachable");
        }
    }

    public record DebitRequest(BigDecimal amount, String idempotencyKey) {
    }

    public record DebitResult(Long userId, BigDecimal balance, String currency, boolean replayed) {
    }
}
