package com.hda.trabajos.usecase.creartrabajo;

import com.hda.trabajos.model.seedwork.AggregateRoot;
import com.hda.trabajos.model.trabajo.CategoriaServicio;
import com.hda.trabajos.model.trabajo.Moneda;
import com.hda.trabajos.model.trabajo.Trabajo;
import com.hda.trabajos.model.trabajo.gateways.TrabajoEventPublisher;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import reactor.core.publisher.Mono;

public class CrearTrabajoUseCase {

    private final TrabajoRepository trabajoRepository;
    private final TrabajoEventPublisher eventPublisher;

    public CrearTrabajoUseCase(TrabajoRepository trabajoRepository, TrabajoEventPublisher eventPublisher) {
        this.trabajoRepository = trabajoRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<Trabajo> ejecutar(CrearTrabajoCommand comando) {
        Trabajo trabajo = Trabajo.crear(
                comando.clienteId(),
                new CategoriaServicio(comando.categoriaServicio()),
                comando.urgencia(),
                comando.ciudad(),
                comando.origen(),
                comando.partnerId(),
                new Moneda(comando.moneda() == null ? "COP" : comando.moneda())
        );

        return trabajoRepository.guardar(trabajo)
                .flatMap(trabajoGuardado ->
                        eventPublisher.publicarTodos(trabajoGuardado.eventosDeDominio())
                                .thenReturn(trabajoGuardado))
                .doOnNext(AggregateRoot::limpiarEventos);
    }
}
