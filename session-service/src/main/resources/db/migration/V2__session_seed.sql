-- Seed one driver wallet. (Panel users are seeded at startup with hashed passwords —
-- see DataSeeder — so no credentials ever live in a committed SQL file.)
INSERT INTO wallets (user_id, balance, currency) VALUES
    (7, 500.00, 'TRY');
