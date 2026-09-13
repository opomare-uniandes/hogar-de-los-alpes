from collections.abc import Callable
from dataclasses import dataclass
from enum import Enum

from integraciones.application.ports import UnidadDeTrabajo
from integraciones.domain.commands import CrearTrabajoCommand
from integraciones.domain.outbox import MensajeOutbox
from integraciones.domain.solicitudes import SolicitudIntegracion


TOPIC_CREAR_TRABAJO_V1 = "persistent://hda/trabajos/comandos/crear-trabajo-v1"


class EstadoRecepcion(str, Enum):
    PROCESADA = "PROCESADA"
    DUPLICADA = "DUPLICADA"


@dataclass(frozen=True, slots=True)
class ResultadoRecepcion:
    estado: EstadoRecepcion
    solicitud_id: str


class RecibirSolicitud:
    """Persiste una solicitud y su comando canónico de forma atómica e idempotente."""

    def __init__(
        self,
        uow_factory: Callable[[], UnidadDeTrabajo],
        clock_ms: Callable[[], int],
    ) -> None:
        self._uow_factory = uow_factory
        self._clock_ms = clock_ms

    def ejecutar(self, comando: CrearTrabajoCommand) -> ResultadoRecepcion:
        with self._uow_factory() as uow:
            existente = uow.solicitudes.buscar_por_clave(
                comando.partner_id,
                comando.external_request_id,
            )
            if existente is not None:
                return ResultadoRecepcion(
                    estado=EstadoRecepcion.DUPLICADA,
                    solicitud_id=str(existente.id),
                )

            solicitud = SolicitudIntegracion.registrar(
                comando,
                recibida_en_ms=self._clock_ms(),
            )
            mensaje = MensajeOutbox.para_crear_trabajo(
                solicitud,
                topic=TOPIC_CREAR_TRABAJO_V1,
            )

            uow.solicitudes.agregar(solicitud)
            uow.outbox.agregar(mensaje)
            uow.commit()

            return ResultadoRecepcion(
                estado=EstadoRecepcion.PROCESADA,
                solicitud_id=str(solicitud.id),
            )
