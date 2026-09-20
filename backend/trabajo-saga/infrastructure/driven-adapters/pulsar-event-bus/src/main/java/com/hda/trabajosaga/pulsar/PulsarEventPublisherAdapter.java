package com.hda.trabajosaga.pulsar;

import com.hda.eventos.notificaciones.commands.v1.NotificarAsignacionCommandV1;
import com.hda.eventos.notificaciones.commands.v1.NotificarFalloCommandV1;
import com.hda.eventos.proveedor.commands.v1.ReservarProveedorCommandV1;
import com.hda.eventos.trabajos.commands.v1.AsignarTrabajoCommandV1;
import com.hda.eventos.trabajos.commands.v1.CancelarTrabajoCommandV1;
import com.hda.trabajosaga.model.sagatrabajo.AsignarTrabajoSolicitadoEvent;
import com.hda.trabajosaga.model.sagatrabajo.CancelarTrabajoSolicitadoEvent;
import com.hda.trabajosaga.model.sagatrabajo.NotificarAsignacionSolicitadoEvent;
import com.hda.trabajosaga.model.sagatrabajo.NotificarFalloSolicitadoEvent;
import com.hda.trabajosaga.model.sagatrabajo.ReservarProveedorSolicitadoEvent;
import com.hda.trabajosaga.model.sagatrabajo.gateways.SagaTrabajoEventPublisher;
import com.hda.trabajosaga.model.seedwork.DomainEvent;
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

/**
 * Traduce los eventos internos "Solicitado" de SagaTrabajo a los 5 comandos Avro que la
 * saga publica hacia los participantes (seccion 2.4 del plan). Cada topico vive bajo el
 * namespace Pulsar del servicio RECEPTOR del comando (hda/proveedor, hda/trabajos,
 * hda/notificaciones), no bajo uno propio de trabajo-saga-service - mismo criterio que ya
 * usa CrearTrabajoCommandV1 (publicado por integracion-service bajo hda/trabajos).
 */
@Component
public class PulsarEventPublisherAdapter implements SagaTrabajoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PulsarEventPublisherAdapter.class);

    private final Producer<ReservarProveedorCommandV1> productorReservarProveedor;
    private final Producer<AsignarTrabajoCommandV1> productorAsignarTrabajo;
    private final Producer<CancelarTrabajoCommandV1> productorCancelarTrabajo;
    private final Producer<NotificarAsignacionCommandV1> productorNotificarAsignacion;
    private final Producer<NotificarFalloCommandV1> productorNotificarFallo;

    public PulsarEventPublisherAdapter(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.topic-reservar-proveedor-command}") String topicoReservarProveedor,
            @Value("${hda.pulsar.topic-asignar-trabajo-command}") String topicoAsignarTrabajo,
            @Value("${hda.pulsar.topic-cancelar-trabajo-command}") String topicoCancelarTrabajo,
            @Value("${hda.pulsar.topic-notificar-asignacion-command}") String topicoNotificarAsignacion,
            @Value("${hda.pulsar.topic-notificar-fallo-command}") String topicoNotificarFallo) throws Exception {

        this.productorReservarProveedor = pulsarClient.newProducer(Schema.AVRO(ReservarProveedorCommandV1.class))
                .topic(topicoReservarProveedor)
                .producerName("trabajo-saga-service-reservar-proveedor-" + UUID.randomUUID())
                .create();

        this.productorAsignarTrabajo = pulsarClient.newProducer(Schema.AVRO(AsignarTrabajoCommandV1.class))
                .topic(topicoAsignarTrabajo)
                .producerName("trabajo-saga-service-asignar-trabajo-" + UUID.randomUUID())
                .create();

        this.productorCancelarTrabajo = pulsarClient.newProducer(Schema.AVRO(CancelarTrabajoCommandV1.class))
                .topic(topicoCancelarTrabajo)
                .producerName("trabajo-saga-service-cancelar-trabajo-" + UUID.randomUUID())
                .create();

        this.productorNotificarAsignacion = pulsarClient.newProducer(Schema.AVRO(NotificarAsignacionCommandV1.class))
                .topic(topicoNotificarAsignacion)
                .producerName("trabajo-saga-service-notificar-asignacion-" + UUID.randomUUID())
                .create();

        this.productorNotificarFallo = pulsarClient.newProducer(Schema.AVRO(NotificarFalloCommandV1.class))
                .topic(topicoNotificarFallo)
                .producerName("trabajo-saga-service-notificar-fallo-" + UUID.randomUUID())
                .create();
    }

    @Override
    public Mono<Void> publicarTodos(List<DomainEvent> eventos) {
        return Flux.fromIterable(eventos)
                .flatMap(this::publicar)
                .then();
    }

    private Mono<Void> publicar(DomainEvent evento) {
        if (evento instanceof ReservarProveedorSolicitadoEvent e) {
            ReservarProveedorCommandV1 mensajeAvro = ReservarProveedorCommandV1.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setCategoriaServicio(e.categoriaServicio())
                    .setCiudad(e.ciudad())
                    .build();
            return Mono.fromFuture(productorReservarProveedor.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[COMANDO: RESERVAR-PROVEEDOR -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        if (evento instanceof AsignarTrabajoSolicitadoEvent e) {
            AsignarTrabajoCommandV1 mensajeAvro = AsignarTrabajoCommandV1.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setProveedorId(e.proveedorId().toString())
                    .build();
            return Mono.fromFuture(productorAsignarTrabajo.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[COMANDO: ASIGNAR-TRABAJO -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        if (evento instanceof CancelarTrabajoSolicitadoEvent e) {
            CancelarTrabajoCommandV1 mensajeAvro = CancelarTrabajoCommandV1.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setMotivo(e.motivo())
                    .build();
            return Mono.fromFuture(productorCancelarTrabajo.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[COMANDO: CANCELAR-TRABAJO -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        if (evento instanceof NotificarAsignacionSolicitadoEvent e) {
            NotificarAsignacionCommandV1 mensajeAvro = NotificarAsignacionCommandV1.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setClienteId(e.clienteId().toString())
                    .setMensaje(e.mensaje())
                    .build();
            return Mono.fromFuture(productorNotificarAsignacion.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[COMANDO: NOTIFICAR-ASIGNACION -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        if (evento instanceof NotificarFalloSolicitadoEvent e) {
            NotificarFalloCommandV1 mensajeAvro = NotificarFalloCommandV1.newBuilder()
                    .setSagaId(e.sagaId().toString())
                    .setTrabajoId(e.trabajoId().toString())
                    .setClienteId(e.clienteId().toString())
                    .setMensaje(e.mensaje())
                    .build();
            return Mono.fromFuture(productorNotificarFallo.sendAsync(mensajeAvro))
                    .doOnSuccess(id -> log.info("[COMANDO: NOTIFICAR-FALLO -> PUBLICADO {}]", mensajeAvro))
                    .then();
        }
        return Mono.empty();
    }
}
