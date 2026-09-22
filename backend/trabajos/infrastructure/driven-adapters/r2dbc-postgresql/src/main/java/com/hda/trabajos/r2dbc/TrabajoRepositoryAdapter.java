package com.hda.trabajos.r2dbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hda.trabajos.model.seedwork.DomainEvent;
import com.hda.trabajos.model.trabajo.*;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import com.hda.trabajos.r2dbc.outbox.OutboxEventoEntity;
import com.hda.trabajos.r2dbc.outbox.OutboxEventoR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public class TrabajoRepositoryAdapter implements TrabajoRepository {

    private static final Logger log = LoggerFactory.getLogger(TrabajoRepositoryAdapter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final DatabaseClient databaseClient;
    private final TrabajoR2dbcRepository r2dbcRepository;
    private final OutboxEventoR2dbcRepository outboxRepository;
    private final TransactionalOperator transactionalOperator;

    public TrabajoRepositoryAdapter(DatabaseClient databaseClient,
                                     TrabajoR2dbcRepository r2dbcRepository,
                                     OutboxEventoR2dbcRepository outboxRepository,
                                     ReactiveTransactionManager transactionManager) {
        this.databaseClient = databaseClient;
        this.r2dbcRepository = r2dbcRepository;
        this.outboxRepository = outboxRepository;
        this.transactionalOperator = TransactionalOperator.create(transactionManager);
    }

    @Override
    public Mono<Trabajo> guardar(Trabajo trabajo) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("""
                        INSERT INTO trabajo (id, cliente_id, categoria_servicio, ciudad, urgencia, origen,
                                              partner_id, moneda, estado, fecha_creacion)
                        VALUES (:id, :clienteId, :categoriaServicio, :ciudad, :urgencia, :origen,
                                :partnerId, :moneda, :estado, :fechaCreacion)
                        ON CONFLICT (id) DO UPDATE SET estado = EXCLUDED.estado
                        """)
                .bind("id", trabajo.getId())
                .bind("clienteId", trabajo.getClienteId())
                .bind("categoriaServicio", trabajo.getCategoriaServicio().nombre())
                .bind("ciudad", trabajo.getCiudad())
                .bind("urgencia", trabajo.getUrgencia().name())
                .bind("origen", trabajo.getOrigen().name())
                .bind("moneda", trabajo.getMoneda().codigoIso4217())
                .bind("estado", trabajo.getEstado().name())
                .bind("fechaCreacion", trabajo.getFechaCreacion());
        spec = trabajo.getPartnerId() != null
                ? spec.bind("partnerId", trabajo.getPartnerId())
                : spec.bindNull("partnerId", UUID.class);

        List<OutboxEventoEntity> eventosPendientes = trabajo.eventosDeDominio().stream()
                .map(this::aOutbox)
                .toList();

        // El agregado y sus eventos pendientes se confirman en la misma transaccion (patron
        // Transactional Outbox): sin esto, un fallo entre el commit y el sendAsync a Pulsar
        // (ahora responsabilidad de OutboxRelay, no de este adaptador) perdia el evento en
        // silencio aunque el estado ya hubiera quedado guardado.
        Mono<Trabajo> escritura = spec.then()
                .thenMany(outboxRepository.saveAll(eventosPendientes))
                .then(Mono.just(trabajo))
                .as(transactionalOperator::transactional);

        return escritura.doOnSuccess(t -> log.info("[TRABAJO GUARDADO] Id: {} | ClienteId: {} | Categoria: {} | "
                        + "Ciudad: {} | Urgencia: {} | Estado: {} | EventosOutbox: {}",
                t.getId(), t.getClienteId(), t.getCategoriaServicio().nombre(),
                t.getCiudad(), t.getUrgencia(), t.getEstado(), eventosPendientes.size()));
    }

    private OutboxEventoEntity aOutbox(DomainEvent evento) {
        try {
            return new OutboxEventoEntity(evento.id(), evento.getClass().getSimpleName(),
                    OBJECT_MAPPER.writeValueAsString(evento), evento.ocurridoEn());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el evento de dominio " + evento.getClass(), e);
        }
    }

    @Override
    public Mono<Trabajo> buscarPorId(UUID id) {
        return r2dbcRepository.findById(id).map(this::aDominio);
    }

    private Trabajo aDominio(TrabajoEntity e) {
        return Trabajo.reconstruir(
                e.getId(),
                e.getClienteId(),
                new CategoriaServicio(e.getCategoriaServicio()),
                Urgencia.valueOf(e.getUrgencia()),
                e.getCiudad(),
                OrigenTrabajo.valueOf(e.getOrigen()),
                e.getPartnerId(),
                new Moneda(e.getMoneda()),
                EstadoTrabajo.valueOf(e.getEstado()),
                e.getFechaCreacion()
        );
    }
}
