# Kubernetes en AWS (EKS)

Despliega los 4 servicios sobre la infraestructura AWS creada por
[`../terraform/`](../terraform/) (EKS, RDS, ElastiCache, ECR) con el mismo autoescalado por
backlog de Pulsar (KEDA) que el flujo local, mas un ALB real (Gateway API) para acceso
externo. Para desarrollo local sin AWS, ver [`../k8s/README.md`](../k8s/README.md) — esta
carpeta es un flujo separado, no reemplaza al local.

## Contenido

```
k8s-cloud/
├── apply.sh                      Renderiza los __PLACEHOLDER__ con outputs de Terraform y aplica todo
├── base/
│   ├── 00-namespace-config.yaml    Namespace hda + ConfigMap hda-endpoints (RDS/Redis/Pulsar)
│   ├── 01-secrets.yaml             Secret postgres-credentials (password generada por Terraform)
│   ├── 02-storageclass.yaml        StorageClass gp3 (para el volumen de Pulsar)
│   ├── 03-postgres-bootstrap.yaml  Job: crea hda_usuarios en RDS si no existe
│   └── 04-pulsar.yaml              Pulsar standalone + PVC gp3 + Job que crea tenant/namespaces
├── apps/                          Los 4 servicios (imagenes de ECR)
│   ├── 20-trabajos-service.yaml
│   ├── 21-integracion-service.yaml
│   ├── 22-notificaciones-service.yaml
│   ├── 23-usuarios-service.yaml
│   └── 24-gateway.yaml             GatewayClass/Gateway/HTTPRoute (ALB, no Ingress)
└── autoscaling/
    └── 30-scaledobjects.yaml       KEDA ScaledObjects (identico al flujo local)
```

## Requisitos

AWS CLI, kubectl, Terraform, Docker, Python 3 (usado por `apply.sh` para leer el output JSON
de ECR).

```bash
aws sso login --profile <your-aws-profile>
```

## Paso 0: aplicar la infraestructura

**Sin esto, todo lo demas falla.** [`../terraform/`](../terraform/) crea la VPC, EKS, RDS,
ElastiCache, ECR y los addons (KEDA + AWS Load Balancer Controller) — nada de esto se aplica
solo, y `apply.sh` (paso 2) depende de sus outputs.

```bash
terraform -chdir=deploy/terraform init
terraform -chdir=deploy/terraform apply
```

Un solo comando: todos los modulos estan wireados en `main.tf`. Tarda ~20-25 min (EKS es lo
que mas tarda).

## Paso 1: imagenes en ECR

Una vez por cambio de codigo. Primero, build local:

```bash
docker build -t hda/trabajos-service:local       --build-arg SERVICE_JAR=trabajos-service.jar       -f deploy/docker-compose/Dockerfile backend
docker build -t hda/integracion-service:local    --build-arg SERVICE_JAR=integracion-service.jar    -f deploy/docker-compose/Dockerfile backend
docker build -t hda/notificaciones-service:local --build-arg SERVICE_JAR=notificaciones-service.jar -f deploy/docker-compose/Dockerfile backend
docker build -t hda/usuarios-service:local       --build-arg SERVICE_JAR=usuarios-service.jar       -f deploy/docker-compose/Dockerfile backend
```

Despues, tag + push a ECR:

```bash
aws ecr get-login-password --region us-east-1 --profile <your-aws-profile> \
  | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

docker tag hda/trabajos-service:local       <account-id>.dkr.ecr.us-east-1.amazonaws.com/hda/trabajos-service:latest
docker tag hda/integracion-service:local    <account-id>.dkr.ecr.us-east-1.amazonaws.com/hda/integracion-service:latest
docker tag hda/notificaciones-service:local <account-id>.dkr.ecr.us-east-1.amazonaws.com/hda/notificaciones-service:latest
docker tag hda/usuarios-service:local       <account-id>.dkr.ecr.us-east-1.amazonaws.com/hda/usuarios-service:latest

docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/hda/trabajos-service:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/hda/integracion-service:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/hda/notificaciones-service:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/hda/usuarios-service:latest
```

(`<account-id>` sale de `terraform -chdir=../terraform output ecr_repository_urls`.)

## Paso 2: desplegar

```bash
./deploy/k8s-cloud/apply.sh
```

Esto: apunta `kubectl` al cluster (`aws eks update-kubeconfig`), renderiza los
`__PLACEHOLDER__` (endpoints de RDS/ElastiCache, password de Postgres, URLs de imagen) desde
`terraform output`, aplica `base/` → espera a Pulsar → aplica `apps/` → aplica
`autoscaling/`, y al final imprime la URL del ALB.

## Verificar

```bash
kubectl get pods -n hda
kubectl get scaledobject -n hda
kubectl get gateway hda-gateway -n hda
```

## Teardown

Borra primero los objetos de Kubernetes que crean recursos AWS fuera de Terraform (el ALB del
Gateway), luego destruye la infraestructura:

```bash
kubectl delete -f deploy/k8s-cloud/apps/24-gateway.yaml
terraform -chdir=deploy/terraform destroy
```

> Si se salta el `kubectl delete` del Gateway, el ALB que creo el AWS Load Balancer Controller
> queda huerfano en AWS (Terraform no lo conoce, no lo puede destruir) y sigue facturando.
