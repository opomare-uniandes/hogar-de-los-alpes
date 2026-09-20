package com.hda.trabajosaga.entrypoints.pulsar;

import com.hda.eventos.dominio.TrabajoCancelado;
import com.hda.trabajosaga.usecase.orquestarsaga.OrquestarSagaTrabajoUseCase;
import com.hda.trabajosaga.usecase.orquestarsaga.TrabajoCanceladoComando;
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

/** COMPENSAR_CANCELAR_TRABAJO=OK (compensacion). */
@Component
public class TrabajoCanceladoListener {

    private final PulsarClient pulsarClient;
    private final OrquestarSagaTrabajoUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private Consumer<TrabajoCancelado> consumidor;

    public TrabajoCanceladoListener(
            PulsarClient pulsarClient,
            OrquestarSagaTrabajoUseCase useCase,
            @Value("${hda.pulsar.topic-trabajo-cancelado}") String topico,
            @Value("${hda.pulsar.subscription-trabajo-cancelado}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(TrabajoCancelado.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<TrabajoCancelado> consumer, Message<TrabajoCancelado> mensaje) {
        TrabajoCancelado entrada = mensaje.getValue();
        TrabajoCanceladoComando comando = new TrabajoCanceladoComando(UUID.fromString(entrada.getSagaId()));
        useCase.manejarTrabajoCancelado(comando)
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(ignored -> consumer.negativeAcknowledge(mensaje))
                .subscribe();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        if (consumidor != null) consumidor.close();
    }
}
