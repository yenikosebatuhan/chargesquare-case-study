DELETE FROM processed_debits;
DELETE FROM wallets;
INSERT INTO wallets (user_id, balance, currency) VALUES (7, 500.00, 'TRY');
