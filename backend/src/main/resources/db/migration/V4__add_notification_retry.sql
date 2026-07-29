ALTER TABLE notification_logs
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_attempt_at TIMESTAMPTZ,
    ADD COLUMN next_retry_at TIMESTAMPTZ;

UPDATE notification_logs
SET attempt_count = 1
WHERE status IN ('SENT', 'FAILED');

ALTER TABLE notification_logs
    ADD CONSTRAINT ck_notification_logs_attempt_count
        CHECK (attempt_count >= 0);

CREATE INDEX idx_notification_logs_due_retry
    ON notification_logs (
        status,
        next_retry_at
    )
    WHERE status = 'FAILED'
      AND next_retry_at IS NOT NULL;
