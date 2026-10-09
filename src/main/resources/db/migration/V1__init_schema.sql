-- Aktifkan ekstensi UUID untuk PostgreSQL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabel baseline verifikasi koneksi
CREATE TABLE IF NOT EXISTS schema_baseline (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL
);

INSERT INTO schema_baseline (status) VALUES ('INITIALIZED');
