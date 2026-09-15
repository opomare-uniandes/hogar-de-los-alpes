package com.hda.integracion.pulsar;

import com.hda.eventos.integracion.TrabajoSiniestroCreado;
import com.hda.integracion.model.trabajosiniestro.TrabajoSiniestro;
import com.hda.integracion.model.trabajosiniestro.gateways.TrabajoSiniestroPublisher;
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
public class TrabajoSiniestroPulsarPublisherAdapter implements TrabajoSiniestroPublisher {

    private final PulsarClient pulsarClient;
    private final String topicoSalida;

    private Producer<TrabajoSiniestroCreado> productor;
    private static final Logger log = LoggerFactory.getLogger(TrabajoSiniestroPulsarPublisherAdapter.class);


    public TrabajoSiniestroPulsarPublisherAdapter(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.topic-trabajo-siniestro-creado}") String topicoSalida) {
        this.pulsarClient = pulsarClient;
        this.topicoSalida = topicoSalida;
    }

    @PostConstruct
    public void iniciar() throws PulsarClientException {

        this.productor = pulsarClient.newProducer(Schema.AVRO(TrabajoSiniestroCreado.class))
                .topic(topicoSalida)
                .producerName("integracion-service-" + UUID.randomUUID())
                .create();
    }

    @Override
    public Mono<Void> publicar(TrabajoSiniestro trabajoSiniestro) {
        TrabajoSiniestroCreado mensajeAvro = TrabajoSiniestroCreado.newBuilder()
                .setTrabajoId(trabajoSiniestro.trabajoId())
                .setPartnerId(trabajoSiniestro.partnerId())
                .setCategoria(trabajoSiniestro.categoria())
                .setCiudad(trabajoSiniestro.ciudad())
                .setEsSiniestro(trabajoSiniestro.esSiniestro())
                .setMoneda(trabajoSiniestro.moneda())
                .setFechaEvento(trabajoSiniestro.fechaEvento())
                .setVersion("v1")
                .build();

        return Mono.fromFuture(productor.sendAsync(mensajeAvro))
                .doOnSuccess(event -> {
                    log.info("[EVENTO: TRABAJO-SINIESTRO-CREADO -> PUBLICADO {}]", mensajeAvro);
                })
                .then();
    }

    @PreDestroy
    public void detener() throws PulsarClientException {
        if (productor != null) productor.close();
    }
}
