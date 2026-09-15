# Despliegue — Hogar de los Alpes

Formas aisladas de levantar el backend, de local a AWS.

```
deploy/
├── docker-compose/            Entorno local rapido (infra + 4 servicios)
│   ├── README.md              Guia del flujo Docker Compose
│   ├── docker-compose.yml
│   ├── Dockerfile             Imagen compartida por los 4 servicios (selecciona el jar)
│   ├── .env.example           Plantilla de variables (copiar a .env)
│   └── postgres-init/         Script que crea la base hda_usuarios
├── k8s/                       Despliegue Kubernetes local con autoescalado por KEDA
│   ├── README.md              Guia del flujo Kubernetes + KEDA
│   ├── kind-config.yaml       Cluster local de Kind (NodePort 30081 -> localhost:8081)
│   ├── base/                  Infra: namespace/config, secrets, Postgres, Redis, Pulsar
│   ├── apps/                  Los 4 servicios (Deployment + Service)
│   └── autoscaling/           KEDA ScaledObjects (Pulsar backlog)
├── terraform/                 Infraestructura AWS (VPC, EKS, RDS, ElastiCache, ECR, KEDA +
│                               AWS Load Balancer Controller) para el PoC en la nube
└── k8s-cloud/                 Los 4 servicios + Pulsar sobre ese EKS, mismo autoescalado
    ├── README.md              Guia del flujo AWS (EKS + Gateway API/ALB)
    ├── apply.sh               Renderiza endpoints desde `terraform output` y aplica todo
    ├── base/                  Infra: namespace/config, secrets, StorageClass, bootstrap RDS, Pulsar
    ├── apps/                  Los 4 servicios (imagenes ECR) + Gateway API (ALB)
    └── autoscaling/           KEDA ScaledObjects (identico al flujo local)
```

## Cual usar

| Flujo | Para que | Guia |
| --- | --- | --- |
| **Docker Compose** | Pruebas rapidas locales (infra + 4 servicios en un comando) | [`docker-compose/README.md`](docker-compose/README.md) |
| **Kubernetes + KEDA** | Autoescalado por backlog de topico de Pulsar, local | [`k8s/README.md`](k8s/README.md) |
| **AWS (Terraform + EKS)** | El mismo autoescalado, en la nube, con un ALB real | [`k8s-cloud/README.md`](k8s-cloud/README.md) |

## Parametrizacion (comun a los tres flujos)

Los servicios ya no tienen hosts/credenciales hardcodeados: leen variables de entorno con
valores por defecto para desarrollo local (ver `backend/<servicio>/application/src/main/resources/application.yml`).

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
- En **AWS** salen del mismo ConfigMap/Secret, pero renderizados por `apply.sh` con los
  endpoints reales de RDS/ElastiCache (`k8s-cloud/base/`) — ver la guia de k8s-cloud.
