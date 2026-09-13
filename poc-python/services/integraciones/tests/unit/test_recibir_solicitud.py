from types import TracebackType

from integraciones.application import EstadoRecepcion, RecibirSolicitud
from integraciones.application.recibir_solicitud import TOPIC_CREAR_TRABAJO_V1
from integraciones.domain import CrearTrabajoCommand, MensajeOutbox, SolicitudIntegracion
from integraciones.infrastructure.contracts import SolicitudTrabajoPartnerV1Create
from integraciones.infrastructure.translators import traducir_v1


class RepositorioSolicitudesMemoria:
    def __init__(self) -> None:
        self.items: dict[tuple[str, str], SolicitudIntegracion] = {}

    def buscar_por_clave(
        self,
        partner_id: str,
        external_request_id: str,
    ) -> SolicitudIntegracion | None:
        return self.items.get((partner_id, external_request_id))

    def agregar(self, solicitud: SolicitudIntegracion) -> None:
        self.items[(solicitud.partner_id, solicitud.external_request_id)] = solicitud


class RepositorioOutboxMemoria:
    def __init__(self) -> None:
        self.items: list[MensajeOutbox] = []

    def agregar(self, mensaje: MensajeOutbox) -> None:
        self.items.append(mensaje)


class UowMemoria:
    def __init__(self) -> None:
        self.solicitudes = RepositorioSolicitudesMemoria()
        self.outbox = RepositorioOutboxMemoria()
        self.commits = 0

    def __enter__(self) -> "UowMemoria":
        return self

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc_value: BaseException | None,
        traceback: TracebackType | None,
    ) -> None:
        return None

    def commit(self) -> None:
        self.commits += 1


def _comando(evento_v1: dict[str, object]) -> CrearTrabajoCommand:
    return traducir_v1(SolicitudTrabajoPartnerV1Create.model_validate(evento_v1))


def test_persiste_agregado_y_outbox_en_una_unidad_de_trabajo(
    evento_v1: dict[str, object],
) -> None:
    uow = UowMemoria()
    caso_uso = RecibirSolicitud(lambda: uow, lambda: 1_736_899_201_000)

    resultado = caso_uso.ejecutar(_comando(evento_v1))

    assert resultado.estado is EstadoRecepcion.PROCESADA
    assert uow.commits == 1
    assert len(uow.solicitudes.items) == 1
    assert len(uow.outbox.items) == 1
    assert uow.outbox.items[0].topic == TOPIC_CREAR_TRABAJO_V1
    assert uow.outbox.items[0].payload["categoriaServicio"] == "PLOMERIA"


def test_duplicado_no_crea_otro_agregado_ni_otro_mensaje(
    evento_v1: dict[str, object],
) -> None:
    uow = UowMemoria()
    caso_uso = RecibirSolicitud(lambda: uow, lambda: 1_736_899_201_000)
    comando = _comando(evento_v1)

    primero = caso_uso.ejecutar(comando)
    segundo = caso_uso.ejecutar(comando)

    assert primero.estado is EstadoRecepcion.PROCESADA
    assert segundo.estado is EstadoRecepcion.DUPLICADA
    assert primero.solicitud_id == segundo.solicitud_id
    assert uow.commits == 1
    assert len(uow.solicitudes.items) == 1
    assert len(uow.outbox.items) == 1
