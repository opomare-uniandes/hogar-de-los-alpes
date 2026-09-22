package com.hda.trabajos.usecase.creartrabajo;

import com.hda.trabajos.model.seedwork.AggregateRoot;
import com.hda.trabajos.model.trabajo.CategoriaServicio;
import com.hda.trabajos.model.trabajo.Moneda;
import com.hda.trabajos.model.trabajo.Trabajo;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import reactor.core.publisher.Mono;

public class CrearTrabajoUseCase {

    private final TrabajoRepository trabajoRepository;

    public CrearTrabajoUseCase(TrabajoRepository trabajoRepository) {
        this.trabajoRepository = trabajoRepository;
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

        // La publicacion del evento ya no ocurre aqui: TrabajoRepositoryAdapter.guardar() lo
        // inserta en outbox_evento en la misma transaccion del agregado; OutboxRelay lo envia
        // a Pulsar despues (ver patron Transactional Outbox).
        return trabajoRepository.guardar(trabajo)
                .doOnNext(AggregateRoot::limpiarEventos);
    }
}
