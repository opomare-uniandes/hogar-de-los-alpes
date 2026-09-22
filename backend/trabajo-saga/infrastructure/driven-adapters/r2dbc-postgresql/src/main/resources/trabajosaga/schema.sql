-- Saga Log de "Asignacion de trabajo con proveedor" (seccion 4.1 del plan).
--
-- Nota: este archivo vive en resources/trabajosaga/schema.sql (no en la raiz del classpath
-- como "schema.sql"), mismo motivo que usuarios-service/proveedor-service (ver sus schema.sql).
-- Ver spring.sql.init.schema-locations en application.yml, que apunta aqui explicitamente.
--
-- Desviacion deliberada de la seccion 4.1 del plan: se agrega cliente_id (no estaba en el
-- DDL original). Es necesario porque NotificarAsignacionCommandV1/NotificarFalloCommandV1
-- necesitan el clienteId del Trabajo, y el unico momento en que la saga lo conoce es en el
-- evento TrabajoCreado inicial - no vuelve a aparecer en ningun evento posterior (TrabajoAsignado/
-- TrabajoCancelado solo llevan sagaId/trabajoId/estado). Guardarlo aqui evita tener que agregarle
-- clienteId a esos eventos solo para uso interno del orquestador (rompería la regla de la seccion
-- 2.3: los participantes no deberian cargar campos que solo le sirven a la saga).
CREATE TABLE IF NOT EXISTS saga_trabajo (
    id              UUID PRIMARY KEY,
    trabajo_id      UUID NOT NULL,
    cliente_id      UUID NOT NULL,
    estado          VARCHAR(30) NOT NULL,       -- INICIADA, PROVEEDOR_RESERVADO, ASIGNADA, COMPENSANDO, CANCELADA, COMPLETADA
    fecha_inicio    TIMESTAMP NOT NULL,
    fecha_fin       TIMESTAMP,
    UNIQUE (trabajo_id)                          -- backstop de idempotencia de TrabajoCreado (a nivel de BD, ademas del chequeo en el caso de uso)
);

CREATE TABLE IF NOT EXISTS saga_paso (
    id              UUID PRIMARY KEY,
    saga_id         UUID NOT NULL REFERENCES saga_trabajo(id),
    paso            VARCHAR(50) NOT NULL,        -- RESERVAR_PROVEEDOR, ASIGNAR_TRABAJO, NOTIFICAR,
                                                  -- COMPENSAR_CANCELAR_TRABAJO, NOTIFICAR_FALLO
    resultado       VARCHAR(10) NOT NULL,        -- OK, FALLO
    detalle         TEXT,
    ocurrido_en     TIMESTAMP NOT NULL,
    UNIQUE (saga_id, paso)                       -- soporta el chequeo de idempotencia de la seccion 3
);

-- Outbox transaccional (patron Transactional Outbox): SagaTrabajoRepositoryAdapter.guardar()
-- inserta aqui los eventos de dominio en la MISMA transaccion que saga_trabajo/saga_paso, para
-- que avanzar el estado de la saga y registrar el comando de salida pendiente sean atomicos.
-- OutboxRelay (modulo application) lee publicado_en IS NULL y los envia a Pulsar.
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
