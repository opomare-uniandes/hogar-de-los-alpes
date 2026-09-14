import pytest

from integraciones.infrastructure.persistence.postgres import (
    crear_engine,
    outbox_table,
    solicitudes_table,
)


def test_modelo_declara_clave_unica_de_idempotencia() -> None:
    constraints = {constraint.name for constraint in solicitudes_table.constraints}
    assert "uq_solicitud_partner_external" in constraints


def test_outbox_referencia_al_agregado() -> None:
    foreign_keys = {str(key.column) for key in outbox_table.foreign_keys}
    assert "solicitudes_integracion.id" in foreign_keys


def test_rechaza_motores_que_no_sean_postgresql() -> None:
    with pytest.raises(ValueError, match="PostgreSQL"):
        crear_engine("sqlite+pysqlite:///:memory:")
