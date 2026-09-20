package com.hda.trabajos.entrypoints.pulsar;

import com.hda.eventos.trabajos.commands.v1.AsignarTrabajoCommandV1;
import com.hda.trabajos.usecase.asignartrabajo.AsignarTrabajoCommand;
import com.hda.trabajos.usecase.asignartrabajo.AsignarTrabajoUseCase;
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

/** Entrada asincrona desde trabajo-saga-service (paso ASIGNAR_TRABAJO). */
@Component
public class AsignarTrabajoCommandListener {

    private final PulsarClient pulsarClient;
    private final AsignarTrabajoUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private Consumer<AsignarTrabajoCommandV1> consumidor;

    public AsignarTrabajoCommandListener(
            PulsarClient pulsarClient,
            AsignarTrabajoUseCase useCase,
            @Value("${hda.pulsar.topic-asignar-trabajo-command}") String topico,
            @Value("${hda.pulsar.subscription-asignar-trabajo-command}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(AsignarTrabajoCommandV1.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<AsignarTrabajoCommandV1> consumer, Message<AsignarTrabajoCommandV1> mensaje) {
        AsignarTrabajoCommandV1 entrada = mensaje.getValue();
        AsignarTrabajoCommand comando = new AsignarTrabajoCommand(
                UUID.fromString(entrada.getTrabajoId()), UUID.fromString(entrada.getSagaId()));
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
