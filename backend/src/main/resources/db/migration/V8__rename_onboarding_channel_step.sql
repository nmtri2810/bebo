ALTER TABLE app_users
    DROP CONSTRAINT IF EXISTS
        ck_app_users_onboarding_step;

UPDATE app_users
SET onboarding_step = 'CHANNELS'
WHERE onboarding_step = 'TELEGRAM';

ALTER TABLE app_users
    ADD CONSTRAINT
        ck_app_users_onboarding_step
        CHECK (
            onboarding_step IN (
                'WELCOME',
                'CYCLE',
                'REMINDER',
                'CHANNELS',
                'COMPLETED'
            )
        );
