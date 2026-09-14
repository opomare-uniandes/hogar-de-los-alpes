from integraciones.domain import CrearTrabajoCommand, TranslationError, Urgencia
from integraciones.infrastructure.contracts import (
    SolicitudTrabajoPartnerV1Create,
    SolicitudTrabajoPartnerV2Create,
)


CATEGORIAS = {
    "PLUMBING": "PLOMERIA",
    "ELECTRICAL": "ELECTRICIDAD",
    "LOCKSMITH": "CERRAJERIA",
}

URGENCIAS = {
    "P4": Urgencia.BAJA,
    "P3": Urgencia.MEDIA,
    "P2": Urgencia.ALTA,
    "P1": Urgencia.CRITICA,
}

CIUDADES = {
    "BOG": "BOGOTA",
    "MDE": "MEDELLIN",
    "CLO": "CALI",
    "MEX": "CIUDAD_DE_MEXICO",
}


def _buscar(mapa: dict[str, object], valor: str, campo: str) -> object:
    try:
        return mapa[valor.strip().upper()]
    except KeyError as exc:
        raise TranslationError(f"{campo} no soportado: {valor}") from exc


def traducir_v1(evento: SolicitudTrabajoPartnerV1Create) -> CrearTrabajoCommand:
    data = evento.data
    return CrearTrabajoCommand.crear(
        correlation_id=evento.correlation_id,
        source_event_id=evento.id,
        source_contract_version="v1",
        partner_id=data.partner_code,
        external_request_id=data.request_number,
        cliente_id=data.insured_customer_id,
        categoria_servicio=str(_buscar(CATEGORIAS, data.assistance_code, "assistanceCode")),
        urgencia=_buscar(URGENCIAS, data.priority_code, "priorityCode"),  # type: ignore[arg-type]
        ciudad=str(_buscar(CIUDADES, data.city_code, "cityCode")),
        pais="CO",
        moneda=data.currency_code,
        fecha_solicitud_ms=data.requested_at,
    )


def traducir_v2(evento: SolicitudTrabajoPartnerV2Create) -> CrearTrabajoCommand:
    data = evento.data
    return CrearTrabajoCommand.crear(
        correlation_id=evento.correlation_id,
        source_event_id=evento.id,
        source_contract_version="v2",
        partner_id=data.partner.code,
        external_request_id=data.request.number,
        cliente_id=data.insured.customer_id,
        categoria_servicio=str(_buscar(CATEGORIAS, data.service.code, "service.code")),
        urgencia=_buscar(URGENCIAS, data.service.priority, "service.priority"),  # type: ignore[arg-type]
        ciudad=str(_buscar(CIUDADES, data.location.city_code, "location.cityCode")),
        pais=data.location.country_code,
        moneda=data.payment.currency_code,
        fecha_solicitud_ms=data.request.requested_at,
    )
