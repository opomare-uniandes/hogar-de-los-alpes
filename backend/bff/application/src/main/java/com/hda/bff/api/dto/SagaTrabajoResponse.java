package com.hda.bff.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SagaTrabajoResponse(
        UUID sagaId,
        UUID trabajoId,
        String estado,
        Instant fechaInicio,
        Instant fechaFin,
        List<PasoSagaResponse> pasos
) {
    public record PasoSagaResponse(String paso, String resultado, String detalle, Instant ocurridoEn) {
    }
}
