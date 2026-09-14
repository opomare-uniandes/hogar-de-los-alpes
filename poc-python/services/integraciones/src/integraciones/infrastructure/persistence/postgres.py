from collections.abc import Callable
from types import TracebackType
from uuid import UUID

from sqlalchemy import (
    BigInteger,
    Column,
    ForeignKey,
    Index,
    Integer,
    MetaData,
    String,
    Table,
    Text,
    UniqueConstraint,
    create_engine,
    insert,
    select,
    update,
)
from sqlalchemy.dialects.postgresql import JSONB, UUID as PG_UUID
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.exc import IntegrityError

from integraciones.application.ports import SolicitudDuplicadaConcurrente
from integraciones.domain.commands import CrearTrabajoCommand, Urgencia
from integraciones.domain.outbox import MensajeOutbox
from integraciones.domain.solicitudes import EstadoSolicitud, SolicitudIntegracion


metadata = MetaData()

solicitudes_table = Table(
    "solicitudes_integracion",
    metadata,
    Column("id", PG_UUID(as_uuid=True), primary_key=True),
    Column("partner_id", String(80), nullable=False),
    Column("external_request_id", String(120), nullable=False),
    Column("correlation_id", PG_UUID(as_uuid=True), nullable=False, index=True),
    Column("source_event_id", PG_UUID(as_uuid=True), nullable=False),
    Column("source_contract_version", String(20), nullable=False),
    Column("estado", String(30), nullable=False),
    Column("canonical_payload", JSONB, nullable=False),
    Column("recibida_en_ms", BigInteger, nullable=False),
    UniqueConstraint(
        "partner_id",
        "external_request_id",
        name="uq_solicitud_partner_external",
    ),
)

outbox_table = Table(
    "outbox_messages",
    metadata,
    Column("id", PG_UUID(as_uuid=True), primary_key=True),
    Column(
        "aggregate_id",
        PG_UUID(as_uuid=True),
        ForeignKey("solicitudes_integracion.id"),
        nullable=False,
    ),
    Column("topic", String(255), nullable=False),
    Column("event_type", String(160), nullable=False),
    Column("correlation_id", PG_UUID(as_uuid=True), nullable=False),
    Column("payload", JSONB, nullable=False),
    Column("estado", String(20), nullable=False, default="PENDIENTE"),
    Column("intentos", Integer, nullable=False, default=0),
    Column("creado_en_ms", BigInteger, nullable=False),
    Column("publicado_en_ms", BigInteger),
    Column("ultimo_error", Text),
)

Index("ix_outbox_pending", outbox_table.c.estado, outbox_table.c.creado_en_ms)


def crear_engine(database_url: str, *, echo: bool = False) -> Engine:
    if not database_url.startswith(("postgresql://", "postgresql+psycopg://")):
        raise ValueError("integraciones-service requiere una URL de PostgreSQL")
    return create_engine(database_url, echo=echo, pool_pre_ping=True)


def crear_session_factory(engine: Engine) -> sessionmaker[Session]:
    return sessionmaker(bind=engine, expire_on_commit=False)


class SqlAlchemyRepositorioSolicitudes:
    def __init__(self, session: Session) -> None:
        self._session = session

    def buscar_por_clave(
        self,
        partner_id: str,
        external_request_id: str,
    ) -> SolicitudIntegracion | None:
        row = self._session.execute(
            select(solicitudes_table).where(
                solicitudes_table.c.partner_id == partner_id,
                solicitudes_table.c.external_request_id == external_request_id,
            )
        ).mappings().one_or_none()
        return _row_a_solicitud(row) if row is not None else None

    def agregar(self, solicitud: SolicitudIntegracion) -> None:
        self._session.execute(
            insert(solicitudes_table).values(
                id=solicitud.id,
                partner_id=solicitud.partner_id,
                external_request_id=solicitud.external_request_id,
                correlation_id=solicitud.correlation_id,
                source_event_id=solicitud.source_event_id,
                source_contract_version=solicitud.source_contract_version,
                estado=solicitud.estado.value,
                canonical_payload=solicitud.comando.campos_persistencia(),
                recibida_en_ms=solicitud.recibida_en_ms,
            )
        )


