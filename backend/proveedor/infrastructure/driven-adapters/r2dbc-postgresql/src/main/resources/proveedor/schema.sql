-- Persistencia minima del agregado Proveedor (1 tabla, mismo criterio que trabajo/usuario:
-- "no se espera implementar toda una base de datos inmensa").
--
-- Nota: este archivo vive en resources/proveedor/schema.sql (no en la raiz del classpath
-- como "schema.sql"), mismo motivo que usuarios-service (ver su schema.sql): evita que
-- Spring Boot ejecute por error el schema.sql de otro servicio via classpath*:schema.sql.
-- Ver spring.sql.init.schema-locations en application.yml, que apunta aqui explicitamente.

CREATE TABLE IF NOT EXISTS proveedor (
    id                  UUID PRIMARY KEY,
    nombre              VARCHAR(120) NOT NULL,
    categoria_servicio  VARCHAR(60)  NOT NULL,
    ciudad              VARCHAR(60)  NOT NULL,
    disponible          BOOLEAN NOT NULL DEFAULT TRUE,
    saga_id_reserva     UUID NULL
);

-- Semilla determinista (seccion 5.2 del plan): Proveedor X (plomeria/Bogota) y
-- Proveedor Y (electricidad/Bogota). Deliberadamente NO hay ningun proveedor de
-- plomeria en Medellin - eso es lo que dispara el camino de compensacion de la demo.
INSERT INTO proveedor (id, nombre, categoria_servicio, ciudad, disponible, saga_id_reserva)
VALUES
    ('22222222-2222-2222-2222-222222222221', 'Proveedor Plomeros&Plomeros', 'plomeria', 'Bogota', TRUE, NULL),
    ('22222222-2222-2222-2222-222222222222', 'Proveedor Eléctricos de la Sabana', 'electricidad', 'Bogota', TRUE, NULL)
ON CONFLICT (id) DO NOTHING;

-- Outbox transaccional (patron Transactional Outbox): ProveedorRepositoryAdapter inserta aqui
-- los eventos de dominio en la MISMA transaccion que el agregado (guardar(), usado por
-- LiberarProveedorUseCase). ReservarProveedorUseCase no muta un agregado propio en todos sus
-- caminos (reservarSiDisponible es un UPDATE atomico aparte, y NoDisponible no cambia estado),
-- asi que encola sus eventos con encolarEventoPendiente() -- un INSERT aislado, tambien durable,
-- pero sin otra escritura con la que compartir transaccion. OutboxRelay (modulo application) lee
-- publicado_en IS NULL y los envia a Pulsar via PulsarEventPublisherAdapter.
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
