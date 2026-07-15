-- Stretch goals: reservation hold + time-of-use (peak) pricing.

ALTER TABLE connectors ADD COLUMN reserved_by    BIGINT;
ALTER TABLE connectors ADD COLUMN reserved_until TIMESTAMPTZ;

-- Widen the status check to allow RESERVED.
ALTER TABLE connectors DROP CONSTRAINT chk_connector_status;
ALTER TABLE connectors ADD  CONSTRAINT chk_connector_status
    CHECK (status IN ('AVAILABLE', 'RESERVED', 'OCCUPIED'));

-- Optional peak price per tariff. Give the DC tariff a higher peak rate to show time-of-use.
ALTER TABLE tariffs ADD COLUMN price_per_kwh_peak NUMERIC(12, 2);
UPDATE tariffs SET price_per_kwh_peak = 10.50 WHERE id = 5;
