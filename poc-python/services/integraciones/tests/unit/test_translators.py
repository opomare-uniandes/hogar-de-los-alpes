import pytest
from pydantic import ValidationError

from integraciones.domain import TranslationError, Urgencia
from integraciones.infrastructure.contracts import (
    SolicitudTrabajoPartnerV1Create,
    SolicitudTrabajoPartnerV2Create,
)
from integraciones.infrastructure.translators import traducir_v1, traducir_v2


def test_v1_y_v2_equivalentes_producen_mismo_comando(evento_v1, evento_v2):
    comando_v1 = traducir_v1(SolicitudTrabajoPartnerV1Create.model_validate(evento_v1))
    comando_v2 = traducir_v2(SolicitudTrabajoPartnerV2Create.model_validate(evento_v2))

    assert comando_v1.command_id == comando_v2.command_id
    assert comando_v1.campos_canonicos() == comando_v2.campos_canonicos()
    assert comando_v1.urgencia is Urgencia.ALTA


def test_v1_rechaza_campos_desconocidos(evento_v1, copiar):
    invalido = copiar(evento_v1)
    invalido["data"]["campoNoContratado"] = "no permitido"

    with pytest.raises(ValidationError):
        SolicitudTrabajoPartnerV1Create.model_validate(invalido)


def test_v2_rechaza_tipo_de_evento_incorrecto(evento_v2, copiar):
    invalido = copiar(evento_v2)
    invalido["type"] = "com.hda.partner.solicitud-trabajo.v3"

    with pytest.raises(ValidationError):
        SolicitudTrabajoPartnerV2Create.model_validate(invalido)


def test_traductor_rechaza_codigo_semantico_desconocido(evento_v1, copiar):
    invalido = copiar(evento_v1)
    invalido["data"]["assistanceCode"] = "UNKNOWN_SERVICE"
    contrato = SolicitudTrabajoPartnerV1Create.model_validate(invalido)

    with pytest.raises(TranslationError, match="assistanceCode no soportado"):
        traducir_v1(contrato)

