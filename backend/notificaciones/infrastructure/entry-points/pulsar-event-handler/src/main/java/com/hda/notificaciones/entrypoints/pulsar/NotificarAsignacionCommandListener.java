package com.hda.notificaciones.entrypoints.pulsar;

import com.hda.eventos.notificaciones.commands.v1.NotificarAsignacionCommandV1;
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

/** Ultimo paso del camino feliz de la saga (NOTIFICAR). Idempotencia por sagaId: este
 * comando no trae un id de instancia propio (ver eventos-shared), pero una saga solo
 * publica NotificarAsignacionCommandV1 una vez en su vida - el sagaId alcanza como clave. */
@Component
public class NotificarAsignacionCommandListener {

    private static final Logger log = LoggerFactory.getLogger(NotificarAsignacionCommandListener.class);

    private final PulsarClient pulsarClient;
    private final EnviarNotificacionSagaUseCase useCase;
    private final EventDeduplicationStore deduplicationStore;
    private final String topico;
    private final String suscripcion;
    private Consumer<NotificarAsignacionCommandV1> consumidor;

    public NotificarAsignacionCommandListener(
            PulsarClient pulsarClient,
            EnviarNotificacionSagaUseCase useCase,
            EventDeduplicationStore deduplicationStore,
            @Value("${hda.pulsar.topic-notificar-asignacion-command}") String topico,
            @Value("${hda.pulsar.subscription-notificar-asignacion-command}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.deduplicationStore = deduplicationStore;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(NotificarAsignacionCommandV1.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<NotificarAsignacionCommandV1> consumer, Message<NotificarAsignacionCommandV1> mensaje) {
        NotificarAsignacionCommandV1 entrada = mensaje.getValue();
        log.info("[COMANDO: NOTIFICAR-ASIGNACION -> RECIBIDO: sagaId={}, trabajoId={}]",
                entrada.getSagaId(), entrada.getTrabajoId());

        deduplicationStore.registrarSiNoVisto(entrada.getSagaId())
                .flatMap(primeraVez -> {
                    if (Boolean.FALSE.equals(primeraVez)) {
                        return Mono.<Void>empty();
                    }
                    NotificarSagaCommand comando = new NotificarSagaCommand(
                            entrada.getSagaId(), entrada.getTrabajoId(), entrada.getClienteId(), entrada.getMensaje());
                    // Si falla despues de marcar visto (p.ej. clienteId inexistente en
                    // usuarios-service), hay que olvidarlo para que la redelivery reintente de
                    // verdad - si no, la saga se queda esperando NotificacionEnviada para siempre.
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
