package com.hda.trabajos.usecase.cancelartrabajo;

import com.hda.trabajos.model.seedwork.AggregateRoot;
import com.hda.trabajos.model.trabajo.Trabajo;
import com.hda.trabajos.model.trabajo.gateways.TrabajoEventPublisher;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class CancelarTrabajoUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelarTrabajoUseCase.class);

    private final TrabajoRepository trabajoRepository;
    private final TrabajoEventPublisher eventPublisher;

    public CancelarTrabajoUseCase(TrabajoRepository trabajoRepository, TrabajoEventPublisher eventPublisher) {
        this.trabajoRepository = trabajoRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<Trabajo> ejecutar(CancelarTrabajoCommand comando) {
        log.info("[TRABAJO CANCELADO] TrabajoId: {} | SagaId: {} | Motivo: {}",
                comando.trabajoId(), comando.sagaId(), comando.motivo());

        return trabajoRepository.buscarPorId(comando.trabajoId())
                .flatMap(trabajo -> {
                    trabajo.cancelar(comando.sagaId());
                    return trabajoRepository.guardar(trabajo);
                })
                .flatMap(trabajoGuardado ->
                        eventPublisher.publicarTodos(trabajoGuardado.eventosDeDominio())
                                .thenReturn(trabajoGuardado))
                .doOnNext(AggregateRoot::limpiarEventos);
    }
}
