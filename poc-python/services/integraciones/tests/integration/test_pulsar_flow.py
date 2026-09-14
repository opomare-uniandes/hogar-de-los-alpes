import os
from pathlib import Path

import psycopg
import pulsar
import pytest
from sqlalchemy import select

from integraciones.application import EstadoRecepcion, RecibirSolicitud
from integraciones.infrastructure.messaging.avro import CatalogoAvro
from integraciones.infrastructure.messaging.pulsar_adapter import (
    TOPIC_CREAR_TRABAJO_V1,
    TOPIC_SOLICITUD_V1,
    TOPIC_SOLICITUD_V2,
    ConsumidorSolicitudes,
    PublicadorOutbox,
)
from integraciones.infrastructure.persistence.postgres import (
    SqlAlchemyUnitOfWork,
    crear_engine,
    crear_session_factory,
    outbox_table,
)


DATABASE_URL = os.getenv("TEST_DATABASE_URL")
PULSAR_URL = os.getenv("TEST_PULSAR_URL")
SERVICE_DIR = Path(__file__).resolve().parents[2]
MIGRATION = SERVICE_DIR / "migrations" / "001_init.sql"
CONTRACTS_DIR = SERVICE_DIR.parents[1] / "contracts"

pytestmark = pytest.mark.skipif(
    not DATABASE_URL or not PULSAR_URL,
    reason="requiere TEST_DATABASE_URL y TEST_PULSAR_URL",
)


@pytest.mark.parametrize(
    ("version", "fixture_name", "topic", "schema_path"),
    [
        (
            "v1",
            "evento_v1",
            TOPIC_SOLICITUD_V1,
            "integration/solicitud_trabajo_partner_v1.avsc",
        ),
        (
            "v2",
            "evento_v2",
            TOPIC_SOLICITUD_V2,
            "integration/solicitud_trabajo_partner_v2.avsc",
        ),
    ],
)
def test_version_llega_por_pulsar_y_outbox_publica_comando_canonico(
    request,
    version: str,
    fixture_name: str,
    topic: str,
    schema_path: str,
) -> None:
    assert DATABASE_URL is not None
    assert PULSAR_URL is not None
    with psycopg.connect(DATABASE_URL.replace("postgresql+psycopg", "postgresql")) as conn:
        conn.execute(MIGRATION.read_text(encoding="utf-8"))
        conn.execute("DELETE FROM outbox_messages")
        conn.execute("DELETE FROM solicitudes_integracion")

    engine = crear_engine(DATABASE_URL)
    session_factory = crear_session_factory(engine)
    client = pulsar.Client(PULSAR_URL)
    catalogo = CatalogoAvro(CONTRACTS_DIR)
    caso_uso = RecibirSolicitud(
        lambda: SqlAlchemyUnitOfWork(session_factory),
        lambda: 1_789_322_401_000,
    )
    evento = request.getfixturevalue(fixture_name)

    entrada = getattr(ConsumidorSolicitudes, version)(client, catalogo, caso_uso)
    salida = client.subscribe(
        TOPIC_CREAR_TRABAJO_V1,
        subscription_name=f"prueba-canonico-{version}",
        schema=catalogo.obtener("commands/crear_trabajo_command_v1.avsc"),
        initial_position=pulsar.InitialPosition.Latest,
    )
    partner = client.create_producer(
        topic,
        schema=catalogo.obtener(schema_path),
        producer_name=f"partner-prueba-{version}",
    )
    publicador = PublicadorOutbox(client, catalogo, session_factory)

    try:
        partner.send(evento)
        resultado = entrada.procesar_siguiente(timeout_millis=10_000)
        publicados = publicador.publicar_pendientes()
        comando = salida.receive(timeout_millis=10_000)

        assert resultado is not None
        assert resultado.estado is EstadoRecepcion.PROCESADA
        assert publicados == 1
        assert comando.value()["data"]["categoriaServicio"] == "PLOMERIA"
        assert comando.properties()["eventType"] == "com.hda.trabajos.crear.v1"

        salida.acknowledge(comando)
        with engine.connect() as connection:
            outbox = connection.execute(select(outbox_table)).mappings().one()
        assert outbox["estado"] == "PUBLICADO"
        assert outbox["publicado_en_ms"] is not None
    finally:
        partner.close()
        entrada.close()
        salida.close()
        publicador.close()
        client.close()
        engine.dispose()
