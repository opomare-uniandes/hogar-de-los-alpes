# Resultados de experimentación — Escenarios de calidad 1, 4 y 7

**Proyecto:** Hogar de los Alpes — backend de microservicios (Java 25, Spring Boot WebFlux,
Apache Pulsar + Avro, arquitectura hexagonal / DDD táctico).
**Equipo:** DDDoers (Víctor Camacho, Osmond Pomare, Fernando Rengifo).
**Objeto del experimento:** este mismo repositorio es el artefacto experimental. Cada
escenario se evalúa contra la implementación real, no contra un prototipo aparte.

Este documento reporta resultados **cuantitativos** y **cualitativos** de la experimentación
sobre los tres escenarios de calidad seleccionados del template de Entrega 3, y concluye si
cada hipótesis se cumplió o no. Los tres escenarios son directamente relevantes al negocio de
HdA: la expansión regional (México/Brasil), los picos de demanda por partner y la
incorporación continua de partners B2B2C —que originan cerca del 70% de los trabajos—.

| # | Atributo | Escenario | Relevancia de negocio |
| --- | --- | --- | --- |
| 1 | Modificabilidad | Nuevo canal de notificación (WhatsApp) sin tocar el resto del sistema | Expansión a México exige nuevos canales sin frenar otros equipos |
| 4 | Escalabilidad | Pico por evento (hasta 4×) escalando de forma aislada el componente afectado | Picos climáticos/estacionales por partner no deben degradar a otros ni al marketplace |
| 7 | Interoperabilidad | Nuevo partner B2B2C con contrato propio traducido a comando canónico | >30 partners con vocabularios distintos; el dominio no puede acoplarse a ninguno |

---

## Escenario 1 — Modificabilidad: nuevo canal de notificación

### Hipótesis
Con arquitectura hexagonal (puerto de salida `CanalNotificacion` + un adaptador por medio),
agregar un canal nuevo (WhatsApp) se hace como **un nuevo adaptador de salida**, sin
modificar código de los demás bounded contexts. **Medida objetivo del escenario:** cambio
desplegado en ≤ 3 días-persona y **0 archivos modificados fuera del bounded context
Notificación**.

### Diseño del experimento
Se generalizó el puerto de dominio (antes `EmailSender`, específico de un solo canal) a
`CanalNotificacion`, y se agregó `WhatsAppChannelAdapter` junto al `EmailChannelAdapter`
existente. Se midió el alcance del cambio con `git diff` sobre el commit que introduce la
generalización + el canal nuevo (`748a53a`), separándolo del resto del PR (que además traía
el `usuarios-service`).

### Resultados cuantitativos
Alcance real del cambio de canal (commit `748a53a`, verificado con `git show --stat`):

| Métrica | Valor medido |
| --- | --- |
| Puerto de dominio agregado | `CanalNotificacion.java` (+17 líneas) |
| Puerto de dominio eliminado | `EmailSender.java` (−10 líneas) |
| Adaptadores concretos | `EmailChannelAdapter.java` (+35), `WhatsAppChannelAdapter.java` (+35) |
| Adaptador viejo eliminado | `SimulatedEmailSenderAdapter.java` (−26) |
| Orquestación afectada | `EnviarNotificacionUseCase.java` (+51/−17, dentro del propio contexto) |
| **Archivos modificados fuera de `notificaciones`** | **0** (verificado: cero archivos bajo `trabajos-service`/`integracion-service`) |
| Archivos nuevos que un tercer canal requeriría | 1 adaptador (`XChannelAdapter`, ~32 líneas) + 1 línea de wiring |

El `WhatsAppChannelAdapter` (32 líneas en su versión actual) es un `@Component` que implementa
la misma interfaz de una sola operación:

```java
public interface CanalNotificacion {
    Mono<NotificacionEnviada> enviar(String sagaId, String trabajoId, String clienteId,
                                     String mensaje, ContactoUsuario contacto);
}
```

### Resultados cualitativos
- **Sin `if/else` por canal en el dominio.** `EnviarNotificacionUseCase` decide qué canales
  disparar leyendo los flags del value object `ContactoUsuario`
  (`notificarPorEmail`/`notificarPorWhatsapp`) y agregando cada envío a un `Flux.merge`. El
  riesgo que el escenario identificaba —"si el equipo cede a agregar if/else por canal dentro
  del núcleo"— **no se materializó**: el núcleo no conoce los canales concretos, solo el
  puerto.
