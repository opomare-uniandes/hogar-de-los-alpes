from dataclasses import dataclass
from uuid import UUID

from integraciones.domain.solicitudes import SolicitudIntegracion


@dataclass(frozen=True, slots=True)
class MensajeOutbox:
    """Mensaje canónico pendiente de publicación, creado en la misma transacción."""

    id: UUID
    aggregate_id: UUID
    topic: str
    event_type: str
    correlation_id: UUID
    payload: dict[str, object]
    creado_en_ms: int

    @classmethod
    def para_crear_trabajo(
        cls,
        solicitud: SolicitudIntegracion,
        *,
        topic: str,
    ) -> "MensajeOutbox":
        comando = solicitud.comando
        return cls(
            id=comando.command_id,
            aggregate_id=solicitud.id,
            topic=topic,
            event_type="com.hogardelosalpes.trabajos.CrearTrabajoCommand.v1",
            correlation_id=comando.correlation_id,
            payload={
                "commandId": str(comando.command_id),
                "correlationId": str(comando.correlation_id),
                "partnerId": comando.partner_id,
                "externalRequestId": comando.external_request_id,
                "clienteId": comando.cliente_id,
                "categoriaServicio": comando.categoria_servicio,
                "urgencia": comando.urgencia.value,
                "ciudad": comando.ciudad,
                "pais": comando.pais,
                "moneda": comando.moneda,
                "fechaSolicitud": comando.fecha_solicitud_ms,
            },
            creado_en_ms=solicitud.recibida_en_ms,
        )

