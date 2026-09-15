package com.hda.trabajos.r2dbc;

import com.hda.trabajos.model.trabajo.*;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class TrabajoRepositoryAdapter implements TrabajoRepository {

    private final TrabajoR2dbcRepository r2dbcRepository;

    public TrabajoRepositoryAdapter(TrabajoR2dbcRepository r2dbcRepository) {
        this.r2dbcRepository = r2dbcRepository;
    }
    private static final Logger log = LoggerFactory.getLogger(TrabajoRepositoryAdapter.class);

    @Override
    public Mono<Trabajo> guardar(Trabajo trabajo) {
        TrabajoEntity entidad = new TrabajoEntity(
                trabajo.getId(),
                trabajo.getClienteId(),
                trabajo.getCategoriaServicio().nombre(),
                trabajo.getCiudad(),
                trabajo.getUrgencia().name(),
                trabajo.getOrigen().name(),
                trabajo.getPartnerId(),
                trabajo.getMoneda().codigoIso4217(),
                trabajo.getEstado().name(),
                trabajo.getFechaCreacion()
        );

        return r2dbcRepository
                .save(entidad)
                .doOnSuccess(entity -> {
                    log.info("[TRABAJO GUARDADO] ClienteId: {} | "
                                    + "Categoria: {} | Ciudad: {} | Urgencia: {}",
                            entity.getClienteId(), entity.getCategoriaServicio(),
                            entity.getCiudad(), entity.getUrgencia());
                })
                .thenReturn(trabajo);
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
