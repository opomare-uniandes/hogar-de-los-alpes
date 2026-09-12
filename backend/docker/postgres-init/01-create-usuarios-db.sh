#!/bin/bash
# Crea la base de datos de usuarios-service (separada de hda_trabajos, para mantener la
# topologia de datos descentralizada: cada servicio con su propia base/esquema).
#
# docker-entrypoint-initdb.d SOLO corre estos scripts la primera vez que se inicializa
# el volumen de datos de Postgres (volumen vacio). Si ya tenias el contenedor corriendo
# de antes de que existiera este script, hay que recrear el volumen para que se aplique:
#   docker compose --env-file backend/.env -f backend/docker-compose.yml down -v
#   docker compose --env-file backend/.env -f backend/docker-compose.yml up -d
set -euo pipefail

DB_USUARIOS="${POSTGRES_USUARIOS_DB:-hda_usuarios}"

# -d postgres: nos conectamos a la base de mantenimiento (siempre existe), no a una base
# con el mismo nombre del usuario (esa no existe y psql la usa por defecto si no se indica).
EXISTE=$(psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -tAc \
    "SELECT 1 FROM pg_database WHERE datname = '${DB_USUARIOS}'")

if [ "$EXISTE" != "1" ]; then
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -c \
        "CREATE DATABASE \"${DB_USUARIOS}\" OWNER \"${POSTGRES_USER}\";"
fi
