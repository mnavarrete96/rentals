-- V1: esquema de negocio inicial
-- Siguiendo ARCHITECTURE §5.1 exactamente.
-- Nota: status del arrendatario (AL_DIA/ATRASADO) se calcula en runtime (ADR-006), no se persiste.

CREATE TABLE tenant (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name       VARCHAR(120)  NOT NULL,
    unit            VARCHAR(40)   NOT NULL,
    contact         VARCHAR(120)  NOT NULL,
    payment_day     INTEGER       NOT NULL CHECK (payment_day BETWEEN 1 AND 28),
    start_date      DATE          NOT NULL,
    monthly_amount  NUMERIC(10,2) NOT NULL CHECK (monthly_amount >= 0),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE paid_month (
    tenant_id   BIGINT  NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    year_month  VARCHAR(7) NOT NULL CHECK (year_month ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    PRIMARY KEY (tenant_id, year_month)
);

CREATE TABLE one_time_charge (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id    BIGINT        NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    description  VARCHAR(200)  NOT NULL,
    amount       NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    paid         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_one_time_charge_tenant ON one_time_charge(tenant_id);
