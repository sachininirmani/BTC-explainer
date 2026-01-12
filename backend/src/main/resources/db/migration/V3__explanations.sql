CREATE TABLE event_explanations (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL UNIQUE REFERENCES market_events(id) ON DELETE CASCADE,
  summary_text TEXT NOT NULL,
  confidence TEXT NOT NULL,
  factors_json TEXT NOT NULL,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE api_call_log (
  id BIGSERIAL PRIMARY KEY,
  provider TEXT NOT NULL,
  endpoint TEXT NOT NULL,
  http_status INT,
  latency_ms INT,
  error_message TEXT,
  called_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE rate_limit_violations (
  id BIGSERIAL PRIMARY KEY,
  ip TEXT NOT NULL,
  path TEXT NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
