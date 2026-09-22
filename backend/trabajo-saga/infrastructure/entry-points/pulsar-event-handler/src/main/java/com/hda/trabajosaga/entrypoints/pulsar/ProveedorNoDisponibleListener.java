package com.hda.trabajosaga.entrypoints.pulsar;

import com.hda.eventos.proveedor.ProveedorNoDisponible;
import com.hda.trabajosaga.usecase.orquestarsaga.OrquestarSagaTrabajoUseCase;
import com.hda.trabajosaga.usecase.orquestarsaga.ProveedorNoDisponibleComando;
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

/** RESERVAR_PROVEEDOR=FALLO (arranca la compensacion). */
@Component
public class ProveedorNoDisponibleListener {

    private final PulsarClient pulsarClient;
    private final OrquestarSagaTrabajoUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private final ConsumoEnVueloTracker enVuelo = new ConsumoEnVueloTracker();
    private Consumer<ProveedorNoDisponible> consumidor;

    public ProveedorNoDisponibleListener(
            PulsarClient pulsarClient,
            OrquestarSagaTrabajoUseCase useCase,
            @Value("${hda.pulsar.topic-proveedor-no-disponible}") String topico,
            @Value("${hda.pulsar.subscription-proveedor-no-disponible}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(ProveedorNoDisponible.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<ProveedorNoDisponible> consumer, Message<ProveedorNoDisponible> mensaje) {
        enVuelo.iniciar();
        ProveedorNoDisponible entrada = mensaje.getValue();
        ProveedorNoDisponibleComando comando = new ProveedorNoDisponibleComando(
                UUID.fromString(entrada.getSagaId()), entrada.getMotivo());
        useCase.manejarProveedorNoDisponible(comando)
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(ignored -> consumer.negativeAcknowledge(mensaje))
                .doFinally(signal -> enVuelo.finalizar())
                .subscribe();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        enVuelo.esperarDrenaje("ProveedorNoDisponibleListener");
        if (consumidor != null) consumidor.close();
    }
}
