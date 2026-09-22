package com.hda.trabajos.usecase.cancelartrabajo;

import com.hda.trabajos.model.seedwork.AggregateRoot;
import com.hda.trabajos.model.trabajo.Trabajo;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class CancelarTrabajoUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelarTrabajoUseCase.class);

    private final TrabajoRepository trabajoRepository;

    public CancelarTrabajoUseCase(TrabajoRepository trabajoRepository) {
        this.trabajoRepository = trabajoRepository;
    }

    public Mono<Trabajo> ejecutar(CancelarTrabajoCommand comando) {
        log.info("[TRABAJO CANCELADO] TrabajoId: {} | SagaId: {} | Motivo: {}",
                comando.trabajoId(), comando.sagaId(), comando.motivo());

        // La publicacion del evento ya no ocurre aqui: TrabajoRepositoryAdapter.guardar() lo
        // inserta en outbox_evento en la misma transaccion del agregado; OutboxRelay lo envia
        // a Pulsar despues (ver patron Transactional Outbox).
        return trabajoRepository.buscarPorId(comando.trabajoId())
                .flatMap(trabajo -> {
                    trabajo.cancelar(comando.sagaId());
                    return trabajoRepository.guardar(trabajo);
                })
                .doOnNext(AggregateRoot::limpiarEventos);
    }
}
