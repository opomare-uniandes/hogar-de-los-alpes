package com.hda.trabajosaga.entrypoints.pulsar;

import com.hda.eventos.notificaciones.NotificacionEnviada;
import com.hda.trabajosaga.usecase.orquestarsaga.NotificacionEnviadaComando;
import com.hda.trabajosaga.usecase.orquestarsaga.OrquestarSagaTrabajoUseCase;
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
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * NOTIFICAR=OK / NOTIFICAR_FALLO=OK (ultimo paso, ambos caminos). NotificacionEnviada es el
 * MISMO evento que notificaciones-service ya publica desde la Entrega 4 para el flujo original
 * (no-saga) de "avisar al cliente que se creo un Trabajo" - por eso sagaId es opcional (ver
 * seccion 6.2 del plan) y este listener debe ignorar en silencio cualquier mensaje sin sagaId:
 * no es una redelivery ni un error, es simplemente una notificacion que no pertenece a esta saga.
 */
@Component
public class NotificacionEnviadaListener {

    private final PulsarClient pulsarClient;
    private final OrquestarSagaTrabajoUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private final ConsumoEnVueloTracker enVuelo = new ConsumoEnVueloTracker();
    private Consumer<NotificacionEnviada> consumidor;

    public NotificacionEnviadaListener(
            PulsarClient pulsarClient,
            OrquestarSagaTrabajoUseCase useCase,
            @Value("${hda.pulsar.topic-notificacion-enviada}") String topico,
            @Value("${hda.pulsar.subscription-notificacion-enviada}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(NotificacionEnviada.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<NotificacionEnviada> consumer, Message<NotificacionEnviada> mensaje) {
        enVuelo.iniciar();
        NotificacionEnviada entrada = mensaje.getValue();
        String sagaId = entrada.getSagaId();

        Mono<Void> resultado = sagaId == null
                ? Mono.empty()
                : useCase.manejarNotificacionEnviada(new NotificacionEnviadaComando(UUID.fromString(sagaId)));

        resultado
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(ignored -> consumer.negativeAcknowledge(mensaje))
                .doFinally(signal -> enVuelo.finalizar())
                .subscribe();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        enVuelo.esperarDrenaje("NotificacionEnviadaListener");
        if (consumidor != null) consumidor.close();
    }
}
