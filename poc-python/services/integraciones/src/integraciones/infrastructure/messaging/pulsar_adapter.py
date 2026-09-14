import json
import logging
from collections.abc import Callable
from pathlib import Path
from time import time_ns
from uuid import UUID, uuid5

import pulsar
from pydantic import ValidationError
from sqlalchemy.orm import Session

from integraciones.application import RecibirSolicitud, ResultadoRecepcion
from integraciones.domain import TranslationError
from integraciones.infrastructure.contracts import (
    SolicitudTrabajoPartnerV1Create,
    SolicitudTrabajoPartnerV2Create,
)
from integraciones.infrastructure.messaging.avro import CatalogoAvro
from integraciones.infrastructure.persistence.postgres import SqlAlchemyRepositorioOutbox
from integraciones.infrastructure.translators import traducir_v1, traducir_v2


TOPIC_SOLICITUD_V1 = "persistent://hda/integracion/solicitud-trabajo-v1"
TOPIC_SOLICITUD_V2 = "persistent://hda/integracion/solicitud-trabajo-v2"
TOPIC_CREAR_TRABAJO_V1 = "persistent://hda/trabajos/comandos/crear-trabajo-v1"
TOPIC_RECHAZOS_V1 = "persistent://hda/integracion/solicitud-trabajo-rechazada-v1"

LOGGER = logging.getLogger(__name__)
REJECTION_NAMESPACE = UUID("f6c29ac8-623a-431f-9bad-0126eb19588c")


def crear_cliente(service_url: str) -> pulsar.Client:
    return pulsar.Client(service_url)


class ProcesadorSolicitud:
    def __init__(
        self,
        model_type,
        traductor: Callable,
        caso_uso: RecibirSolicitud,
    ) -> None:
        self._model_type = model_type
        self._traductor = traductor
        self._caso_uso = caso_uso

    def procesar(self, payload: dict[str, object]) -> ResultadoRecepcion:
        evento = self._model_type.model_validate(payload)
        return self._caso_uso.ejecutar(self._traductor(evento))


