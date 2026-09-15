package com.hda.notificaciones.pulsar;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.notificacion.gateways.NotificacionEnviadaPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class NotificacionEnviadaPulsarPublisherAdapter implements NotificacionEnviadaPublisher {

    private final PulsarClient pulsarClient;
    private final String topicoSalida;

    private Producer<com.hda.eventos.notificaciones.NotificacionEnviada> productor;
    private static final Logger log = LoggerFactory.getLogger(NotificacionEnviadaPulsarPublisherAdapter.class);

    public NotificacionEnviadaPulsarPublisherAdapter(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.topic-notificacion-enviada}") String topicoSalida) {
        this.pulsarClient = pulsarClient;
        this.topicoSalida = topicoSalida;
    }

    @PostConstruct
    public void iniciar() throws PulsarClientException {

        this.productor = pulsarClient.newProducer(Schema.AVRO(com.hda.eventos.notificaciones.NotificacionEnviada.class))
                .topic(topicoSalida)
                .producerName("notificaciones-service-" + UUID.randomUUID())
                .create();
    }

    @Override
    public Mono<Void> publicar(NotificacionEnviada notificacionEnviada) {
        com.hda.eventos.notificaciones.NotificacionEnviada mensajeAvro =
                com.hda.eventos.notificaciones.NotificacionEnviada.newBuilder()
                        .setTrabajoId(notificacionEnviada.trabajoId())
                        .setClienteId(notificacionEnviada.clienteId())
                        .setCanal(notificacionEnviada.canal())
                        .setDestinatario(notificacionEnviada.destinatario())
                        .setMensaje(notificacionEnviada.mensaje())
                        .setFechaEnvio(notificacionEnviada.fechaEnvio())
                        .build();

        return Mono.fromFuture(productor.sendAsync(mensajeAvro))
                .doOnSuccess(event -> {
                    log.info("[EVENTO: NOTIFICACION-ENVIADA -> PUBLICADO {}]", mensajeAvro);
                })
                .then();
    }

    @PreDestroy
    public void detener() throws PulsarClientException {
        if (productor != null) productor.close();
    }
}
