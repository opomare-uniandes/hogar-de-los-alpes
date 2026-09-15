package com.hda.integracion.entrypoints.pulsar.partner;

import com.hda.eventos.partner.v1.SolicitudTrabajoPartnerV1;
import com.hda.eventos.partner.v2.SolicitudTrabajoPartnerV2;
import com.hda.integracion.model.partner.SolicitudTrabajoPartner;

/** Adaptador de entrada: conoce Avro V1/V2, pero el caso de uso no. */
final class SolicitudTrabajoPartnerMapper {

    private SolicitudTrabajoPartnerMapper() {
    }

    static SolicitudTrabajoPartner desdeV1(SolicitudTrabajoPartnerV1 evento) {
        return new SolicitudTrabajoPartner(
                evento.getId(), evento.getCorrelationId(), "v1", evento.getPartnerCode(),
                evento.getRequestNumber(), evento.getInsuredCustomerId(), evento.getAssistanceCode(),
                evento.getPriorityCode(), evento.getCityCode(), "CO", evento.getCurrencyCode(), evento.getRequestedAt());
    }

    static SolicitudTrabajoPartner desdeV2(SolicitudTrabajoPartnerV2 evento) {
        return new SolicitudTrabajoPartner(
                evento.getId(), evento.getCorrelationId(), "v2", evento.getPartner().getCode(),
                evento.getRequest().getNumber(), evento.getInsured().getCustomerId(), evento.getService().getCode(),
                evento.getService().getPriority(), evento.getLocation().getCityCode(), evento.getLocation().getCountryCode(),
                evento.getPayment().getCurrencyCode(), evento.getRequest().getRequestedAt());
    }
}
