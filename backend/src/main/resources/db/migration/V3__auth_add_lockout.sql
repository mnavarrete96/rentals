-- V3: Brute-force lockout columns for app_user (D-6, approved 2026-10-04)
-- failed_attempts: count of consecutive failed login attempts; resets on success or when account locks
-- locked_until: if non-null and in the future, login is denied regardless of password

ALTER TABLE app_user
    ADD COLUMN failed_attempts SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until    TIMESTAMPTZ;
