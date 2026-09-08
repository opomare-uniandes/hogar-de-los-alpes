package com.hda.trabajos.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

/** Repositorio tecnico de Spring Data R2DBC. Solo lo usa el adaptador (TrabajoRepositoryAdapter). */
public interface TrabajoR2dbcRepository extends ReactiveCrudRepository<TrabajoEntity, UUID> {
}
