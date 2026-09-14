import json
from pathlib import Path

from fastavro import parse_schema


CONTRACTS_DIR = Path(__file__).resolve().parents[4] / "contracts"


def test_todos_los_contratos_son_esquemas_avro_validos():
    schemas = sorted(CONTRACTS_DIR.rglob("*.avsc"))
    assert {schema.name for schema in schemas} == {
        "crear_trabajo_command_v1.avsc",
        "solicitud_trabajo_partner_v1.avsc",
        "solicitud_trabajo_partner_v2.avsc",
        "solicitud_trabajo_rechazada_v1.avsc",
    }

    for schema_path in schemas:
        with schema_path.open(encoding="utf-8") as schema_file:
            parse_schema(json.load(schema_file))
