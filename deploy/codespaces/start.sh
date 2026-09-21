#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
COMPOSE_DIR="${REPO_ROOT}/deploy/docker-compose"
BASE_COMPOSE="${COMPOSE_DIR}/docker-compose.yml"
OVERRIDE_COMPOSE="${SCRIPT_DIR}/docker-compose.codespaces.yml"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-hda-codespaces}"

if [[ ! -f "${COMPOSE_DIR}/.env" ]]; then
  cp "${COMPOSE_DIR}/.env.example" "${COMPOSE_DIR}/.env"
fi

export COMPOSE_PARALLEL_LIMIT="${COMPOSE_PARALLEL_LIMIT:-1}"

echo "Construyendo el entorno (la primera ejecución puede tardar varios minutos)..."
docker compose \
  --project-name "${PROJECT_NAME}" \
  --env-file "${COMPOSE_DIR}/.env" \
  -f "${BASE_COMPOSE}" \
  -f "${OVERRIDE_COMPOSE}" \
  up -d --build

echo "Esperando que el BFF responda..."
for attempt in $(seq 1 90); do
  if curl --fail --silent http://localhost:8090/actuator/health >/dev/null; then
    break
  fi
  if [[ "${attempt}" -eq 90 ]]; then
    echo "El BFF no respondió a tiempo. Revisa: docker compose --project-name ${PROJECT_NAME} --env-file ${COMPOSE_DIR}/.env -f ${BASE_COMPOSE} -f ${OVERRIDE_COMPOSE} logs bff-service" >&2
    exit 1
  fi
  sleep 5
done

if [[ -n "${CODESPACE_NAME:-}" ]] && command -v gh >/dev/null 2>&1; then
  gh codespace ports visibility 8090:public -c "${CODESPACE_NAME}" >/dev/null
fi

echo "Entorno listo."
echo "Salud local: http://localhost:8090/actuator/health"

if [[ -n "${CODESPACE_NAME:-}" && -n "${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN:-}" ]]; then
  PUBLIC_URL="https://${CODESPACE_NAME}-8090.${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN}"
  echo "BFF público: ${PUBLIC_URL}"
  echo "Usa esa URL como valor de baseUrl en Postman."
else
  echo "Fuera de Codespaces, publica manualmente el puerto 8090 si necesitas acceso externo."
fi
