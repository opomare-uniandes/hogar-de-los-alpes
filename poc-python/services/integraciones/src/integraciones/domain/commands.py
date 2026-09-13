from dataclasses import dataclass
from enum import Enum
from uuid import UUID, uuid5


COMMAND_NAMESPACE = UUID("6842e7d4-6c51-4f7f-8e90-d198f9a7b14e")


class Urgencia(str, Enum):
    BAJA = "BAJA"
    MEDIA = "MEDIA"
    ALTA = "ALTA"
    CRITICA = "CRITICA"


@dataclass(frozen=True, slots=True)
class CrearTrabajoCommand:
    """Representacion interna del comando canonico dirigido a Trabajos."""

    command_id: UUID
    correlation_id: UUID
    partner_id: str
    external_request_id: str
    cliente_id: str
    categoria_servicio: str
    urgencia: Urgencia
    ciudad: str
    pais: str
    moneda: str
    fecha_solicitud_ms: int

    @classmethod
    def crear(
        cls,
        *,
        correlation_id: UUID,
        partner_id: str,
        external_request_id: str,
        cliente_id: str,
        categoria_servicio: str,
        urgencia: Urgencia,
        ciudad: str,
        pais: str,
        moneda: str,
        fecha_solicitud_ms: int,
    ) -> "CrearTrabajoCommand":
        command_id = uuid5(
            COMMAND_NAMESPACE,
            f"{partner_id.strip()}:{external_request_id.strip()}",
        )
        return cls(
            command_id=command_id,
            correlation_id=correlation_id,
            partner_id=partner_id.strip(),
            external_request_id=external_request_id.strip(),
            cliente_id=cliente_id.strip(),
            categoria_servicio=categoria_servicio.strip(),
            urgencia=urgencia,
            ciudad=ciudad.strip(),
            pais=pais.strip().upper(),
            moneda=moneda.strip().upper(),
            fecha_solicitud_ms=fecha_solicitud_ms,
        )

    def campos_canonicos(self) -> tuple[object, ...]:
        """Campos usados para probar equivalencia semantica entre versiones."""

        return (
            self.partner_id,
            self.external_request_id,
            self.cliente_id,
            self.categoria_servicio,
            self.urgencia,
            self.ciudad,
            self.pais,
            self.moneda,
            self.fecha_solicitud_ms,
        )

