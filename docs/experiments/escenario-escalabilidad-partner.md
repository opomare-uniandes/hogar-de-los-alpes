# Escenario de calidad 4 — Escalabilidad: pico por evento, escalado aislado por partner

**Atributo de calidad:** Escalabilidad.
**Escenario (Entrega 3):** ante un evento climático u otro que dispara un incremento súbito en
las peticiones de un partner B2B2C, el sistema debe escalar de forma aislada el componente
afectado sin degradar a otros partners ni al marketplace.
**Estímulo:** incremento de hasta 4× en las peticiones entrantes por partner.
**Medida de la respuesta:** el sistema soporta hasta 4× en throughput de un partner sin
degradar el p95 de otros partners/marketplace.
**Relevancia de negocio:** los picos climáticos/estacionales concentrados en un partner no
deben consumir la capacidad de otros partners ni del marketplace; B2B2C origina cerca del 70%
de los trabajos.

Este documento reporta los resultados cuantitativos y cualitativos de la experimentación sobre
este escenario, ejecutada sobre el propio sistema (Hogar de los Alpes) desplegado en un cluster
local de Kubernetes (minikube `hda`) con autoescalado por KEDA.

---

## Hipótesis
Autoescalar por **rezago (backlog) de eventos pendientes** —no solo por CPU/memoria— permite
que cada componente escale de forma **independiente y aislada** cuando le llega un pico, sin
consumir la capacidad de otros. **Medida objetivo del escenario:** soportar hasta 4× de
throughput de un partner sin degradar el p95 de otros partners/marketplace.

## Diseño del experimento
La estrategia se implementó con **KEDA** sobre Kubernetes: cada consumidor de eventos/comandos
tiene un `ScaledObject` con triggers de tipo `pulsar` que miden el backlog de *su propia
suscripción*. Los servicios de entrada sin consumo de tópicos (`usuarios-service`) escalan por
CPU con un HPA nativo. El ejercicio de carga documentado inunda `POST /trabajos` con `oha`
para acumular backlog y observar el escalado por servicio.

**Ajuste clave (arquitectura verificada en el cluster):** el ACL de entrada de partners ya
**no** vive como triggers adicionales dentro de un único `integracion-service`, sino en
**instancias dedicadas del mismo jar, una por partner** — `integracion-service-seguros-los-alpes`
(contrato v1) e `integracion-service-partner-b` (contrato v2) — cada una con **su propio
Deployment, su propio ScaledObject y su propio presupuesto de CPU/memoria**. El
`integracion-service` original quedó solo con el flujo de *salida* (`trabajo-creado` →
partners externos). Este cambio es el que convierte el aislamiento de "lógico" a "físico" (ver
Conclusión).

## Resultados cuantitativos
Configuración real verificada en vivo (`kubectl get scaledobject -n hda`) y en
`deploy/k8s/autoscaling/30-scaledobjects.yaml` / `31-hpa-entry-api.yaml`:

| Servicio (Deployment) | Tipo de disparador | Umbral | min→max réplicas |
| --- | --- | --- | --- |
| `integracion-service` (solo salida) | backlog Pulsar (`trabajo-creado`) | `msgBacklogThreshold: 10` | 1 → 4 |
| `integracion-service-seguros-los-alpes` (partner v1) | backlog Pulsar (`solicitud-trabajo-seguros-los-alpes`) | 10 | 1 → 4 |
| `integracion-service-partner-b` (partner v2) | backlog Pulsar (`solicitud-trabajo-partner-b`) | 10 | 1 → 4 |
| `notificaciones-service` | backlog Pulsar (`trabajo-creado`) | 10 | 1 → 4 |
| `proveedor-service` | backlog Pulsar (`reservar-proveedor-command`) | 10 | 1 → 4 |
| `trabajos-service` | **CPU (60%) + backlog** de 3 comandos | 60% / 10 | 1 → 5 |
| `trabajo-saga-service` | backlog de **6 suscripciones** (todo el ciclo de la saga) | 10 | 1 → 6 |
| `usuarios-service` | CPU (HPA nativo) | `averageUtilization: 60` | 1 → 5 |

Cada instancia por partner reserva su propia capacidad (`requests` 200m CPU / 384Mi;
`limits` 1 CPU / 768Mi), aislada de la del otro partner y de la del flujo de salida.
Parámetros de reacción comunes a los `ScaledObject`: `pollingInterval: 15s`,
`cooldownPeriod: 30s`. El HPA de entrada usa `scaleUp` de +2 pods/30s con ventana de
estabilización 0, y `scaleDown` de −1 pod/60s con ventana de 120s.

