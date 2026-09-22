package com.hda.trabajos.usecase.asignartrabajo;

import com.hda.trabajos.model.seedwork.AggregateRoot;
import com.hda.trabajos.model.trabajo.Trabajo;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import reactor.core.publisher.Mono;

public class AsignarTrabajoUseCase {

    private final TrabajoRepository trabajoRepository;

    public AsignarTrabajoUseCase(TrabajoRepository trabajoRepository) {
        this.trabajoRepository = trabajoRepository;
    }

    public Mono<Trabajo> ejecutar(AsignarTrabajoCommand comando) {
        // La publicacion del evento ya no ocurre aqui: TrabajoRepositoryAdapter.guardar() lo
        // inserta en outbox_evento en la misma transaccion del agregado; OutboxRelay lo envia
        // a Pulsar despues (ver patron Transactional Outbox).
        return trabajoRepository.buscarPorId(comando.trabajoId())
                .flatMap(trabajo -> {
                    trabajo.asignar(comando.sagaId());
                    return trabajoRepository.guardar(trabajo);
                })
                .doOnNext(AggregateRoot::limpiarEventos);
    }
}
