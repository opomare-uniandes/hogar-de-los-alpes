# Kubernetes + KEDA — autoescalado por backlog de Pulsar

Despliega los 4 servicios y su infraestructura en un cluster local (Kind, o minikube como
alternativa — ver [Alternativa: minikube](#alternativa-minikube)), con autoescalado
de los dos consumidores por el backlog de sus suscripciones de Pulsar (KEDA). Para pruebas
rapidas sin autoescalado, ver [`../docker-compose/README.md`](../docker-compose/README.md).

## Contenido

```
k8s/
├── kind-config.yaml            Cluster de Kind (NodePort 30081 -> localhost:8081; no aplica a minikube)
├── base/                       Infraestructura
│   ├── 00-namespace-config.yaml  Namespace hda + ConfigMap hda-endpoints
│   ├── 01-secrets.yaml           Secret postgres-credentials
│   ├── 10-postgres.yaml          Postgres (PVC + init ConfigMap)
│   ├── 11-redis.yaml             Redis
│   └── 12-pulsar.yaml            Pulsar standalone + Job que crea tenant/namespaces
├── apps/                       Los 4 servicios (Deployment + Service)
│   ├── 20-trabajos-service.yaml    (+ NodePort 30081 para acceso local)
│   ├── 21-integracion-service.yaml
│   ├── 22-notificaciones-service.yaml
│   └── 23-usuarios-service.yaml
└── autoscaling/
    └── 30-scaledobjects.yaml   KEDA ScaledObjects (integracion + notificaciones)
```

Las cadenas de conexion salen del ConfigMap `hda-endpoints` y el Secret
`postgres-credentials` (`base/`), inyectados como variables de entorno en cada Deployment.

## Requisitos

Docker, [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation), Helm, kubectl. Los comandos se ejecutan **desde la raiz del repositorio**
(el build context de las imagenes es `backend/`).

> **Host con ZFS (o donde Kind falle al crear el cluster).** Kind corre el nodo como un
> contenedor y comparte el filesystem del host; sobre **ZFS** el kubelet/cAdvisor no puede
> leer el rootfs y `kubeadm init` falla en `wait-control-plane` (API server
> `connection refused`). En ese caso usa **minikube con un driver de VM**, que aisla el
> filesystem del nodo dentro de la VM — ver [Alternativa: minikube](#alternativa-minikube)
> al final. Los manifiestos son los mismos; solo cambian crear-cluster, cargar-imagenes y
> el acceso.

## Pasos

```bash
# 1. Cluster local
kind create cluster --config deploy/k8s/kind-config.yaml

# 2. KEDA (autoescalador dirigido por eventos)
helm repo add kedacore https://kedacore.github.io/charts && helm repo update
helm install keda kedacore/keda --namespace keda --create-namespace

# 3. Construir imagenes (una por servicio, mismo Dockerfile) y cargarlas al cluster
docker build -t hda/trabajos-service:local       --build-arg SERVICE=trabajos       -f deploy/docker-compose/Dockerfile backend
docker build -t hda/integracion-service:local    --build-arg SERVICE=integracion    -f deploy/docker-compose/Dockerfile backend
docker build -t hda/notificaciones-service:local --build-arg SERVICE=notificaciones -f deploy/docker-compose/Dockerfile backend
docker build -t hda/usuarios-service:local       --build-arg SERVICE=usuarios       -f deploy/docker-compose/Dockerfile backend

kind load docker-image hda/trabajos-service:local       --name hda
kind load docker-image hda/integracion-service:local    --name hda
kind load docker-image hda/notificaciones-service:local --name hda
kind load docker-image hda/usuarios-service:local       --name hda

# 4. Desplegar
kubectl apply -f deploy/k8s/base/
kubectl wait --for=condition=ready pod -l app=pulsar -n hda --timeout=180s
kubectl apply -f deploy/k8s/apps/
kubectl apply -f deploy/k8s/autoscaling/

# 5. Verificar
kubectl get pods -n hda
kubectl get scaledobject -n hda
```

> El `Dockerfile` vive en `deploy/docker-compose/` y lo comparten ambos flujos: cada
> servicio es su propio modulo Gradle bootable, seleccionado via el arg `SERVICE`
> (trabajos/integracion/notificaciones/usuarios).

## Probar el autoescalado

`trabajos-service` queda en `localhost:8081` (NodePort mapeado por `kind-config.yaml`).
Inundar con `POST /trabajos` genera backlog en `trabajo-creado`; KEDA escala
`integracion-service` y `notificaciones-service` segun el backlog de su propia suscripcion
(`msgBacklogThreshold: 10`, hasta 10 replicas).

```bash
for i in $(seq 1 200); do
  curl -s -X POST http://localhost:8081/trabajos -H 'Content-Type: application/json' \
    -d '{"clienteId":"11111111-1111-1111-1111-111111111113","categoriaServicio":"plomeria","urgencia":"ALTA","ciudad":"Bogota","origen":"MARKETPLACE","partnerId":null,"moneda":"COP"}' &
done; wait

kubectl get pods -n hda -l app=integracion-service -w
kubectl get hpa -n hda -w
```

## Por que las replicas empiezan en 1 (no en 0)

KEDA mide el backlog por *suscripcion*. La suscripcion Shared solo se crea en Pulsar cuando
el consumidor se conecta la primera vez; si arrancara en 0, el admin daria 404 y KEDA no
escalaria (deadlock). Con 1 replica base la suscripcion queda registrada y KEDA la escala
hacia arriba con la carga. Ver `autoscaling/30-scaledobjects.yaml`.

## Teardown

```bash
kind delete cluster --name hda
```

## Alternativa: minikube

Usa minikube cuando Kind no funcione en tu host.

**Los manifiestos (`base/`, `apps/`, `autoscaling/`) no cambian.** Solo cambian tres cosas:
crear el cluster, cargar las imagenes y el acceso a `trabajos-service`. `kind-config.yaml`
**no se usa** con minikube (su `extraPortMappings` es especifico de Kind).

Requisitos: Docker, [minikube](https://minikube.sigs.k8s.io/docs/start/), [Helm](https://helm.sh/docs/intro/install/), kubectl, y
un driver de VM. Comandos desde la raiz del repositorio.

```bash
# 1. Cluster local en una VM
minikube start --driver=docker --cpus=4 --memory=6g --profile hda

# 2. KEDA (igual que con Kind)
helm repo add kedacore https://kedacore.github.io/charts && helm repo update
helm install keda kedacore/keda --namespace keda --create-namespace

# 3. Construir imagenes y cargarlas en la VM
docker build -t hda/trabajos-service:local       --build-arg SERVICE=trabajos       -f deploy/docker-compose/Dockerfile backend
docker build -t hda/integracion-service:local    --build-arg SERVICE=integracion    -f deploy/docker-compose/Dockerfile backend
docker build -t hda/notificaciones-service:local --build-arg SERVICE=notificaciones -f deploy/docker-compose/Dockerfile backend
docker build -t hda/usuarios-service:local       --build-arg SERVICE=usuarios       -f deploy/docker-compose/Dockerfile backend

minikube image load hda/trabajos-service:local       -p hda
minikube image load hda/integracion-service:local    -p hda
minikube image load hda/notificaciones-service:local -p hda
minikube image load hda/usuarios-service:local       -p hda

# 4. Desplegar
kubectl apply -f deploy/k8s/base/
kubectl wait --for=condition=ready pod -l app=pulsar -n hda --timeout=180s
kubectl apply -f deploy/k8s/apps/
kubectl apply -f deploy/k8s/autoscaling/

# 5. Verificar
kubectl get pods -n hda
kubectl get scaledobject -n hda
```

> **Cargar imagenes mas rapido (opcional).** Con driver de VM, `minikube image load` copia
> cada imagen del Docker del host a la VM y puede ser lento. Alternativa: construir
> directamente dentro del Docker de la VM y saltarte el `load`:
> ```bash
> eval $(minikube -p hda docker-env)      # apunta tu docker a la VM
> # ...corre aqui los cuatro docker build...
> eval $(minikube -p hda docker-env -u)   # revierte al terminar
> ```
> `imagePullPolicy: IfNotPresent` de los manifiestos ya sirve para ambos enfoques.

### Acceso a trabajos-service

minikube no mapea NodePort a `localhost` como el `extraPortMappings` de Kind. Usa una de:

```bash
# Opcion A - port-forward (la mas simple; sirve con cualquier driver, no usa NodePort):
kubectl port-forward -n hda svc/trabajos-service 8081:8081
# entonces POST a http://localhost:8081/trabajos como en el flujo de Kind.

# Opcion B - el Service NodePort ya existente (trabajos-service-nodeport, 30081):
minikube -p hda service trabajos-service-nodeport -n hda --url
# imprime un http://<vm-ip>:30081; usa esa URL en vez de localhost:8081.
```

Para la prueba de carga/autoescalado, apunta el `curl` a la
URL que hayas obtenido. El comportamiento de KEDA es identico al del flujo de Kind.

### Teardown (minikube)

```bash
minikube delete -p hda
```

