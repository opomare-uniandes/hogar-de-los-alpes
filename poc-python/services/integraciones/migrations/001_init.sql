CREATE TABLE IF NOT EXISTS solicitudes_integracion (
    id UUID PRIMARY KEY,
    partner_id VARCHAR(80) NOT NULL,
    external_request_id VARCHAR(120) NOT NULL,
    correlation_id UUID NOT NULL,
    source_event_id UUID NOT NULL,
    source_contract_version VARCHAR(20) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    canonical_payload JSONB NOT NULL,
    recibida_en_ms BIGINT NOT NULL,
    CONSTRAINT uq_solicitud_partner_external
        UNIQUE (partner_id, external_request_id)
);

CREATE INDEX IF NOT EXISTS ix_solicitudes_correlation_id
    ON solicitudes_integracion (correlation_id);

CREATE TABLE IF NOT EXISTS outbox_messages (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL
        REFERENCES solicitudes_integracion(id),
    topic VARCHAR(255) NOT NULL,
    event_type VARCHAR(160) NOT NULL,
    correlation_id UUID NOT NULL,
    payload JSONB NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INTEGER NOT NULL DEFAULT 0,
    creado_en_ms BIGINT NOT NULL,
    publicado_en_ms BIGINT NULL,
    ultimo_error TEXT NULL
);

CREATE INDEX IF NOT EXISTS ix_outbox_pending
    ON outbox_messages (estado, creado_en_ms)
    WHERE estado = 'PENDIENTE';
