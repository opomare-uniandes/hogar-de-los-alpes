from types import TracebackType
from typing import Protocol

from integraciones.domain.outbox import MensajeOutbox
from integraciones.domain.solicitudes import SolicitudIntegracion


class RepositorioSolicitudes(Protocol):
    def buscar_por_clave(
        self,
        partner_id: str,
        external_request_id: str,
    ) -> SolicitudIntegracion | None: ...

    def agregar(self, solicitud: SolicitudIntegracion) -> None: ...


class RepositorioOutbox(Protocol):
    def agregar(self, mensaje: MensajeOutbox) -> None: ...


class UnidadDeTrabajo(Protocol):
    solicitudes: RepositorioSolicitudes
    outbox: RepositorioOutbox

    def __enter__(self) -> "UnidadDeTrabajo": ...

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc_value: BaseException | None,
        traceback: TracebackType | None,
    ) -> bool | None: ...

    def commit(self) -> None: ...
