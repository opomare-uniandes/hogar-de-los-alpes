package com.hda.proveedor.entrypoints.pulsar;

import com.hda.eventos.proveedor.commands.v1.ReservarProveedorCommandV1;
import com.hda.proveedor.usecase.reservarproveedor.ReservarProveedorCommand;
import com.hda.proveedor.usecase.reservarproveedor.ReservarProveedorUseCase;
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

/** Entrada asincrona al contexto Proveedor desde trabajo-saga-service (paso RESERVAR_PROVEEDOR). */
@Component
public class ReservarProveedorCommandListener {

    private final PulsarClient pulsarClient;
    private final ReservarProveedorUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private Consumer<ReservarProveedorCommandV1> consumidor;

    private static final Logger log = LoggerFactory.getLogger(ReservarProveedorCommandListener.class);

    public ReservarProveedorCommandListener(
            PulsarClient pulsarClient,
            ReservarProveedorUseCase useCase,
            @Value("${hda.pulsar.topic-reservar-proveedor-command}") String topico,
            @Value("${hda.pulsar.subscription-reservar-proveedor-command}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(ReservarProveedorCommandV1.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<ReservarProveedorCommandV1> consumer, Message<ReservarProveedorCommandV1> mensaje) {
        ReservarProveedorCommandV1 entrada = mensaje.getValue();
        ReservarProveedorCommand comando = new ReservarProveedorCommand(
                UUID.fromString(entrada.getSagaId()), UUID.fromString(entrada.getTrabajoId()),
                entrada.getCategoriaServicio(), entrada.getCiudad());

        useCase.ejecutar(comando)
                .doOnSubscribe(s -> log.info("[COMANDO: RESERVAR-PROVEEDOR-COMMANDO -> RECIBIDO: {}]", comando))
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(ignored -> consumer.negativeAcknowledge(mensaje))
                .subscribe();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        if (consumidor != null) consumidor.close();
    }
}
