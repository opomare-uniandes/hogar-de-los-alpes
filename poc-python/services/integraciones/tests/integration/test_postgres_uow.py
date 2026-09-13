import os
from pathlib import Path

import psycopg
import pytest
from sqlalchemy import func, select

from integraciones.application import EstadoRecepcion, RecibirSolicitud
from integraciones.infrastructure.contracts import SolicitudTrabajoPartnerV1Create
from integraciones.infrastructure.persistence.postgres import (
    SqlAlchemyUnitOfWork,
    crear_engine,
    crear_session_factory,
    outbox_table,
    solicitudes_table,
)
from integraciones.infrastructure.translators import traducir_v1


DATABASE_URL = os.getenv("TEST_DATABASE_URL")
MIGRATION = Path(__file__).resolve().parents[2] / "migrations" / "001_init.sql"

pytestmark = pytest.mark.skipif(
    not DATABASE_URL,
    reason="TEST_DATABASE_URL no configurada; requiere PostgreSQL real",
)


@pytest.fixture(scope="module")
def persistence():
    assert DATABASE_URL is not None
    with psycopg.connect(DATABASE_URL.replace("postgresql+psycopg", "postgresql")) as conn:
        conn.execute(MIGRATION.read_text(encoding="utf-8"))

    engine = crear_engine(DATABASE_URL)
    session_factory = crear_session_factory(engine)
    yield engine, session_factory
    engine.dispose()


@pytest.fixture(autouse=True)
def limpiar_tablas(persistence) -> None:
    engine, _ = persistence
    with engine.begin() as connection:
        connection.execute(outbox_table.delete())
        connection.execute(solicitudes_table.delete())


def test_transaccion_real_persiste_solicitud_y_outbox(
    persistence,
    evento_v1: dict[str, object],
) -> None:
    engine, session_factory = persistence
    caso_uso = RecibirSolicitud(
        lambda: SqlAlchemyUnitOfWork(session_factory),
        lambda: 1_789_322_401_000,
    )
    comando = traducir_v1(SolicitudTrabajoPartnerV1Create.model_validate(evento_v1))

    resultado = caso_uso.ejecutar(comando)

    assert resultado.estado is EstadoRecepcion.PROCESADA
    with engine.connect() as connection:
        solicitud = connection.execute(select(solicitudes_table)).mappings().one()
        outbox = connection.execute(select(outbox_table)).mappings().one()
    assert solicitud["source_contract_version"] == "v1"
    assert solicitud["source_event_id"] == comando.source_event_id
    assert outbox["estado"] == "PENDIENTE"
    assert outbox["payload"]["data"]["externalRequestId"] == "SIN-2026-0001"


def test_reentrega_es_idempotente_en_postgresql(
    persistence,
    evento_v1: dict[str, object],
) -> None:
    engine, session_factory = persistence
    caso_uso = RecibirSolicitud(
        lambda: SqlAlchemyUnitOfWork(session_factory),
        lambda: 1_789_322_401_000,
    )
    comando = traducir_v1(SolicitudTrabajoPartnerV1Create.model_validate(evento_v1))

    primero = caso_uso.ejecutar(comando)
    segundo = caso_uso.ejecutar(comando)

    assert primero.estado is EstadoRecepcion.PROCESADA
    assert segundo.estado is EstadoRecepcion.DUPLICADA
    with engine.connect() as connection:
        solicitudes = connection.scalar(select(func.count()).select_from(solicitudes_table))
        mensajes = connection.scalar(select(func.count()).select_from(outbox_table))
    assert solicitudes == 1
    assert mensajes == 1


def test_sin_commit_se_revierten_todas_las_escrituras(
    persistence,
    evento_v1: dict[str, object],
) -> None:
    engine, session_factory = persistence
    comando = traducir_v1(SolicitudTrabajoPartnerV1Create.model_validate(evento_v1))

    with SqlAlchemyUnitOfWork(session_factory) as uow:
        from integraciones.domain import SolicitudIntegracion

        uow.solicitudes.agregar(
            SolicitudIntegracion.registrar(comando, recibida_en_ms=1_789_322_401_000)
        )

    with engine.connect() as connection:
        solicitudes = connection.scalar(select(func.count()).select_from(solicitudes_table))
        mensajes = connection.scalar(select(func.count()).select_from(outbox_table))
    assert solicitudes == 0
    assert mensajes == 0

