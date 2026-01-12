-- Adds optional context columns to support richer explanations.
-- Safe / additive migration.

ALTER TABLE news_daily_stats
  ADD COLUMN IF NOT EXISTS coverage_pct DOUBLE PRECISION;

ALTER TABLE fx_rates_daily
  ADD COLUMN IF NOT EXISTS source_date DATE;

-- Backfill: for existing FX rows, the source_date is assumed to be the same as rate_date.
UPDATE fx_rates_daily
SET source_date = rate_date
WHERE source_date IS NULL;
