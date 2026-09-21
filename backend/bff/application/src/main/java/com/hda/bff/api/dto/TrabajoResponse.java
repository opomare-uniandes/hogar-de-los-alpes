package com.hda.bff.api.dto;

import java.time.Instant;
import java.util.UUID;

public record TrabajoResponse(
        UUID id,
        UUID clienteId,
        String categoriaServicio,
        String urgencia,
        String ciudad,
        String origen,
        UUID partnerId,
        String moneda,
        String estado,
        Instant fechaCreacion
) {
}
