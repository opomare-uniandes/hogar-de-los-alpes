package com.hda.integracion.entrypoints.pulsar.partner;

import com.hda.eventos.partner.v1.SolicitudTrabajoPartnerV1;
import com.hda.eventos.partner.v2.SolicitudTrabajoPartnerV2;
import com.hda.integracion.model.idempotencia.gateways.EventDeduplicationStore;
import com.hda.integracion.model.partner.SolicitudTrabajoPartner;
import com.hda.integracion.model.partner.TranslationException;
import com.hda.integracion.model.rechazo.SolicitudTrabajoRechazada;
import com.hda.integracion.model.rechazo.gateways.SolicitudTrabajoRechazoPublisher;
import com.hda.integracion.usecase.traducirsolicitudpartner.TraducirSolicitudPartnerUseCase;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Consume UN SOLO topico de partner (una version de contrato, un partner) por instancia de este
 * servicio -- ver escenario 4 de escalabilidad: cada partner B2B2C debe escalar de forma aislada
 * ante un pico de trafico, sin consumir la capacidad de otro. Eso exige que cada partner tenga su
 * propio Deployment con su propio ScaledObject de KEDA (ver deploy/k8s/apps e
 * deploy/k8s/autoscaling), lo cual a su vez exige que cada instancia de integracion-service
 * escuche solo el topico/suscripcion/version de ESE partner, configurables por entorno
 * (hda.pulsar.partner.*), en vez de los dos topicos v1+v2 hardcodeados de antes.
 */
@Component
@ConditionalOnProperty(prefix = "hda.integracion.inbound-partner", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SolicitudTrabajoPartnerListener {

    private static final Logger log = LoggerFactory.getLogger(SolicitudTrabajoPartnerListener.class);

    private final PulsarClient pulsarClient;
    private final TraducirSolicitudPartnerUseCase useCase;
    private final EventDeduplicationStore deduplicationStore;
    private final SolicitudTrabajoRechazoPublisher rechazoPublisher;
    private final String versionContrato;
    private final String topico;
    private final String suscripcion;
    private Consumer<?> consumidor;

    public SolicitudTrabajoPartnerListener(
            PulsarClient pulsarClient,
            TraducirSolicitudPartnerUseCase useCase,
            EventDeduplicationStore deduplicationStore,
            SolicitudTrabajoRechazoPublisher rechazoPublisher,
            @Value("${hda.pulsar.partner.contract-version}") String versionContrato,
            @Value("${hda.pulsar.partner.topic}") String topico,
            @Value("${hda.pulsar.partner.subscription}") String suscripcion) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.deduplicationStore = deduplicationStore;
        this.rechazoPublisher = rechazoPublisher;
        this.versionContrato = versionContrato;
        this.topico = topico;
        this.suscripcion = suscripcion;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        if ("v2".equalsIgnoreCase(versionContrato)) {
            consumidor = pulsarClient.newConsumer(Schema.AVRO(SolicitudTrabajoPartnerV2.class))
                    .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                    .messageListener((Consumer<SolicitudTrabajoPartnerV2> c, Message<SolicitudTrabajoPartnerV2> mensaje) ->
                            procesar(c, mensaje, SolicitudTrabajoPartnerMapper.desdeV2(mensaje.getValue())))
                    .subscribe();
        } else if ("v1".equalsIgnoreCase(versionContrato)) {
            consumidor = pulsarClient.newConsumer(Schema.AVRO(SolicitudTrabajoPartnerV1.class))
                    .topic(topico).subscriptionName(suscripcion).subscriptionType(SubscriptionType.Shared)
                    .messageListener((Consumer<SolicitudTrabajoPartnerV1> c, Message<SolicitudTrabajoPartnerV1> mensaje) ->
                            procesar(c, mensaje, SolicitudTrabajoPartnerMapper.desdeV1(mensaje.getValue())))
                    .subscribe();
        } else {
            throw new IllegalStateException(
                    "hda.pulsar.partner.contract-version debe ser 'v1' o 'v2', llego: " + versionContrato);
        }
        log.info("[PARTNER] Escuchando topico={} suscripcion={} version={}", topico, suscripcion, versionContrato);
    }

    private <T> void procesar(Consumer<T> consumer, Message<T> mensaje, SolicitudTrabajoPartner solicitud) {
        deduplicationStore.registrarSiNoVisto(solicitud.claveIdempotencia())
                .flatMap(primeraVez -> Boolean.TRUE.equals(primeraVez)
                        ? useCase.ejecutar(solicitud)
                                .onErrorResume(TranslationException.class, error -> publicarRechazo(solicitud, error))
                        : Mono.empty())
                .doOnSuccess(ignored -> consumer.acknowledgeAsync(mensaje))
                .doOnError(error -> {
                    log.error("No se pudo traducir solicitud partner id={} version={}", solicitud.id(), solicitud.versionContrato(), error);
                    consumer.negativeAcknowledge(mensaje);
                })
                .subscribe();
    }

    private Mono<Void> publicarRechazo(SolicitudTrabajoPartner solicitud, TranslationException error) {
        SolicitudTrabajoRechazada rechazo = new SolicitudTrabajoRechazada(
                UUID.randomUUID().toString(), solicitud.correlationId(), solicitud.id(), solicitud.partnerId(),
                solicitud.externalRequestId(), solicitud.versionContrato(), error.getMessage(), Instant.now());
        return rechazoPublisher.publicar(rechazo);
    }

    @PreDestroy
    void detener() throws PulsarClientException {
        if (consumidor != null) consumidor.close();
    }
}
