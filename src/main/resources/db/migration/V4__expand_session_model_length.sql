ALTER TABLE session
    ALTER COLUMN model TYPE VARCHAR(255);

COMMENT ON COLUMN session.model IS '模型标识（modelId 或模型名）';
