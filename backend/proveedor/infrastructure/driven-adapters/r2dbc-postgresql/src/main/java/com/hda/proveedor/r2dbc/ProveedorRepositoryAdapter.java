package com.hda.proveedor.r2dbc;

import com.hda.proveedor.model.proveedor.Proveedor;
import com.hda.proveedor.model.proveedor.gateways.ProveedorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class ProveedorRepositoryAdapter implements ProveedorRepository {

    private static final Logger log = LoggerFactory.getLogger(ProveedorRepositoryAdapter.class);

    private final ProveedorR2dbcRepository r2dbcRepository;

    public ProveedorRepositoryAdapter(ProveedorR2dbcRepository r2dbcRepository) {
        this.r2dbcRepository = r2dbcRepository;
    }

    @Override
    public Mono<Proveedor> guardar(Proveedor proveedor) {
        ProveedorEntity entidad = new ProveedorEntity(
                proveedor.getId(), proveedor.getNombre(), proveedor.getCategoriaServicio(),
                proveedor.getCiudad(), proveedor.isDisponible(), proveedor.getSagaIdReserva());

        return r2dbcRepository.save(entidad)
                .doOnSuccess(entity -> log.info("[PROVEEDOR ACTUALIZADO] Id: {} | Disponible: {} | SagaIdReserva: {}",
                        entity.getId(), entity.isDisponible(), entity.getSagaIdReserva()))
                .thenReturn(proveedor);
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
