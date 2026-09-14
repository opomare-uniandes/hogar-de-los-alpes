# Escenario de calidad 8 — evolución y convivencia de contratos

## Atributo y riesgo

**Interoperabilidad.** Un partner B2B2C evoluciona la solicitud de asistencia de V1 (campos planos) a V2 (objetos anidados), mientras ambos contratos siguen activos. El riesgo es que la versión nueva fuerce cambios en el dominio de Trabajos, rompa solicitudes V1 o genere comandos duplicados.

## Diseño que se valida

`integraciones-service` es una ACL que consume tópicos y esquemas Avro independientes para V1 y V2. Valida el mensaje, lo traduce al mismo `CrearTrabajoCommandV1`, lo persiste de forma idempotente con outbox en PostgreSQL y lo publica al tópico canónico de Trabajos.

Los detalles de las decisiones están en el [ADR 0001](../adr/0001-interoperabilidad-contratos-versionados.md).

## Estímulos

| Caso | Estímulo | Respuesta esperada |
| --- | --- | --- |
| E1 | Publicar una solicitud válida V1. | Un comando canónico Avro publicado y un registro/outbox local. |
| E2 | Publicar una solicitud V2 semánticamente equivalente a E1. | El mismo conjunto de campos canónicos obligatorios, sin modificar Trabajos. |
| E3 | Reenviar E1 con el mismo `(partner_id, external_request_id)`. | Cero agregado y cero mensaje de outbox adicionales. |
| E4 | Enviar un código de asistencia no soportado. | `SolicitudTrabajoRechazadaV1` con causa y correlación; sin comando canónico ni reintento infinito. |

## Ejecución reproducible

Desde `poc-python`, levante la composición. Si el equipo ya usa los puertos por defecto, seleccione puertos alternos:

```shell
POSTGRES_PORT=55432 PULSAR_PORT=56650 PULSAR_ADMIN_PORT=58080 docker compose up --build -d
```

Instale las dependencias de prueba y ejecute el conjunto completo contra esas dependencias reales:

```shell
cd services/integraciones
python -m venv .venv
source .venv/bin/activate
pip install -e '.[dev,persistence,messaging]'
TEST_DATABASE_URL=postgresql+psycopg://hda:hda@localhost:55432/hda_integraciones \
TEST_PULSAR_URL=pulsar://localhost:56650 \
pytest
```

Para limpiar solo los recursos de la prueba:

```shell
cd ../..
POSTGRES_PORT=55432 PULSAR_PORT=56650 PULSAR_ADMIN_PORT=58080 docker compose down -v
```

## Medidas y evidencia

| Medida | Instrumento | Criterio de aceptación |
| --- | --- | --- |
| Procesamiento V1/V2 | `test_pulsar_flow.py` con PostgreSQL y Pulsar reales | 2 de 2 contratos producen `CrearTrabajoCommandV1`. |
| Compatibilidad de esquema | `fastavro` y Schema Registry de Pulsar | Los cuatro `.avsc` son válidos; V1/V2 coexisten en tópicos distintos. |
| Idempotencia | `test_postgres_uow.py` | Una reentrega no crea un segundo agregado ni un segundo outbox. |
| Rechazo trazable | `test_pulsar_adapter.py` | Error semántico genera evento de rechazo y confirma el mensaje solo tras publicarlo. |

**Evidencia obtenida el 13 de septiembre de 2026:** la suite con PostgreSQL 16 y Pulsar 3.3.1 reales completó **21 pruebas exitosas**. Incluye el recorrido E1/E2: tópico V1 o V2 → ACL → transacción PostgreSQL/outbox → tópico canónico Avro.

## Interpretación

El experimento no pretende medir capacidad del broker. Demuestra que una evolución incompatible se aísla en la frontera de Integraciones: coexistir V1 y V2 requiere un consumidor/traductor/contrato adicional, pero no un cambio en el agregado ni en el adaptador de Trabajos. La repetición es segura por la restricción única de PostgreSQL y el outbox evita una transacción distribuida entre base de datos y Pulsar.
