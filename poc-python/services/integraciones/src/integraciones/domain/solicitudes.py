from dataclasses import dataclass
from enum import Enum
from uuid import UUID

from integraciones.domain.commands import CrearTrabajoCommand


class EstadoSolicitud(str, Enum):
    RECIBIDA = "RECIBIDA"


@dataclass(frozen=True, slots=True)
class SolicitudIntegracion:
    """Agregado que representa una solicitud externa aceptada por el ACL."""

    id: UUID
    partner_id: str
    external_request_id: str
    correlation_id: UUID
    estado: EstadoSolicitud
    comando: CrearTrabajoCommand
    recibida_en_ms: int

    @classmethod
    def registrar(
        cls,
        comando: CrearTrabajoCommand,
        *,
        recibida_en_ms: int,
    ) -> "SolicitudIntegracion":
        if recibida_en_ms <= 0:
            raise ValueError("recibida_en_ms debe ser positivo")

        return cls(
            id=comando.command_id,
            partner_id=comando.partner_id,
            external_request_id=comando.external_request_id,
            correlation_id=comando.correlation_id,
            estado=EstadoSolicitud.RECIBIDA,
            comando=comando,
            recibida_en_ms=recibida_en_ms,
        )

