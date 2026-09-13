# POC Python - Entrega 4

Esta carpeta contiene la prueba de concepto de cuatro microservicios Python para la Entrega 4. La primera vertical implementada es la convivencia de contratos V1/V2 del bounded context de Integraciones B2B2C.

## Estado actual

- [x] Decision arquitectonica de interoperabilidad.
- [x] Contratos Avro validos V1, V2 y comando canonico.
- [x] DTO de entrada y traductores ACL V1/V2.
- [x] Agregado, puertos y caso de uso idempotente de Integraciones.
- [x] Persistencia PostgreSQL, idempotencia y outbox transaccional.
- [ ] Consumidores y productor Apache Pulsar.
- [ ] Contenedores de los cuatro servicios.
- [ ] Experimento reproducible y resultados.

La implementacion Java anterior permanece en `backend/` como referencia. La POC exigida en la Entrega 4 se desarrolla en Python dentro de esta carpeta.

## Contratos

| Contrato | Proposito |
| --- | --- |
| `solicitud_trabajo_partner_v1.avsc` | Contrato externo V1 con estructura plana. |
| `solicitud_trabajo_partner_v2.avsc` | Contrato externo V2 con objetos anidados y pais explicito. |
| `crear_trabajo_command_v1.avsc` | Comando canonico dirigido a Trabajos. |

Las decisiones de versionamiento, topicos, idempotencia y trazabilidad se encuentran en `docs/adr/0001-interoperabilidad-contratos-versionados.md`.

## Datos de Integraciones

El bounded context es propietario de la base `hda_integraciones` y no accede a tablas de otros servicios.

| Tabla | Responsabilidad |
| --- | --- |
| `solicitudes_integracion` | Conserva la solicitud aceptada, la version fuente, los identificadores de trazabilidad y el payload canonico. |
| `outbox_messages` | Conserva comandos pendientes de publicacion a Pulsar dentro de la misma transaccion local. |

La restriccion `uq_solicitud_partner_external` sobre `(partner_id, external_request_id)` refuerza en PostgreSQL la idempotencia requerida por la entrega al menos una vez. El esquema reproducible se encuentra en `services/integraciones/migrations/001_init.sql`.

## Verificacion local

Desde `poc-python/services/integraciones`:

```shell
python -m venv .venv
source .venv/bin/activate
pip install -e '.[dev,persistence]'
pytest
python -m integraciones
```

Las pruebas contractuales parsean los archivos `.avsc` con `fastavro`. Las pruebas unitarias comprueban la equivalencia semantica V1/V2, el rechazo de entradas invalidas y que una repeticion no cree otro agregado ni otro mensaje de outbox.

Las pruebas de integración nunca sustituyen PostgreSQL por SQLite. Para ejecutarlas contra una instancia real:

```shell
TEST_DATABASE_URL=postgresql+psycopg://hda:hda@localhost:5432/hda_integraciones pytest
```

Sin `TEST_DATABASE_URL`, esas tres pruebas se omiten explícitamente y las demás continúan ejecutándose.
