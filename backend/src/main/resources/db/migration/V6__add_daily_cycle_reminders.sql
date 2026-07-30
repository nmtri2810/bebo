ALTER TABLE notification_logs
    ADD COLUMN delivery_local_date DATE,
    ADD COLUMN reminder_stage VARCHAR(30),
    ADD COLUMN days_relative_to_prediction INTEGER,
    ADD COLUMN message_body TEXT;

UPDATE notification_logs notification_log
SET delivery_local_date =
        (
            notification_log.scheduled_for
            AT TIME ZONE app_user.timezone
        )::DATE
FROM app_users app_user
WHERE app_user.id =
    notification_log.user_id;

UPDATE notification_logs
SET days_relative_to_prediction =
        delivery_local_date
        - predicted_period_date,

    reminder_stage =
        CASE
            WHEN delivery_local_date
                < predicted_period_date
                THEN 'UPCOMING'

            WHEN delivery_local_date
                = predicted_period_date
                THEN 'EXPECTED_TODAY'

            ELSE 'OVERDUE'
        END,

    message_body =
        'bebo cycle reminder'
        || E'\n\n'
        || 'The next period is estimated to start on '
        || TO_CHAR(
            predicted_period_date,
            'FMMonth FMDD, YYYY')
        || '.'
        || E'\n\n'
        || 'This may be a good time to check in and be a little more attentive. '
        || 'Ask how your partner is feeling and whether there is anything you can do to help.'
        || E'\n\n'
        || 'This is only an estimate. Log the new period in bebo when it starts '
        || 'so daily reminders can stop and the next estimate can update.';

ALTER TABLE notification_logs
    ALTER COLUMN delivery_local_date
        SET NOT NULL,

    ALTER COLUMN reminder_stage
        SET NOT NULL,

    ALTER COLUMN days_relative_to_prediction
        SET NOT NULL,

    ALTER COLUMN message_body
        SET NOT NULL;

ALTER TABLE notification_logs
    DROP CONSTRAINT
        uk_notification_logs_dedup;

ALTER TABLE notification_logs
    ADD CONSTRAINT
        uk_notification_logs_daily_dedup
        UNIQUE (
            user_id,
            predicted_period_date,
            delivery_local_date,
            notification_type,
            channel_type
        ),

    ADD CONSTRAINT
        ck_notification_logs_reminder_stage
        CHECK (
            reminder_stage IN (
                'UPCOMING',
                'EXPECTED_TODAY',
                'OVERDUE'
            )
        ),

    ADD CONSTRAINT
        ck_notification_logs_stage_relative_day
        CHECK (
            (
                reminder_stage = 'UPCOMING'
                AND days_relative_to_prediction < 0
            )
            OR
            (
                reminder_stage = 'EXPECTED_TODAY'
                AND days_relative_to_prediction = 0
            )
            OR
            (
                reminder_stage = 'OVERDUE'
                AND days_relative_to_prediction > 0
            )
        );

CREATE INDEX
    idx_notification_logs_daily_lookup
    ON notification_logs (
        user_id,
        delivery_local_date DESC,
        channel_type,
        notification_type
    );
