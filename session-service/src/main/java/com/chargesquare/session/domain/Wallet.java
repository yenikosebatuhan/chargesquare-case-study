package com.chargesquare.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** A driver's prepaid balance. Keyed by userId (one wallet per user). */
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
     * Deduct a charge. We allow the balance to go negative rather than reject the stop:
     * a session that has physically ended must always be closeable and the connector freed.
     * (See DESIGN.md for the trade-off.)
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
