package com.hda.notificaciones.entrypoints.pulsar;

import com.hda.eventos.notificaciones.commands.v1.NotificarFalloCommandV1;
import com.hda.notificaciones.model.idempotencia.gateways.EventDeduplicationStore;
import com.hda.notificaciones.usecase.enviarnotificacionsaga.EnviarNotificacionSagaUseCase;
import com.hda.notificaciones.usecase.enviarnotificacionsaga.NotificarSagaCommand;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Ultimo paso del camino de compensacion de la saga (NOTIFICAR_FALLO). Misma idempotencia
 * por sagaId que NotificarAsignacionCommandListener. */
@Component
public class NotificarFalloCommandListener {

    private static final Logger log = LoggerFactory.getLogger(NotificarFalloCommandListener.class);

    private final PulsarClient pulsarClient;
    private final EnviarNotificacionSagaUseCase useCase;
    private final EventDeduplicationStore deduplicationStore;
    private final String topico;
    private final String suscripcion;
    private Consumer<NotificarFalloCommandV1> consumidor;

    public NotificarFalloCommandListener(
            PulsarClient pulsarClient,
            EnviarNotificacionSagaUseCase useCase,
            EventDeduplicationStore deduplicationStore,
            @Value("${hda.pulsar.topic-notificar-fallo-command}") String topico,
            @Value("${hda.pulsar.subscription-notificar-fallo-command}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.deduplicationStore = deduplicationStore;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(NotificarFalloCommandV1.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<NotificarFalloCommandV1> consumer, Message<NotificarFalloCommandV1> mensaje) {
        NotificarFalloCommandV1 entrada = mensaje.getValue();
        log.info("[COMANDO: NOTIFICAR-FALLO -> RECIBIDO: sagaId={}, trabajoId={}]",
                entrada.getSagaId(), entrada.getTrabajoId());

        deduplicationStore.registrarSiNoVisto(entrada.getSagaId())
                .flatMap(primeraVez -> {
                    if (Boolean.FALSE.equals(primeraVez)) {
                        return Mono.<Void>empty();
                    }
                    NotificarSagaCommand comando = new NotificarSagaCommand(
                            entrada.getSagaId(), entrada.getTrabajoId(), entrada.getClienteId(), entrada.getMensaje());
                    // Si falla despues de marcar visto, hay que olvidarlo para que la redelivery
                    // reintente de verdad - si no, la saga se queda en COMPENSANDO para siempre.
                    return useCase.ejecutar(comando)
                            .onErrorResume(err -> deduplicationStore.olvidar(entrada.getSagaId()).then(Mono.error(err)));
                })
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(err -> consumer.negativeAcknowledge(mensaje))
                .subscribe();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        if (consumidor != null) consumidor.close();
    }
}
