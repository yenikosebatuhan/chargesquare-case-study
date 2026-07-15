package com.chargesquare.wallet.web.dto;

import com.chargesquare.wallet.domain.Wallet;

import java.math.BigDecimal;

public record WalletResponse(Long userId, BigDecimal balance, String currency) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getUserId(), wallet.getBalance(), wallet.getCurrency());
    }
}
