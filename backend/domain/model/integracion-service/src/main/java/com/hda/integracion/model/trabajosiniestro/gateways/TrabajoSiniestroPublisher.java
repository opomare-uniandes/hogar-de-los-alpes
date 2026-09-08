package com.hda.integracion.model.trabajosiniestro.gateways;

import com.hda.integracion.model.trabajosiniestro.TrabajoSiniestro;
import reactor.core.publisher.Mono;

public interface TrabajoSiniestroPublisher {

    Mono<Void> publicar(TrabajoSiniestro trabajoSiniestro);
}
