# Despliegue alternativo en AWS EKS

Esta opción despliega los seis servicios de negocio y el BFF sobre la infraestructura de
[`../terraform/`](../terraform/): EKS, RDS, ElastiCache y ECR. Pulsar corre dentro del
clúster. KEDA escala consumidores por backlog; HPA y metrics-server escalan servicios
de entrada por CPU. Un ALB mediante Gateway API publica **solo el BFF**; los servicios
de negocio permanecen como `ClusterIP`.

La entrega académica verificada usa [GitHub Codespaces](../codespaces/README.md), no AWS.
Esta ruta requiere una cuenta AWS y puede generar costos. Para Kubernetes local sin AWS,
consulte [`../k8s/README.md`](../k8s/README.md).

## Topología

```text
Internet -> ALB/Gateway -> bff-service (2 réplicas)
                             |-> trabajos-service -> RDS/hda_trabajos
                             `-> trabajo-saga-service -> RDS/hda_trabajo_saga

Pulsar -> integracion-service
       -> notificaciones-service -> usuarios-service -> RDS/hda_usuarios
       -> proveedor-service -> RDS/hda_proveedor
       -> trabajo-saga-service
```

`base/` define el namespace, ConfigMap, secretos, bases auxiliares y Pulsar. `apps/`
contiene los siete Deployments y el Gateway. `autoscaling/` configura KEDA y HPA.
`apply.sh` renderiza los marcadores `__PLACEHOLDER__` con outputs de Terraform en un
directorio temporal antes de aplicar los manifiestos.

## Requisitos

- AWS CLI con sesión válida, Terraform, kubectl, Docker y Python 3.
- Permisos para EKS, ECR, RDS, ElastiCache, VPC, IAM y ALB.
- Un perfil AWS definido en `deploy/terraform/sandbox.auto.tfvars` (archivo ignorado).

```bash
aws sso login --profile <your-aws-profile>
cp deploy/terraform/sandbox.auto.tfvars.example deploy/terraform/sandbox.auto.tfvars
```

El exporter OTLP de métricas y trazas necesita el header de autorización. Configure
`otel_exporter_otlp_headers_authorization` en `sandbox.auto.tfvars`; nunca suba el valor
al repositorio. `otel_collector_host` tiene un valor predeterminado no secreto que puede
cambiarse allí.

## 1. Crear o actualizar infraestructura

Terraform crea la VPC, EKS, RDS, ElastiCache, siete repositorios ECR y los addons
necesarios (KEDA, metrics-server y AWS Load Balancer Controller). No se ejecuta
automáticamente al aplicar Kubernetes.

```bash
terraform -chdir=deploy/terraform init
terraform -chdir=deploy/terraform plan
terraform -chdir=deploy/terraform apply
```

## 2. Construir y publicar las siete imágenes

```bash
chmod +x deploy/k8s-cloud/build-and-push.sh deploy/k8s-cloud/apply.sh
./deploy/k8s-cloud/build-and-push.sh
```

El script obtiene región, perfil y direcciones ECR desde los outputs de Terraform.

## 3. Desplegar

```bash
./deploy/k8s-cloud/apply.sh
```

El script configura kubectl, renderiza endpoints y secretos en un directorio temporal,
crea las bases auxiliares en RDS, espera a Pulsar, despliega los siete servicios,
aplica el autoescalado y muestra la dirección del ALB.

## 4. Verificar desde el BFF

```bash
kubectl get deployments,pods,services -n hda
kubectl get gateway,httproute,hpa,scaledobjects -n hda

export BFF_URL="http://$(kubectl get gateway hda-gateway -n hda -o jsonpath='{.status.addresses[0].value}')"
curl "$BFF_URL/actuator/health"
```

Asigne `BFF_URL` a `baseUrl` en el ambiente Postman y ejecute la colección de
[`../../docs/postman`](../../docs/postman/README.md). No anuncie una URL hasta que
el health check y la colección respondan correctamente.

## Actualizar y eliminar

Para actualizar imágenes, ejecute `build-and-push.sh` y reinicie los Deployments:

```bash
./deploy/k8s-cloud/build-and-push.sh
kubectl rollout restart deployment -n hda
kubectl rollout status deployment/bff-service -n hda
```

Al eliminar, quite primero el Gateway: el ALB está fuera del estado de Terraform y
podría quedar huérfano con cargos asociados.

```bash
kubectl delete -f deploy/k8s-cloud/apps/27-gateway.yaml
terraform -chdir=deploy/terraform destroy
```