Estado verificado en el cluster local (minikube `hda`): los 3 Deployments de integración
corriendo (`integracion-service`, `-seguros-los-alpes`, `-partner-b`) y sus 3 ScaledObjects
independientes registrados en KEDA.

## Resultados cualitativos
- **Escalado dirigido por la señal correcta.** El riesgo que planteaba el escenario —"si el
  autoescalado solo mira CPU o memoria, esas métricas se ven normales mientras se acumula
  rezago"— se mitigó explícitamente: los consumidores escalan por `msgBacklogThreshold`, que
  es exactamente el "rezago de eventos pendientes por procesar" del artefacto del escenario.
- **Aislamiento físico por partner (bulkhead real).** Cada partner de entrada tiene su propio
  Deployment, su propio ScaledObject y su propio presupuesto de CPU/memoria. Un pico de hasta
  4× en `seguros-los-alpes` escala **solo** las réplicas de esa instancia, sin tocar las de
  `partner-b` ni las del flujo de salida (`integracion-service`). Esto responde a la letra del
  estímulo del escenario: *"un partner sobrecargado no consume la capacidad de otro"*.
- **Suscripciones Shared** (`SubscriptionType.Shared`, confirmado en
  `SolicitudTrabajoPartnerListener`) permiten que varias réplicas de una misma instancia
  consuman su tópico en paralelo sin competir con las de otro partner.
- **Decisión de arranque en 1 réplica (no 0):** documentada como necesaria porque la
  suscripción Shared solo existe en Pulsar cuando el consumidor se conecta; con 0 réplicas el
  admin daría 404 y KEDA no podría escalar (deadlock). Es un tradeoff consciente (costo base
  de 1 pod por partner) a favor de que el autoescalado funcione.

## Conclusión — Hipótesis 4: **CUMPLIDA a nivel de mecanismo, con escalado dinámico observado; PARCIAL solo en la medida p95**
El mecanismo que la hipótesis proponía —autoescalar por backlog de eventos, de forma aislada
por componente— **está implementado, es funcional y se observó en ejecución**: KEDA escala
cada consumidor de 1 a 4–6 réplicas según el rezago de su propia suscripción, de forma
independiente entre servicios.

**Cambio respecto a la versión inicial (tras el último ajuste de despliegue):**
el aislamiento entre partners pasó de **lógico/parcial** a **físico/real**. En el diseño
anterior, un único `integracion-service` con varios triggers escalaba, pero sus réplicas eran
**compartidas** entre los flujos de todos los partners y el de salida — un pico en un partner
sí podía consumir capacidad destinada a otro, lo que **no** satisfacía la letra del escenario.
Con las instancias dedicadas por partner (Deployment + ScaledObject + recursos propios) el
requisito *"un partner sobrecargado no consume la capacidad de otro"* ahora se cumple de forma
verificable. Esta conclusión **se fortalece**, no se debilita, con los ajustes.

**Corrida de carga ejecutada (base + 4×, en el cluster local minikube `hda`).** Se generó
carga publicando solicitudes Avro directamente en el tópico de cada partner (el flujo de
entrada es asíncrono por Pulsar, no HTTP; ver Anexo). Con `seguros-los-alpes` recibiendo ~4×
la tasa de `partner-b` de forma sostenida, KEDA escaló de forma **asimétrica e independiente**:

| Instancia | Carga aplicada | Réplicas alcanzadas (min→observado) |
| --- | --- | --- |
| `integracion-service-seguros-los-alpes` | pico 4× (≈500 msg/s sostenido) | 1 → **4** (su máximo) |
| `integracion-service-partner-b` | baseline (≈125 msg/s) | 1 → **2** |

El partner bajo el pico escaló a su máximo mientras el partner en baseline se quedó en 2
réplicas: cada uno reaccionó a **su propio** backlog, sin que el pico de uno arrastrara al
otro a escalar. Esto demuestra el escalado dinámico *y* el aislamiento en ejecución, no solo
en la configuración.

> **Hallazgo del experimento (bug de despliegue corregido).** Las primeras corridas medían 0
> backlog de forma engañosa: los pods desplegados corrían una **imagen stale** cuyo listener se
> suscribía a los tópicos compartidos `solicitud-trabajo-v1/v2` en vez del tópico dedicado por
> partner (`solicitud-trabajo-seguros-los-alpes` / `-partner-b`) que sí define el código
> actual. La carga se publicaba a los tópicos per-partner que **nadie** consumía, por eso no
> había backlog ni escalado. Tras redeployar las tres instancias de `integracion` con la imagen
> del código actual, cada una quedó suscrita a **su** tópico y la corrida arrojó el escalado
> asimétrico de la tabla. Es la misma clase de caché de imágenes de minikube que afectó a
> `proveedor-service`; conviene versionar el tag de imagen en cada cambio para evitarlo.

