-- Wallet Service schema: per-user balances + an idempotency ledger for debits.

CREATE TABLE wallets (
    user_id  BIGINT PRIMARY KEY,
    balance  NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3)     NOT NULL
);

-- Every debit carries an idempotency key (the session id). A replay with the same key
-- returns the recorded outcome instead of debiting again — so a retried stop never double-charges.
CREATE TABLE processed_debits (
    idempotency_key VARCHAR(80)    PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    balance_after   NUMERIC(12, 2) NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL
);
