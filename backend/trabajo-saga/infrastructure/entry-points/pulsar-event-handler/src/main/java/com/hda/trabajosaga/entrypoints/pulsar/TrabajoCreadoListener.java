package com.hda.trabajosaga.entrypoints.pulsar;

import com.hda.eventos.dominio.TrabajoCreado;
import com.hda.trabajosaga.usecase.orquestarsaga.OrquestarSagaTrabajoUseCase;
import com.hda.trabajosaga.usecase.orquestarsaga.TrabajoCreadoComando;
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

/** Arranca la saga (paso INICIADA). Tercer suscriptor Shared, independiente, del mismo
 * topico que ya consumen integracion-service y notificaciones-service. */
@Component
public class TrabajoCreadoListener {

    private final PulsarClient pulsarClient;
    private final OrquestarSagaTrabajoUseCase useCase;
    private final String topico;
    private final String suscripcion;
    private Consumer<TrabajoCreado> consumidor;

    public TrabajoCreadoListener(
            PulsarClient pulsarClient,
            OrquestarSagaTrabajoUseCase useCase,
            @Value("${hda.pulsar.topic-trabajo-creado}") String topico,
            @Value("${hda.pulsar.subscription-trabajo-creado}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidor = pulsarClient.newConsumer(Schema.AVRO(TrabajoCreado.class))
                .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                .messageListener(this::procesar).subscribe();
    }

    private void procesar(Consumer<TrabajoCreado> consumer, Message<TrabajoCreado> mensaje) {
        TrabajoCreado entrada = mensaje.getValue();
        TrabajoCreadoComando comando = new TrabajoCreadoComando(
                UUID.fromString(entrada.getTrabajoId()), UUID.fromString(entrada.getClienteId()),
                entrada.getCategoriaServicio(), entrada.getCiudad());
        useCase.manejarTrabajoCreado(comando)
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(ignored -> consumer.negativeAcknowledge(mensaje))
                .subscribe();
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        if (consumidor != null) consumidor.close();
    }
}
