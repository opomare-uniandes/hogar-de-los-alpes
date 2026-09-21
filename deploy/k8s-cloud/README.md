# Despliegue evaluable en AWS EKS

Despliega los seis servicios de negocio y el BFF sobre EKS. PostgreSQL se ejecuta en RDS,
Redis en ElastiCache y Pulsar dentro del clúster. Un ALB publica únicamente el BFF; el
resto de servicios conserva direcciones privadas `ClusterIP`.

## Topología desplegada

```text
Internet -> ALB/Gateway -> bff-service (2 réplicas)
                             |-> trabajos-service -> RDS/hda_trabajos
                             `-> trabajo-saga-service -> RDS/hda_trabajo_saga

Pulsar -> integracion-service
       -> notificaciones-service -> usuarios-service -> RDS/hda_usuarios
       -> proveedor-service -> RDS/hda_proveedor
       -> trabajo-saga-service
```

## Requisitos

- AWS CLI con sesión válida.
- Terraform, kubectl, Docker y Python 3.
- Permisos para EKS, ECR, RDS, ElastiCache, VPC, IAM y ALB.

## 1. Crear o actualizar infraestructura

```bash
terraform -chdir=deploy/terraform init
terraform -chdir=deploy/terraform plan
terraform -chdir=deploy/terraform apply
```

Terraform crea siete repositorios ECR: `trabajos`, `integracion`, `notificaciones`,
`usuarios`, `proveedor`, `trabajo-saga` y `bff`.

## 2. Construir y publicar imágenes

```bash
chmod +x deploy/k8s-cloud/build-and-push.sh deploy/k8s-cloud/apply.sh
./deploy/k8s-cloud/build-and-push.sh
```

El script obtiene región, perfil y direcciones de ECR desde los outputs de Terraform.

## 3. Desplegar

```bash
./deploy/k8s-cloud/apply.sh
```

El script:

1. Configura `kubectl` para el EKS.
2. Renderiza endpoints y secretos en un directorio temporal.
3. Crea las bases auxiliares en RDS.
4. Espera a Pulsar y despliega los siete servicios.
5. Aplica KEDA y obtiene la dirección pública del ALB.

## 4. Evidencia y prueba

```bash
kubectl get deployments,pods,services -n hda
kubectl get gateway,httproute -n hda
kubectl get scaledobjects -n hda

export BFF_URL="http://$(kubectl get gateway hda-gateway -n hda -o jsonpath='{.status.addresses[0].value}')"
curl "$BFF_URL/actuator/health"
```

Actualice `baseUrl` en `docs/postman/Cloud.postman_environment.json` y ejecute la colección.
La entrega no debe anunciar una URL hasta que `health` y la colección respondan correctamente.

## Actualizar una versión

```bash
./deploy/k8s-cloud/build-and-push.sh
kubectl rollout restart deployment -n hda
kubectl rollout status deployment/bff-service -n hda
```

## Eliminación segura

El Gateway crea un ALB fuera del estado de Terraform. Elimínelo primero para evitar
recursos huérfanos y cargos:

```bash
kubectl delete -f deploy/k8s-cloud/apps/27-gateway.yaml
terraform -chdir=deploy/terraform destroy
```
