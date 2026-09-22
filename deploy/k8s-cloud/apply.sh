#!/usr/bin/env bash
# Renders the __PLACEHOLDER__ tokens in base/, apps/ and autoscaling/ from Terraform outputs
# (RDS/Redis endpoints, RDS password, ECR image URLs) and applies everything with kubectl.
# Nothing secret is ever written to a committed file: rendering happens into a throwaway
# temp directory that is removed on exit.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TF_DIR="$SCRIPT_DIR/../terraform"

# Account-specific values (profile, region) come from Terraform's own outputs, which read
# them from the gitignored sandbox.auto.tfvars -- nothing account-specific is hardcoded here.
AWS_PROFILE="${AWS_PROFILE:-$(terraform -chdir="$TF_DIR" output -raw aws_profile)}"
AWS_REGION="${AWS_REGION:-$(terraform -chdir="$TF_DIR" output -raw aws_region)}"
export AWS_PROFILE AWS_REGION

CLUSTER_NAME=$(terraform -chdir="$TF_DIR" output -raw eks_cluster_name)
RDS_ENDPOINT=$(terraform -chdir="$TF_DIR" output -raw rds_endpoint)
REDIS_ENDPOINT=$(terraform -chdir="$TF_DIR" output -raw redis_endpoint)
POSTGRES_PASSWORD=$(terraform -chdir="$TF_DIR" output -raw rds_master_password)
OTEL_COLLECTOR_HOST=$(terraform -chdir="$TF_DIR" output -raw otel_collector_host)
OTEL_EXPORTER_OTLP_HEADERS_AUTHORIZATION=$(terraform -chdir="$TF_DIR" output -raw otel_exporter_otlp_headers_authorization)

ECR_JSON=$(terraform -chdir="$TF_DIR" output -json ecr_repository_urls)
TRABAJOS_IMAGE="$(echo "$ECR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["trabajos-service"] + ":latest")')"
INTEGRACION_IMAGE="$(echo "$ECR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["integracion-service"] + ":latest")')"
NOTIFICACIONES_IMAGE="$(echo "$ECR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["notificaciones-service"] + ":latest")')"
USUARIOS_IMAGE="$(echo "$ECR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["usuarios-service"] + ":latest")')"
PROVEEDOR_IMAGE="$(echo "$ECR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["proveedor-service"] + ":latest")')"
TRABAJO_SAGA_IMAGE="$(echo "$ECR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["trabajo-saga-service"] + ":latest")')"
BFF_IMAGE="$(echo "$ECR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["bff-service"] + ":latest")')"

echo "Cluster: $CLUSTER_NAME"
aws eks update-kubeconfig --name "$CLUSTER_NAME" --region "$AWS_REGION" --profile "$AWS_PROFILE"

RENDER_DIR="$(mktemp -d)"
trap 'rm -rf "$RENDER_DIR"' EXIT

cp -r "$SCRIPT_DIR"/base "$SCRIPT_DIR"/apps "$SCRIPT_DIR"/autoscaling "$RENDER_DIR"/

# Python evita las diferencias entre GNU sed y BSD sed (macOS).
export RENDER_DIR RDS_ENDPOINT REDIS_ENDPOINT POSTGRES_PASSWORD
export OTEL_COLLECTOR_HOST OTEL_EXPORTER_OTLP_HEADERS_AUTHORIZATION
export TRABAJOS_IMAGE INTEGRACION_IMAGE NOTIFICACIONES_IMAGE USUARIOS_IMAGE
export PROVEEDOR_IMAGE TRABAJO_SAGA_IMAGE BFF_IMAGE
python3 - <<'PY'
import os
from pathlib import Path

replacements = {
    "__RDS_ENDPOINT__": os.environ["RDS_ENDPOINT"],
    "__REDIS_ENDPOINT__": os.environ["REDIS_ENDPOINT"],
    "__POSTGRES_PASSWORD__": os.environ["POSTGRES_PASSWORD"],
    "__OTEL_COLLECTOR_HOST__": os.environ["OTEL_COLLECTOR_HOST"],
    "__OTEL_EXPORTER_OTLP_HEADERS_AUTHORIZATION__": os.environ["OTEL_EXPORTER_OTLP_HEADERS_AUTHORIZATION"],
    "__TRABAJOS_IMAGE__": os.environ["TRABAJOS_IMAGE"],
    "__INTEGRACION_IMAGE__": os.environ["INTEGRACION_IMAGE"],
    "__NOTIFICACIONES_IMAGE__": os.environ["NOTIFICACIONES_IMAGE"],
    "__USUARIOS_IMAGE__": os.environ["USUARIOS_IMAGE"],
    "__PROVEEDOR_IMAGE__": os.environ["PROVEEDOR_IMAGE"],
    "__TRABAJO_SAGA_IMAGE__": os.environ["TRABAJO_SAGA_IMAGE"],
    "__BFF_IMAGE__": os.environ["BFF_IMAGE"],
}

for path in Path(os.environ["RENDER_DIR"]).rglob("*.yaml"):
    content = path.read_text()
    for placeholder, value in replacements.items():
        content = content.replace(placeholder, value)
    path.write_text(content)
PY

kubectl apply -f "$RENDER_DIR/base"
kubectl wait --for=condition=ready pod -l app=pulsar -n hda --timeout=180s
kubectl wait --for=condition=complete job/postgres-databases-bootstrap-v2 -n hda --timeout=180s
kubectl apply -f "$RENDER_DIR/apps"
kubectl apply -f "$RENDER_DIR/autoscaling"

echo
echo "Pods:"
kubectl get pods -n hda

echo
echo "Esperando la URL del ALB (puede tardar 1-2 min tras el primer apply)..."
for _ in $(seq 1 24); do
  ADDR=$(kubectl get gateway hda-gateway -n hda -o jsonpath='{.status.addresses[0].value}' 2>/dev/null || true)
  if [ -n "$ADDR" ]; then
    echo "BFF listo: http://$ADDR"
    echo "Health:    http://$ADDR/actuator/health"
    exit 0
  fi
  sleep 5
done
echo "El Gateway aun no tiene direccion asignada; revisa con:"
echo "  kubectl get gateway hda-gateway -n hda -o yaml"
