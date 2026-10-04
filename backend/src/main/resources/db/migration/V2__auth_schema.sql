-- V2: Authentication schema (ADR-008, ADR-009)
-- app_user: users with roles ADMIN or ARRENDATARIO
-- refresh_token: opaque refresh tokens stored as SHA-256 hash

CREATE TABLE app_user (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username             VARCHAR(50)  NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    role                 VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'ARRENDATARIO')),
    tenant_id            BIGINT       NULL UNIQUE REFERENCES tenant(id) ON DELETE RESTRICT,
    enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    password_changed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_username_lower CHECK (username = lower(username)),
    CONSTRAINT ck_app_user_role_tenant CHECK (
        (role = 'ADMIN'        AND tenant_id IS NULL) OR
        (role = 'ARRENDATARIO' AND tenant_id IS NOT NULL)
    )
);

CREATE TABLE refresh_token (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash  CHAR(64)    NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);
