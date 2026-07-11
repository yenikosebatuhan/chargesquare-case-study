-- Idempotent across test methods (shared in-memory DB).
DELETE FROM sessions;
DELETE FROM wallets;

INSERT INTO wallets (user_id, balance, currency) VALUES (7, 500.00, 'TRY');
