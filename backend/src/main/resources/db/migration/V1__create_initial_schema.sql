CREATE TABLE app_users (
    id UUID PRIMARY KEY,

    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    timezone VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_app_users_email
        UNIQUE (email),

    CONSTRAINT ck_app_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE cycle_settings (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    default_cycle_length INTEGER NOT NULL DEFAULT 28,
    reminder_days_before INTEGER NOT NULL DEFAULT 3,
    notification_time TIME NOT NULL DEFAULT '08:00',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_cycle_settings_user
        UNIQUE (user_id),

    CONSTRAINT fk_cycle_settings_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_cycle_settings_length
        CHECK (default_cycle_length BETWEEN 15 AND 60),

    CONSTRAINT ck_cycle_settings_reminder
        CHECK (reminder_days_before BETWEEN 0 AND 14)
);

CREATE TABLE cycle_records (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    start_date DATE NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cycle_records_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_cycle_records_user_start
        UNIQUE (user_id, start_date)
);

CREATE INDEX idx_cycle_records_user_start
    ON cycle_records(user_id, start_date DESC);

CREATE TABLE notification_channels (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    channel_type VARCHAR(30) NOT NULL,
    external_recipient_id VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notification_channels_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_notification_channels_user_type
        UNIQUE (user_id, channel_type),

    CONSTRAINT uk_notification_channels_recipient
        UNIQUE (channel_type, external_recipient_id),

    CONSTRAINT ck_notification_channels_type
        CHECK (channel_type IN ('TELEGRAM'))
);

CREATE TABLE notification_logs (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    cycle_record_id UUID,

    channel_type VARCHAR(30) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,

    predicted_period_date DATE NOT NULL,
    scheduled_for TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,

    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notification_logs_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notification_logs_cycle
        FOREIGN KEY (cycle_record_id)
        REFERENCES cycle_records(id)
        ON DELETE SET NULL,

    CONSTRAINT uk_notification_logs_dedup
        UNIQUE (
            user_id,
            predicted_period_date,
            notification_type,
            channel_type
        ),

    CONSTRAINT ck_notification_logs_channel
        CHECK (channel_type IN ('TELEGRAM')),

    CONSTRAINT ck_notification_logs_type
        CHECK (
            notification_type IN ('CYCLE_APPROACHING')
        ),

    CONSTRAINT ck_notification_logs_status
        CHECK (
            status IN ('PENDING', 'SENT', 'FAILED')
        )
);