- **El punto de sensibilidad se mantuvo controlado.** El contrato de `CanalNotificacion` se
  redujo a lo mínimo que un canal necesita para enviar y construir la `NotificacionEnviada`;
  agregar WhatsApp no obligó a re-tocar `EmailChannelAdapter`.
- **Efecto colateral positivo (Modificabilidad 1.1):** el mismo puerto permitió que la saga
  de Entrega 5 reutilizara los canales para notificar asignación/fallo sin duplicar código.

### Conclusión — Hipótesis 1: **CUMPLIDA**
Agregar el canal WhatsApp no modificó ni un archivo de `trabajos-service` ni de
`integracion-service` (0 fuera del contexto, contra el objetivo de 0). El canal se
implementó como adaptador nuevo detrás del puerto, y el dominio quedó libre de lógica por
canal. La medida de respuesta del escenario se satisface de forma verificable.

> **Límite conocido / honestidad del resultado:** los adaptadores son *simulados* (solo
> loguean, sin proveedor real de WhatsApp Business API), y **no hay pruebas automatizadas**
> para este contexto; la evidencia es estructural (alcance del diff), no de ejecución. La
> métrica de "días-persona" no se instrumentó formalmente. Para un canal productivo faltaría
> el adaptador real y sus pruebas de contrato, pero eso no cambia la conclusión sobre el
> aislamiento del cambio.

---

## Escenario 4 — Escalabilidad: pico por evento, escalado aislado

### Hipótesis
Autoescalar por **rezago (backlog) de eventos pendientes** —no solo por CPU/memoria— permite
que cada componente escale de forma **independiente y aislada** cuando le llega un pico, sin
consumir la capacidad de otros. **Medida objetivo del escenario:** soportar hasta 4× de
throughput de un partner sin degradar el p95 de otros partners/marketplace.

### Diseño del experimento
La estrategia se implementó con **KEDA** sobre Kubernetes: cada consumidor de eventos/comandos
tiene un `ScaledObject` con triggers de tipo `pulsar` que miden el backlog de *su propia
suscripción*. Los servicios de entrada sin consumo de tópicos (`usuarios-service`) escalan por
CPU con un HPA nativo. El ejercicio de carga documentado inunda `POST /trabajos` con `oha`
para acumular backlog y observar el escalado por servicio.

### Resultados cuantitativos
Configuración real verificada en `deploy/k8s/autoscaling/30-scaledobjects.yaml` y
`31-hpa-entry-api.yaml`:

| Servicio | Tipo de disparador | Umbral | min→max réplicas |
| --- | --- | --- | --- |
| `integracion-service` | backlog Pulsar × 3 tópicos (`trabajo-creado`, `solicitud-trabajo-v1`, `solicitud-trabajo-v2`) | `msgBacklogThreshold: 10` | 1 → 4 |
| `notificaciones-service` | backlog Pulsar (`trabajo-creado`) | 10 | 1 → 4 |
| `proveedor-service` | backlog Pulsar (`reservar-proveedor-command`) | 10 | 1 → 4 |
| `trabajos-service` | **CPU (60%) + backlog** de 3 comandos | 60% / 10 | 1 → 5 |
| `trabajo-saga-service` | backlog de **6 suscripciones** (todo el ciclo de la saga) | 10 | 1 → 6 |
| `usuarios-service` | CPU (HPA nativo) | `averageUtilization: 60` | 1 → 5 |

Parámetros de reacción comunes a los `ScaledObject`: `pollingInterval: 15s`,
`cooldownPeriod: 30s`. El HPA de entrada usa `scaleUp` de +2 pods/30s con ventana de
estabilización 0, y `scaleDown` de −1 pod/60s con ventana de 120s.

### Resultados cualitativos
- **Escalado dirigido por la señal correcta.** El riesgo que planteaba el escenario —"si el
  autoescalado solo mira CPU o memoria, esas métricas se ven normales mientras se acumula
  rezago"— se mitigó explícitamente: los consumidores escalan por `msgBacklogThreshold`, que
  es exactamente el "rezago de eventos pendientes por procesar" del artefacto del escenario.
- **Aislamiento por suscripción.** Cada trigger mira *su propia* suscripción Shared en Pulsar.
  Un pico en las solicitudes de un partner (`solicitud-trabajo-v1/v2`) escala
  `integracion-service` sin tocar las réplicas de `notificaciones` o `proveedor` —el
  "bulkhead" lógico que pedía el escenario—.
