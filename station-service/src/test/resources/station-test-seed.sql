-- Reset first so the script is idempotent across test methods (shared in-memory DB).
DELETE FROM connectors;
DELETE FROM tariffs;
DELETE FROM stations;

INSERT INTO stations (id, name) VALUES (1, 'Test Station');
INSERT INTO tariffs (id, price_per_kwh, start_fee, currency) VALUES (5, 8.50, 2.00, 'TRY');
INSERT INTO connectors (id, station_id, tariff_id, type, power_kw, status)
    VALUES (10, 1, 5, 'CCS2-DC', 60, 'AVAILABLE');
