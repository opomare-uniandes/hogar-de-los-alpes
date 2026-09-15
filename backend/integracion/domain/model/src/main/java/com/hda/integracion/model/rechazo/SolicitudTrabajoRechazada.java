package com.hda.integracion.model.rechazo;

import java.time.Instant;

/** Evento de integración que comunica un rechazo semántico al partner emisor. */
public record SolicitudTrabajoRechazada(
        String id,
        String correlationId,
        String sourceEventId,
        String partnerId,
        String externalRequestId,
        String contractVersion,
        String reason,
        Instant rejectedAt
) {
}
