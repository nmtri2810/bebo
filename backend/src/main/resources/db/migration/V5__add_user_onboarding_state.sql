ALTER TABLE app_users
    ADD COLUMN onboarding_step VARCHAR(20) NOT NULL DEFAULT 'WELCOME',
    ADD COLUMN onboarding_completed_at TIMESTAMPTZ;

UPDATE app_users user_record
SET onboarding_step = 'COMPLETED',
    onboarding_completed_at = COALESCE(user_record.updated_at, NOW())
WHERE EXISTS (
    SELECT 1
    FROM cycle_records cycle_record
    WHERE cycle_record.user_id = user_record.id
);

ALTER TABLE app_users
    ADD CONSTRAINT ck_app_users_onboarding_step
        CHECK (
            onboarding_step IN (
                'WELCOME',
                'CYCLE',
                'REMINDER',
                'TELEGRAM',
                'COMPLETED'
            )
        ),
    ADD CONSTRAINT ck_app_users_onboarding_completion
        CHECK (
            (
                onboarding_step = 'COMPLETED'
                AND onboarding_completed_at IS NOT NULL
            )
            OR
            (
                onboarding_step <> 'COMPLETED'
                AND onboarding_completed_at IS NULL
            )
        );
