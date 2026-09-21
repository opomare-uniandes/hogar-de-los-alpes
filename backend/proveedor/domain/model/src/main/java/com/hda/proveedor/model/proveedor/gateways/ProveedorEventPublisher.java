package com.hda.proveedor.model.proveedor.gateways;

import com.hda.proveedor.model.seedwork.DomainEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProveedorEventPublisher {

    Mono<Void> publicarTodos(List<DomainEvent> eventos);
}
