package com.hda.trabajos.model.trabajo.gateways;

import com.hda.trabajos.model.trabajo.Trabajo;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TrabajoRepository {

    Mono<Trabajo> guardar(Trabajo trabajo);

    Mono<Trabajo> buscarPorId(UUID id);
}
