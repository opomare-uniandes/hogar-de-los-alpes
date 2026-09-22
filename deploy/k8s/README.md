# Kubernetes + KEDA — autoescalado por backlog de Pulsar y por CPU

Despliega los 6 servicios y su infraestructura en un cluster local (Kind, o minikube como
alternativa — ver [Alternativa: minikube](#alternativa-minikube)), con **dos formas de
autoescalado complementarias**:

- **Por backlog de Pulsar (KEDA)** — para los consumidores de eventos/comandos
  (`integracion-service`, `notificaciones-service`, `proveedor-service`, `trabajo-saga-service`):
  escalan segun el backlog de su suscripcion. `trabajos-service` combina este trigger con el de
  CPU en el mismo ScaledObject (recibe comandos por Pulsar ademas de trafico REST). Ver
  `autoscaling/30-scaledobjects.yaml`.
- **Por CPU (HorizontalPodAutoscaler nativo)** — para el servicio de entrada HTTP que
  **no** consume topicos (`usuarios-service`): ante un pico inesperado de llamadas no hay
  backlog que medir, asi que escala por utilizacion de CPU. Ver
  `autoscaling/31-hpa-entry-api.yaml` y la seccion
  [Autoescalado por CPU](#autoescalado-por-cpu-servicios-de-entrada).

Para pruebas rapidas sin autoescalado, ver
[`../docker-compose/README.md`](../docker-compose/README.md).

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
├── apps/                       Los 6 servicios (Deployment + Service)
│   ├── 20-trabajos-service.yaml    (+ NodePort 30081 para acceso local)
│   ├── 21-integracion-service.yaml
│   ├── 22-notificaciones-service.yaml
│   ├── 23-usuarios-service.yaml
│   ├── 24-proveedor-service.yaml
│   └── 25-trabajo-saga-service.yaml
└── autoscaling/
    ├── 30-scaledobjects.yaml   KEDA ScaledObjects (integracion + notificaciones + proveedor + trabajo-saga por backlog; trabajos por backlog + CPU)
    └── 31-hpa-entry-api.yaml   HPA nativo por CPU (usuarios, servicio de entrada sin Pulsar)
```

Las cadenas de conexion salen del ConfigMap `hda-endpoints` y el Secret
`postgres-credentials` (`base/`), inyectados como variables de entorno en cada Deployment.

## Requisitos

Docker, [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation), Helm, kubectl. Los comandos se ejecutan **desde la raiz del repositorio**
(el build context de las imagenes es `backend/`). El autoescalado por CPU necesita ademas
**metrics-server** en el cluster (se instala en el paso 3).

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

# 2. KEDA (autoescalador dirigido por eventos, para los consumidores de Pulsar)
helm repo add kedacore https://kedacore.github.io/charts && helm repo update
helm install keda kedacore/keda --namespace keda --create-namespace

# 3. metrics-server (fuente de metricas de CPU/memoria para el HPA de los servicios de
#    entrada). En Kind/minikube hay que arrancarlo con --kubelet-insecure-tls (los kubelets
#    usan certs autofirmados que metrics-server rechazaria por defecto).
helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/ && helm repo update
helm install metrics-server metrics-server/metrics-server \
  --namespace kube-system \
  --set args="{--kubelet-insecure-tls}"
kubectl wait --for=condition=available deploy/metrics-server -n kube-system --timeout=120s

# 4. Construir imagenes (una por servicio, mismo Dockerfile) y cargarlas al cluster
docker build -t hda/trabajos-service:local       --build-arg SERVICE=trabajos       -f deploy/docker-compose/Dockerfile backend
docker build -t hda/integracion-service:local    --build-arg SERVICE=integracion    -f deploy/docker-compose/Dockerfile backend
docker build -t hda/notificaciones-service:local --build-arg SERVICE=notificaciones -f deploy/docker-compose/Dockerfile backend
docker build -t hda/usuarios-service:local       --build-arg SERVICE=usuarios       -f deploy/docker-compose/Dockerfile backend
docker build -t hda/proveedor-service:local      --build-arg SERVICE=proveedor      -f deploy/docker-compose/Dockerfile backend
docker build -t hda/trabajo-saga-service:local   --build-arg SERVICE=trabajo-saga   -f deploy/docker-compose/Dockerfile backend

kind load docker-image hda/trabajos-service:local       --name hda
kind load docker-image hda/integracion-service:local    --name hda
kind load docker-image hda/notificaciones-service:local --name hda
kind load docker-image hda/usuarios-service:local       --name hda
kind load docker-image hda/proveedor-service:local      --name hda
kind load docker-image hda/trabajo-saga-service:local   --name hda

# 5. Desplegar
kubectl apply -f deploy/k8s/base/
kubectl wait --for=condition=ready pod -l app=pulsar -n hda --timeout=180s
kubectl apply -f deploy/k8s/apps/
kubectl apply -f deploy/k8s/autoscaling/

# 6. Verificar
kubectl get pods -n hda
kubectl get scaledobject -n hda   # KEDA: integracion + notificaciones + proveedor + trabajos + trabajo-saga (por backlog; trabajos combina backlog + CPU)
kubectl get hpa -n hda            # HPA:  usuarios (por CPU)
```

> El `Dockerfile` vive en `deploy/docker-compose/` y lo comparten ambos flujos: cada
> servicio es su propio modulo Gradle bootable, seleccionado via el arg `SERVICE`
> (trabajos/integracion/notificaciones/usuarios/proveedor/trabajo-saga).

## Probar el autoescalado

Hay dos autoescaladores distintos que se pueden ejercitar por separado.

### Por backlog de Pulsar (consumidores)

`trabajos-service` queda en `localhost:8081` (NodePort mapeado por `kind-config.yaml`).
Inundar con `POST /trabajos` genera backlog en `trabajo-creado`; KEDA escala
`integracion-service` y `notificaciones-service` segun el backlog de su propia suscripcion
(`msgBacklogThreshold: 10`, hasta 10 replicas).

```bash
# con oha (https://github.com/hatoo/oha, cargo install oha): 200 POST /trabajos para
# acumular backlog en trabajo-creado
oha -n 200 -c 50 -m POST -T 'application/json' \
  -d '{"clienteId":"11111111-1111-1111-1111-111111111113","categoriaServicio":"plomeria","urgencia":"ALTA","ciudad":"Bogota","origen":"MARKETPLACE","partnerId":null,"moneda":"COP"}' \
  http://localhost:8081/trabajos

kubectl get pods -n hda -l app=integracion-service -w
kubectl get hpa -n hda -w
```

### Autoescalado por CPU (servicios de entrada)

`trabajos-service` y `usuarios-service` no consumen topicos, asi que escalan por
utilizacion de CPU con el HPA nativo (`autoscaling/31-hpa-entry-api.yaml`,
`averageUtilization: 60`, hasta 5 replicas). Un pico sostenido de llamadas HTTP sube la CPU
por pod y dispara el escalado.

Primero confirma que metrics-server ya reporta metricas (si `kubectl top` da error, espera
unos segundos a que junte la primera muestra):

```bash
kubectl top pods -n hda
kubectl get hpa -n hda   # TARGETS debe mostrar un % real, no <unknown>
```

Genera carga sostenida contra el punto de entrada y observa el escalado. `oha`
(https://github.com/hatoo/oha, instalable con `cargo install oha`) mantiene mejor la
presion que un `for` de curls; con curl basta con repetir en paralelo durante un rato:

```bash
# con oha: 60s de carga concurrente sobre POST /trabajos
oha -z 60s -c 50 -m POST -T 'application/json' \
  -d '{"clienteId":"11111111-1111-1111-1111-111111111113","categoriaServicio":"plomeria","urgencia":"ALTA","ciudad":"Bogota","origen":"MARKETPLACE","partnerId":null,"moneda":"COP"}' \
  http://localhost:8081/trabajos

# observa el HPA reaccionar (REPLICAS sube al pasar TARGETS de 60%)
kubectl get hpa trabajos-service-hpa -n hda -w
kubectl get pods -n hda -l app=trabajos-service -w
```

`usuarios-service` no esta expuesto en `localhost` (solo lo llama `notificaciones-service`
dentro del cluster). Su HPA se ejercita de dos formas:

- **Indirecta (realista):** la prueba de backlog de arriba hace que `notificaciones-service`
  escale y consulte mas veces `GET /usuarios/{id}/contacto`, subiendo la CPU de
  `usuarios-service`.
- **Directa:** un `port-forward` para golpearlo desde el host:
  ```bash
  kubectl port-forward -n hda svc/usuarios-service 8084:8084
  oha -z 60s -c 50 http://localhost:8084/usuarios/11111111-1111-1111-1111-111111111113/contacto
  kubectl get hpa usuarios-service-hpa -n hda -w
  ```

> **`TARGETS` en `<unknown>`.** Casi siempre es que metrics-server no esta listo o le falta
> `--kubelet-insecure-tls` en Kind/minikube (ver paso 3). Revisa
> `kubectl logs -n kube-system deploy/metrics-server`.

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

# 3. metrics-server para el HPA por CPU. En minikube viene como addon (mas simple que Helm);
#    el addon ya se configura solo para los kubelets de la VM.
minikube addons enable metrics-server -p hda
kubectl wait --for=condition=available deploy/metrics-server -n kube-system --timeout=120s

# 4. Construir imagenes y cargarlas en la VM
docker build -t hda/trabajos-service:local       --build-arg SERVICE=trabajos       -f deploy/docker-compose/Dockerfile backend
docker build -t hda/integracion-service:local    --build-arg SERVICE=integracion    -f deploy/docker-compose/Dockerfile backend
docker build -t hda/notificaciones-service:local --build-arg SERVICE=notificaciones -f deploy/docker-compose/Dockerfile backend
docker build -t hda/usuarios-service:local       --build-arg SERVICE=usuarios       -f deploy/docker-compose/Dockerfile backend
docker build -t hda/proveedor-service:local      --build-arg SERVICE=proveedor      -f deploy/docker-compose/Dockerfile backend
docker build -t hda/trabajo-saga-service:local   --build-arg SERVICE=trabajo-saga   -f deploy/docker-compose/Dockerfile backend

minikube image load hda/trabajos-service:local       -p hda
minikube image load hda/integracion-service:local    -p hda
minikube image load hda/notificaciones-service:local -p hda
minikube image load hda/usuarios-service:local       -p hda
minikube image load hda/proveedor-service:local      -p hda
minikube image load hda/trabajo-saga-service:local   -p hda

# 5. Desplegar
kubectl apply -f deploy/k8s/base/
kubectl wait --for=condition=ready pod -l app=pulsar -n hda --timeout=180s
kubectl apply -f deploy/k8s/apps/
kubectl apply -f deploy/k8s/autoscaling/

# 6. Verificar
kubectl get pods -n hda
kubectl get scaledobject -n hda
kubectl get hpa -n hda
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

### Enviar metricas, trazas y logs al Grafana local (OTLP)

Los seis servicios exportan **metricas**, **trazas** y **logs** por OTLP/HTTP (Arconia OTel
-> `/v1/metrics`, `/v1/traces`, `/v1/logs`). El `ConfigMap hda-endpoints`
(`base/00-namespace-config.yaml`) trae `OTEL_COLLECTOR_HOST: "http://host.minikube.internal:4318"`
por defecto — el host-gateway de **minikube** (tu maquina, donde corre Grafana en `:4318`).
Si usas **Kind** en su lugar, el host-gateway es `host.docker.internal`, asi que sobreescribe
el host antes de desplegar las apps:

```bash
# Solo si usas Kind (en minikube ya es el valor por defecto):
kubectl patch configmap hda-endpoints -n hda --type merge \
  -p '{"data":{"OTEL_COLLECTOR_HOST":"http://host.docker.internal:4318"}}'

# Si ya habias desplegado apps/, reinicia para que tomen el nuevo valor:
kubectl rollout restart deployment -n hda \
  trabajos-service integracion-service notificaciones-service usuarios-service \
  proveedor-service trabajo-saga-service
```

> `host.minikube.internal` lo resuelve minikube automaticamente dentro de la VM; no hace
> falta tocar `/etc/hosts` ni `extra_hosts`. Confirma que tu Grafana escucha OTLP/HTTP en
> `0.0.0.0:4318` (no solo en `127.0.0.1`), o la VM no podra alcanzarlo.

Genera trafico y verifica que llega. Un `oha` contra `trabajos-service` produce metricas
(CPU, http server requests) y trazas (los spans del POST -> publicacion en Pulsar):

```bash
# con trabajos-service accesible por port-forward u la URL del NodePort (ver arriba)
oha -z 60s -c 50 -m POST -T 'application/json' \
  -d '{"clienteId":"11111111-1111-1111-1111-111111111113","categoriaServicio":"plomeria","urgencia":"ALTA","ciudad":"Bogota","origen":"MARKETPLACE","partnerId":null,"moneda":"COP"}' \
  http://localhost:8081/trabajos
```

- **Metricas:** en Grafana, explora la fuente de metricas (Prometheus/Mimir) y busca series
  con la etiqueta `service_name` = `trabajos`/`integracion`/`notificaciones`/`usuarios`/`proveedor`/`trabajo-saga`
  (ver `resource-attributes.service.name` en cada `application.yml`). El `step` de export es
  `1m`, asi que da hasta un minuto.
- **Trazas:** en la fuente de trazas (Tempo), filtra por `service.name` = `trabajos`; deben
  aparecer los spans del `POST /trabajos`.

Si no llega nada, revisa el export desde un pod:

```bash
kubectl logs -n hda deploy/trabajos-service | grep -i otlp
kubectl exec -n hda deploy/trabajos-service -- \
  sh -c 'wget -qO- http://host.minikube.internal:4318/ || echo "no alcanza el host:4318"'
```

### Teardown (minikube)

```bash
minikube delete -p hda
```

