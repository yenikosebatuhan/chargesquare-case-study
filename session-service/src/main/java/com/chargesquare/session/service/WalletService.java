package com.chargesquare.session.service;

import com.chargesquare.session.domain.Wallet;
import com.chargesquare.session.error.NotFoundException;
import com.chargesquare.session.repo.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Wallet reads and the top-up management action (a stretch endpoint, ADMIN-only when secured). */
@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository wallets;

    public WalletService(WalletRepository wallets) {
        this.wallets = wallets;
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
}
