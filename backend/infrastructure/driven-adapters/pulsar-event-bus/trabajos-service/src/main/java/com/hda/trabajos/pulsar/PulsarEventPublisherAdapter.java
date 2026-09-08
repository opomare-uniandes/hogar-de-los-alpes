package com.hda.trabajos.pulsar;

import com.hda.eventos.dominio.OrigenTrabajo;
import com.hda.eventos.dominio.TrabajoCreado;
import com.hda.eventos.dominio.Urgencia;
import com.hda.trabajos.model.seedwork.DomainEvent;
import com.hda.trabajos.model.trabajo.TrabajoCreadoDomainEvent;
import com.hda.trabajos.model.trabajo.gateways.TrabajoEventPublisher;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
public class PulsarEventPublisherAdapter implements TrabajoEventPublisher {

    private final Producer<TrabajoCreado> productorTrabajoCreado;
    private static final Logger log = LoggerFactory.getLogger(PulsarEventPublisherAdapter.class);

    public PulsarEventPublisherAdapter(PulsarClient pulsarClient,
                                        @Value("${hda.pulsar.topic-trabajo-creado}") String topico) throws Exception {

        this.productorTrabajoCreado = pulsarClient.newProducer(Schema.AVRO(TrabajoCreado.class))
                .topic(topico)
                .producerName("trabajos-service-" + UUID.randomUUID())
                .create();
    }

    @Override
    public Mono<Void> publicarTodos(List<DomainEvent> eventos) {
        return Flux.fromIterable(eventos)
                .flatMap(this::publicar)
                .then();
    }

    private Mono<Void> publicar(DomainEvent evento) {
        if (evento instanceof TrabajoCreadoDomainEvent e) {
            TrabajoCreado mensajeAvro = TrabajoCreado.newBuilder()
                    .setId(e.id().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setClienteId(e.clienteId().toString())
                    .setCategoriaServicio(e.categoriaServicio())
                    .setUrgencia(Urgencia.valueOf(e.urgencia().name()))
                    .setCiudad(e.ciudad())
                    .setOrigenTrabajo(OrigenTrabajo.valueOf(e.origen().name()))
                    .setPartnerId(e.partnerId() == null ? null : e.partnerId().toString())
                    .setMoneda(e.moneda())
                    .setFechaCreacion(e.ocurridoEn())
                    .build();

            return Mono.fromFuture(productorTrabajoCreado.sendAsync(mensajeAvro))
                    .doOnSuccess(event -> log.info("[EVENTO: TRABAJO-CREADO -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        return Mono.empty();
    }
}
