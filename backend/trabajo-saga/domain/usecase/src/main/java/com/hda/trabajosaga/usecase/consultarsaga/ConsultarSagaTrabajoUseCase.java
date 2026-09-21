package com.hda.trabajosaga.usecase.consultarsaga;

import com.hda.trabajosaga.model.sagatrabajo.SagaTrabajo;
import com.hda.trabajosaga.model.sagatrabajo.gateways.SagaTrabajoRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class ConsultarSagaTrabajoUseCase {

    private final SagaTrabajoRepository sagaTrabajoRepository;

    public ConsultarSagaTrabajoUseCase(SagaTrabajoRepository sagaTrabajoRepository) {
        this.sagaTrabajoRepository = sagaTrabajoRepository;
    }

    /** GET /sagas/trabajos/{trabajoId} (seccion 4.3 del plan). */
    public Mono<SagaTrabajo> buscarPorTrabajoId(UUID trabajoId) {
        return sagaTrabajoRepository.buscarPorTrabajoId(trabajoId);
    }

    /** GET /sagas/{sagaId} (seccion 4.3 del plan). */
    public Mono<SagaTrabajo> buscarPorSagaId(UUID sagaId) {
        return sagaTrabajoRepository.buscarPorId(sagaId);
    }
}
