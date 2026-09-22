package com.hda.integracion.demo;

import com.hda.eventos.partner.v1.SolicitudTrabajoPartnerV1;
import com.hda.eventos.partner.v2.InsuredReferenceV2;
import com.hda.eventos.partner.v2.PartnerReferenceV2;
import com.hda.eventos.partner.v2.PaymentReferenceV2;
import com.hda.eventos.partner.v2.RequestReferenceV2;
import com.hda.eventos.partner.v2.RequestedServiceV2;
import com.hda.eventos.partner.v2.ServiceLocationV2;
import com.hda.eventos.partner.v2.SolicitudTrabajoPartnerV2;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publicador opcional para demostrar el contrato del partner que ESTA instancia de
 * integracion-service atiende (hda.pulsar.partner.* -- ver SolicitudTrabajoPartnerListener).
 *
 * No participa en el flujo productivo: solo se activa con HDA_DEMO_PARTNER_ENABLED=true. Publica
 * hda.demo.partner.count solicitudes (default 1) con la version de contrato y hacia el topico que
 * esta instancia consume, simulando trafico entrante de SU partner. Con count alto es el generador
 * de carga del escenario 4: publica un lote (carga base o pico 4x) en el topico de un partner
 * especifico y se observa que solo su Deployment/ScaledObject reacciona, sin tocar al otro partner
 * ni al flujo de salida.
 */
@Component
@ConditionalOnProperty(prefix = "hda.demo.partner", name = "enabled", havingValue = "true")
public class PartnerContractDemoPublisher implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerContractDemoPublisher.class);
    private static final String CLIENTE_ID = "22222222-2222-2222-2222-222222222222";

    private final PulsarClient pulsarClient;
    private final String versionContrato;
    private final String topico;
    private final String partnerId;
    private final int count;
    private final int ratePerSecond;

    public PartnerContractDemoPublisher(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.partner.contract-version}") String versionContrato,
            @Value("${hda.pulsar.partner.topic}") String topico,
            @Value("${hda.demo.partner.partner-id}") String partnerId,
            @Value("${hda.demo.partner.count:1}") int count,
            @Value("${hda.demo.partner.rate-per-second:0}") int ratePerSecond) {
        this.pulsarClient = pulsarClient;
        this.versionContrato = versionContrato;
        this.topico = topico;
        this.partnerId = partnerId;
        this.count = count;
        this.ratePerSecond = ratePerSecond;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Instant inicio = Instant.now();
        if ("v2".equalsIgnoreCase(versionContrato)) {
            publicarLote(Schema.AVRO(SolicitudTrabajoPartnerV2.class), () -> solicitudV2(Instant.now()));
        } else {
            publicarLote(Schema.AVRO(SolicitudTrabajoPartnerV1.class), () -> solicitudV1(Instant.now()));
        }
        Duration transcurrido = Duration.between(inicio, Instant.now());
        LOGGER.info("Carga de partner publicada: {} solicitudes de contrato {} a {} (partnerId={}) en {} ms.",
                count, versionContrato, topico, partnerId, transcurrido.toMillis());
    }

    /**
     * Publica el lote reutilizando un unico Producer y sendAsync para lograr throughput real; si
     * rate-per-second es mayor que 0, espacia los envios para sostener aproximadamente esa tasa,
     * de modo que un lote grande genere backlog gradual en la suscripcion del partner en vez de
     * un unico burst instantaneo. Espera a que todos los envios se confirmen antes de terminar.
     */
    private <T> void publicarLote(Schema<T> schema, java.util.function.Supplier<T> fabrica) throws Exception {
        try (Producer<T> producer = pulsarClient.newProducer(schema).topic(topico).create()) {
            List<CompletableFuture<?>> enviados = new ArrayList<>(count);
            long intervaloNanos = ratePerSecond > 0 ? 1_000_000_000L / ratePerSecond : 0;
            for (int i = 0; i < count; i++) {
                TypedMessageBuilder<T> mensaje = producer.newMessage().value(fabrica.get());
                enviados.add(mensaje.sendAsync());
                if (intervaloNanos > 0 && i < count - 1) {
                    esperar(intervaloNanos);
                }
            }
            CompletableFuture.allOf(enviados.toArray(CompletableFuture[]::new)).join();
        }
    }

    private void esperar(long nanos) {
        try {
            Thread.sleep(Duration.ofNanos(nanos));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SolicitudTrabajoPartnerV1 solicitudV1(Instant requestedAt) {
        return SolicitudTrabajoPartnerV1.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setCorrelationId(UUID.randomUUID().toString())
                .setPartnerCode(partnerId)
                .setRequestNumber("demo-v1-" + UUID.randomUUID())
                .setInsuredCustomerId(CLIENTE_ID)
                .setAssistanceCode("PLUMBING")
                .setPriorityCode("P1")
                .setCityCode("BOG")
                .setCurrencyCode("COP")
                .setRequestedAt(requestedAt)
                .build();
    }

    private SolicitudTrabajoPartnerV2 solicitudV2(Instant requestedAt) {
        return SolicitudTrabajoPartnerV2.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setCorrelationId(UUID.randomUUID().toString())
                .setPartner(PartnerReferenceV2.newBuilder().setCode(partnerId).build())
                .setRequest(RequestReferenceV2.newBuilder()
                        .setNumber("demo-v2-" + UUID.randomUUID())
                        .setRequestedAt(requestedAt)
                        .build())
                .setInsured(InsuredReferenceV2.newBuilder().setCustomerId(CLIENTE_ID).build())
                .setService(RequestedServiceV2.newBuilder().setCode("ELECTRICAL").setPriority("P2").build())
                .setLocation(ServiceLocationV2.newBuilder().setCityCode("MDE").setCountryCode("CO").build())
                .setPayment(PaymentReferenceV2.newBuilder().setCurrencyCode("COP").build())
                .build();
    }
}
