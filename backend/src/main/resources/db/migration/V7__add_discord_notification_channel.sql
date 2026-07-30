ALTER TABLE notification_channels
    ADD COLUMN IF NOT EXISTS
        external_username VARCHAR(100);

UPDATE notification_channels
SET external_username =
    telegram_username
WHERE channel_type = 'TELEGRAM'
  AND external_username IS NULL;

ALTER TABLE notification_channels
    DROP CONSTRAINT IF EXISTS
        ck_notification_channels_type;

ALTER TABLE notification_channels
    ADD CONSTRAINT
        ck_notification_channels_type
        CHECK (
            channel_type IN (
                'TELEGRAM',
                'DISCORD'
            )
        );

ALTER TABLE notification_logs
    DROP CONSTRAINT IF EXISTS
        ck_notification_logs_channel;

ALTER TABLE notification_logs
    ADD CONSTRAINT
        ck_notification_logs_channel
        CHECK (
            channel_type IN (
                'TELEGRAM',
                'DISCORD'
            )
        );

CREATE INDEX IF NOT EXISTS
    idx_notification_channels_connected_type
    ON notification_channels (
        channel_type,
        connection_status,
        enabled
    );
