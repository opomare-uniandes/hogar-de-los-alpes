package com.hda.trabajosaga.r2dbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hda.trabajosaga.model.sagatrabajo.EstadoSaga;
import com.hda.trabajosaga.model.sagatrabajo.PasoSaga;
import com.hda.trabajosaga.model.sagatrabajo.PasoSagaTipo;
import com.hda.trabajosaga.model.sagatrabajo.ResultadoPaso;
import com.hda.trabajosaga.model.sagatrabajo.SagaTrabajo;
import com.hda.trabajosaga.model.sagatrabajo.gateways.SagaTrabajoRepository;
import com.hda.trabajosaga.model.seedwork.DomainEvent;
import com.hda.trabajosaga.r2dbc.outbox.OutboxEventoEntity;
import com.hda.trabajosaga.r2dbc.outbox.OutboxEventoR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class SagaTrabajoRepositoryAdapter implements SagaTrabajoRepository {

    private static final Logger log = LoggerFactory.getLogger(SagaTrabajoRepositoryAdapter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final DatabaseClient databaseClient;
    private final SagaTrabajoR2dbcRepository sagaR2dbcRepository;
    private final SagaPasoR2dbcRepository pasoR2dbcRepository;
    private final OutboxEventoR2dbcRepository outboxRepository;
    private final TransactionalOperator transactionalOperator;

    public SagaTrabajoRepositoryAdapter(DatabaseClient databaseClient,
                                         SagaTrabajoR2dbcRepository sagaR2dbcRepository,
                                         SagaPasoR2dbcRepository pasoR2dbcRepository,
                                         OutboxEventoR2dbcRepository outboxRepository,
                                         ReactiveTransactionManager transactionManager) {
        this.databaseClient = databaseClient;
        this.sagaR2dbcRepository = sagaR2dbcRepository;
        this.pasoR2dbcRepository = pasoR2dbcRepository;
        this.outboxRepository = outboxRepository;
        this.transactionalOperator = TransactionalOperator.create(transactionManager);
    }

    @Override
    public Mono<SagaTrabajo> guardar(SagaTrabajo saga) {
        DatabaseClient.GenericExecuteSpec upsertSaga = databaseClient.sql("""
                        INSERT INTO saga_trabajo (id, trabajo_id, cliente_id, estado, fecha_inicio, fecha_fin)
                        VALUES (:id, :trabajoId, :clienteId, :estado, :fechaInicio, :fechaFin)
                        ON CONFLICT (id) DO UPDATE SET
                            estado = CASE
                                WHEN saga_trabajo.estado IN ('COMPLETADA', 'CANCELADA')
                                    THEN saga_trabajo.estado
                                ELSE EXCLUDED.estado
                            END,
                            fecha_fin = COALESCE(saga_trabajo.fecha_fin, EXCLUDED.fecha_fin)
                        """)
                .bind("id", saga.getId())
                .bind("trabajoId", saga.getTrabajoId())
                .bind("clienteId", saga.getClienteId())
                .bind("estado", saga.getEstado().name())
                .bind("fechaInicio", saga.getFechaInicio());
        upsertSaga = saga.getFechaFin() != null
                ? upsertSaga.bind("fechaFin", saga.getFechaFin())
                : upsertSaga.bindNull("fechaFin", Instant.class);

        List<SagaPasoEntity> pasosNuevos = saga.pasosNuevos().stream()
                .map(p -> new SagaPasoEntity(p.id(), saga.getId(), p.tipo().name(), p.resultado().name(),
                        p.detalle(), p.ocurridoEn()))
                .toList();

        List<OutboxEventoEntity> eventosPendientes = saga.eventosDeDominio().stream()
                .map(this::aOutbox)
                .toList();

        // El upsert de saga_trabajo, el insert de saga_paso y el insert de los eventos
        // pendientes en outbox_evento (patron Transactional Outbox) deben confirmarse juntos:
        // al escalar trabajo-saga-service a varias replicas, un pod puede ser terminado
        // (scale-down de KEDA) entre las escrituras. Sin transaccion, eso deja el estado ya
        // avanzado pero el paso o el comando de salida sin registrar, y la redelivery reprocesa
        // el paso y publica su comando de salida una segunda vez - justo lo que el backstop de
        // idempotencia de abajo deberia evitar.
        Mono<SagaTrabajo> escritura = upsertSaga.then()
                .thenMany(pasoR2dbcRepository.saveAll(pasosNuevos))
                .thenMany(outboxRepository.saveAll(eventosPendientes))
                .then(Mono.just(saga))
                .as(transactionalOperator::transactional)
                .doOnNext(s -> log.info("[SAGA_TRABAJO GUARDADA] Id: {} | TrabajoId: {} | Estado: {} | "
                                + "PasosNuevos: {} | EventosOutbox: {}",
                        s.getId(), s.getTrabajoId(), s.getEstado(), pasosNuevos.size(), eventosPendientes.size()));

        // Backstop de idempotencia (seccion 3 del plan): UNIQUE (saga_id, paso) y UNIQUE
        // (trabajo_id) existen justamente para rechazar una escritura repetida. Si otra
        // ejecucion concurrente (p.ej. una redelivery de Pulsar) ya inserto lo mismo, eso
        // NO es un fallo - es el resultado esperado. Tratarlo como error dejaria el mensaje
        // en negativeAcknowledge y forzaria una redelivery inutil 60s despues.
        return escritura.onErrorResume(DuplicateKeyException.class, e -> {
            log.info("[SAGA_TRABAJO] Escritura duplicada rechazada por el backstop de idempotencia "
                    + "(ver seccion 3 del plan) - se ignora. SagaId: {}", saga.getId());
            return Mono.empty();
        });
    }

    @Override
    public Mono<SagaTrabajo> buscarPorId(UUID sagaId) {
        return sagaR2dbcRepository.findById(sagaId)
                .flatMap(this::cargarConPasos);
    }

    @Override
    public Mono<SagaTrabajo> buscarPorTrabajoId(UUID trabajoId) {
        return sagaR2dbcRepository.findByTrabajoId(trabajoId).flatMap(this::cargarConPasos);
    }

    private OutboxEventoEntity aOutbox(DomainEvent evento) {
        try {
            return new OutboxEventoEntity(evento.id(), evento.getClass().getSimpleName(),
                    OBJECT_MAPPER.writeValueAsString(evento), evento.ocurridoEn());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el evento de dominio " + evento.getClass(), e);
        }
    }

    private Mono<SagaTrabajo> cargarConPasos(SagaTrabajoEntity entidad) {
        return pasoR2dbcRepository.findBySagaIdOrderByOcurridoEnAsc(entidad.getId())
                .map(this::aPasoDominio)
                .collectList()
                .map(pasos -> SagaTrabajo.reconstruir(entidad.getId(), entidad.getTrabajoId(), entidad.getClienteId(),
                        EstadoSaga.valueOf(entidad.getEstado()), entidad.getFechaInicio(), entidad.getFechaFin(), pasos));
    }

    private PasoSaga aPasoDominio(SagaPasoEntity e) {
        return new PasoSaga(e.getId(), PasoSagaTipo.valueOf(e.getPaso()), ResultadoPaso.valueOf(e.getResultado()),
                e.getDetalle(), e.getOcurridoEn());
    }
}
