import logging
import os
from pathlib import Path
from time import sleep
from time import time_ns

import psycopg

from integraciones.application import RecibirSolicitud
from integraciones.infrastructure.messaging import ConsumidorSolicitudes, PublicadorOutbox, crear_cliente
from integraciones.infrastructure.messaging.pulsar_adapter import crear_catalogo
from integraciones.infrastructure.persistence import (
    SqlAlchemyUnitOfWork,
    crear_engine,
    crear_session_factory,
)


LOGGER = logging.getLogger(__name__)
SERVICE_DIR = Path(__file__).resolve().parents[2]
POC_DIR = Path(__file__).resolve().parents[4]


def _clock_ms() -> int:
    return time_ns() // 1_000_000


def _aplicar_migracion(database_url: str) -> None:
    migration = Path(
        os.getenv("MIGRATION_PATH", str(SERVICE_DIR / "migrations" / "001_init.sql"))
    )
    psycopg_url = database_url.replace("postgresql+psycopg", "postgresql")
    with psycopg.connect(psycopg_url) as connection:
        connection.execute(migration.read_text(encoding="utf-8"))


def _reintentar(descripcion: str, operacion, intentos: int = 30):
    """Espera dependencias de infraestructura sin mezclar reintentos con dominio."""

    for intento in range(1, intentos + 1):
        try:
            return operacion()
        except Exception:
            if intento == intentos:
                raise
            LOGGER.warning("esperando %s (intento %s/%s)", descripcion, intento, intentos)
            sleep(2)


def construir_runtime():
    """Construye adaptadores desde ambiente sin mezclar transporte con dominio."""

    database_url = os.getenv(
        "DATABASE_URL",
        "postgresql+psycopg://hda:hda@postgres:5432/hda_integraciones",
    )
    pulsar_url = os.getenv("PULSAR_URL", "pulsar://pulsar:6650")
    contracts_dir = Path(os.getenv("CONTRACTS_DIR", str(POC_DIR / "contracts")))

    _reintentar("PostgreSQL", lambda: _aplicar_migracion(database_url))
    engine = crear_engine(database_url)
    session_factory = crear_session_factory(engine)
    use_case = RecibirSolicitud(
        lambda: SqlAlchemyUnitOfWork(session_factory),
        _clock_ms,
    )
    client = _reintentar("Pulsar", lambda: crear_cliente(pulsar_url))
    catalogo = crear_catalogo(contracts_dir)
    consumers = [
        ConsumidorSolicitudes.v1(client, catalogo, use_case),
        ConsumidorSolicitudes.v2(client, catalogo, use_case),
    ]
    publisher = PublicadorOutbox(client, catalogo, session_factory, _clock_ms)
    return engine, client, consumers, publisher


def main() -> None:
    """Ejecuta los consumidores V1/V2 y drena el outbox canónico."""

    logging.basicConfig(level=logging.INFO)
    engine, client, consumers, publisher = construir_runtime()
    LOGGER.info("integraciones-service listo: consume V1/V2 y publica outbox")
    try:
        while True:
            for consumer in consumers:
                try:
                    consumer.procesar_siguiente(timeout_millis=200)
                except Exception:
                    LOGGER.exception("error procesando solicitud; Pulsar reintentara")
            publisher.publicar_pendientes()
    except KeyboardInterrupt:
        LOGGER.info("deteniendo integraciones-service")
    finally:
        for consumer in consumers:
            consumer.close()
        publisher.close()
        client.close()
        engine.dispose()


if __name__ == "__main__":
    main()
