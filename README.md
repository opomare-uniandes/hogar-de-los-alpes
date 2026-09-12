# Hogar de los Alpes — Backend de microservicios (Entrega 4)

Proyecto multi-módulo Gradle (Java 25) que implementa una porción mínima de 4 agregados
(`Trabajo`, `Usuario`, y los traductores de `integracion-service`/`notificaciones-service`),
siguiendo DDD táctico y arquitectura hexagonal, con **Spring Boot WebFlux** (reactivo),
**Apache Pulsar + Avro** como bus de eventos, y **una sola excepción síncrona explícita**
(`notificaciones-service → usuarios-service` por HTTP, una consulta de solo lectura).

## Portal web

El repositorio incluye en `frontend` una aplicación React + TypeScript que permite
crear trabajos y consultarlos por su identificador. Durante el desarrollo, Vite redirige
`/api` a `trabajos-service` en el puerto `8081`.

```shell
cd frontend
npm install
npm run dev
```

La guía completa de ejecución, pruebas y configuración está en
[`frontend/README.md`](frontend/README.md).

## Arquitectura (vista de componentes)

Vista de alto nivel en tiempo de ejecución

```mermaid
graph LR
    Cliente(["Cliente / API consumer"])
    Externos(["Partners externos<br/>(p. ej. Seguros de los Alpes)"])
    Email(["Email (simulado)"])
    WhatsApp(["WhatsApp (simulado)"])

    Trabajos["trabajos-service :8081<br/>REST · WebFlux"]
    Integracion["integracion-service :8082"]
    Notificaciones["notificaciones-service :8083"]
    Usuarios["usuarios-service :8084<br/>REST · WebFlux"]

    DBTrabajos[("Postgres<br/>hda_trabajos")]
    DBUsuarios[("Postgres<br/>hda_usuarios")]

    subgraph Pulsar["Apache Pulsar"]
        TopicCreado{{"trabajo-creado<br/>evento de dominio"}}
        TopicSiniestro{{"trabajo-siniestro-creado<br/>evento de integración v1"}}
        TopicNotificacion{{"notificacion-enviada<br/>evento de integración"}}
    end

    Cliente -->|"POST / GET /trabajos"| Trabajos
    Trabajos -->|R2DBC| DBTrabajos
    Trabajos -->|publica| TopicCreado
    TopicCreado -->|"consume (Shared)"| Integracion
    TopicCreado -->|"consume (Shared)"| Notificaciones
    Integracion -->|publica| TopicSiniestro
    TopicSiniestro -->|consume| Externos
    Notificaciones -->|"GET /usuarios/{id}/contacto<br/>(único síncrono, HTTP)"| Usuarios
    Usuarios -->|R2DBC| DBUsuarios
    Notificaciones -->|"envía (simulado)"| Email
    Notificaciones -->|"envía (simulado)"| WhatsApp
    Notificaciones -->|publica| TopicNotificacion

    classDef servicio fill:#cfe8ff,stroke:#4a90d9,color:#000000;
    classDef infra fill:#ffe8b3,stroke:#d9a441,color:#000000;
    classDef topic fill:#d9f2d9,stroke:#4aa64a,color:#000000;
    classDef externo fill:#f0f0f0,stroke:#999999,color:#000000;

    class Trabajos,Integracion,Notificaciones,Usuarios servicio;
    class DBTrabajos,DBUsuarios infra;
    class TopicCreado,TopicSiniestro,TopicNotificacion topic;
    class Cliente,Externos,Email,WhatsApp externo;
```

- Azul: los cuatro servicios Spring Boot (procesos independientes, cada uno con su propio
  `main()` - ver `applications`).
- Naranja: Postgres — un solo contenedor, con **una base separada por servicio**
  (`hda_trabajos`, `hda_usuarios`; topología de datos descentralizada). `integracion-service`
  y `notificaciones-service` no persisten nada propio (solo Redis para idempotencia).
- Verde: los tópicos de Pulsar, viven dentro del broker.
- Gris: actores externos al sistema (incluye los canales simulados de notificación).
  Lo importante del diagrama: **ningún servicio le hace una llamada directa a otro, con
  una sola excepción explícita** — `notificaciones-service → usuarios-service` por HTTP,
  y es una *consulta* (no un comando): todo lo demás sigue pasando por los tópicos de
  Pulsar, que es la regla obligatoria que exige la guía (ver más abajo).

