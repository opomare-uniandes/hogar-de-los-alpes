# Docker Compose — entorno local rapido

Levanta la infraestructura (Postgres, Pulsar, Redis) y los 6 servicios Spring Boot en un
solo comando. Pensado para pruebas rapidas locales. Para autoescalado, ver
[`../k8s/README.md`](../k8s/README.md).

## Contenido

```
docker-compose/
├── docker-compose.yml    Infra + pulsar-init (one-shot) + 6 servicios
├── Dockerfile            Compartido por los 6 servicios; cada uno es su propio modulo Gradle (arg SERVICE)
├── .env.example          Plantilla de variables (copiar a .env)
└── postgres-init/        Script que crea la base hda_usuarios en el primer arranque
```

Los servicios leen host/credenciales por variables de entorno; en compose apuntan a los
**nombres de servicio internos** (`postgres`, `pulsar`, `redis`, `usuarios-service`), no a
`localhost`.

El compose incluye ademas un servicio one-shot `pulsar-init` que crea el tenant `hda` y
los namespaces (`hda/trabajos`, `hda/integracion`, `hda/notificaciones`) antes de arrancar
los servicios que publican/consumen ahi — ver la nota mas abajo.

## Uso

Todos los comandos se ejecutan **desde este directorio** (`deploy/docker-compose/`).
Docker Compose lee el `.env` automaticamente desde aqui.

```bash
cd deploy/docker-compose
cp .env.example .env          # ajusta al menos POSTGRES_PASSWORD
docker compose up -d          # infra + 6 servicios
```

Solo infra (y correr los jars por fuera con `java -jar`, como en el runbook del README raiz):

```bash
docker compose up -d postgres pulsar redis
```

Puertos expuestos: trabajos `8081`, integracion `8082`, notificaciones `8083`,
usuarios `8084`, proveedor `8085`, trabajo-saga `8086`.

> **Tenant y namespaces de Pulsar (`pulsar-init`).** Los servicios publican y consumen en
> topicos bajo `persistent://hda/...`, que requieren que existan el tenant `hda` y sus
> namespaces (`hda/trabajos`, `hda/integracion`, `hda/notificaciones`, `hda/proveedor`,
> `hda/partner`). Pulsar no los crea solo: sin ellos, `trabajos`, `integracion`,
> `notificaciones`, `proveedor` y `trabajo-saga` fallan al arrancar con
> `Namespace not found` (solo `usuarios-service` sobrevive, porque no usa Pulsar).
>
> El servicio one-shot `pulsar-init` los crea automaticamente: espera a que Pulsar este
> `healthy`, corre `pulsar-admin ... tenants/namespaces create` (idempotente) y termina.
> Los cinco servicios que dependen de Pulsar declaran
> `depends_on: pulsar-init: condition: service_completed_successfully`, asi que solo
> arrancan una vez que los namespaces existen. Es el equivalente en compose del Job
> `pulsar-bootstrap` del flujo de k8s, y reemplaza el paso manual `pulsar-admin ... create`
> que estaba en el runbook del README raiz.

## Probar el flujo

```bash
curl -X POST http://localhost:8081/trabajos -H 'Content-Type: application/json' \
  -d '{"clienteId":"11111111-1111-1111-1111-111111111113","categoriaServicio":"plomeria","urgencia":"ALTA","ciudad":"Bogota","origen":"MARKETPLACE","partnerId":null,"moneda":"COP"}'
```

Consulta directa a usuarios-service:

```bash
curl http://localhost:8084/usuarios/11111111-1111-1111-1111-111111111111/contacto
```

## Variables (`.env`)

| Variable | Para que | Default |
| --- | --- | --- |
| `POSTGRES_DB` | Base de `trabajos-service` | `hda_trabajos` |
| `POSTGRES_USUARIOS_DB` | Base de `usuarios-service` | `hda_usuarios` |
| `POSTGRES_USER` | Usuario de Postgres | `hda` |
| `POSTGRES_PASSWORD` | Contraseña de Postgres | `changeit` (¡cambiala!) |
| `POSTGRES_PORT` | Puerto de Postgres publicado | `5432` |

El `.env` real esta en `.gitignore`; solo se versiona `.env.example`.

> **`hda_usuarios` solo se crea en un volumen nuevo de Postgres.** El script
> `postgres-init/01-create-usuarios-db.sh` corre automaticamente la primera vez que se
> inicializa el volumen `pgdata` (`docker-entrypoint-initdb.d` solo se ejecuta en un volumen
> vacio). Si ya tenias el contenedor de antes, recrea el volumen una vez:
>
> ```bash
> docker compose down -v
> docker compose up -d
> ```

## Teardown

```bash
docker compose down -v        # -v borra los volumenes (datos de prueba locales)
```
