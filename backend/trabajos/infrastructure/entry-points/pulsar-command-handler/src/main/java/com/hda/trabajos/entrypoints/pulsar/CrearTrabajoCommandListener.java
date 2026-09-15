package com.hda.trabajos.entrypoints.pulsar;

import com.hda.eventos.trabajos.commands.v1.CrearTrabajoCommandV1;
import com.hda.trabajos.model.trabajo.OrigenTrabajo;
import com.hda.trabajos.model.trabajo.Urgencia;
import com.hda.trabajos.usecase.creartrabajo.CrearTrabajoCommand;
import com.hda.trabajos.usecase.creartrabajo.CrearTrabajoUseCase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Entrada asíncrona al contexto Trabajos desde el comando canónico de Integración. */
@Component
public class CrearTrabajoCommandListener {

    private final PulsarClient pulsarClient;
    private final CrearTrabajoUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private Consumer<CrearTrabajoCommandV1> consumidor;

    public CrearTrabajoCommandListener(
            PulsarClient pulsarClient,
            CrearTrabajoUseCase useCase,
            @Value("${hda.pulsar.topic-crear-trabajo-command}") String topico,
            @Value("${hda.pulsar.subscription-crear-trabajo-command}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(CrearTrabajoCommandV1.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<CrearTrabajoCommandV1> consumer, Message<CrearTrabajoCommandV1> mensaje) {
        CrearTrabajoCommandV1 entrada = mensaje.getValue();
        CrearTrabajoCommand comando = new CrearTrabajoCommand(
                UUID.fromString(entrada.getClienteId()), entrada.getCategoriaServicio(),
                Urgencia.valueOf(entrada.getUrgencia().name()), entrada.getCiudad(), OrigenTrabajo.SINIESTRO,
                UUID.fromString(entrada.getPartnerId()), entrada.getMoneda());
        useCase.ejecutar(comando)
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(ignored -> consumer.negativeAcknowledge(mensaje))
                .subscribe();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        if (consumidor != null) consumidor.close();
    }
}
