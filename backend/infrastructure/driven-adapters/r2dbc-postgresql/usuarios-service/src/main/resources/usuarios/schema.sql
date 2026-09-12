-- Persistencia minima del agregado Usuario (1 tabla, ver schema.sql de trabajos-service
-- para el mismo criterio: "no se espera implementar toda una base de datos inmensa").
--
-- Nota: este archivo vive en resources/usuarios/schema.sql (no en la raiz del classpath
-- como "schema.sql") a proposito. Spring Boot busca "schema.sql" con el patron
-- classpath*:schema.sql, que recorre TODOS los jars del classpath combinado de
-- applications/ (todos los servicios comparten ese classpath, solo cambia la Main-Class
-- del boot jar). Si este archivo se llamara igual que el de trabajos-service, cualquiera
-- de los dos servicios terminaria ejecutando el schema.sql del otro contra su propia base.
-- Ver spring.sql.init.schema-locations en application-usuarios.yml, que apunta
-- explicitamente aqui y evita ese cruce en ambas direcciones.

CREATE TABLE IF NOT EXISTS usuario (
    id                       UUID PRIMARY KEY,
    nombre                   VARCHAR(120) NOT NULL,
    correo                   VARCHAR(160) NOT NULL,
    celular                  VARCHAR(30)  NOT NULL,
    notificar_por_email      BOOLEAN NOT NULL DEFAULT FALSE,
    notificar_por_whatsapp   BOOLEAN NOT NULL DEFAULT FALSE
);

-- 3 usuarios semilla con UUID fijo y conocido, para probar a mano los 3 casos de canal
-- (usarlos como clienteId al hacer POST /trabajos durante las pruebas end-to-end).
INSERT INTO usuario (id, nombre, correo, celular, notificar_por_email, notificar_por_whatsapp)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Usuario A (solo email)',    'usuarioA@hda.test', '+57 300 0000001', TRUE,  FALSE),
    ('22222222-2222-2222-2222-222222222222', 'Usuario B (solo WhatsApp)', 'usuarioB@hda.test', '+57 300 0000002', FALSE, TRUE),
    ('33333333-3333-3333-3333-333333333333', 'Usuario C (ambos canales)', 'usuarioC@hda.test', '+57 300 0000003', TRUE,  TRUE)
ON CONFLICT (id) DO NOTHING;