- **Suscripciones Shared** (`SubscriptionType.Shared`, confirmado en
  `SolicitudTrabajoPartnerListener`) permiten que varias réplicas consuman el mismo tópico en
  paralelo sin competir con las de otros servicios.
- **Decisión de arranque en 1 réplica (no 0):** documentada como necesaria porque la
  suscripción Shared solo existe en Pulsar cuando el consumidor se conecta; con 0 réplicas el
  admin daría 404 y KEDA no podría escalar (deadlock). Es un tradeoff consciente (costo base
  de 1 pod) a favor de que el autoescalado funcione.

### Conclusión — Hipótesis 4: **CUMPLIDA a nivel de mecanismo; PARCIAL a nivel de medida**
El mecanismo que la hipótesis proponía —autoescalar por backlog de eventos, de forma aislada
por componente— **está implementado y es funcional**: KEDA escala cada consumidor de 1 a 4–6
réplicas según el rezago de su propia suscripción, de forma independiente entre servicios. El
aislamiento por partición/suscripción responde directamente al estímulo del escenario.

> **Límite conocido / honestidad del resultado:** **no se ejecutó una medición formal de p95
> bajo 4× de carga** con y sin el pico, que es la *medida de la respuesta* literal del
> escenario ("4× throughput sin degradar p95 de otros"). Lo verificado es la configuración y
> el procedimiento de carga (`oha`), más la infraestructura de observabilidad (OTLP →
> Grafana/Tempo/Mimir) lista para capturar esas métricas. Por eso la conclusión es *cumplida
> en el diseño y mecanismo, pendiente de la corrida de carga cuantitativa* para cerrar la
> medida numérica. El `maxReplicaCount` de 4 acota el techo de absorción; para un 4× sostenido
> real convendría validar que 4 réplicas bastan o subir el tope.

---

## Escenario 7 — Interoperabilidad: nuevo partner B2B2C con contrato propio

### Hipótesis
Una capa anticorrupción (ACL) en `integracion-service` permite recibir solicitudes de partners
con **identificadores, estructura y vocabulario propios** (incluso en versiones distintas del
contrato), traducirlas a un **comando canónico** y entregarlas al dominio, **sin acoplar
`trabajos-service` a esos contratos**. **Medida objetivo del escenario:** 100% de mensajes
válidos preserva campos obligatorios y significado; 100% de inválidos se rechaza antes del
agregado; 0 cambios en el dominio; 100% de pruebas contractuales aprueba.

### Diseño del experimento
Se modelaron dos versiones del contrato externo en Avro (`SolicitudTrabajoPartnerV1` plano y
`SolicitudTrabajoPartnerV2` con objetos anidados), cada una en su propio tópico y suscripción.
El `SolicitudTrabajoPartnerMapper` (ACL) normaliza ambas al modelo interno
`SolicitudTrabajoPartner`; `TraducirSolicitudPartnerUseCase` traduce los códigos externos
(asistencia, prioridad, ciudad) al lenguaje ubicuo y produce `CrearTrabajoIntegradoCommand`.
Se validó con pruebas automatizadas (JUnit) ejecutadas con Gradle.

### Resultados cuantitativos
Ejecución verificada (`./gradlew :integracion-usecase:test :integracion-pulsar-event-handler:test
--rerun-tasks` → **BUILD SUCCESSFUL**):

| Prueba | Casos | Resultado |
| --- | --- | --- |
| `SolicitudTrabajoPartnerMapperTest` | 1 | ✅ 0 fallos / 0 errores |
| `TraducirSolicitudPartnerUseCaseTest` | 2 | ✅ 0 fallos / 0 errores |
| **Total interoperabilidad** | **3** | **✅ 100% aprobado** |

Cobertura semántica de los casos:
- **Equivalencia de versiones:** V1 y V2 con el mismo significado producen el **mismo** modelo
  interno (mapper) y el **mismo** comando canónico (`assertEquals(desdeV1, desdeV2)`), con
  `PLUMBING→PLOMERIA`, `P1→CRITICA`, `BOG→BOGOTA`.
- **Rechazo de lo inválido:** un código externo sin equivalencia de dominio (`UNKNOWN`) lanza
  `TranslationException` — se rechaza **antes** de tocar el agregado.

Tablas de traducción implementadas (ACL): 3 categorías de servicio, 4 niveles de urgencia
(P1–P4), 4 ciudades (incluida `MEX→CIUDAD_DE_MEXICO`, ya lista para la expansión a México).

