package com.hda.trabajos.model.trabajo.gateways;

import com.hda.trabajos.model.seedwork.DomainEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TrabajoEventPublisher {

    Mono<Void> publicarTodos(List<DomainEvent> eventos);
}
