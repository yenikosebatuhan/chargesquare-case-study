package com.chargesquare.wallet.web;

import com.chargesquare.wallet.domain.Wallet;
import com.chargesquare.wallet.service.WalletService;
import com.chargesquare.wallet.service.WalletService.DebitResult;
import com.chargesquare.wallet.web.dto.DebitRequest;
import com.chargesquare.wallet.web.dto.DebitResponse;
import com.chargesquare.wallet.web.dto.TopUpRequest;
import com.chargesquare.wallet.web.dto.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService service;

    public WalletController(WalletService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public WalletResponse getWallet(@PathVariable Long userId) {
        return WalletResponse.from(service.getWallet(userId));
    }

    /** Ops action: add funds (ADMIN when secured). */
    @PostMapping("/{userId}/topup")
    public WalletResponse topUp(@PathVariable Long userId, @Valid @RequestBody TopUpRequest request) {
        return WalletResponse.from(service.topUp(userId, request.amount()));
    }

    /** Service-to-service: idempotent debit at session stop (ADMIN when secured). */
    @PostMapping("/{userId}/debit")
    public DebitResponse debit(@PathVariable Long userId, @Valid @RequestBody DebitRequest request) {
        Wallet wallet = service.getWallet(userId);
        DebitResult result = service.debit(userId, request.amount(), request.idempotencyKey());
        return new DebitResponse(userId, result.balanceAfter(), wallet.getCurrency(), result.replayed());
    }
}
