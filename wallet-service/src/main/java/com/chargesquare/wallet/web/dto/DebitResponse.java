package com.chargesquare.wallet.web.dto;

import java.math.BigDecimal;

/** {@code replayed=true} means the key was already processed and no new debit occurred. */
public record DebitResponse(Long userId, BigDecimal balance, String currency, boolean replayed) {
}
