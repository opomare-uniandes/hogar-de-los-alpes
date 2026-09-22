-- Persistencia minima del agregado Trabajo (1-2 tablas, como pide la guia:
-- "no se espera que implemente toda una base de datos inmensa").

CREATE TABLE IF NOT EXISTS trabajo (
    id                  UUID PRIMARY KEY,
    cliente_id          UUID NOT NULL,
    categoria_servicio  VARCHAR(60)  NOT NULL,
    ciudad              VARCHAR(60)  NOT NULL,
    urgencia            VARCHAR(20)  NOT NULL,
    origen              VARCHAR(20)  NOT NULL,
    partner_id          UUID NULL,
    moneda              VARCHAR(3)   NOT NULL DEFAULT 'COP',
    estado              VARCHAR(20)  NOT NULL,
    fecha_creacion      TIMESTAMP    NOT NULL
);

-- Segunda tabla: hitos del flujo del trabajo (entidad interna del agregado Trabajo,
-- ver Entrega 2 - Vista de Informacion). Se deja preparada para futuros escenarios,
-- aunque el comando CrearTrabajo de esta entrega no la use todavia.
CREATE TABLE IF NOT EXISTS hito_flujo (
    id              UUID PRIMARY KEY,
    trabajo_id      UUID NOT NULL REFERENCES trabajo(id),
    descripcion     VARCHAR(120) NOT NULL,
    orden           INT NOT NULL,
    completado      BOOLEAN NOT NULL DEFAULT FALSE
);

-- Outbox transaccional (patron Transactional Outbox): TrabajoRepositoryAdapter inserta aqui
-- los eventos de dominio en la MISMA transaccion que el agregado, para que guardar el estado
-- y registrar el evento pendiente sean atomicos. OutboxRelay (modulo application) lee las filas
-- con publicado_en IS NULL, las envia a Pulsar via PulsarEventPublisherAdapter y las marca.
CREATE TABLE IF NOT EXISTS outbox_evento (
    id              UUID PRIMARY KEY,
    tipo_evento     VARCHAR(100) NOT NULL,
    payload         TEXT NOT NULL,
    ocurrido_en     TIMESTAMP NOT NULL,
    publicado_en    TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_evento_pendiente
    ON outbox_evento (ocurrido_en)
    WHERE publicado_en IS NULL;
