#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-hda-codespaces}"

docker compose \
  --project-name "${PROJECT_NAME}" \
  --env-file "${REPO_ROOT}/deploy/docker-compose/.env" \
  -f "${REPO_ROOT}/deploy/docker-compose/docker-compose.yml" \
  -f "${SCRIPT_DIR}/docker-compose.codespaces.yml" \
  down

echo "Entorno detenido. Detén también el Codespace desde GitHub para no consumir la cuota gratuita."
