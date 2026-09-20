package com.hda.trabajosaga.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SagaTrabajoR2dbcRepository extends ReactiveCrudRepository<SagaTrabajoEntity, UUID> {

    Mono<SagaTrabajoEntity> findByTrabajoId(UUID trabajoId);
}
