# Despliegue — Hogar de los Alpes

| Flujo | Alcance | Guía |
| --- | --- | --- |
| Docker Compose | Infraestructura, seis servicios y BFF para validación local | [`docker-compose/README.md`](docker-compose/README.md) |
| GitHub Codespaces | Despliegue académico temporal y público, cubierto por la cuota gratuita personal | [`codespaces/README.md`](codespaces/README.md) |
| Kubernetes local | Laboratorio previo de autoescalado de la Entrega 4 | [`k8s/README.md`](k8s/README.md) |
| AWS EKS | Alternativa productiva con costo: seis servicios, BFF, ALB, RDS, ElastiCache y Pulsar | [`k8s-cloud/README.md`](k8s-cloud/README.md) |

## Regla de exposición

En nube solamente `bff-service` se publica mediante el ALB. Los seis servicios de negocio
son internos y se comunican principalmente mediante Pulsar. El BFF realiza únicamente las
consultas y delegaciones HTTP necesarias para presentar el API del consumidor; no contiene
reglas de dominio ni coordina la saga.

## Variables principales

| Variable | Consumidor | Propósito |
| --- | --- | --- |
| `TRABAJOS_SERVICE_BASE_URL` | BFF | Dirección interna de Gestión de Trabajos |
| `TRABAJO_SAGA_SERVICE_BASE_URL` | BFF | Dirección interna del Saga Log |
| `BFF_UPSTREAM_TIMEOUT` | BFF | Límite para llamadas internas |
| `USUARIOS_SERVICE_BASE_URL` | Notificaciones | Consulta síncrona de contacto |
| `PULSAR_SERVICE_URL` | Servicios orientados a eventos | Comunicación binaria con Pulsar |
| `POSTGRES_*_DB` | Servicios con persistencia | Base lógica propia por servicio |

El contrato público está en [`../docs/api/openapi.yaml`](../docs/api/openapi.yaml) y la
colección ejecutable en [`../docs/postman`](../docs/postman/README.md).
