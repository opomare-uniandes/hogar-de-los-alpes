package com.hda.trabajosaga.model.sagatrabajo.gateways;

import com.hda.trabajosaga.model.sagatrabajo.SagaTrabajo;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SagaTrabajoRepository {

    Mono<SagaTrabajo> guardar(SagaTrabajo saga);

    /** Carga la saga completa (con todos sus pasos, ordenados) por su propio id. */
    Mono<SagaTrabajo> buscarPorId(UUID sagaId);

    /** Clave de idempotencia de TrabajoCreado (no hay sagaId todavia en ese punto) y tambien
     * lo que usa el endpoint de consulta GET /sagas/trabajos/{trabajoId} (seccion 4.3). */
    Mono<SagaTrabajo> buscarPorTrabajoId(UUID trabajoId);
}
