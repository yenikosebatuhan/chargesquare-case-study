package com.chargesquare.session.web.dto;

import com.chargesquare.session.domain.Wallet;

import java.math.BigDecimal;

public record WalletResponse(Long userId, BigDecimal balance, String currency) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getUserId(), wallet.getBalance(), wallet.getCurrency());
    }
}
