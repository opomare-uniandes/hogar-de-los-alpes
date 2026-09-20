package com.hda.trabajosaga.entrypoints.pulsar;

import com.hda.eventos.proveedor.ProveedorReservado;
import com.hda.trabajosaga.usecase.orquestarsaga.OrquestarSagaTrabajoUseCase;
import com.hda.trabajosaga.usecase.orquestarsaga.ProveedorReservadoComando;
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

/** RESERVAR_PROVEEDOR=OK (camino feliz). */
@Component
public class ProveedorReservadoListener {

    private final PulsarClient pulsarClient;
    private final OrquestarSagaTrabajoUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private Consumer<ProveedorReservado> consumidor;

    public ProveedorReservadoListener(
            PulsarClient pulsarClient,
            OrquestarSagaTrabajoUseCase useCase,
            @Value("${hda.pulsar.topic-proveedor-reservado}") String topico,
            @Value("${hda.pulsar.subscription-proveedor-reservado}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(ProveedorReservado.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<ProveedorReservado> consumer, Message<ProveedorReservado> mensaje) {
        ProveedorReservado entrada = mensaje.getValue();
        ProveedorReservadoComando comando = new ProveedorReservadoComando(
                UUID.fromString(entrada.getSagaId()), UUID.fromString(entrada.getProveedorId()),
                entrada.getNombreProveedor());
        useCase.manejarProveedorReservado(comando)
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(ignored -> consumer.negativeAcknowledge(mensaje))
                .subscribe();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        if (consumidor != null) consumidor.close();
    }
}
