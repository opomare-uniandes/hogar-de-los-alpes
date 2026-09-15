package com.hda.integracion.pulsar.partner;

import com.hda.eventos.trabajos.commands.v1.CrearTrabajoCommandV1;
import com.hda.eventos.trabajos.commands.v1.UrgenciaTrabajoV1;
import com.hda.integracion.model.trabajo.CrearTrabajoIntegradoCommand;
import com.hda.integracion.model.trabajo.gateways.CrearTrabajoCommandPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Driven adapter: serializa el comando canónico, no los DTO externos de los partners. */
@Component
public class CrearTrabajoPulsarPublisherAdapter implements CrearTrabajoCommandPublisher {

    private final PulsarClient pulsarClient;
    private final String topicoSalida;
    private Producer<CrearTrabajoCommandV1> productor;

    public CrearTrabajoPulsarPublisherAdapter(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.topic-crear-trabajo-command}") String topicoSalida) {
        this.pulsarClient = pulsarClient;
        this.topicoSalida = topicoSalida;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        productor = pulsarClient.newProducer(Schema.AVRO(CrearTrabajoCommandV1.class)).topic(topicoSalida).create();
    }

    @Override
    public Mono<Void> publicar(CrearTrabajoIntegradoCommand comando) {
        CrearTrabajoCommandV1 mensaje = CrearTrabajoCommandV1.newBuilder()
                .setId(comando.id()).setCorrelationId(comando.correlationId())
                .setPartnerId(comando.partnerId()).setExternalRequestId(comando.externalRequestId())
                .setClienteId(comando.clienteId()).setCategoriaServicio(comando.categoriaServicio())
                .setUrgencia(UrgenciaTrabajoV1.valueOf(comando.urgencia())).setCiudad(comando.ciudad())
                .setPais(comando.pais()).setMoneda(comando.moneda()).setFechaSolicitud(comando.fechaSolicitud())
                .build();
        return Mono.fromFuture(productor.sendAsync(mensaje)).then();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        if (productor != null) productor.close();
    }
}
