package com.hda.integracion.model.partner;

import java.time.Instant;

/**
 * Representacion interna de una solicitud de partner. Conserva el lenguaje externo
 * para que la ACL sea el unico sitio que conoce sus codigos y versiones.
 */
public record SolicitudTrabajoPartner(
        String id,
        String correlationId,
        String versionContrato,
        String partnerId,
        String externalRequestId,
        String clienteId,
        String codigoAsistencia,
        String codigoPrioridad,
        String codigoCiudad,
        String pais,
        String moneda,
        Instant fechaSolicitud
) {
    public String claveIdempotencia() {
        return partnerId + ":" + externalRequestId;
    }
}
