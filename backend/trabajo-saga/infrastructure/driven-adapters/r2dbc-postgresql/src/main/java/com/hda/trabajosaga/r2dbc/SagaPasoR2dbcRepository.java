package com.hda.trabajosaga.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface SagaPasoR2dbcRepository extends ReactiveCrudRepository<SagaPasoEntity, UUID> {

    /** Orden requerido por SagaTrabajo.reconstruir() y por el endpoint de consulta (seccion 4.3). */
    Flux<SagaPasoEntity> findBySagaIdOrderByOcurridoEnAsc(UUID sagaId);
}
