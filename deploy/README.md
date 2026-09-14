# Despliegue — Hogar de los Alpes

Dos formas aisladas de levantar el backend. El `frontend` no se toca.

```
deploy/
├── docker-compose/            Entorno local rapido (infra + 4 servicios)
│   ├── README.md              Guia del flujo Docker Compose
│   ├── docker-compose.yml
│   ├── Dockerfile             Imagen compartida por los 4 servicios (selecciona el jar)
│   ├── .env.example           Plantilla de variables (copiar a .env)
│   └── postgres-init/         Script que crea la base hda_usuarios
└── k8s/                       Despliegue Kubernetes con autoescalado por KEDA
    ├── README.md              Guia del flujo Kubernetes + KEDA
    ├── kind-config.yaml       Cluster local de Kind (NodePort 30081 -> localhost:8081)
    ├── base/                  Infra: namespace/config, secrets, Postgres, Redis, Pulsar
    ├── apps/                  Los 4 servicios (Deployment + Service)
    └── autoscaling/           KEDA ScaledObjects (Pulsar backlog)
```

## Cual usar

| Flujo | Para que | Guia |
| --- | --- | --- |
| **Docker Compose** | Pruebas rapidas locales (infra + 4 servicios en un comando) | [`docker-compose/README.md`](docker-compose/README.md) |
| **Kubernetes + KEDA** | Autoescalado por backlog de topico de Pulsar | [`k8s/README.md`](k8s/README.md) |

## Parametrizacion (comun a los dos flujos)

Los servicios ya no tienen hosts/credenciales hardcodeados: leen variables de entorno con
valores por defecto para desarrollo local (ver `backend/applications/src/main/resources/application-*.yml`).

| Variable | Uso | Default |
| --- | --- | --- |
| `POSTGRES_HOST` | Host de Postgres | `localhost` |
| `REDIS_HOST` | Host de Redis | `localhost` |
| `PULSAR_SERVICE_URL` | Broker binario de Pulsar | `pulsar://localhost:6650` |
| `PULSAR_ADMIN_URL` | Admin REST de Pulsar | `http://localhost:8080` |
| `USUARIOS_SERVICE_BASE_URL` | notificaciones -> usuarios (sincrono) | `http://localhost:8084` |

- En **Docker Compose** apuntan a los nombres de servicio (`postgres`, `redis`, `pulsar`,
  `usuarios-service`) — ver la guia de compose.
- En **Kubernetes** salen del ConfigMap `hda-endpoints` y el Secret `postgres-credentials`
  (`k8s/base/`) — ver la guia de k8s.
