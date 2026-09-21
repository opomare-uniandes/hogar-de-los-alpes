#!/bin/bash
# Crea la base de datos de proveedor-service (separada de hda_trabajos/hda_usuarios, mismo
# criterio de topologia de datos descentralizada que 01-create-usuarios-db.sh).
#
# docker-entrypoint-initdb.d SOLO corre estos scripts la primera vez que se inicializa
# el volumen de datos de Postgres (volumen vacio). Si ya tenias el contenedor corriendo
# de antes de que existiera este script, hay que recrear el volumen para que se aplique:
#   docker compose down -v && docker compose up -d   (desde deploy/docker-compose/)
set -euo pipefail

DB_PROVEEDOR="${POSTGRES_PROVEEDOR_DB:-hda_proveedor}"

EXISTE=$(psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -tAc \
    "SELECT 1 FROM pg_database WHERE datname = '${DB_PROVEEDOR}'")

if [ "$EXISTE" != "1" ]; then
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -c \
        "CREATE DATABASE \"${DB_PROVEEDOR}\" OWNER \"${POSTGRES_USER}\";"
fi
