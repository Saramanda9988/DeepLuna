CREATE TABLE IF NOT EXISTS app_config
(
    config_key   VARCHAR(128) PRIMARY KEY,
    config_value VARCHAR(512) NOT NULL,
    description  VARCHAR(255),
    updated_time TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL
);

COMMENT ON TABLE app_config IS '应用运行时配置表';
COMMENT ON COLUMN app_config.config_key IS '配置键';
COMMENT ON COLUMN app_config.config_value IS '配置值';
COMMENT ON COLUMN app_config.description IS '配置描述';
COMMENT ON COLUMN app_config.updated_time IS '更新时间';

INSERT INTO app_config(config_key, config_value, description)
VALUES ('websearch.provider', 'tavily', 'Active web search provider id')
ON CONFLICT (config_key) DO NOTHING;

