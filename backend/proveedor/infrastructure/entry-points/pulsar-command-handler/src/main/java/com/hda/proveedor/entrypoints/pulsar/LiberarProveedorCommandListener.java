package com.hda.proveedor.entrypoints.pulsar;

import com.hda.eventos.proveedor.commands.v1.LiberarProveedorCommandV1;
import com.hda.proveedor.usecase.liberarproveedor.LiberarProveedorCommand;
import com.hda.proveedor.usecase.liberarproveedor.LiberarProveedorUseCase;
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

import java.util.UUID;

/**
 * Entrada asincrona al contexto Proveedor desde trabajo-saga-service: compensacion
 * tardia (libera un proveedor ya reservado cuando un paso posterior de la saga falla).
 * El camino de fallo demostrado en esta entrega (proveedor no disponible) nunca la
 * dispara, pero el handler esta implementado de verdad (ver seccion 3 del plan).
 */
@Component
public class LiberarProveedorCommandListener {

    private final PulsarClient pulsarClient;
    private final LiberarProveedorUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private Consumer<LiberarProveedorCommandV1> consumidor;

    private static final Logger log = LoggerFactory.getLogger(LiberarProveedorCommandListener.class);

    public LiberarProveedorCommandListener(
            PulsarClient pulsarClient,
            LiberarProveedorUseCase useCase,
            @Value("${hda.pulsar.topic-liberar-proveedor-command}") String topico,
            @Value("${hda.pulsar.subscription-liberar-proveedor-command}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(LiberarProveedorCommandV1.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<LiberarProveedorCommandV1> consumer, Message<LiberarProveedorCommandV1> mensaje) {
        LiberarProveedorCommandV1 entrada = mensaje.getValue();
        LiberarProveedorCommand comando = new LiberarProveedorCommand(
                UUID.fromString(entrada.getSagaId()), UUID.fromString(entrada.getProveedorId()));

        log.info("[COMANDO: LIBERAR-PROVEEDOR-COMMANDO -> RECIBIDO: {}]", comando);

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
