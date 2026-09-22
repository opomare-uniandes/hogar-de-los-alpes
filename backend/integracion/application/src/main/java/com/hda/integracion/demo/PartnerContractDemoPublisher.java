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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicador opcional para demostrar el contrato del partner que ESTA instancia de
 * integracion-service atiende (hda.pulsar.partner.* -- ver SolicitudTrabajoPartnerListener).
 *
 * No participa en el flujo productivo: solo se activa con HDA_DEMO_PARTNER_ENABLED=true. Publica
 * una unica solicitud, con la version de contrato y hacia el topico que esta instancia consume,
 * simulando trafico entrante de SU partner (util para el escenario 4: generar backlog en el
 * topico de un partner especifico y observar que solo su Deployment/ScaledObject reacciona).
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

    public PartnerContractDemoPublisher(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.partner.contract-version}") String versionContrato,
            @Value("${hda.pulsar.partner.topic}") String topico,
            @Value("${hda.demo.partner.partner-id}") String partnerId) {
        this.pulsarClient = pulsarClient;
        this.versionContrato = versionContrato;
        this.topico = topico;
        this.partnerId = partnerId;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Instant requestedAt = Instant.now();

        if ("v2".equalsIgnoreCase(versionContrato)) {
            try (Producer<SolicitudTrabajoPartnerV2> producer = pulsarClient.newProducer(Schema.AVRO(SolicitudTrabajoPartnerV2.class))
                    .topic(topico).create()) {
                producer.send(solicitudV2(requestedAt));
            }
        } else {
            try (Producer<SolicitudTrabajoPartnerV1> producer = pulsarClient.newProducer(Schema.AVRO(SolicitudTrabajoPartnerV1.class))
                    .topic(topico).create()) {
                producer.send(solicitudV1(requestedAt));
            }
        }

        LOGGER.info("Demostracion de partner publicada: contrato {} enviado a {} (partnerId={}).",
                versionContrato, topico, partnerId);
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