class SqlAlchemyRepositorioOutbox:
    def __init__(self, session: Session) -> None:
        self._session = session

    def agregar(self, mensaje: MensajeOutbox) -> None:
        self._session.execute(
            insert(outbox_table).values(
                id=mensaje.id,
                aggregate_id=mensaje.aggregate_id,
                topic=mensaje.topic,
                event_type=mensaje.event_type,
                correlation_id=mensaje.correlation_id,
                payload=mensaje.payload,
                estado="PENDIENTE",
                intentos=0,
                creado_en_ms=mensaje.creado_en_ms,
            )
        )

    def obtener_pendientes(self, limite: int = 100) -> list[MensajeOutbox]:
        rows = self._session.execute(
            select(outbox_table)
            .where(outbox_table.c.estado == "PENDIENTE")
            .order_by(outbox_table.c.creado_en_ms)
            .limit(limite)
            .with_for_update(skip_locked=True)
        ).mappings()
        return [
            MensajeOutbox(
                id=UUID(str(row["id"])),
                aggregate_id=UUID(str(row["aggregate_id"])),
                topic=str(row["topic"]),
                event_type=str(row["event_type"]),
                correlation_id=UUID(str(row["correlation_id"])),
                payload=dict(row["payload"]),
                creado_en_ms=int(row["creado_en_ms"]),
            )
            for row in rows
        ]

    def marcar_publicado(self, mensaje_id: UUID, publicado_en_ms: int) -> None:
        self._session.execute(
            update(outbox_table)
            .where(outbox_table.c.id == mensaje_id)
            .values(
                estado="PUBLICADO",
                publicado_en_ms=publicado_en_ms,
                ultimo_error=None,
            )
        )

    def marcar_error(self, mensaje_id: UUID, error: str) -> None:
        self._session.execute(
            update(outbox_table)
            .where(outbox_table.c.id == mensaje_id)
            .values(
                intentos=outbox_table.c.intentos + 1,
                ultimo_error=error[:2000],
            )
        )


class SqlAlchemyUnitOfWork:
    def __init__(self, session_factory: Callable[[], Session]) -> None:
        self._session_factory = session_factory

    def __enter__(self) -> "SqlAlchemyUnitOfWork":
        self._session = self._session_factory()
        self.solicitudes = SqlAlchemyRepositorioSolicitudes(self._session)
        self.outbox = SqlAlchemyRepositorioOutbox(self._session)
        return self

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc_value: BaseException | None,
        traceback: TracebackType | None,
    ) -> None:
        if exc_type is not None:
            self._session.rollback()
        self._session.close()

    def commit(self) -> None:
        try:
            self._session.commit()
        except IntegrityError as exc:
            self._session.rollback()
            diagnostic = getattr(exc.orig, "diag", None)
            if getattr(diagnostic, "constraint_name", None) == "uq_solicitud_partner_external":
                raise SolicitudDuplicadaConcurrente from exc
            raise


def _row_a_solicitud(row: object) -> SolicitudIntegracion:
    values = row
    payload = values["canonical_payload"]  # type: ignore[index]
    comando = CrearTrabajoCommand.crear(
        correlation_id=UUID(str(values["correlation_id"])),  # type: ignore[index]
        source_event_id=UUID(str(values["source_event_id"])),  # type: ignore[index]
        source_contract_version=str(values["source_contract_version"]),  # type: ignore[index]
        partner_id=str(values["partner_id"]),  # type: ignore[index]
        external_request_id=str(values["external_request_id"]),  # type: ignore[index]
        cliente_id=str(payload["clienteId"]),
        categoria_servicio=str(payload["categoriaServicio"]),
        urgencia=Urgencia(str(payload["urgencia"])),
        ciudad=str(payload["ciudad"]),
        pais=str(payload["pais"]),
        moneda=str(payload["moneda"]),
        fecha_solicitud_ms=int(payload["fechaSolicitud"]),
    )
    return SolicitudIntegracion(
        id=UUID(str(values["id"])),  # type: ignore[index]
        partner_id=str(values["partner_id"]),  # type: ignore[index]
        external_request_id=str(values["external_request_id"]),  # type: ignore[index]
        correlation_id=UUID(str(values["correlation_id"])),  # type: ignore[index]
        source_event_id=UUID(str(values["source_event_id"])),  # type: ignore[index]
        source_contract_version=str(values["source_contract_version"]),  # type: ignore[index]
        estado=EstadoSolicitud(str(values["estado"])),  # type: ignore[index]
        comando=comando,
        recibida_en_ms=int(values["recibida_en_ms"]),  # type: ignore[index]
    )