Notar el *fan-out* en `trabajo-creado`: `integracion-service` y `notificaciones-service`
tienen cada uno su propia suscripción `Shared` sobre el mismo tópico — ambos reciben
**todos** los eventos de forma independiente (no compiten entre sí por los mensajes;
eso solo pasa *dentro* de la suscripción de cada uno, si se escala a varias réplicas).
Es la forma más simple de agregar un tercer consumidor sin tocar `trabajos-service` ni
a `integracion-service` en absoluto - la evidencia de código de por qué el desacople
por eventos importa (Modificabilidad 1.1). Agregar `usuarios-service` como cuarto
componente siguió la misma lógica: ninguno de los otros tres tuvo que cambiar para que
existiera (solo `notificaciones-service`, que es quien lo consulta).

## Estructura: arquitectura limpia, 4 servicios

Sigue el layout de carpetas del
[scaffold-clean-architecture de Bancolombia](https://github.com/bancolombia/scaffold-clean-architecture)
con `applications`, `domain` e `infrastructure` como carpetas de **primer nivel del
proyecto** (una por capa). Dentro de `domain` e `infrastructure` va primero el **tipo**
de módulo (`model`/`usecase`, `entry-points/<adaptador>`, `driven-adapters/<adaptador>`) y
el servicio (`trabajos-service`/`integracion-service`/`notificaciones-service`/
`usuarios-service`) queda como la carpeta más interna — la que directamente contiene su
propio `backend/build.gradle` y `src/`. Así, `domain` o `infrastructure` se ven de inmediato
bajando por el árbol, sin que el nombre del servicio se interponga antes. La regla de
dependencia se cumple igual: todo apunta hacia adentro, hacia `domain`.

`applications` es la excepción: ahí **no hay ambigüedad de nombres entre servicios**
(a diferencia de `model`, `usecase`, `pulsar-event-bus`, que se repiten), así que es un
único módulo Gradle (`:applications`) que contiene los cuatro `main()`
(`TrabajosServiceApplication`, `IntegracionServiceApplication`,
`NotificacionesServiceApplication`, `UsuariosServiceApplication`) uno junto al otro — sus
paquetes (`com.hda.trabajos` / `com.hda.integracion` / `com.hda.notificaciones` /
`com.hda.usuarios`) no chocan. Sigue produciendo cuatro jars ejecutables independientes vía
cuatro tareas Gradle `BootJar` explícitas, en vez de una carpeta por servicio.

Como el resto del repo sí tiene **cuatro servicios** en un solo build Gradle (algo que los
ejemplos de un solo servicio del scaffold no cubren), esos nombres de proyecto Gradle se
prefijan por servicio (`:trabajos-model`, `:integracion-usecase`, `:notificaciones-model`,
`:usuarios-model`, etc. — ver `backend/settings.gradle`) para no colisionar entre si.

```shell
hda-trabajos-parent/
├── eventos-shared/                            Esquemas Avro (published language) — el
│                                               contrato entre servicios, y hacia afuera.
│                                               No sigue el layout del scaffold: es una
│                                               libreria plana compartida por los cuatro
│                                               servicios (shared kernel).
│
├── applications/                              UN solo módulo Gradle para los cuatro main():
│   └── src/main/java/com/hda/
│       ├── trabajos/                          TrabajosServiceApplication + wiring
│       │                                      (UseCasesConfig, PulsarConfig).
│       ├── integracion/                       IntegracionServiceApplication + wiring
│       │                                      (UseCasesConfig, PulsarClientConfig).
│       ├── notificaciones/                    NotificacionesServiceApplication + wiring
│       │                                      (UseCasesConfig, PulsarClientConfig).
│       └── usuarios/                          UsuariosServiceApplication + wiring
│                                               (UseCasesConfig).
│       resources/
│       ├── application-trabajos.yml            (spring.config.name por servicio, ver main())
│       ├── application-integracion.yml
│       ├── application-notificaciones.yml
│       └── application-usuarios.yml
│
├── domain/
│   ├── model/
│   │   ├── trabajos-service/                  Trabajo, Moneda, CategoriaServicio, ... y los
│   │   │                                      puertos (gateways/): TrabajoRepository,
│   │   │                                      TrabajoEventPublisher. Cero dependencias.
│   │   ├── integracion-service/                TrabajoCreadoEvento (entrada), TrabajoSiniestro
│   │   │                                        (salida) y el puerto TrabajoSiniestroPublisher.
│   │   ├── notificaciones-service/              TrabajoCreadoEvento (entrada, con clienteId),
│   │   │                                        NotificacionEnviada (salida), ContactoUsuario y
│   │   │                                        los puertos CanalNotificacion (antes EmailSender,
│   │   │                                        generalizado - Modificabilidad 1.1),
│   │   │                                        ConsultaUsuarioGateway y
│   │   │                                        NotificacionEnviadaPublisher.
│   │   └── usuarios-service/                    Usuario (AggregateRoot<UUID>, 2 flags de canal)
│   │                                             y el puerto UsuarioRepository. Seedwork propio,
│   │                                             duplicado (no compartido) - mismo criterio que
│   │                                             trabajos-service (integracion-service y
│   │                                             notificaciones-service no llevan seedwork: no
│   │                                             modelan un agregado propio).
│   └── usecase/
│       ├── trabajos-service/                  CrearTrabajoUseCase, ConsultarTrabajoUseCase.
│       │                                      Sin anotaciones de Spring a propósito.
│       ├── integracion-service/               TraducirTrabajoCreadoUseCase.
│       ├── notificaciones-service/             EnviarNotificacionUseCase (consulta el contacto
│       │                                      vía ConsultaUsuarioGateway y dispara 0, 1 o 2
│       │                                      canales según sus flags).
│       └── usuarios-service/                   ConsultarUsuarioUseCase.
│
├── infrastructure/
│   ├── entry-points/
│   │   ├── reactive-web/
│   │   │   ├── trabajos-service/               TrabajoController (API HTTP, WebFlux).
│   │   │   └── usuarios-service/                UsuarioController: único endpoint
│   │   │                                         GET /usuarios/{clienteId}/contacto.
│   │   └── pulsar-event-handler/
│   │       ├── integracion-service/                Consume trabajo-creado (driving adapter).
│   │       └── notificaciones-service/              Consume trabajo-creado (suscripción propia).
│   └── driven-adapters/
│       ├── r2dbc-postgresql/
│       │   ├── trabajos-service/               Implementa TrabajoRepository (hda_trabajos).
│       │   └── usuarios-service/                Implementa UsuarioRepository (hda_usuarios -
│       │                                         base separada, ver docker/postgres-init/).
│       ├── pulsar-event-bus/
│       │   ├── trabajos-service/                   Implementa TrabajoEventPublisher (Pulsar+Avro).
│       │   ├── integracion-service/                Implementa TrabajoSiniestroPublisher,
│       │   │                                         publica trabajo-siniestro-creado.
│       │   └── notificaciones-service/              Implementa NotificacionEnviadaPublisher,
│       │                                             publica notificacion-enviada.
│       ├── canal-notificacion/notificaciones-service/  Implementa CanalNotificacion: dos
│       │                                         adaptadores simulados (solo loguean, sin
│       │                                         proveedor real) - EmailChannelAdapter y
│       │                                         WhatsAppChannelAdapter.
│       └── http-client/notificaciones-service/     Implementa ConsultaUsuarioGateway (WebClient) -
│                                                     el único punto síncrono del sistema, hacia
│                                                     GET /usuarios/{clienteId}/contacto.
│
├── docker/postgres-init/                      Script que crea la base hda_usuarios al levantar
│                                               el contenedor Postgres (ver "Cómo levantarlo").
│
├── docker-compose.yml                         Postgres + Pulsar + Redis.
```

La regla obligatoria de la guía se cumple explícitamente, **con una sola excepción
documentada**: ningún servicio le llama a otro por HTTP/directo, salvo
`notificaciones-service → usuarios-service` (una consulta de solo lectura, nunca un
comando). Todo lo demás sigue el mismo patrón de Entrega 3: `trabajos-service` publica en
el tópico `trabajo-creado` de Pulsar; `integracion-service` y `notificaciones-service` lo
consumen cada uno por su lado (fan-out, ver diagrama arriba) y publican
`trabajo-siniestro-creado`/`notificacion-enviada` respectivamente. Son cuatro
procesos/servicios Spring Boot independientes.

## Mapeo con las decisiones de diseño (Entregas 2, 3 y 4)

| Decisión de diseño | Dónde vive en el código |
| --- | --- |
| Seedwork (Entity, ValueObject, AggregateRoot, DomainEvent) — POJOs sin dependencia de framework | `domain` (duplicado por servicio a propósito - `trabajos-service` y `usuarios-service` lo tienen cada uno el suyo, no comparten módulo) |
| Agregado raíz `Trabajo` con Factory (`Trabajo.crear(...)`) | `domain` |
| Agregado raíz `Usuario` (Entrega 4) con Factory (`Usuario.crear(...)`), 2 flags independientes de canal (`notificarPorEmail`/`notificarPorWhatsapp`) en vez de un enum - agregar un canal nuevo es agregar un flag, no tocar los existentes | `domain/model/usuarios-service` |
| Objeto de valor `Moneda` pensado para el escenario de modificabilidad 1.3 (nuevo país sin tocar el resto del agregado) | `domain` |
| Arquitectura hexagonal: puertos (`TrabajoRepository`, `TrabajoEventPublisher`) en el dominio vs. adaptadores concretos | `domain` (puertos) + `infrastructure` (adaptadores) |
| CQS: comando `CrearTrabajoCommand`/`CrearTrabajoUseCase` vs. consulta `ConsultarTrabajoUseCase` | `domain` y `.../consultartrabajo/` |
| Evento de dominio `TrabajoCreado` (Avro) vs. evento de integración `TrabajoSiniestroCreado` (Avro, v1) — separación exigida por el escenario 3.3 | `eventos-shared` |
| Escalabilidad (escenario 2.3): suscripción `Shared` de Pulsar en los consumidores de trabajo-creado, para poder correr varias instancias en paralelo — demo en vivo: sección "Levantarlo todo con Docker Compose" | `infrastructure` |
| Persistencia real (no sqlite/h2), mínima, con topología de datos descentralizada (una base Postgres por servicio, no una tabla compartida) | `backend/infrastructure/driven-adapters/r2dbc-postgresql/{trabajos,usuarios}-service/src/main/resources/schema.sql`, `backend/docker/postgres-init/` |
| Modificabilidad (escenario 1.1): agregar un consumidor nuevo (`notificaciones-service`) sin tocar `trabajos-service` ni `integracion-service` - solo una suscripción `Shared` más sobre `trabajo-creado` | `backend/infrastructure/entry-points/pulsar-event-handler/notificaciones-service` |
| Modificabilidad (Entrega 4): puerto `EmailSender` generalizado a `CanalNotificacion`; se agrega `WhatsAppChannelAdapter` sin tocar `trabajos-service`, `integracion-service` ni `usuarios-service` | `backend/domain/model/notificaciones-service`, `backend/infrastructure/driven-adapters/canal-notificacion/notificaciones-service` |
| Interoperabilidad (Entrega 4): agregar un cuarto servicio (`usuarios-service`) no obliga a escalar ni a cambiar los otros tres; la única comunicación nueva es una consulta HTTP síncrona explícita, no un comando, y queda documentada como la única excepción a "todo por eventos" | `backend/infrastructure/driven-adapters/http-client/notificaciones-service` |
| Puerto separado para la acción real ("enviar" la notificación) vs. el evento que solo registra que ya se envió - mismo principio hexagonal que `TrabajoRepository`/`TrabajoEventPublisher` en trabajos-service | `domain` |

## Versión de Spring Boot y de Gradle

El proyecto usa **Spring Boot 4.1.1**. Esa versión de su plugin de Gradle exige
**Gradle 9.7.1 y Java 25** corriendo Gradle directamente.

## Cómo levantarlo

> **Directorios.** Todos los comandos de esta sección se ejecutan **desde la raíz del
> repositorio** (la carpeta que contiene este `README.md`). El `docker-compose.yml`, el
> `gradlew` y el `.env` viven dentro de `backend/`, así que los comandos apuntan ahí
> explícitamente (`-f backend/...`, `--env-file backend/.env`, `./backend/gradlew`).
> Si prefieres, puedes `cd backend` una vez y omitir esos prefijos — al final de cada
> paso se indica el equivalente "desde `backend/`".

0. Credenciales por variables de entorno. Las credenciales de Postgres **no están
   hardcodeadas** en el repositorio: se leen de variables de entorno, tanto en
   `backend/docker-compose.yml` (para inicializar el contenedor) como en
   `application-trabajos.yml`/`application-usuarios.yml` (para la conexión R2DBC de cada
   servicio). Copia la plantilla versionada y ajústala con tus propios valores:

```shell
   cp backend/.env.example backend/.env
   # edita backend/.env y cambia al menos POSTGRES_PASSWORD
   ```

   El archivo `.env` real está en `.gitignore` (nunca se sube); solo se versiona la
   plantilla `.env.example`.

   | Variable | Para qué | Valor por defecto |
   | --- | --- | --- |
   | `POSTGRES_DB` | Base de datos de `trabajos-service` | `hda_trabajos` |
   | `POSTGRES_USUARIOS_DB` | Base de datos de `usuarios-service` (separada, mismo Postgres) | `hda_usuarios` |
   | `POSTGRES_USER` | Usuario de Postgres | `hda` |
   | `POSTGRES_PASSWORD` | Contraseña de Postgres | `hda` (¡cámbiala!) |
   | `POSTGRES_HOST` | Host al que se conectan `trabajos-service`/`usuarios-service` | `localhost` |
   | `POSTGRES_PORT` | Puerto de Postgres | `5432` |

   Si no defines nada, aplican los valores por defecto (pensados solo para desarrollo
   local). Si arrancas un servicio **fuera** de Docker Compose (con `java -jar`), exporta
   las variables en esa terminal para que las lea la app, por ejemplo:

```shell
   set -a; source backend/.env; set +a
   java -jar backend/applications/build/libs/trabajos-service.jar
   ```

   > **`hda_usuarios` solo se crea en un volumen nuevo de Postgres.** El script
   > `backend/docker/postgres-init/01-create-usuarios-db.sh` corre automáticamente la
   > primera vez que se inicializa el volumen `pgdata` (Postgres solo ejecuta
   > `docker-entrypoint-initdb.d` en un volumen vacío). Si ya tenías el contenedor de una
   > sesión anterior a que existiera `usuarios-service`, hay que recrear el volumen una
   > vez para que se aplique:
   > ```shell
   > docker compose --env-file backend/.env -f backend/docker-compose.yml down -v
   > docker compose --env-file backend/.env -f backend/docker-compose.yml up -d
   > ```
   > (`down -v` borra los datos de **ambas** bases en ese volumen - solo son datos de
   > prueba locales, no hay nada que perder en desarrollo.)

1. Infraestructura: levanta Postgres (`5432`), Pulsar (`6650`/`8080`) y Redis (`6379`).
   Redis lo usan `integracion-service` y `notificaciones-service` como almacén de
   idempotencia (deduplicación de eventos por su `id`).

```shell
   docker compose --env-file backend/.env -f backend/docker-compose.yml up -d
   ```

   > **Importante al ejecutar desde la raíz:** hay que pasar **ambos**
   > `--env-file backend/.env` **y** `-f backend/docker-compose.yml`. Docker Compose busca
   > el `.env` en el directorio actual (la raíz), no junto al `docker-compose.yml`; sin
   > `--env-file` usaría los valores por defecto en vez de los de tu `.env`.
   >
   > Equivalente desde `backend/`: `cd backend && docker compose up -d` (ahí Compose lee
   > `.env` automáticamente).

2. Crear el tenant y los namespaces de Pulsar que usan los tópicos de este proyecto:

```shell
   docker exec hda-pulsar bin/pulsar-admin tenants create hda
   docker exec hda-pulsar bin/pulsar-admin namespaces create hda/trabajos
   docker exec hda-pulsar bin/pulsar-admin namespaces create hda/integracion
   docker exec hda-pulsar bin/pulsar-admin namespaces create hda/notificaciones
   ```

   (Estos usan `docker exec` sobre el contenedor `hda-pulsar`, así que funcionan igual
   desde cualquier directorio.)

3. Compilar los cuatro servicios. Los cuatro jars se generan en el mismo directorio
   (`backend/applications/build/libs/`), así que **no uses `clean` entre servicio y
   servicio**: borraría los jars ya generados de los otros tres. Compílalos todos de una
   sola vez:

```shell
./backend/gradlew -p backend :applications:bootJarTrabajos :applications:bootJarIntegracion :applications:bootJarNotificaciones :applications:bootJarUsuarios
```

   (equivalente: `./backend/gradlew -p backend :applications:assemble`, que depende de las
   cuatro tareas; o `cd backend && ./gradlew :applications:assemble`.)

   Si en algún momento necesitas una compilación totalmente limpia, corre
   `./backend/gradlew -p backend clean` **una sola vez** y luego el comando de arriba —
   nunca `clean` por servicio.

3.1. Correr cada servicio (cada uno en su propia terminal):
    - `usuarios-service` (`com.hda.usuarios.UsuariosServiceApplication`) → puerto `8084`.
      **Arráncalo antes que `notificaciones-service`** - es a quien le consulta el
      contacto de cada cliente (único punto síncrono del sistema).

```shell
java -jar backend/applications/build/libs/usuarios-service.jar
```

- `trabajos-service` (`com.hda.trabajos.TrabajosServiceApplication`) → puerto `8081`.

```shell
java -jar backend/applications/build/libs/trabajos-service.jar
```

- `integracion-service` (`com.hda.integracion.IntegracionServiceApplication`) →
  puerto `8082`.

```shell
java -jar backend/applications/build/libs/integracion-service.jar
```

- `notificaciones-service` (`com.hda.notificaciones.NotificacionesServiceApplication`) →
  puerto `8083`.

```shell
java -jar backend/applications/build/libs/notificaciones-service.jar
```

4. `usuarios-service` viene con 3 usuarios semilla (UUID fijo, ver
   `backend/infrastructure/driven-adapters/r2dbc-postgresql/usuarios-service/src/main/resources/usuarios/schema.sql`),
   pensados justo para probar los 3 casos de canal:

   | `clienteId` | Canal |
   | --- | --- |
   | `11111111-1111-1111-1111-111111111111` | Solo email |
   | `22222222-2222-2222-2222-222222222222` | Solo WhatsApp |
   | `33333333-3333-3333-3333-333333333333` | Ambos |

   Se pueden consultar directamente, sin pasar por `trabajos-service`:

```shell
curl http://localhost:8084/usuarios/11111111-1111-1111-1111-111111111111/contacto
```

5. Probar el flujo completo, una vez por cada `clienteId` de la tabla anterior:

```shell
curl -X POST http://localhost:8081/trabajos \
-H 'Content-Type: application/json' \
-d '{"clienteId": "11111111-1111-1111-1111-111111111111", "categoriaServicio": "plomeria", "urgencia": "ALTA", "ciudad": "Bogota", "origen": "MARKETPLACE", "partnerId": null, "moneda": "COP"}'
```

Esto crea el `Trabajo`, lo persiste, y publica `TrabajoCreado` en Pulsar.
`integracion-service` y `notificaciones-service` lo consumen cada uno por su lado
(fan-out): el primero publica `TrabajoSiniestroCreado`; el segundo consulta el contacto
del `clienteId` en `usuarios-service` (único punto síncrono) y "envía" (simulado, solo lo
loguea) por el canal que corresponda según sus flags, publicando un `NotificacionEnviada`
por cada canal. Con los 3 `clienteId` de la tabla anterior, el log de
`notificaciones-service` debería mostrar exactamente:
- Usuario A → solo `[EMAIL SIMULADO] Para: usuarioA@hda.test ...`
- Usuario B → solo `[WHATSAPP SIMULADO] Para: +57 300 0000002 ...`
- Usuario C → ambos logs (dos eventos `NotificacionEnviada`, uno por canal)

La respuesta trae el `id` generado (UUID), por ejemplo:

```json
{"id":"a63f8965-6b70-4e2b-bf01-0636401287a2","clienteId":"11111111-1111-1111-1111-111111111111","categoriaServicio":"plomeria","urgencia":"ALTA","ciudad":"Bogota","origen":"MARKETPLACE","partnerId":null,"moneda":"COP","estado":"CREADO","fechaCreacion":"2026-09-06T05:56:25.688907Z"}
```

Consulta (reemplaza `<id>` por el `id` de la respuesta anterior - `{id}` tal
cual, sin reemplazar, no es un id válido y da `400 Bad Request`):

```shell
curl http://localhost:8081/trabajos/<id>
```

## Cómo inspeccionar Postgres y Pulsar directamente

Util para confirmar que un `POST /trabajos` realmente persistió y publicó el evento,
sin depender solo de la respuesta HTTP.

### Postgres (la tabla `trabajo`)

Sesión interactiva:

```shell
docker exec -it hda-postgres psql -U hda -d hda_trabajos
```

Dentro: `\dt` lista las tablas, `select * from trabajo;` muestra las filas, `\q` sale.
O en un solo comando, sin entrar a la sesión interactiva:

```shell
docker exec hda-postgres psql -U hda -d hda_trabajos \
-c "select id, cliente_id, categoria_servicio, ciudad, estado, fecha_creacion from trabajo;"
```

### Postgres (la tabla `usuario`, base separada `hda_usuarios`)

```shell
docker exec hda-postgres psql -U hda -d hda_usuarios \
-c "select id, nombre, notificar_por_email, notificar_por_whatsapp from usuario;"
```

Deben aparecer los 3 usuarios semilla (ver tabla de `clienteId` más arriba) - si esta base
no existe todavía, ver la nota sobre `docker-entrypoint-initdb.d` en el paso 0.

### Pulsar (los eventos publicados)

Ver cuántos mensajes se han publicado por tópico, sin consumirlos (no interfiere con
las suscripciones reales de `integracion-service`/`notificaciones-service`):

```shell
docker exec hda-pulsar bin/pulsar-admin topics stats persistent://hda/trabajos/trabajo-creado
docker exec hda-pulsar bin/pulsar-admin topics stats persistent://hda/integracion/trabajo-siniestro-creado
docker exec hda-pulsar bin/pulsar-admin topics stats persistent://hda/notificaciones/notificacion-enviada
```

Fijarse en `msgInCounter` (total publicado desde que existe el tópico). Los últimos dos
tópicos recién se crean (y el comando recién responde) cuando `integracion-service`/
`notificaciones-service` han corrido y procesado al menos un `TrabajoCreado` cada uno -
antes de eso dan "Topic ... not found".

Ver el contenido de los últimos mensajes (tampoco consume/hace ack, así que no afecta
las suscripciones reales):

```shell
docker exec hda-pulsar bin/pulsar-admin topics peek-messages \
persistent://hda/trabajos/trabajo-creado -s inspeccion -n 5
```

**Ver los eventos en vivo, a medida que van llegando** (una terminal que se queda
abierta, en vez de mirar mensajes ya publicados): `bin/pulsar-client consume` con
`-n 0` (consumir para siempre) y `-st auto_consume` (decodifica el Avro a texto
legible en vez de bytes crudos):

```shell
# Terminal 1: eventos de dominio (los publica trabajos-service)
docker exec -it hda-pulsar bin/pulsar-client consume persistent://hda/trabajos/trabajo-creado \
-s watch-trabajo-creado -n 0 -p Latest -st auto_consume

# Terminal 2: eventos de integración hacia partners (los publica integracion-service)
docker exec -it hda-pulsar bin/pulsar-client consume persistent://hda/integracion/trabajo-siniestro-creado \
-s watch-trabajo-siniestro -n 0 -p Latest -st auto_consume

# Terminal 3: eventos de notificación enviada (los publica notificaciones-service)
docker exec -it hda-pulsar bin/pulsar-client consume persistent://hda/notificaciones/notificacion-enviada \
-s watch-notificacion-enviada -n 0 -p Latest -st auto_consume
```

## Actividades por integrante (Entrega 4)

> **TODO (equipo):** completar esta tabla antes de entregar — falta el detalle de lo que
> hizo cada integrante (Osmond: despliegue en AWS; Víctor: `frontend/`). No se completa
> automáticamente aquí para no inventar actividades que no se verificaron.

| Integrante | Actividades |
| --- | --- |
| Fernando | `usuarios-service` (nuevo, completo: dominio, caso de uso, API REST, persistencia R2DBC/Postgres en base propia `hda_usuarios`); refactor de `notificaciones-service` (puerto `EmailSender` → `CanalNotificacion`, `WhatsAppChannelAdapter`, cliente HTTP hacia `usuarios-service`); actualización de este README. |
| Osmond | _pendiente completar_ — despliegue en AWS (punto 9 de la rúbrica). |
| Víctor | _pendiente completar_ — `frontend/`. |
