-- Baseline data so a reviewer can drive the flow immediately.
-- Sample values are arbitrary but kept consistent with the README and tests.

INSERT INTO stations (id, name) VALUES
    (1, 'ChargeSquare Kadikoy');

INSERT INTO tariffs (id, price_per_kwh, start_fee, currency) VALUES
    (5, 8.50, 2.00, 'TRY'),
    (6, 5.00, 0.00, 'TRY');

INSERT INTO connectors (id, station_id, tariff_id, type, power_kw, status) VALUES
    (10, 1, 5, 'CCS2-DC',  60, 'AVAILABLE'),
    (11, 1, 6, 'Type2-AC', 22, 'AVAILABLE');

-- Keep identity sequences ahead of the seeded ids so future inserts don't collide.
ALTER TABLE stations   ALTER COLUMN id RESTART WITH 2;
ALTER TABLE tariffs    ALTER COLUMN id RESTART WITH 7;
ALTER TABLE connectors ALTER COLUMN id RESTART WITH 12;
