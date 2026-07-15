package com.chargesquare.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** A driver's prepaid balance, keyed by userId. */
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    protected Wallet() {
    }

    public Wallet(Long userId, BigDecimal balance, String currency) {
        this.userId = userId;
        this.balance = balance;
        this.currency = currency;
    }

    /**
     * Deduct a charge. The balance may go negative rather than reject the debit: a session that
     * has physically ended must always be settleable. (See DESIGN.md for the trade-off.)
     */
    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void topUp(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }
}
