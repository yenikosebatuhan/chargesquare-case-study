package com.chargesquare.session.web;

import com.chargesquare.session.service.WalletService;
import com.chargesquare.session.web.dto.TopUpRequest;
import com.chargesquare.session.web.dto.WalletResponse;
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

    /** Stretch: top up a driver's balance (ADMIN-only when security is enabled). */
    @PostMapping("/{userId}/topup")
    public WalletResponse topUp(@PathVariable Long userId, @Valid @RequestBody TopUpRequest request) {
        return WalletResponse.from(service.topUp(userId, request.amount()));
    }
}
