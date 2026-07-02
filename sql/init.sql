-- =============================================
-- SoulMate AI 数据库初始化脚本
-- 数据库: PostgreSQL 16
-- 创建日期: 2026-06-05
-- =============================================

-- 创建数据库（需手动执行）
-- CREATE DATABASE soulmate WITH ENCODING = 'UTF8' LC_COLLATE = 'en_US.UTF-8';

-- =============================================
-- 1. 用户模块
-- =============================================

CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT       PRIMARY KEY,
    email           VARCHAR(128) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    nickname        VARCHAR(64)  NOT NULL,
    avatar_url      VARCHAR(512),
    gender          SMALLINT     DEFAULT 0,
    birthday        DATE,
    guest_flag      SMALLINT     DEFAULT 0,
    status          SMALLINT     DEFAULT 1,
    last_login_time TIMESTAMP,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_user_create_time ON t_user(create_time);

CREATE TABLE IF NOT EXISTS t_user_profile (
    id                 BIGINT       PRIMARY KEY,
    user_id            BIGINT       NOT NULL UNIQUE,
    personality_type   VARCHAR(32),
    personality_result JSONB,
    interests          JSONB,
    chat_style_pref    VARCHAR(32),
    topics_blacklist   JSONB,
    create_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            SMALLINT     DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_user_settings (
    id              BIGINT       PRIMARY KEY,
    user_id         BIGINT       NOT NULL UNIQUE,
    dark_mode       SMALLINT     DEFAULT 0,
    font_size       VARCHAR(16)  DEFAULT 'normal',
    language        VARCHAR(16)  DEFAULT 'zh-CN',
    message_notify  SMALLINT     DEFAULT 1,
    proactive_care  SMALLINT     DEFAULT 1,
    model_base_url  VARCHAR(256),
    model_name      VARCHAR(64),
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);

-- =============================================
-- 2. AI伴侣模块
-- =============================================

CREATE TABLE IF NOT EXISTS t_companion (
    id                BIGINT       PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    name              VARCHAR(64)  NOT NULL,
    gender            SMALLINT     NOT NULL,
    relationship_type VARCHAR(32)  NOT NULL,
    description       TEXT,
    speaking_style    VARCHAR(32)  DEFAULT 'casual',
    birthday          DATE,
    avatar_url        VARCHAR(512),
    theme_color       VARCHAR(16),
    status            SMALLINT     DEFAULT 1,
    companion_order   INT          DEFAULT 0,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_companion_user_id ON t_companion(user_id);
CREATE INDEX IF NOT EXISTS idx_companion_user_status ON t_companion(user_id, status);

CREATE TABLE IF NOT EXISTS t_companion_personality (
    id              BIGINT      PRIMARY KEY,
    companion_id    BIGINT      NOT NULL,
    personality_key VARCHAR(32) NOT NULL,
    create_time     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT    DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cp_companion_id ON t_companion_personality(companion_id);

CREATE TABLE IF NOT EXISTS t_companion_voice (
    id           BIGINT       PRIMARY KEY,
    companion_id BIGINT       NOT NULL UNIQUE,
    voice_id     VARCHAR(64)  NOT NULL,
    voice_name   VARCHAR(64),
    pitch        DECIMAL(3,1) DEFAULT 0.0,
    speed        DECIMAL(3,1) DEFAULT 1.0,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT     DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_companion_avatar (
    id           BIGINT       PRIMARY KEY,
    companion_id BIGINT       NOT NULL UNIQUE,
    avatar_type  VARCHAR(32)  NOT NULL,
    image_url    VARCHAR(512) NOT NULL,
    expression   VARCHAR(32)  DEFAULT 'normal',
    lottie_url   VARCHAR(512),
    sd_prompt    TEXT,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT     DEFAULT 0
);

-- =============================================
-- 3. 对话消息模块
-- =============================================

CREATE TABLE IF NOT EXISTS t_conversation (
    id                   BIGINT       PRIMARY KEY,
    user_id              BIGINT       NOT NULL,
    companion_id         BIGINT       NOT NULL,
    scene_mode           VARCHAR(32)  DEFAULT 'daily',
    last_message_preview VARCHAR(256),
    last_message_time    TIMESTAMP,
    unread_count         INT          DEFAULT 0,
    pinned               SMALLINT     DEFAULT 0,
    context_window       INT          DEFAULT 15,
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_conv_user_id ON t_conversation(user_id);
CREATE INDEX IF NOT EXISTS idx_conv_companion_id ON t_conversation(companion_id);
CREATE INDEX IF NOT EXISTS idx_conv_user_last_msg ON t_conversation(user_id, last_message_time DESC);

CREATE TABLE IF NOT EXISTS t_message (
    id              BIGINT       PRIMARY KEY,
    conversation_id BIGINT       NOT NULL,
    sender_type     VARCHAR(16)  NOT NULL,
    content         TEXT         NOT NULL,
    content_type    VARCHAR(16)  DEFAULT 'text',
    voice_url       VARCHAR(512),
    voice_duration  INT,
    image_url       VARCHAR(512),
    emotion_tag     VARCHAR(32),
    emotion_score   DECIMAL(3,2),
    tokens_used     INT          DEFAULT 0,
    llm_model       VARCHAR(64),
    read_status     SMALLINT     DEFAULT 0,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_msg_conversation_id ON t_message(conversation_id);
CREATE INDEX IF NOT EXISTS idx_msg_conversation_time ON t_message(conversation_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_msg_sender_type ON t_message(sender_type);

-- =============================================
-- 4. 记忆模块
-- =============================================

CREATE TABLE IF NOT EXISTS t_memory (
    id                BIGINT       PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    companion_id      BIGINT       NOT NULL,
    category          VARCHAR(32)  NOT NULL,
    title             VARCHAR(128) NOT NULL,
    content           TEXT         NOT NULL,
    thought           TEXT,
    emotion           VARCHAR(32),
    source_message_id BIGINT,
    importance        SMALLINT     DEFAULT 5,
    vector_id         VARCHAR(64),
    access_count      INT          DEFAULT 0,
    last_access_time  TIMESTAMP,
    user_visible      SMALLINT     DEFAULT 1,
    user_edited       SMALLINT     DEFAULT 0,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_memory_user_id ON t_memory(user_id);
CREATE INDEX IF NOT EXISTS idx_memory_companion_id ON t_memory(companion_id);
CREATE INDEX IF NOT EXISTS idx_memory_user_category ON t_memory(user_id, category);
CREATE INDEX IF NOT EXISTS idx_memory_vector_id ON t_memory(vector_id);

CREATE TABLE IF NOT EXISTS t_memory_tag (
    id          BIGINT      PRIMARY KEY,
    memory_id   BIGINT      NOT NULL,
    tag_name    VARCHAR(64) NOT NULL,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT    DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_mtag_memory_id ON t_memory_tag(memory_id);
CREATE INDEX IF NOT EXISTS idx_mtag_tag_name ON t_memory_tag(tag_name);

-- =============================================
-- 5. 情感模块
-- =============================================

CREATE TABLE IF NOT EXISTS t_emotion_record (
    id               BIGINT       PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    message_id       BIGINT       NOT NULL,
    emotion_label    VARCHAR(32)  NOT NULL,
    emotion_score    DECIMAL(3,2) NOT NULL,
    intensity        SMALLINT,
    detection_method VARCHAR(32)  DEFAULT 'llm',
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_er_user_id ON t_emotion_record(user_id);
CREATE INDEX IF NOT EXISTS idx_er_message_id ON t_emotion_record(message_id);
CREATE INDEX IF NOT EXISTS idx_er_user_time ON t_emotion_record(user_id, create_time);

CREATE TABLE IF NOT EXISTS t_emotion_diary (
    id                 BIGINT       PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    diary_date         DATE         NOT NULL,
    overall_emotion    VARCHAR(32),
    avg_score          DECIMAL(3,2),
    summary            TEXT,
    keyword            VARCHAR(128),
    conversation_count INT          DEFAULT 0,
    create_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            SMALLINT     DEFAULT 0,
    UNIQUE(user_id, diary_date)
);

-- =============================================
-- 6. 订阅支付模块
-- =============================================

CREATE TABLE IF NOT EXISTS t_subscription_plan (
    id                 BIGINT        PRIMARY KEY,
    plan_code          VARCHAR(32)   NOT NULL UNIQUE,
    plan_name          VARCHAR(64)   NOT NULL,
    price_monthly      DECIMAL(10,2) NOT NULL,
    max_companions     INT           NOT NULL,
    max_daily_messages INT           NOT NULL,
    voice_message      SMALLINT      DEFAULT 0,
    voice_call         SMALLINT      DEFAULT 0,
    advanced_memory    SMALLINT      DEFAULT 0,
    custom_voice       SMALLINT      DEFAULT 0,
    priority_response  SMALLINT      DEFAULT 0,
    display_order      INT           DEFAULT 0,
    status             SMALLINT      DEFAULT 1,
    create_time        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            SMALLINT      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_user_subscription (
    id          BIGINT    PRIMARY KEY,
    user_id     BIGINT    NOT NULL,
    plan_id     BIGINT    NOT NULL,
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP NOT NULL,
    auto_renew  SMALLINT  DEFAULT 1,
    status      SMALLINT  DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT  DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_us_user_id ON t_user_subscription(user_id);
CREATE INDEX IF NOT EXISTS idx_us_user_status ON t_user_subscription(user_id, status);
CREATE INDEX IF NOT EXISTS idx_us_end_time ON t_user_subscription(end_time);

CREATE TABLE IF NOT EXISTS t_payment_order (
    id              BIGINT        PRIMARY KEY,
    order_no        VARCHAR(64)   NOT NULL UNIQUE,
    user_id         BIGINT        NOT NULL,
    plan_id         BIGINT        NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    payment_channel VARCHAR(32)   DEFAULT 'alipay',
    payment_status  SMALLINT      DEFAULT 0,
    trade_no        VARCHAR(128),
    paid_time       TIMESTAMP,
    period_type     VARCHAR(16)   DEFAULT 'monthly',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT      DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_po_user_id ON t_payment_order(user_id);
CREATE INDEX IF NOT EXISTS idx_po_payment_status ON t_payment_order(payment_status);

-- =============================================
-- 7. 通知日程模块
-- =============================================

CREATE TABLE IF NOT EXISTS t_notification (
    id           BIGINT       PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    companion_id BIGINT,
    type         VARCHAR(32)  NOT NULL,
    title        VARCHAR(128) NOT NULL,
    content      TEXT,
    read_status  SMALLINT     DEFAULT 0,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notif_user_id ON t_notification(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_user_read ON t_notification(user_id, read_status);

CREATE TABLE IF NOT EXISTS t_schedule_reminder (
    id           BIGINT       PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    companion_id BIGINT       NOT NULL,
    title        VARCHAR(128) NOT NULL,
    content      TEXT,
    remind_time  TIMESTAMP    NOT NULL,
    repeat_type  VARCHAR(16)  DEFAULT 'once',
    status       SMALLINT     DEFAULT 0,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sr_user_id ON t_schedule_reminder(user_id);
CREATE INDEX IF NOT EXISTS idx_sr_remind_time ON t_schedule_reminder(remind_time, status);

-- =============================================
-- 8. 系统配置模块
-- =============================================

CREATE TABLE IF NOT EXISTS t_system_config (
    id           BIGINT       PRIMARY KEY,
    config_key   VARCHAR(128) NOT NULL UNIQUE,
    config_value TEXT         NOT NULL,
    description  VARCHAR(256),
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_model_config (
    id             BIGINT       PRIMARY KEY,
    model_code     VARCHAR(64)  NOT NULL UNIQUE,
    model_name     VARCHAR(128) NOT NULL,
    provider       VARCHAR(32)  NOT NULL,
    base_url       VARCHAR(256) NOT NULL,
    api_key        VARCHAR(256),
    max_tokens     INT          DEFAULT 4096,
    temperature    DECIMAL(2,1) DEFAULT 0.7,
    support_stream SMALLINT     DEFAULT 1,
    support_vision SMALLINT     DEFAULT 0,
    status         SMALLINT     DEFAULT 1,
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT     DEFAULT 0
);

-- =============================================
-- 初始化数据
-- =============================================

-- 套餐数据
INSERT INTO t_subscription_plan (id, plan_code, plan_name, price_monthly, max_companions, max_daily_messages, voice_message, voice_call, advanced_memory, custom_voice, priority_response, display_order, status, create_time, update_time)
VALUES
    (1, 'FREE',     '免费版', 0,  1,  30, 0, 0, 0, 0, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'BASIC',    '基础版', 18, 3,  -1, 1, 0, 0, 0, 0, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'PREMIUM',  '高级版', 38, 5,  -1, 1, 1, 1, 1, 0, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'ULTIMATE', '尊享版', 68, -1, -1, 1, 1, 1, 1, 1, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (plan_code) DO NOTHING;

-- 模型配置数据
INSERT INTO t_model_config (id, model_code, model_name, provider, base_url, max_tokens, temperature, support_stream, support_vision, status, create_time, update_time)
VALUES
    (1, 'gpt-4o',        'GPT-4o',            'openai',    'https://api.openai.com/v1',                4096, 0.7, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'claude-3.5',    'Claude 3.5 Sonnet', 'anthropic', 'https://api.anthropic.com/v1',              4096, 0.7, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'qwen-max',      '通义千问 Max',         'alibaba',   'https://dashscope.aliyuncs.com/api/v1',    4096, 0.7, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'local-default', '本地模型',             'local',     'http://localhost:1234/v1',                  4096, 0.7, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'mimo-v2.5-pro', 'mimo-v2.5-pro',     'xiaomi',    'https://api.xiaomimimo.com/v1', 2048, 0.7, 1, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (model_code) DO NOTHING;

-- =============================================
-- 6. AI 伴侣定时唤醒提醒模块
-- =============================================

CREATE TABLE IF NOT EXISTS t_companion_reminder (
    id              BIGINT       PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    companion_id    BIGINT       NOT NULL,
    reminder_time   VARCHAR(5)   NOT NULL, -- 格式 "HH:mm"，例如 "07:30"
    repeat_days     VARCHAR(32),           -- 逗号分隔如 "1,2,3,4,5"，空代表仅一次
    text_template   VARCHAR(512) NOT NULL, -- 主动叫醒/提醒说话模板
    type            VARCHAR(32)  NOT NULL, -- 'WAKE_UP' (叫醒) 或 'NOTIFICATION' (通知)
    enabled         SMALLINT     DEFAULT 1, -- 1=启用，0=停用
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_reminder_user_id ON t_companion_reminder(user_id);

