package com.hda.integracion.usecase.traducirtrabajocreado;

import com.hda.integracion.model.trabajocreado.TrabajoCreadoEvento;
import com.hda.integracion.model.trabajosiniestro.TrabajoSiniestro;
import com.hda.integracion.model.trabajosiniestro.gateways.TrabajoSiniestroPublisher;
import reactor.core.publisher.Mono;

public class TraducirTrabajoCreadoUseCase {

    private final TrabajoSiniestroPublisher trabajoSiniestroPublisher;

    public TraducirTrabajoCreadoUseCase(TrabajoSiniestroPublisher trabajoSiniestroPublisher) {
        this.trabajoSiniestroPublisher = trabajoSiniestroPublisher;
    }

    public Mono<Void> ejecutar(TrabajoCreadoEvento evento) {
        TrabajoSiniestro trabajoSiniestro = new TrabajoSiniestro(
                evento.trabajoId(),
                evento.partnerId(),
                evento.categoriaServicio(),
                evento.ciudad(),
                "SINIESTRO".equals(evento.origenTrabajo()),
                evento.moneda(),
                evento.fechaCreacion()
        );

        return trabajoSiniestroPublisher.publicar(trabajoSiniestro);
    }
}
