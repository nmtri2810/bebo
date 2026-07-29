ALTER TABLE notification_channels
    ADD COLUMN IF NOT EXISTS connection_status VARCHAR(20)
        NOT NULL DEFAULT 'DISCONNECTED',

    ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT,

    ADD COLUMN IF NOT EXISTS telegram_username VARCHAR(100),

    ADD COLUMN IF NOT EXISTS connect_token_hash VARCHAR(64),

    ADD COLUMN IF NOT EXISTS connect_token_expires_at TIMESTAMPTZ,

    ADD COLUMN IF NOT EXISTS connected_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS
    uq_notification_channels_connect_token_hash
    ON notification_channels (connect_token_hash)
    WHERE connect_token_hash IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS
    uq_notification_channels_telegram_chat_id
    ON notification_channels (telegram_chat_id)
    WHERE telegram_chat_id IS NOT NULL;
