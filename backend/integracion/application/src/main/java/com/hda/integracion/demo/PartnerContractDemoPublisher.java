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
 * Publicador opcional para demostrar la convivencia de los contratos de partner V1 y V2.
 *
 * No participa en el flujo productivo: solo se activa con HDA_DEMO_PARTNER_ENABLED=true.
 * Publica dos solicitudes semanticamente validas pero con estructuras externas distintas;
 * ambas pasan por el ACL de integracion-service y terminan como comandos canonicos.
 */
@Component
@ConditionalOnProperty(prefix = "hda.demo.partner", name = "enabled", havingValue = "true")
public class PartnerContractDemoPublisher implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerContractDemoPublisher.class);
    private static final String PARTNER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String CLIENTE_ID = "22222222-2222-2222-2222-222222222222";

    private final PulsarClient pulsarClient;
    private final String topicV1;
    private final String topicV2;

    public PartnerContractDemoPublisher(
            PulsarClient pulsarClient,
            @Value("${hda.pulsar.topic-solicitud-partner-v1}") String topicV1,
            @Value("${hda.pulsar.topic-solicitud-partner-v2}") String topicV2) {
        this.pulsarClient = pulsarClient;
        this.topicV1 = topicV1;
        this.topicV2 = topicV2;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Instant requestedAt = Instant.now();

        try (Producer<SolicitudTrabajoPartnerV1> producerV1 = pulsarClient.newProducer(Schema.AVRO(SolicitudTrabajoPartnerV1.class))
                .topic(topicV1).create();
             Producer<SolicitudTrabajoPartnerV2> producerV2 = pulsarClient.newProducer(Schema.AVRO(SolicitudTrabajoPartnerV2.class))
                     .topic(topicV2).create()) {
            producerV1.send(solicitudV1(requestedAt));
            producerV2.send(solicitudV2(requestedAt));
        }

        LOGGER.info("Demostracion de interoperabilidad publicada: contratos partner V1 y V2 enviados a Pulsar.");
    }

    private SolicitudTrabajoPartnerV1 solicitudV1(Instant requestedAt) {
        return SolicitudTrabajoPartnerV1.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setCorrelationId(UUID.randomUUID().toString())
                .setPartnerCode(PARTNER_ID)
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
                .setPartner(PartnerReferenceV2.newBuilder().setCode(PARTNER_ID).build())
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
