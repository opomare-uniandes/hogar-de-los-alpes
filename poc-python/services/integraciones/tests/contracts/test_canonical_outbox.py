import json
from pathlib import Path

from fastavro import parse_schema
from fastavro.validation import validate

from integraciones.application.recibir_solicitud import TOPIC_CREAR_TRABAJO_V1
from integraciones.domain import MensajeOutbox, SolicitudIntegracion
from integraciones.infrastructure.contracts import SolicitudTrabajoPartnerV1Create
from integraciones.infrastructure.translators import traducir_v1


CONTRACTS_DIR = Path(__file__).resolve().parents[4] / "contracts"


def test_outbox_cumple_con_contrato_avro_canonico(
    evento_v1: dict[str, object],
) -> None:
    comando = traducir_v1(SolicitudTrabajoPartnerV1Create.model_validate(evento_v1))
    solicitud = SolicitudIntegracion.registrar(
        comando,
        recibida_en_ms=1_789_322_401_000,
    )
    mensaje = MensajeOutbox.para_crear_trabajo(
        solicitud,
        topic=TOPIC_CREAR_TRABAJO_V1,
    )

    with (CONTRACTS_DIR / "commands/crear_trabajo_command_v1.avsc").open(
        encoding="utf-8"
    ) as schema_file:
        schema = parse_schema(json.load(schema_file))
    assert validate(mensaje.payload, schema)
