CREATE TABLE chat_history
(
    id           VARCHAR(255) NOT NULL,
    session_id   VARCHAR(255) NOT NULL,
    question     VARCHAR(255),
    answer       TEXT,
    round_number INTEGER      NOT NULL,
    completed    BOOLEAN NOT NULL,
    created_time TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    PRIMARY KEY (id)
);
COMMENT ON COLUMN chat_history.id IS '主键id';
COMMENT ON COLUMN chat_history.session_id IS '对应会话';
COMMENT ON COLUMN chat_history.question IS '用户的问题';
COMMENT ON COLUMN chat_history.answer IS '回答';
COMMENT ON COLUMN chat_history.round_number IS '第几轮';
COMMENT ON COLUMN chat_history.completed IS '是否是完成状态';
COMMENT ON COLUMN chat_history.created_time IS '创建时间';

CREATE TABLE model
(
    model_id    VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    token       VARCHAR(255) NOT NULL,
    url         VARCHAR(255) NOT NULL,
    create_time TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    PRIMARY KEY (model_id)
);
COMMENT ON COLUMN model.model_id IS '模型主键';
COMMENT ON COLUMN model.name IS '模型名称';
COMMENT ON COLUMN model.token IS '对应token';
COMMENT ON COLUMN model.url IS '访问url';
COMMENT ON COLUMN model.create_time IS '创建时间';

CREATE TABLE session
(
    session_id     VARCHAR(255) NOT NULL,
    user_id        BIGINT       NOT NULL,
    model          VARCHAR(20)  NOT NULL,
    status         SMALLINT     NOT NULL,
    research_brief TEXT,
    created_time   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    update_time    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    summary        VARCHAR(255),
    PRIMARY KEY (session_id)
);
COMMENT ON COLUMN session.session_id IS '会话id';
COMMENT ON COLUMN session.user_id IS '对应用户id';
COMMENT ON COLUMN session.model IS '模型名称';
COMMENT ON COLUMN session.status IS '会话状态';
COMMENT ON COLUMN session.research_brief IS '会话的brief';
COMMENT ON COLUMN session.created_time IS '创建时间';
COMMENT ON COLUMN session.summary IS '内容总结';

CREATE TABLE task
(
    id            VARCHAR(255) NOT NULL,
    session_id    VARCHAR(255) NOT NULL,
    agent_type    SMALLINT     NOT NULL,
    payload       TEXT,
    status        SMALLINT     NOT NULL,
    started_time  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    finished_time TIMESTAMP WITHOUT TIME ZONE,
    PRIMARY KEY (id)
);
COMMENT ON COLUMN task.id IS '主键id';
COMMENT ON COLUMN task.session_id IS '对应会话';
COMMENT ON COLUMN task.agent_type IS '子agent类型';
COMMENT ON COLUMN task.payload IS '步骤的结果';
COMMENT ON COLUMN task.status IS '任务状态';
COMMENT ON COLUMN task.started_time IS '启动时间';
COMMENT ON COLUMN task.finished_time IS '结束时间';

CREATE TABLE "user"
(
    user_id   BIGINT       NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id)
);
COMMENT ON COLUMN "user".user_id IS '用户id';
COMMENT ON COLUMN "user".user_name IS '用户名称';
COMMENT ON COLUMN "user".password IS '用户密码';