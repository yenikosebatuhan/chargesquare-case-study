package com.chargesquare.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Idempotency ledger entry: records the outcome of a debit so replays don't re-charge. */
@Entity
@Table(name = "processed_debits")
public class ProcessedDebit {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProcessedDebit() {
    }

    public ProcessedDebit(String idempotencyKey, Long userId, BigDecimal amount,
                          BigDecimal balanceAfter, Instant createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }
}