class ConsumidorSolicitudes:
    """Consume una versión; confirma solo luego de una transacción local exitosa."""

    def __init__(
        self,
        consumer,
        procesador: ProcesadorSolicitud,
        *,
        version: str,
        topic: str = "",
        reject_producer=None,
        clock_ms: Callable[[], int] | None = None,
    ) -> None:
        self._consumer = consumer
        self._procesador = procesador
        self._version = version
        self._topic = topic
        self._reject_producer = reject_producer
        self._clock_ms = clock_ms or (lambda: time_ns() // 1_000_000)

    @classmethod
    def v1(
        cls,
        client: pulsar.Client,
        catalogo: CatalogoAvro,
        caso_uso: RecibirSolicitud,
    ) -> "ConsumidorSolicitudes":
        consumer = client.subscribe(
            TOPIC_SOLICITUD_V1,
            subscription_name="integraciones-acl-v1",
            schema=catalogo.obtener("integration/solicitud_trabajo_partner_v1.avsc"),
            consumer_type=pulsar.ConsumerType.Shared,
            dead_letter_policy=pulsar.ConsumerDeadLetterPolicy(
                max_redeliver_count=3,
                dead_letter_topic=f"{TOPIC_SOLICITUD_V1}-DLQ",
            ),
        )
        reject_producer = client.create_producer(
            TOPIC_RECHAZOS_V1,
            schema=catalogo.obtener("integration/solicitud_trabajo_rechazada_v1.avsc"),
            producer_name="integraciones-rechazos-v1",
        )
        return cls(
            consumer,
            ProcesadorSolicitud(SolicitudTrabajoPartnerV1Create, traducir_v1, caso_uso),
            version="v1",
            topic=TOPIC_SOLICITUD_V1,
            reject_producer=reject_producer,
        )

    @classmethod
    def v2(
        cls,
        client: pulsar.Client,
        catalogo: CatalogoAvro,
        caso_uso: RecibirSolicitud,
    ) -> "ConsumidorSolicitudes":
        consumer = client.subscribe(
            TOPIC_SOLICITUD_V2,
            subscription_name="integraciones-acl-v2",
            schema=catalogo.obtener("integration/solicitud_trabajo_partner_v2.avsc"),
            consumer_type=pulsar.ConsumerType.Shared,
            dead_letter_policy=pulsar.ConsumerDeadLetterPolicy(
                max_redeliver_count=3,
                dead_letter_topic=f"{TOPIC_SOLICITUD_V2}-DLQ",
            ),
        )
        reject_producer = client.create_producer(
            TOPIC_RECHAZOS_V1,
            schema=catalogo.obtener("integration/solicitud_trabajo_rechazada_v1.avsc"),
            producer_name="integraciones-rechazos-v2",
        )
        return cls(
            consumer,
            ProcesadorSolicitud(SolicitudTrabajoPartnerV2Create, traducir_v2, caso_uso),
            version="v2",
            topic=TOPIC_SOLICITUD_V2,
            reject_producer=reject_producer,
        )

    def procesar_siguiente(self, timeout_millis: int = 1000) -> ResultadoRecepcion | None:
        try:
            message = self._consumer.receive(timeout_millis=timeout_millis)
        except pulsar.Timeout:
            return None

        try:
            payload = message.value()
            resultado = self._procesador.procesar(payload)
        except (ValidationError, TranslationError) as exc:
            LOGGER.warning(
                "solicitud_rechazada version=%s message_id=%s causa=%s",
                self._version,
                message.message_id(),
                exc,
            )
            if self._reject_producer is not None:
                try:
                    rejection_id = uuid5(
                        REJECTION_NAMESPACE,
                        f"{self._topic}:{message.message_id()}",
                    )
                    self._reject_producer.send(
                        {
                            "specversion": "1.0",
                            "id": str(rejection_id),
                            "source": "urn:hda:service:integraciones",
                            "type": "com.hda.integracion.solicitud-rechazada.v1",
                            "subject": f"rechazo/{rejection_id}",
                            "time": self._clock_ms(),
                            "datacontenttype": "application/avro",
                            "dataschema": "urn:hda:schema:solicitud-trabajo-rechazada:v1",
                            "data": {
                                "originalTopic": self._topic,
                                "contractVersion": self._version,
                                "sourceEventId": str(payload.get("id")) if payload.get("id") else None,
                                "errorType": type(exc).__name__,
                                "reason": str(exc),
                                "rawPayload": json.dumps(payload, default=str, sort_keys=True),
                            },
                        },
                        partition_key=str(rejection_id),
                    )
                except Exception:
                    self._consumer.negative_acknowledge(message)
                    raise
            self._consumer.acknowledge(message)
            return None
        except Exception:
            LOGGER.exception(
                "fallo_transitorio version=%s message_id=%s",
                self._version,
                message.message_id(),
            )
            self._consumer.negative_acknowledge(message)
            raise

        self._consumer.acknowledge(message)
        return resultado

    def close(self) -> None:
        self._consumer.close()
        if self._reject_producer is not None:
            self._reject_producer.close()


class PublicadorOutbox:
    """Publica pendientes; un fallo deja el mensaje listo para reintento."""

    def __init__(
        self,
        client: pulsar.Client,
        catalogo: CatalogoAvro,
        session_factory: Callable[[], Session],
        clock_ms: Callable[[], int] | None = None,
    ) -> None:
        self._producer = client.create_producer(
            TOPIC_CREAR_TRABAJO_V1,
            schema=catalogo.obtener("commands/crear_trabajo_command_v1.avsc"),
            producer_name="integraciones-outbox-v1",
            block_if_queue_full=True,
        )
        self._session_factory = session_factory
        self._clock_ms = clock_ms or (lambda: time_ns() // 1_000_000)

    def publicar_pendientes(self, limite: int = 100) -> int:
        session = self._session_factory()
        repo = SqlAlchemyRepositorioOutbox(session)
        publicados = 0
        try:
            for mensaje in repo.obtener_pendientes(limite):
                try:
                    self._producer.send(
                        mensaje.payload,
                        partition_key=str(mensaje.aggregate_id),
                        properties={
                            "correlationId": str(mensaje.correlation_id),
                            "eventType": mensaje.event_type,
                        },
                    )
                except Exception as exc:
                    repo.marcar_error(mensaje.id, str(exc))
                    session.commit()
                    LOGGER.exception("fallo_publicacion_outbox mensaje_id=%s", mensaje.id)
                    continue

                repo.marcar_publicado(mensaje.id, self._clock_ms())
                session.commit()
                publicados += 1
        finally:
            session.close()
        return publicados

    def close(self) -> None:
        self._producer.close()


def crear_catalogo(contracts_dir: str | Path) -> CatalogoAvro:
    return CatalogoAvro(Path(contracts_dir))
