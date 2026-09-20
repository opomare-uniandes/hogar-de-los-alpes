package com.hda.trabajosaga.model.sagatrabajo.gateways;

import com.hda.trabajosaga.model.seedwork.DomainEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SagaTrabajoEventPublisher {

    Mono<Void> publicarTodos(List<DomainEvent> eventos);
}
