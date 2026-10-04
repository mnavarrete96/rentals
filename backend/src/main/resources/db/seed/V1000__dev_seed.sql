-- V1000__dev_seed.sql
-- Semilla de datos para el perfil dev.
-- Carga los 3 arrendatarios de src/js/data.js con los mismos nombres, importes y meses pagados.
-- Solo se aplica en el perfil dev (application-dev.yml incluye classpath:db/seed).
--
-- Fechas absolutas (no now()) para que los tests de paridad sean deterministas.
-- No incluye usuarios app_user (eso lo hace V1001__dev_seed_users.sql en B-06).
-- No incluye usuarios ADMIN (AdminBootstrapRunner los crea desde variables de entorno).
--
-- Correspondencia con src/js/data.js:
--   tenant id 1 → t-001: María Pérez    (AL_DIA:   todos los meses hasta 2026-10 pagados)
--   tenant id 2 → t-002: Carlos Ramos   (ATRASADO: 2026-09 y 2026-10 sin pagar)
--   tenant id 3 → t-003: Ana Torres     (ATRASADO: cargo único "Reparación ventana" pendiente)

DO $$
DECLARE
    t1_id BIGINT;
    t2_id BIGINT;
    t3_id BIGINT;
BEGIN

    -- -------------------------------------------------------------------------
    -- t-001: María Pérez — Al día (todos los meses pagados hasta 2026-10)
    -- -------------------------------------------------------------------------
    INSERT INTO tenant (full_name, unit, contact, payment_day, start_date, monthly_amount, created_at)
    VALUES ('María Pérez', 'Depto 301', 'maria.perez@mail.com', 5,
            '2026-07-01', 450.00, '2026-07-01 09:00:00+00')
    RETURNING id INTO t1_id;

    INSERT INTO paid_month (tenant_id, year_month) VALUES
        (t1_id, '2026-07'),
        (t1_id, '2026-08'),
        (t1_id, '2026-09'),
        (t1_id, '2026-10');

    INSERT INTO one_time_charge (tenant_id, description, amount, paid, created_at) VALUES
        (t1_id, 'Garantía', 450.00, TRUE, '2026-07-01 09:00:00+00');

    -- -------------------------------------------------------------------------
    -- t-002: Carlos Ramos — Atrasado 2 meses (2026-09 y 2026-10 sin pagar)
    -- -------------------------------------------------------------------------
    INSERT INTO tenant (full_name, unit, contact, payment_day, start_date, monthly_amount, created_at)
    VALUES ('Carlos Ramos', 'Depto 302', '+56 9 1234 5678', 10,
            '2026-06-01', 500.00, '2026-06-01 09:00:00+00')
    RETURNING id INTO t2_id;

    INSERT INTO paid_month (tenant_id, year_month) VALUES
        (t2_id, '2026-06'),
        (t2_id, '2026-07'),
        (t2_id, '2026-08');

    INSERT INTO one_time_charge (tenant_id, description, amount, paid, created_at) VALUES
        (t2_id, 'Garantía', 500.00, TRUE, '2026-06-01 09:00:00+00');

    -- -------------------------------------------------------------------------
    -- t-003: Ana Torres — Meses al día, pero cargo "Reparación ventana" pendiente
    -- -------------------------------------------------------------------------
    INSERT INTO tenant (full_name, unit, contact, payment_day, start_date, monthly_amount, created_at)
    VALUES ('Ana Torres', 'Depto 201', 'ana.torres@mail.com', 1,
            '2026-08-01', 400.00, '2026-08-01 09:00:00+00')
    RETURNING id INTO t3_id;

    INSERT INTO paid_month (tenant_id, year_month) VALUES
        (t3_id, '2026-08'),
        (t3_id, '2026-09'),
        (t3_id, '2026-10');

    INSERT INTO one_time_charge (tenant_id, description, amount, paid, created_at) VALUES
        (t3_id, 'Garantía',              400.00, TRUE,  '2026-08-01 09:00:00+00'),
        (t3_id, 'Reparación ventana',    120.00, FALSE, '2026-09-15 10:00:00+00');

END $$;
