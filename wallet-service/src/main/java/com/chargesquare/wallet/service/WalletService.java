package com.chargesquare.wallet.service;

import com.chargesquare.wallet.domain.ProcessedDebit;
import com.chargesquare.wallet.domain.Wallet;
import com.chargesquare.wallet.error.NotFoundException;
import com.chargesquare.wallet.repo.ProcessedDebitRepository;
import com.chargesquare.wallet.repo.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository wallets;
    private final ProcessedDebitRepository processedDebits;
    private final Clock clock;

    public WalletService(WalletRepository wallets, ProcessedDebitRepository processedDebits, Clock clock) {
        this.wallets = wallets;
        this.processedDebits = processedDebits;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(Long userId) {
        return wallets.findById(userId)
                .orElseThrow(() -> new NotFoundException("WALLET_NOT_FOUND", "No wallet for user " + userId));
    }

    @Transactional
    public Wallet topUp(Long userId, BigDecimal amount) {
        Wallet wallet = getWallet(userId);
        wallet.topUp(amount);
        log.info("wallet topped up {} for user {} (balance now {})", amount, userId, wallet.getBalance());
        return wallet;
    }

    /**
     * Debit a wallet, keyed by an idempotency key (the session id). A replay with the same key
     * returns the recorded balance without debiting again — so a retried stop never double-charges.
     */
    @Transactional
    public DebitResult debit(Long userId, BigDecimal amount, String idempotencyKey) {
        ProcessedDebit existing = processedDebits.findById(idempotencyKey).orElse(null);
        if (existing != null) {
            log.info("debit replay for key {} — returning recorded balance {}",
                    idempotencyKey, existing.getBalanceAfter());
            return new DebitResult(existing.getBalanceAfter(), true);
        }

        Wallet wallet = getWallet(userId);
        wallet.debit(amount);
        processedDebits.save(new ProcessedDebit(
                idempotencyKey, userId, amount, wallet.getBalance(), Instant.now(clock)));
        log.info("wallet debited {} for user {} (key {}, balance now {})",
                amount, userId, idempotencyKey, wallet.getBalance());
        return new DebitResult(wallet.getBalance(), false);
    }

    public record DebitResult(BigDecimal balanceAfter, boolean replayed) {
    }
}
