package com.hda.proveedor.r2dbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hda.proveedor.model.proveedor.Proveedor;
import com.hda.proveedor.model.proveedor.gateways.ProveedorRepository;
import com.hda.proveedor.model.seedwork.DomainEvent;
import com.hda.proveedor.r2dbc.outbox.OutboxEventoEntity;
import com.hda.proveedor.r2dbc.outbox.OutboxEventoR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public class ProveedorRepositoryAdapter implements ProveedorRepository {

    private static final Logger log = LoggerFactory.getLogger(ProveedorRepositoryAdapter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ProveedorR2dbcRepository r2dbcRepository;
    private final OutboxEventoR2dbcRepository outboxRepository;
    private final TransactionalOperator transactionalOperator;

    public ProveedorRepositoryAdapter(ProveedorR2dbcRepository r2dbcRepository,
                                       OutboxEventoR2dbcRepository outboxRepository,
                                       ReactiveTransactionManager transactionManager) {
        this.r2dbcRepository = r2dbcRepository;
        this.outboxRepository = outboxRepository;
        this.transactionalOperator = TransactionalOperator.create(transactionManager);
    }

    @Override
    public Mono<Proveedor> guardar(Proveedor proveedor) {
        ProveedorEntity entidad = new ProveedorEntity(
                proveedor.getId(), proveedor.getNombre(), proveedor.getCategoriaServicio(),
                proveedor.getCiudad(), proveedor.isDisponible(), proveedor.getSagaIdReserva());

        List<OutboxEventoEntity> eventosPendientes = proveedor.eventosDeDominio().stream()
                .map(this::aOutbox)
                .toList();

        // El agregado y sus eventos pendientes se confirman en la misma transaccion (patron
        // Transactional Outbox) -- ver TrabajoRepositoryAdapter.guardar() para el mismo patron.
        Mono<Proveedor> escritura = r2dbcRepository.save(entidad)
                .then(outboxRepository.saveAll(eventosPendientes).then())
                .then(Mono.just(proveedor))
                .as(transactionalOperator::transactional);

        return escritura.doOnSuccess(p -> log.info("[PROVEEDOR ACTUALIZADO] Id: {} | Disponible: {} | "
                        + "SagaIdReserva: {} | EventosOutbox: {}",
                p.getId(), p.isDisponible(), p.getSagaIdReserva(), eventosPendientes.size()));
    }

    @Override
    public Mono<Void> encolarEventoPendiente(DomainEvent evento) {
        return outboxRepository.save(aOutbox(evento)).then();
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
    public Mono<Proveedor> buscarPorId(UUID id) {
        return r2dbcRepository.findById(id).map(this::aDominio);
    }

    @Override
    public Mono<Proveedor> buscarPorSagaIdReserva(UUID sagaId) {
        return r2dbcRepository.findBySagaIdReserva(sagaId).map(this::aDominio);
    }

    @Override
    public Mono<Proveedor> reservarSiDisponible(String categoriaServicio, String ciudad, UUID sagaId) {
        return r2dbcRepository.reservarSiDisponible(categoriaServicio, ciudad, sagaId)
                .doOnNext(entity -> log.info("[PROVEEDOR RESERVADO ATOMICAMENTE] Id: {} | SagaId: {}",
                        entity.getId(), sagaId))
                .map(this::aDominio);
    }

    private Proveedor aDominio(ProveedorEntity e) {
        return Proveedor.reconstruir(e.getId(), e.getNombre(), e.getCategoriaServicio(), e.getCiudad(),
                e.isDisponible(), e.getSagaIdReserva());
    }
}
