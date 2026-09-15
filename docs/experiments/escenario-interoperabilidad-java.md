# Escenario de calidad: interoperabilidad de contratos B2B2C

## Hipotesis

Hogar de los Alpes puede recibir solicitudes de asistencia de partners que usan versiones distintas de su contrato sin acoplar el dominio de `trabajos-service` a esos contratos. `integracion-service` actua como capa anticorrupcion (ACL): conserva el vocabulario externo en el borde, lo normaliza a un comando canonico y publica el resultado para que Trabajos lo procese.

## Alcance implementado

| Elemento | Implementacion |
| --- | --- |
| Contratos externos | Avro `SolicitudTrabajoPartnerV1` y `SolicitudTrabajoPartnerV2` en topicos separados. V2 reorganiza los mismos datos en objetos anidados. |
| ACL | `SolicitudTrabajoPartnerMapper` adapta ambas versiones a `SolicitudTrabajoPartner`; `TraducirSolicitudPartnerUseCase` valida y traduce codigos de asistencia, prioridad y ciudad. |
| Contrato interno | Avro `CrearTrabajoCommandV1`, publicado en `persistent://hda/trabajos/crear-trabajo-command`. |
| Consumidor de destino | `CrearTrabajoCommandListener` en `trabajos-service`, que convierte el comando canonico al caso de uso existente y crea el agregado `Trabajo`. |
| Version invalida/semantica invalida | Se confirma el mensaje y se publica `SolicitudTrabajoRechazadaV1` con `correlationId`, version, motivo y solicitud original; asi se evita reintentos infinitos por errores no transitorios. |
| Duplicados | `integracion-service` usa Redis con la clave `partnerId:externalRequestId`; una solicitud ya procesada no vuelve a originar un comando. |

La topologia es asincrona y basada en comandos/eventos: un partner no llama directamente a Trabajos ni conoce su modelo de dominio.

```mermaid
flowchart LR
    P1["Partner V1"] -->|"solicitud-trabajo-v1"| ACL["integracion-service / ACL"]
    P2["Partner V2"] -->|"solicitud-trabajo-v2"| ACL
    ACL -->|"CrearTrabajoCommandV1"| T["trabajos-service"]
    ACL -->|"SolicitudTrabajoRechazadaV1"| R["Tópico de rechazo"]
    ACL <--> D[(Redis: idempotencia)]
    T --> DB[(PostgreSQL: agregados Trabajo)]
```

## Como ejecutar la evidencia local

Desde la raiz del repositorio:

```bash
cd deploy/docker-compose
HDA_DEMO_PARTNER_ENABLED=true docker compose up -d --build
docker compose logs --follow integracion-service trabajos-service
```

La propiedad activa `PartnerContractDemoPublisher` **solo para la demostracion**. El publicador emite una solicitud V1 (`PLUMBING`, `P1`, `BOG`) y otra V2 (`ELECTRICAL`, `P2`, `MDE`). En los logs debe observarse que Integracion las consume y que Trabajos procesa los comandos resultantes.

Para verificar que los dos contratos llegaron y que Trabajos persistio los agregados:

```bash
docker compose exec pulsar bin/pulsar-admin topics stats persistent://hda/partner/solicitud-trabajo-v1
docker compose exec pulsar bin/pulsar-admin topics stats persistent://hda/partner/solicitud-trabajo-v2
docker compose exec postgres psql -U hda -d hda_trabajos -c 'SELECT id, categoria_servicio, urgencia, ciudad, origen FROM trabajo ORDER BY fecha_creacion DESC LIMIT 5;'
```

Para volver a una ejecucion normal, apague el entorno y levantelo sin `HDA_DEMO_PARTNER_ENABLED=true`:

```bash
docker compose down
docker compose up -d --build
```

## Evidencia automatizada

Los tests de `SolicitudTrabajoPartnerMapperTest` prueban que V1 y V2 con el mismo significado se convierten al mismo modelo interno. `TraducirSolicitudPartnerUseCaseTest` prueba las traducciones y el rechazo de codigos no soportados:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25 PATH=/opt/homebrew/opt/openjdk@25/bin:$PATH \
  backend/gradlew -p backend :integracion-usecase:test :integracion-pulsar-event-handler:test
```

## Criterio de exito y limite conocido

El escenario se considera exitoso si cada version externa se consume, se traduce al contrato canonico y da lugar a un agregado `Trabajo`, sin que `trabajos-service` importe contratos de partners V1/V2. Un contrato con codigos no soportados debe terminar como evento de rechazo trazable, no como reintento infinito.

La idempotencia actual protege duplicados en el borde de Integracion. Para garantizar entrega atomica entre persistencia y publicacion ante una caida de infraestructura, el siguiente refinamiento es implementar un **outbox transaccional** en el servicio que persiste el agregado.
