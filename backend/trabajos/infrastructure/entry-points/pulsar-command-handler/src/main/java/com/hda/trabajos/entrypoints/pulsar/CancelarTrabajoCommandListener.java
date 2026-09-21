package com.hda.trabajos.entrypoints.pulsar;

import com.hda.eventos.trabajos.commands.v1.CancelarTrabajoCommandV1;
import com.hda.trabajos.usecase.cancelartrabajo.CancelarTrabajoCommand;
import com.hda.trabajos.usecase.cancelartrabajo.CancelarTrabajoUseCase;
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

/** Entrada asincrona desde trabajo-saga-service (paso COMPENSAR_CANCELAR_TRABAJO, compensacion). */
@Component
public class CancelarTrabajoCommandListener {

    private final PulsarClient pulsarClient;
    private final CancelarTrabajoUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private Consumer<CancelarTrabajoCommandV1> consumidor;

    public CancelarTrabajoCommandListener(
            PulsarClient pulsarClient,
            CancelarTrabajoUseCase useCase,
            @Value("${hda.pulsar.topic-cancelar-trabajo-command}") String topico,
            @Value("${hda.pulsar.subscription-cancelar-trabajo-command}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(CancelarTrabajoCommandV1.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<CancelarTrabajoCommandV1> consumer, Message<CancelarTrabajoCommandV1> mensaje) {
        CancelarTrabajoCommandV1 entrada = mensaje.getValue();
        CancelarTrabajoCommand comando = new CancelarTrabajoCommand(
                UUID.fromString(entrada.getTrabajoId()), UUID.fromString(entrada.getSagaId()), entrada.getMotivo());
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