> **Límite conocido / honestidad del resultado:** lo que **sí** se midió es el escalado por
> réplicas (backlog → réplicas, aislado por partner). Lo que **no** se midió es un **p95 de
> latencia** con y sin el pico — la *medida de la respuesta* literal del escenario ("4×
> throughput sin degradar p95 de otros"). En este entorno local esa medida es además difícil de
> capturar de forma limpia por dos razones observadas: (1) la traducción del ACL es tan liviana
> que un solo replica drena burst grandes en segundos, por debajo del `pollingInterval: 15s` de
> KEDA, así que el backlog debe **sostenerse** para disparar el escalado; y (2) el
> `pulsar-admin topics stats` se satura bajo carga alta y sus lecturas de backlog se vuelven
> intermitentes. Para cerrar la medida p95 numérica convendría instrumentar la latencia
> extremo-a-extremo (solicitud publicada → `CrearTrabajoCommand` emitido) vía las trazas OTLP ya
> disponibles, en vez de derivarla del backlog. El `maxReplicaCount` de 4 por instancia acota el
> techo de absorción; para un 4× sostenido mayor convendría subir el tope.

---

## Anexo — Cómo reproducir la evidencia

```bash
# Config de autoescalado, aislamiento por partner y estado en vivo
cat deploy/k8s/autoscaling/30-scaledobjects.yaml
kubectl get deploy -n hda | grep integracion   # 3 Deployments: salida + 2 por partner
kubectl get scaledobject -n hda                 # 1 ScaledObject por cada uno

# Generar backlog en el flujo general (POST /trabajos -> trabajo-creado) y observar el escalado:
oha -n 200 -c 50 -m POST -T 'application/json' \
  -d '{"clienteId":"11111111-1111-1111-1111-111111111113","categoriaServicio":"plomeria","urgencia":"ALTA","ciudad":"Bogota","origen":"MARKETPLACE","partnerId":null,"moneda":"COP"}' \
  http://localhost:8081/trabajos
kubectl get scaledobject -n hda -w

# Carga base + 4x por partner (flujo de ENTRADA, asincrono por Pulsar, no HTTP). El generador
# vive en integracion-service (PartnerContractDemoPublisher), activado por env vars, publicando
# HDA_DEMO_PARTNER_COUNT solicitudes Avro a HDA_DEMO_PARTNER_RATE msg/s en el topico del partner.
# Se corre como pod efimero con la imagen del servicio, con inbound/outbound desactivados para
# que SOLO publique (no consuma). Pico 4x a seguros (500 msg/s) vs baseline partner-b (125 msg/s):
kubectl run spike-seg -n hda --image=hda/integracion-service:local --restart=Never \
  --env=HDA_DEMO_PARTNER_ENABLED=true --env=HDA_INTEGRACION_OUTBOUND_ENABLED=false \
  --env=HDA_INTEGRACION_INBOUND_PARTNER_ENABLED=false --env=PARTNER_CONTRACT_VERSION=v1 \
  --env=PARTNER_TOPIC=persistent://hda/partner/solicitud-trabajo-seguros-los-alpes \
  --env=PARTNER_ID=11111111-1111-1111-1111-111111111111 \
  --env=HDA_DEMO_PARTNER_COUNT=30000 --env=HDA_DEMO_PARTNER_RATE=500 \
  --env=PULSAR_SERVICE_URL=pulsar://pulsar.hda.svc.cluster.local:6650 \
  --env=PULSAR_ADMIN_URL=http://pulsar.hda.svc.cluster.local:8080 \
  --env=REDIS_HOST=redis --env=REDIS_PORT=6379

# (repetir con PARTNER_TOPIC=.../solicitud-trabajo-partner-b, PARTNER_CONTRACT_VERSION=v2,
#  HDA_DEMO_PARTNER_COUNT=7500, HDA_DEMO_PARTNER_RATE=125 para el baseline del otro partner)

# Observar el escalado asimetrico: seguros llega a su max (4) y partner-b se queda en ~2.
watch -n2 'kubectl get deploy -n hda | grep -E "seguros|partner-b"'
```

> Importante: las 3 instancias de `integracion` deben correr la imagen del **código actual**
> (listener per-partner que se suscribe al `PARTNER_TOPIC` configurado). Una imagen stale se
> suscribe a los tópicos compartidos `v1/v2` y la carga per-partner no llega a ningún
> consumidor (0 backlog engañoso). Verificar con:
> `kubectl logs -n hda deploy/integracion-service-seguros-los-alpes | grep "Subscribed to topic"`
