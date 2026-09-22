package com.hda.integracion.entrypoints.pulsar;

import com.hda.eventos.dominio.TrabajoCreado;
import com.hda.integracion.model.idempotencia.gateways.EventDeduplicationStore;
import com.hda.integracion.model.trabajocreado.TrabajoCreadoEvento;
import com.hda.integracion.usecase.traducirtrabajocreado.TraducirTrabajoCreadoUseCase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Flujo de salida (trabajo-creado -> trabajo-siniestro-creado hacia partners externos), no es
 * especifico de un partner de entrada. Se puede desactivar (hda.integracion.outbound.enabled=false)
 * en los despliegues dedicados por partner (ver escenario 4 de escalabilidad: cada partner de
 * entrada escala de forma aislada en su propio Deployment, que no necesita este flujo de salida).
 */
@Component
@ConditionalOnProperty(prefix = "hda.integracion.outbound", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TrabajoCreadoListener {

    private final PulsarClient pulsarClient;
    private final TraducirTrabajoCreadoUseCase traducirTrabajoCreadoUseCase;
    private final EventDeduplicationStore deduplicationStore;
    private final String topicoEntrada;
    private final String suscripcion;
    private static final Logger log = LoggerFactory.getLogger(TrabajoCreadoListener.class);

    private Consumer<TrabajoCreado> consumidor;

    public TrabajoCreadoListener(
            PulsarClient pulsarClient,
            TraducirTrabajoCreadoUseCase traducirTrabajoCreadoUseCase,
            EventDeduplicationStore deduplicationStore,
            @Value("${hda.pulsar.topic-trabajo-creado}") String topicoEntrada,
            @Value("${hda.pulsar.subscription}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.traducirTrabajoCreadoUseCase = traducirTrabajoCreadoUseCase;
        this.deduplicationStore = deduplicationStore;
        this.topicoEntrada = topicoEntrada;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    public void iniciar() throws PulsarClientException {
        this.consumidor = pulsarClient.newConsumer(Schema.AVRO(TrabajoCreado.class))
                .topic(topicoEntrada)
                .subscriptionName(suscripcion)
                .subscriptionType(SubscriptionType.Shared) // permite escalar consumidores en paralelo (ver escenario 2.3)
                .messageListener((Consumer<TrabajoCreado> c, Message<TrabajoCreado> mensaje) ->
                        procesar(c, mensaje))
                .subscribe();
    }

    private void procesar(Consumer<TrabajoCreado> c, Message<TrabajoCreado> mensaje) {
        TrabajoCreado evento = mensaje.getValue();

        TrabajoCreadoEvento eventoDominio = new TrabajoCreadoEvento(
                evento.getId(),
                evento.getTrabajoId(),
                evento.getPartnerId(),
                evento.getCategoriaServicio(),
                evento.getCiudad(),
                evento.getOrigenTrabajo().toString(),
                evento.getMoneda(),
                evento.getFechaCreacion()
        );

        log.info("[EVENTO: TRABAJO-CREADO -> RECIBIDO: {}]", eventoDominio);

        deduplicationStore.registrarSiNoVisto(eventoDominio.id())
                .flatMap(primeraVez -> {
                    if (Boolean.FALSE.equals(primeraVez)) {
                        // duplicado: se hizo ack para no reprocesar; no se ejecuta el caso de uso.
                        return reactor.core.publisher.Mono.empty();
                    }
                    return traducirTrabajoCreadoUseCase.ejecutar(eventoDominio);
                })
                .doOnSuccess(v -> c.acknowledgeAsync(mensaje))
                .doOnError(err -> c.negativeAcknowledge(mensaje))
                .subscribe();
    }

    @PreDestroy
    public void detener() throws PulsarClientException {
        if (consumidor != null) consumidor.close();
    }
}
