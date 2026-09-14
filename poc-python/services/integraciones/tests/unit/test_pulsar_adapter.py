from dataclasses import dataclass

import pytest

from integraciones.application import EstadoRecepcion, ResultadoRecepcion
from integraciones.domain import TranslationError
from integraciones.infrastructure.messaging.pulsar_adapter import ConsumidorSolicitudes


@dataclass
class MensajeFake:
    payload: dict[str, object]

    def value(self) -> dict[str, object]:
        return self.payload

    def message_id(self) -> str:
        return "fake-message-id"


class ConsumidorFake:
    def __init__(self, message: MensajeFake) -> None:
        self.message = message
        self.acknowledged = False
        self.negative_acknowledged = False

    def receive(self, timeout_millis: int):
        return self.message

    def acknowledge(self, message: MensajeFake) -> None:
        self.acknowledged = True

    def negative_acknowledge(self, message: MensajeFake) -> None:
        self.negative_acknowledged = True


class ProcesadorFake:
    def __init__(self, error: Exception | None = None) -> None:
        self.error = error

    def procesar(self, payload: dict[str, object]) -> ResultadoRecepcion:
        if self.error is not None:
            raise self.error
        return ResultadoRecepcion(EstadoRecepcion.PROCESADA, "solicitud-1")


class ProductorRechazoFake:
    def __init__(self, error: Exception | None = None) -> None:
        self.error = error
        self.messages: list[dict[str, object]] = []

    def send(self, payload: dict[str, object], **kwargs: object) -> None:
        if self.error is not None:
            raise self.error
        self.messages.append(payload)


def test_ack_sucede_despues_de_procesar_exitosamente() -> None:
    consumer = ConsumidorFake(MensajeFake({"ok": True}))
    adapter = ConsumidorSolicitudes(consumer, ProcesadorFake(), version="v1")

    resultado = adapter.procesar_siguiente()

    assert resultado is not None
    assert consumer.acknowledged
    assert not consumer.negative_acknowledged


def test_error_semantico_se_confirma_para_evitar_reintento_infinito() -> None:
    consumer = ConsumidorFake(MensajeFake({"invalido": True}))
    producer = ProductorRechazoFake()
    adapter = ConsumidorSolicitudes(
        consumer,
        ProcesadorFake(TranslationError("codigo no soportado")),
        version="v2",
        topic="persistent://hda/integracion/solicitud-trabajo-v2",
        reject_producer=producer,
        clock_ms=lambda: 1_789_322_401_000,
    )

    resultado = adapter.procesar_siguiente()

    assert resultado is None
    assert consumer.acknowledged
    assert not consumer.negative_acknowledged
    assert producer.messages[0]["data"]["contractVersion"] == "v2"  # type: ignore[index]


def test_fallo_al_publicar_rechazo_conserva_el_mensaje_para_reintento() -> None:
    consumer = ConsumidorFake(MensajeFake({"invalido": True}))
    adapter = ConsumidorSolicitudes(
        consumer,
        ProcesadorFake(TranslationError("codigo no soportado")),
        version="v1",
        topic="persistent://hda/integracion/solicitud-trabajo-v1",
        reject_producer=ProductorRechazoFake(RuntimeError("pulsar no disponible")),
    )

    with pytest.raises(RuntimeError, match="pulsar"):
        adapter.procesar_siguiente()

    assert not consumer.acknowledged
    assert consumer.negative_acknowledged


def test_error_transitorio_genera_negative_ack() -> None:
    consumer = ConsumidorFake(MensajeFake({"ok": True}))
    adapter = ConsumidorSolicitudes(
        consumer,
        ProcesadorFake(RuntimeError("postgres no disponible")),
        version="v1",
    )

    with pytest.raises(RuntimeError, match="postgres"):
        adapter.procesar_siguiente()

    assert not consumer.acknowledged
    assert consumer.negative_acknowledged
