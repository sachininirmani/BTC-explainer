ALTER TABLE event_explanations
  ADD COLUMN ai_explanation_text TEXT,
  ADD COLUMN ai_explanation_source TEXT NOT NULL DEFAULT 'NONE',
  ADD COLUMN ai_model TEXT,
  ADD COLUMN ai_generated_at TIMESTAMPTZ,
  ADD COLUMN ai_error_message TEXT;

UPDATE event_explanations
SET ai_explanation_source = 'NONE'
WHERE ai_explanation_source IS NULL;
