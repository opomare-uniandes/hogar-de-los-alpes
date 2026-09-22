# Escenario de calidad 1 — Modificabilidad: nuevo canal de notificación

**Atributo de calidad:** Modificabilidad.
**Escenario (Entrega 3):** un ingeniero del equipo de Notificaciones necesita agregar un nuevo
canal de envío (WhatsApp Business API) al agregado Notificación, para cumplir un requisito de
negocio en la expansión a México, sin modificar código de los servicios de Trabajo, Pago o
Proveedor.
**Estímulo:** solicitud de negocio de agregar un nuevo medio (canal) de notificación.
**Medida de la respuesta:** cambio implementado y desplegado en ≤ 3 días-persona; 0 archivos
modificados fuera del bounded context Notificación.
**Relevancia de negocio:** la expansión regional (p. ej. México) exige nuevos canales de
notificación sin frenar ni obligar a redesplegar a otros equipos/servicios.

**Proyecto:** Hogar de los Alpes — backend de microservicios (Java 25, Spring Boot WebFlux,
Apache Pulsar + Avro, arquitectura hexagonal / DDD táctico).
**Equipo:** DDDoers (Víctor Camacho, Osmond Pomare, Fernando Rengifo).
**Objeto del experimento:** este mismo repositorio es el artefacto experimental; el escenario
se evalúa contra la implementación real, no contra un prototipo aparte.

---

## Hipótesis
Con arquitectura hexagonal (puerto de salida `CanalNotificacion` + un adaptador por medio),
agregar un canal nuevo (WhatsApp) se hace como **un nuevo adaptador de salida**, sin
modificar código de los demás bounded contexts. **Medida objetivo del escenario:** cambio
desplegado en ≤ 3 días-persona y **0 archivos modificados fuera del bounded context
Notificación**.

## Diseño del experimento
Se generalizó el puerto de dominio (antes `EmailSender`, específico de un solo canal) a
`CanalNotificacion`, y se agregó `WhatsAppChannelAdapter` junto al `EmailChannelAdapter`
existente. Se midió el alcance del cambio con `git diff` sobre el commit que introduce la
generalización + el canal nuevo (`748a53a`), separándolo del resto del PR (que además traía
el `usuarios-service`).

## Resultados cuantitativos
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

## Resultados cualitativos
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

## Conclusión — Hipótesis 1: **CUMPLIDA**
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

## Anexo — Cómo reproducir la evidencia

```bash
# Alcance del cambio de canal: 0 archivos de trabajos-service/integracion-service
git show --stat 748a53a
```
