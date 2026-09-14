package com.hda.integracion.pulsar.rechazo;

import com.hda.eventos.partner.rechazo.v1.SolicitudTrabajoRechazadaV1;
import com.hda.integracion.model.rechazo.SolicitudTrabajoRechazada;
import com.hda.integracion.model.rechazo.gateways.SolicitudTrabajoRechazoPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SolicitudTrabajoRechazoPulsarPublisherAdapter implements SolicitudTrabajoRechazoPublisher {

    private final PulsarClient pulsarClient;
    private final String topicoSalida;
    private Producer<SolicitudTrabajoRechazadaV1> productor;

    public SolicitudTrabajoRechazoPulsarPublisherAdapter(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.topic-solicitud-partner-rechazada}") String topicoSalida) {
        this.pulsarClient = pulsarClient;
        this.topicoSalida = topicoSalida;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        productor = pulsarClient.newProducer(Schema.AVRO(SolicitudTrabajoRechazadaV1.class)).topic(topicoSalida).create();
    }

    @Override
    public Mono<Void> publicar(SolicitudTrabajoRechazada rechazo) {
        SolicitudTrabajoRechazadaV1 mensaje = SolicitudTrabajoRechazadaV1.newBuilder()
                .setId(rechazo.id()).setCorrelationId(rechazo.correlationId())
                .setSourceEventId(rechazo.sourceEventId()).setPartnerId(rechazo.partnerId())
                .setExternalRequestId(rechazo.externalRequestId()).setContractVersion(rechazo.contractVersion())
                .setReason(rechazo.reason()).setRejectedAt(rechazo.rejectedAt()).build();
        return Mono.fromFuture(productor.sendAsync(mensaje)).then();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        if (productor != null) productor.close();
    }
}
