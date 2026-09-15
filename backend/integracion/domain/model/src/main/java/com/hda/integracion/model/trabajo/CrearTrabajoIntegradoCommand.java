package com.hda.integracion.model.trabajo;

import java.time.Instant;

/**
 * Comando canónico de salida de Integración. Es independiente de V1 y V2 y se
 * serializa mediante Avro solo al cruzar el límite hacia trabajos-service.
 */
public record CrearTrabajoIntegradoCommand(
        String id,
        String correlationId,
        String partnerId,
        String externalRequestId,
        String clienteId,
        String categoriaServicio,
        String urgencia,
        String ciudad,
        String pais,
        String moneda,
        Instant fechaSolicitud
) {
}
