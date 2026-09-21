package com.hda.proveedor.pulsar;

import com.hda.eventos.proveedor.ProveedorLiberado;
import com.hda.eventos.proveedor.ProveedorNoDisponible;
import com.hda.eventos.proveedor.ProveedorReservado;
import com.hda.proveedor.model.proveedor.ProveedorLiberadoDomainEvent;
import com.hda.proveedor.model.proveedor.ProveedorNoDisponibleDomainEvent;
import com.hda.proveedor.model.proveedor.ProveedorReservadoDomainEvent;
import com.hda.proveedor.model.proveedor.gateways.ProveedorEventPublisher;
import com.hda.proveedor.model.seedwork.DomainEvent;
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
public class PulsarEventPublisherAdapter implements ProveedorEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PulsarEventPublisherAdapter.class);

    private final Producer<ProveedorReservado> productorReservado;
    private final Producer<ProveedorNoDisponible> productorNoDisponible;
    private final Producer<ProveedorLiberado> productorLiberado;

    public PulsarEventPublisherAdapter(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.topic-proveedor-reservado}") String topicoReservado,
            @Value("${hda.pulsar.topic-proveedor-no-disponible}") String topicoNoDisponible,
            @Value("${hda.pulsar.topic-proveedor-liberado}") String topicoLiberado) throws Exception {

        this.productorReservado = pulsarClient.newProducer(Schema.AVRO(ProveedorReservado.class))
                .topic(topicoReservado)
                .producerName("proveedor-service-reservado-" + UUID.randomUUID())
                .create();

        this.productorNoDisponible = pulsarClient.newProducer(Schema.AVRO(ProveedorNoDisponible.class))
                .topic(topicoNoDisponible)
                .producerName("proveedor-service-no-disponible-" + UUID.randomUUID())
                .create();

        this.productorLiberado = pulsarClient.newProducer(Schema.AVRO(ProveedorLiberado.class))
                .topic(topicoLiberado)
                .producerName("proveedor-service-liberado-" + UUID.randomUUID())
                .create();
    }

    @Override
    public Mono<Void> publicarTodos(List<DomainEvent> eventos) {
        return Flux.fromIterable(eventos)
                .flatMap(this::publicar)
                .then();
    }

    private Mono<Void> publicar(DomainEvent evento) {
        if (evento instanceof ProveedorReservadoDomainEvent e) {
            ProveedorReservado mensajeAvro = ProveedorReservado.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setProveedorId(e.proveedorId().toString())
                    .setNombreProveedor(e.nombreProveedor())
                    .build();
            return Mono.fromFuture(productorReservado.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[EVENTO: PROVEEDOR-RESERVADO -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        if (evento instanceof ProveedorNoDisponibleDomainEvent e) {
            ProveedorNoDisponible mensajeAvro = ProveedorNoDisponible.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setMotivo(e.motivo())
                    .build();
            return Mono.fromFuture(productorNoDisponible.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[EVENTO: PROVEEDOR-NO-DISPONIBLE -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        if (evento instanceof ProveedorLiberadoDomainEvent e) {
            ProveedorLiberado mensajeAvro = ProveedorLiberado.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setProveedorId(e.proveedorId().toString())
                    .build();
            return Mono.fromFuture(productorLiberado.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[EVENTO: PROVEEDOR-LIBERADO -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        return Mono.empty();
    }
}
