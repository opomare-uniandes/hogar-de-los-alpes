package com.hda.integracion.entrypoints.pulsar.partner;

import com.hda.eventos.partner.v1.SolicitudTrabajoPartnerV1;
import com.hda.eventos.partner.v2.InsuredReferenceV2;
import com.hda.eventos.partner.v2.PartnerReferenceV2;
import com.hda.eventos.partner.v2.PaymentReferenceV2;
import com.hda.eventos.partner.v2.RequestReferenceV2;
import com.hda.eventos.partner.v2.RequestedServiceV2;
import com.hda.eventos.partner.v2.ServiceLocationV2;
import com.hda.eventos.partner.v2.SolicitudTrabajoPartnerV2;
import com.hda.integracion.model.partner.SolicitudTrabajoPartner;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolicitudTrabajoPartnerMapperTest {

    private static final Instant FECHA = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void normalizaLasDosVersionesAlMismoModeloInterno() {
        SolicitudTrabajoPartner desdeV1 = SolicitudTrabajoPartnerMapper.desdeV1(v1());
        SolicitudTrabajoPartner desdeV2 = SolicitudTrabajoPartnerMapper.desdeV2(v2());

        assertEquals(desdeV1.partnerId(), desdeV2.partnerId());
        assertEquals(desdeV1.externalRequestId(), desdeV2.externalRequestId());
        assertEquals(desdeV1.clienteId(), desdeV2.clienteId());
        assertEquals(desdeV1.codigoAsistencia(), desdeV2.codigoAsistencia());
        assertEquals(desdeV1.codigoPrioridad(), desdeV2.codigoPrioridad());
        assertEquals(desdeV1.codigoCiudad(), desdeV2.codigoCiudad());
        assertEquals(desdeV1.fechaSolicitud(), desdeV2.fechaSolicitud());
        assertEquals("v1", desdeV1.versionContrato());
        assertEquals("v2", desdeV2.versionContrato());
    }

    private SolicitudTrabajoPartnerV1 v1() {
        return SolicitudTrabajoPartnerV1.newBuilder()
                .setId("evento-1").setCorrelationId("correlacion-1")
                .setPartnerCode("partner-1").setRequestNumber("request-1")
                .setInsuredCustomerId("cliente-1").setAssistanceCode("PLUMBING")
                .setPriorityCode("P1").setCityCode("BOG").setCurrencyCode("COP")
                .setRequestedAt(FECHA).build();
    }

    private SolicitudTrabajoPartnerV2 v2() {
        return SolicitudTrabajoPartnerV2.newBuilder()
                .setId("evento-1").setCorrelationId("correlacion-1")
                .setPartner(PartnerReferenceV2.newBuilder().setCode("partner-1").build())
                .setRequest(RequestReferenceV2.newBuilder().setNumber("request-1").setRequestedAt(FECHA).build())
                .setInsured(InsuredReferenceV2.newBuilder().setCustomerId("cliente-1").build())
                .setService(RequestedServiceV2.newBuilder().setCode("PLUMBING").setPriority("P1").build())
                .setLocation(ServiceLocationV2.newBuilder().setCityCode("BOG").setCountryCode("CO").build())
                .setPayment(PaymentReferenceV2.newBuilder().setCurrencyCode("COP").build())
                .build();
    }
}
