# ADR 0001: Contratos versionados para integraciones B2B2C

- Estado: aceptada
- Fecha: 2026-09-13
- Escenario de calidad: Interoperabilidad #8 - Evolucion y convivencia de contratos

## Contexto

Hogar de los Alpes debe recibir solicitudes de partners B2B2C cuyos contratos evolucionan a ritmos distintos. Un partner puede publicar una version V2 mientras V1 continua activa. El lenguaje externo no debe propagarse al agregado `Trabajo` ni exigir despliegues coordinados de los otros servicios.

La entrega parcial exige comunicacion por comandos y eventos, Apache Pulsar, esquemas explicitamente definidos, una estrategia de evolucion y persistencia propia por microservicio.

## Decision

Se implementara un microservicio `integraciones-service` en Python que actuara como capa anticorrupcion (ACL):

1. Consumira solicitudes de integracion V1 y V2 desde topicos independientes.
2. Validara el contrato y las reglas semanticas antes de construir objetos del dominio interno.
3. Traducira ambas versiones a `CrearTrabajoCommandV1`, el contrato canonico que consume `trabajos-service`.
4. Persistira la recepcion, version, resultado y trazabilidad en una base de datos propia.
5. Registrara el comando en un outbox transaccional y lo publicara por Apache Pulsar.

Los contratos externos y el comando canonico usan un sobre basado en los atributos principales de CloudEvents: `specversion`, `id`, `source`, `type`, `subject`, `time`, `datacontenttype`, `dataschema` y `correlationId`.

## Clasificacion de mensajes

| Mensaje | Tipo | Justificacion |
| --- | --- | --- |
| `SolicitudTrabajoPartnerV1` | Evento de integracion | Representa una solicitud emitida fuera del bounded context y conserva el vocabulario del partner. |
| `SolicitudTrabajoPartnerV2` | Evento de integracion | Es una nueva version mayor del contrato externo con estructura diferente. |
| `CrearTrabajoCommandV1` | Comando | Solicita una accion concreta al servicio de Trabajos usando el lenguaje canonico de Hogar de los Alpes. |
| `TrabajoCreado` | Evento de dominio/integracion publicado | Informa un hecho ocurrido despues de que Trabajos acepte el comando. |

## Topicos

| Contrato | Topico Pulsar |
| --- | --- |
| `SolicitudTrabajoPartnerV1` | `persistent://hda/integracion/solicitud-trabajo-v1` |
| `SolicitudTrabajoPartnerV2` | `persistent://hda/integracion/solicitud-trabajo-v2` |
| `CrearTrabajoCommandV1` | `persistent://hda/trabajos/comandos/crear-trabajo-v1` |
| Solicitud rechazada | `persistent://hda/integracion/solicitud-trabajo-rechazada-v1` |

## Politica de evolucion

- Los cambios compatibles dentro de una version mayor conservan el topico y deben cumplir compatibilidad `FULL_TRANSITIVE` en Pulsar Schema Registry.
- Todo campo nuevo dentro de una version existente debe ser opcional o declarar un valor por defecto.
- Un cambio estructural incompatible crea una version mayor, un nuevo esquema y un nuevo topico.
- V1 y V2 pueden operar simultaneamente durante la ventana de migracion.
- Retirar una version requiere medir que ya no recibe trafico y comunicar la fecha de retiro al partner.
- Una version no soportada se rechaza antes de publicar el comando y queda registrada con su causa.

## Modelo de entrega e idempotencia

Pulsar ofrece entrega al menos una vez. El servicio usara `(partner_id, external_request_id)` como clave unica de negocio. Una repeticion devolvera el resultado previamente registrado y no creara un segundo comando. `correlationId` conectara la solicitud externa, el registro local, el comando canonico y el evento resultante.

La solicitud y el comando pendiente se guardaran en una misma transaccion PostgreSQL. Un publicador de outbox enviara el comando a Pulsar y marcara el registro como publicado. Asi, un fallo entre la escritura local y la publicacion no pierde el comando ni obliga a una transaccion distribuida.

El mensaje de entrada se confirmara (`ack`) despues de confirmar la transaccion local. Los fallos transitorios usaran reintentos acotados; los fallos contractuales o semanticos se registraran y confirmaran sin reintentarse indefinidamente.

## Topologia de datos

Se adopta una topologia descentralizada. `integraciones-service` sera propietario de `hda_integraciones`; no consultara ni escribira las tablas de Trabajos, Usuarios o Notificaciones.

La base contiene `solicitudes_integracion`, con una clave unica de negocio sobre `(partner_id, external_request_id)`, y `outbox_messages`, enlazada al agregado mediante `aggregate_id`. La primera conserva `source_event_id`, `source_contract_version`, `correlation_id` y el payload canonico; la segunda conserva el sobre Avro que debe publicarse y su estado operativo.

## Criterios de aceptacion del experimento

- El 100 % de los mensajes validos de la muestra V1 y V2 es procesado.
- El 100 % de los pares V1/V2 semanticamente equivalentes produce los mismos campos canonicos obligatorios.
- El 100 % de las versiones no soportadas de la muestra es rechazado con trazabilidad.
- Agregar el traductor V2 requiere cero cambios en el dominio de Trabajos y en adaptadores existentes.
- El 100 % de las pruebas contractuales definidas para la POC aprueba.
- Una solicitud duplicada produce cero efectos adicionales.

## Consecuencias y trade-offs

### Positivas

- El modelo de dominio queda protegido del vocabulario y de la evolucion de cada partner.
- V1 y V2 pueden desplegarse y retirarse sin coordinar cambios en Trabajos.
- La trazabilidad permite auditar equivalencias, rechazos y duplicados.

### Costos

- Cada version mayor necesita consumidor, traductor, esquema y pruebas contractuales propios.
- Los topicos versionados aumentan el inventario operativo.
- `FULL_TRANSITIVE` restringe cambios dentro de una version mayor, intencionalmente.

## Fuera del alcance de la entrega parcial

- Coordinacion de Sagas.
- Backend for Frontend.
- Service Mesh.
- Implementacion funcional completa de los cuatro bounded contexts.
