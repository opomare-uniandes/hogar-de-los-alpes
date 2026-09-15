package com.hda.integracion.model.trabajo.gateways;

import com.hda.integracion.model.trabajo.CrearTrabajoIntegradoCommand;
import reactor.core.publisher.Mono;

/** Puerto de salida: Integración solicita a Trabajos crear el agregado mediante un comando. */
public interface CrearTrabajoCommandPublisher {
    Mono<Void> publicar(CrearTrabajoIntegradoCommand comando);
}
