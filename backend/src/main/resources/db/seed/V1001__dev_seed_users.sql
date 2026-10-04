-- V1001: Dev seed users — only applied with profile dev (classpath:db/seed).
-- Creates one ARRENDATARIO user per tenant from V1000, plus one ADMIN dev user.
-- Passwords are BCrypt strength 10. These are NOT secrets: dev-only fictitious data.
--
-- admin-dev-2024   → {bcrypt}$2a$10$awKVTKI94D/v1oIJectMNe2MHQPsMa16wi.9z4w0svwV.Nt6jDrFa
-- dev-password-123 → {bcrypt}$2a$10$HEu2yAnFau3NyGpMzx1kJuBRD.7XnhkFLLdftDCZFGtehVNdkQkRu

-- Admin dev user (no tenant_id; ADMIN role)
INSERT INTO app_user (username, password_hash, role, tenant_id, enabled, password_changed_at, created_at)
VALUES (
    'admin',
    '{bcrypt}$2a$10$awKVTKI94D/v1oIJectMNe2MHQPsMa16wi.9z4w0svwV.Nt6jDrFa',
    'ADMIN',
    NULL,
    TRUE,
    '2026-07-01T09:00:00Z',
    '2026-07-01T09:00:00Z'
);

-- Arrendatario 1: ana.garcia → tenant María Pérez (unit 'Depto 301')
INSERT INTO app_user (username, password_hash, role, tenant_id, enabled, password_changed_at, created_at)
VALUES (
    'ana.garcia',
    '{bcrypt}$2a$10$HEu2yAnFau3NyGpMzx1kJuBRD.7XnhkFLLdftDCZFGtehVNdkQkRu',
    'ARRENDATARIO',
    (SELECT id FROM tenant WHERE unit = 'Depto 301'),
    TRUE,
    '2026-07-01T09:00:00Z',
    '2026-07-01T09:00:00Z'
);

-- Arrendatario 2: luis.lopez → tenant Carlos Ramos (unit 'Depto 402')
INSERT INTO app_user (username, password_hash, role, tenant_id, enabled, password_changed_at, created_at)
VALUES (
    'luis.lopez',
    '{bcrypt}$2a$10$HEu2yAnFau3NyGpMzx1kJuBRD.7XnhkFLLdftDCZFGtehVNdkQkRu',
    'ARRENDATARIO',
    (SELECT id FROM tenant WHERE unit = 'Depto 302'),
    TRUE,
    '2026-06-01T09:00:00Z',
    '2026-06-01T09:00:00Z'
);

-- Arrendatario 3: pedro.vera → tenant Ana Torres (unit 'Depto 501')
INSERT INTO app_user (username, password_hash, role, tenant_id, enabled, password_changed_at, created_at)
VALUES (
    'pedro.vera',
    '{bcrypt}$2a$10$HEu2yAnFau3NyGpMzx1kJuBRD.7XnhkFLLdftDCZFGtehVNdkQkRu',
    'ARRENDATARIO',
    (SELECT id FROM tenant WHERE unit = 'Depto 201'),
    TRUE,
    '2026-08-01T09:00:00Z',
    '2026-08-01T09:00:00Z'
);
