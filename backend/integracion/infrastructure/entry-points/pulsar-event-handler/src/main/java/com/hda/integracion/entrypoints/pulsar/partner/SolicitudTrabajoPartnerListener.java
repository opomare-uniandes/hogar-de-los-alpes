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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Consume en paralelo las versiones de contrato que los partners mantengan activas.
 * Cada versión tiene su tópico y suscripción; la ACL las reduce al mismo modelo interno.
 */
@Component
public class SolicitudTrabajoPartnerListener {

    private static final Logger log = LoggerFactory.getLogger(SolicitudTrabajoPartnerListener.class);

    private final PulsarClient pulsarClient;
    private final TraducirSolicitudPartnerUseCase useCase;
    private final EventDeduplicationStore deduplicationStore;
    private final SolicitudTrabajoRechazoPublisher rechazoPublisher;
    private final String topicoV1;
    private final String topicoV2;
    private final String suscripcionV1;
    private final String suscripcionV2;
    private Consumer<SolicitudTrabajoPartnerV1> consumidorV1;
    private Consumer<SolicitudTrabajoPartnerV2> consumidorV2;

    public SolicitudTrabajoPartnerListener(
            PulsarClient pulsarClient,
            TraducirSolicitudPartnerUseCase useCase,
            EventDeduplicationStore deduplicationStore,
            SolicitudTrabajoRechazoPublisher rechazoPublisher,
            @Value("${hda.pulsar.topic-solicitud-partner-v1}") String topicoV1,
            @Value("${hda.pulsar.topic-solicitud-partner-v2}") String topicoV2,
            @Value("${hda.pulsar.subscription-partner-v1}") String suscripcionV1,
            @Value("${hda.pulsar.subscription-partner-v2}") String suscripcionV2) {
        this.pulsarClient = pulsarClient;
        this.useCase = useCase;
        this.deduplicationStore = deduplicationStore;
        this.rechazoPublisher = rechazoPublisher;
        this.topicoV1 = topicoV1;
        this.topicoV2 = topicoV2;
        this.suscripcionV1 = suscripcionV1;
        this.suscripcionV2 = suscripcionV2;
    }

    @PostConstruct
    void iniciar() throws PulsarClientException {
        consumidorV1 = pulsarClient.newConsumer(Schema.AVRO(SolicitudTrabajoPartnerV1.class))
                .topic(topicoV1).subscriptionName(suscripcionV1).subscriptionType(SubscriptionType.Shared)
                .messageListener((consumer, mensaje) -> procesar(consumer, mensaje, SolicitudTrabajoPartnerMapper.desdeV1(mensaje.getValue())))
                .subscribe();
        consumidorV2 = pulsarClient.newConsumer(Schema.AVRO(SolicitudTrabajoPartnerV2.class))
                .topic(topicoV2).subscriptionName(suscripcionV2).subscriptionType(SubscriptionType.Shared)
                .messageListener((consumer, mensaje) -> procesar(consumer, mensaje, SolicitudTrabajoPartnerMapper.desdeV2(mensaje.getValue())))
                .subscribe();
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
        if (consumidorV1 != null) consumidorV1.close();
        if (consumidorV2 != null) consumidorV2.close();
    }
}
