#!/usr/bin/env bash
# Construye y publica las siete imagenes que componen la entrega.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TF_DIR="$SCRIPT_DIR/../terraform"
BACKEND_DIR="$SCRIPT_DIR/../../backend"

AWS_PROFILE="${AWS_PROFILE:-$(terraform -chdir="$TF_DIR" output -raw aws_profile)}"
AWS_REGION="${AWS_REGION:-$(terraform -chdir="$TF_DIR" output -raw aws_region)}"
export AWS_PROFILE AWS_REGION

ECR_JSON=$(terraform -chdir="$TF_DIR" output -json ecr_repository_urls)
REGISTRY=$(echo "$ECR_JSON" | python3 -c 'import json,sys; print(next(iter(json.load(sys.stdin).values())).split("/")[0])')

aws ecr get-login-password --region "$AWS_REGION" --profile "$AWS_PROFILE" \
  | docker login --username AWS --password-stdin "$REGISTRY"

SERVICES=(trabajos integracion notificaciones usuarios proveedor trabajo-saga bff)
for SERVICE in "${SERVICES[@]}"; do
  REPOSITORY_NAME="${SERVICE}-service"
  IMAGE=$(echo "$ECR_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin)['${REPOSITORY_NAME}'])")
  echo "Construyendo ${REPOSITORY_NAME}..."
  docker build \
    --build-arg "SERVICE=${SERVICE}" \
    --tag "${IMAGE}:latest" \
    --file "$SCRIPT_DIR/../docker-compose/Dockerfile" \
    "$BACKEND_DIR"
  docker push "${IMAGE}:latest"
done

echo "Siete imagenes publicadas en ECR."
