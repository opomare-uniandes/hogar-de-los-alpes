package com.hda.trabajos.usecase.asignartrabajo;

import com.hda.trabajos.model.seedwork.AggregateRoot;
import com.hda.trabajos.model.trabajo.Trabajo;
import com.hda.trabajos.model.trabajo.gateways.TrabajoEventPublisher;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import reactor.core.publisher.Mono;

public class AsignarTrabajoUseCase {

    private final TrabajoRepository trabajoRepository;
    private final TrabajoEventPublisher eventPublisher;

    public AsignarTrabajoUseCase(TrabajoRepository trabajoRepository, TrabajoEventPublisher eventPublisher) {
        this.trabajoRepository = trabajoRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<Trabajo> ejecutar(AsignarTrabajoCommand comando) {
        return trabajoRepository.buscarPorId(comando.trabajoId())
                .flatMap(trabajo -> {
                    trabajo.asignar(comando.sagaId());
                    return trabajoRepository.guardar(trabajo);
                })
                .flatMap(trabajoGuardado ->
                        eventPublisher.publicarTodos(trabajoGuardado.eventosDeDominio())
                                .thenReturn(trabajoGuardado))
                .doOnNext(AggregateRoot::limpiarEventos);
    }
}
