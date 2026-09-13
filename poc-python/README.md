# POC Python - Entrega 4

Esta carpeta contiene la prueba de concepto de cuatro microservicios Python para la Entrega 4. La primera vertical implementada es la convivencia de contratos V1/V2 del bounded context de Integraciones B2B2C.

## Estado actual

- [x] Decision arquitectonica de interoperabilidad.
- [x] Contratos Avro validos V1, V2 y comando canonico.
- [x] DTO de entrada y traductores ACL V1/V2.
- [ ] Agregado, puertos y caso de uso de Integraciones.
- [ ] Persistencia PostgreSQL de Integraciones.
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

## Verificacion local

Desde `poc-python/services/integraciones`:

```shell
python -m venv .venv
source .venv/bin/activate
pip install -e '.[dev]'
pytest
python -m integraciones
```

Las pruebas contractuales parsean los archivos `.avsc` con `fastavro`. Las pruebas unitarias comprueban la equivalencia semantica V1/V2 y el rechazo de entradas contractuales o semanticamente invalidas.
