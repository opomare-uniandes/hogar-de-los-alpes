package com.hda.integracion.model.rechazo.gateways;

import com.hda.integracion.model.rechazo.SolicitudTrabajoRechazada;
import reactor.core.publisher.Mono;

public interface SolicitudTrabajoRechazoPublisher {
    Mono<Void> publicar(SolicitudTrabajoRechazada rechazo);
}