### Resultados cualitativos
- **Dominio no acoplado al contrato externo.** El `mapper` "conoce Avro V1/V2, pero el caso de
  uso no"; `trabajos-service` consume solo el `CrearTrabajoCommandV1` canónico vía
  `CrearTrabajoCommandListener`. No importa contratos de partner V1/V2. Esto satisface "0
  cambios en el dominio" del escenario.
- **Convivencia de versiones (backward compat).** V1 y V2 se consumen en paralelo, cada una en
  su suscripción `Shared`; agregar V2 no obligó a retirar V1 —el escenario hermano #8—.
- **Idempotencia en el borde.** La clave `partnerId:externalRequestId` (`claveIdempotencia()`)
  contra Redis evita que un duplicado origine un segundo comando.
- **Rechazo trazable, no reintento infinito.** Un error de traducción se ACK-ea y publica un
  `SolicitudTrabajoRechazada` con `correlationId`, versión, motivo y solicitud original —evita
  el ciclo de reintentos por errores no transitorios y deja rastro para auditoría—.

### Conclusión — Hipótesis 7: **CUMPLIDA**
La ACL traduce dos versiones de contrato externo al mismo comando canónico y protege el
dominio: 3/3 pruebas contractuales aprobadas (100%), equivalencia semántica V1≡V2 demostrada,
rechazo de inválidos antes del agregado, y **cero cambios en `trabajos-service`**. Las cuatro
condiciones de la medida de respuesta del escenario se cumplen a nivel de las pruebas
existentes.

> **Límite conocido / honestidad del resultado:** las pruebas cubren la equivalencia y el
> rechazo por código no soportado, pero **no** cubren aún mensajes sintácticamente
> malformados ni el 100% del catálogo real de >30 partners (el experimento usa 2 versiones de
> un contrato). La idempotencia protege duplicados en el borde de Integración; para entrega
> atómica entre persistencia y publicación ante caída de infraestructura, el refinamiento
> pendiente es un **outbox transaccional** (ya anotado en el doc de interoperabilidad previo).

---

## Conclusión general

| Escenario | Hipótesis | Veredicto | Base de la evidencia |
| --- | --- | --- | --- |
| 1 — Modificabilidad | Canal nuevo = adaptador nuevo, 0 impacto externo | **Cumplida** | `git diff` del cambio: 0 archivos fuera del contexto |
| 4 — Escalabilidad | Autoescalado por backlog, aislado por componente | **Cumplida (mecanismo); parcial (medida p95)** | `ScaledObject`/HPA verificados; falta corrida de carga cuantitativa |
| 7 — Interoperabilidad | ACL traduce V1/V2 a comando canónico sin tocar el dominio | **Cumplida** | 3/3 pruebas JUnit aprobadas; 0 cambios en dominio |

Los tres escenarios son relevantes al negocio (expansión regional, resiliencia ante picos por
partner e incorporación continua de partners B2B2C) y en los tres la decisión arquitectónica
central —puertos/adaptadores, autoescalado por rezago de eventos y ACL con contrato canónico—
respondió al estímulo previsto. Las dos brechas honestas para cerrar por completo las
*medidas* (no las hipótesis) son: (a) la corrida de carga con p95 bajo 4× para el escenario 4,
y (b) ampliar la cobertura de pruebas contractuales a mensajes malformados y más versiones para
el escenario 7.

---

### Anexo — Cómo reproducir la evidencia

```bash
# Escenario 7 (pruebas de interoperabilidad) — desde la raíz del repo
cd backend && ./gradlew :integracion-usecase:test :integracion-pulsar-event-handler:test --rerun-tasks

# Escenario 1 (alcance del cambio de canal)
git show --stat 748a53a        # 0 archivos de trabajos-service/integracion-service

# Escenario 4 (config de autoescalado y prueba de carga)
cat deploy/k8s/autoscaling/30-scaledobjects.yaml
# ...desplegar en Kind/minikube y generar backlog (ver deploy/k8s/README.md):
oha -n 200 -c 50 -m POST -T 'application/json' \
  -d '{"clienteId":"11111111-1111-1111-1111-111111111113","categoriaServicio":"plomeria","urgencia":"ALTA","ciudad":"Bogota","origen":"MARKETPLACE","partnerId":null,"moneda":"COP"}' \
  http://localhost:8081/trabajos
kubectl get scaledobject -n hda -w
```
