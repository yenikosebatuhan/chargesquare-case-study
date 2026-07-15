-- Idempotent across test methods (shared in-memory DB). Wallets now live in the Wallet Service.
DELETE FROM stop_idempotency;
DELETE FROM sessions;
