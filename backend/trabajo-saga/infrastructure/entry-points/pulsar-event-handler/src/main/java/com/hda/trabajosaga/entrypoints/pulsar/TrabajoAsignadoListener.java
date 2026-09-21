package com.hda.trabajosaga.entrypoints.pulsar;

import com.hda.eventos.dominio.TrabajoAsignado;
import com.hda.trabajosaga.usecase.orquestarsaga.OrquestarSagaTrabajoUseCase;
import com.hda.trabajosaga.usecase.orquestarsaga.TrabajoAsignadoComando;
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

/** ASIGNAR_TRABAJO=OK (camino feliz). */
@Component
public class TrabajoAsignadoListener {

    private final PulsarClient pulsarClient;
    private final OrquestarSagaTrabajoUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private final ConsumoEnVueloTracker enVuelo = new ConsumoEnVueloTracker();
    private Consumer<TrabajoAsignado> consumidor;

    public TrabajoAsignadoListener(
            PulsarClient pulsarClient,
            OrquestarSagaTrabajoUseCase useCase,
            @Value("${hda.pulsar.topic-trabajo-asignado}") String topico,
            @Value("${hda.pulsar.subscription-trabajo-asignado}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(TrabajoAsignado.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<TrabajoAsignado> consumer, Message<TrabajoAsignado> mensaje) {
        enVuelo.iniciar();
        TrabajoAsignado entrada = mensaje.getValue();
        TrabajoAsignadoComando comando = new TrabajoAsignadoComando(UUID.fromString(entrada.getSagaId()));
        useCase.manejarTrabajoAsignado(comando)
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(ignored -> consumer.negativeAcknowledge(mensaje))
                .doFinally(signal -> enVuelo.finalizar())
                .subscribe();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        enVuelo.esperarDrenaje("TrabajoAsignadoListener");
        if (consumidor != null) consumidor.close();
    }
}
