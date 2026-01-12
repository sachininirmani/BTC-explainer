CREATE TABLE market_events (
  id BIGSERIAL PRIMARY KEY,
  asset_id BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
  event_date DATE NOT NULL,
  direction TEXT NOT NULL,
  pct_change NUMERIC(10,4) NOT NULL,
  threshold_used TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(asset_id, event_date)
);

CREATE TABLE news_daily_stats (
  id BIGSERIAL PRIMARY KEY,
  stat_date DATE NOT NULL UNIQUE,
  query_tag TEXT NOT NULL,
  article_count INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE news_items_sample (
  id BIGSERIAL PRIMARY KEY,
  item_date DATE NOT NULL,
  title TEXT NOT NULL,
  source TEXT,
  url TEXT,
  published_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sentiment_fng_daily (
  id BIGSERIAL PRIMARY KEY,
  sentiment_date DATE NOT NULL UNIQUE,
  value INT NOT NULL,
  classification TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE fx_rates_daily (
  id BIGSERIAL PRIMARY KEY,
  rate_date DATE NOT NULL,
  base TEXT NOT NULL,
  quote TEXT NOT NULL,
  rate NUMERIC(18,8) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(rate_date, base, quote)
);

CREATE TABLE weather_extremes_daily (
  id BIGSERIAL PRIMARY KEY,
  wx_date DATE NOT NULL,
  region_key TEXT NOT NULL,
  extreme_type TEXT NOT NULL,
  severity NUMERIC(10,4),
  details_json TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(wx_date, region_key, extreme_type)
);
