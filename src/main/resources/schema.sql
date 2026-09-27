CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_quiz_set_title_trgm
    ON quiz_set USING gin (title gin_trgm_ops);