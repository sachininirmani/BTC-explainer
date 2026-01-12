-- Add severity level to market_events so the frontend can show a stronger/weaker move
-- and the backend can keep multiple thresholds (e.g. 4/3/2).

ALTER TABLE market_events
  ADD COLUMN IF NOT EXISTS severity SMALLINT;

-- Backfill existing rows based on pct_change (absolute value)
UPDATE market_events
SET severity = CASE
  WHEN abs(pct_change) >= 4 THEN 4
  WHEN abs(pct_change) >= 3 THEN 3
  WHEN abs(pct_change) >= 2 THEN 2
  ELSE 1
END
WHERE severity IS NULL;

ALTER TABLE market_events
  ALTER COLUMN severity SET NOT NULL;
