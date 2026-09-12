package com.hda.usuarios.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

/** Repositorio tecnico de Spring Data R2DBC. Solo lo usa el adaptador (UsuarioRepositoryAdapter). */
public interface UsuarioR2dbcRepository extends ReactiveCrudRepository<UsuarioEntity, UUID> {
}
