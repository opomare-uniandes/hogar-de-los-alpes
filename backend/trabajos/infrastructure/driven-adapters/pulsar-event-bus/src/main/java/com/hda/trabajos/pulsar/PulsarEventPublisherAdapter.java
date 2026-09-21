package com.hda.trabajos.pulsar;

import com.hda.eventos.dominio.OrigenTrabajo;
import com.hda.eventos.dominio.TrabajoAsignado;
import com.hda.eventos.dominio.TrabajoCancelado;
import com.hda.eventos.dominio.TrabajoCreado;
import com.hda.eventos.dominio.Urgencia;
import com.hda.trabajos.model.seedwork.DomainEvent;
import com.hda.trabajos.model.trabajo.TrabajoAsignadoDomainEvent;
import com.hda.trabajos.model.trabajo.TrabajoCanceladoDomainEvent;
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
    private final Producer<TrabajoAsignado> productorTrabajoAsignado;
    private final Producer<TrabajoCancelado> productorTrabajoCancelado;
    private static final Logger log = LoggerFactory.getLogger(PulsarEventPublisherAdapter.class);

    public PulsarEventPublisherAdapter(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.topic-trabajo-creado}") String topicoTrabajoCreado,
            @Value("${hda.pulsar.topic-trabajo-asignado}") String topicoTrabajoAsignado,
            @Value("${hda.pulsar.topic-trabajo-cancelado}") String topicoTrabajoCancelado) throws Exception {

        this.productorTrabajoCreado = pulsarClient.newProducer(Schema.AVRO(TrabajoCreado.class))
                .topic(topicoTrabajoCreado)
                .producerName("trabajos-service-creado-" + UUID.randomUUID())
                .create();

        this.productorTrabajoAsignado = pulsarClient.newProducer(Schema.AVRO(TrabajoAsignado.class))
                .topic(topicoTrabajoAsignado)
                .producerName("trabajos-service-asignado-" + UUID.randomUUID())
                .create();

        this.productorTrabajoCancelado = pulsarClient.newProducer(Schema.AVRO(TrabajoCancelado.class))
                .topic(topicoTrabajoCancelado)
                .producerName("trabajos-service-cancelado-" + UUID.randomUUID())
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
        if (evento instanceof TrabajoAsignadoDomainEvent e) {
            TrabajoAsignado mensajeAvro = TrabajoAsignado.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setEstado("ASIGNADO")
                    .build();

            return Mono.fromFuture(productorTrabajoAsignado.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[EVENTO: TRABAJO-ASIGNADO -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        if (evento instanceof TrabajoCanceladoDomainEvent e) {
            TrabajoCancelado mensajeAvro = TrabajoCancelado.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setEstado("CANCELADO")
                    .build();

            return Mono.fromFuture(productorTrabajoCancelado.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[EVENTO: TRABAJO-CANCELADO -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        return Mono.empty();
    }
}
