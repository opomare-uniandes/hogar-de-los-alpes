package com.hda.trabajos.r2dbc;

import com.hda.trabajos.model.trabajo.*;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class TrabajoRepositoryAdapter implements TrabajoRepository {

    private static final Logger log = LoggerFactory.getLogger(TrabajoRepositoryAdapter.class);

    private final DatabaseClient databaseClient;
    private final TrabajoR2dbcRepository r2dbcRepository;

    public TrabajoRepositoryAdapter(DatabaseClient databaseClient, TrabajoR2dbcRepository r2dbcRepository) {
        this.databaseClient = databaseClient;
        this.r2dbcRepository = r2dbcRepository;
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

        return spec.then()
                .doOnSuccess(v -> log.info("[TRABAJO GUARDADO] Id: {} | ClienteId: {} | Categoria: {} | "
                                + "Ciudad: {} | Urgencia: {} | Estado: {}",
                        trabajo.getId(), trabajo.getClienteId(), trabajo.getCategoriaServicio().nombre(),
                        trabajo.getCiudad(), trabajo.getUrgencia(), trabajo.getEstado()))
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
