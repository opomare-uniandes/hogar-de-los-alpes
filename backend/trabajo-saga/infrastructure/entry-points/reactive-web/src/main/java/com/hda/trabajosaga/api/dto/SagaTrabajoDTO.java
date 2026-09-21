package com.hda.trabajosaga.api.dto;

import com.hda.trabajosaga.model.sagatrabajo.PasoSaga;
import com.hda.trabajosaga.model.sagatrabajo.SagaTrabajo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO de lectura de GET /sagas/trabajos/{trabajoId} y GET /sagas/{sagaId} (seccion 4.3 del plan). */
public record SagaTrabajoDTO(
        UUID sagaId,
        UUID trabajoId,
        String estado,
        Instant fechaInicio,
        Instant fechaFin,
        List<PasoSagaDTO> pasos
) {
    public static SagaTrabajoDTO desde(SagaTrabajo saga) {
        return new SagaTrabajoDTO(
                saga.getId(),
                saga.getTrabajoId(),
                saga.getEstado().name(),
                saga.getFechaInicio(),
                saga.getFechaFin(),
                saga.getPasos().stream().map(PasoSagaDTO::desde).toList()
        );
    }

    public record PasoSagaDTO(String paso, String resultado, String detalle, Instant ocurridoEn) {
        public static PasoSagaDTO desde(PasoSaga paso) {
            return new PasoSagaDTO(paso.tipo().name(), paso.resultado().name(), paso.detalle(), paso.ocurridoEn());
        }
    }
}
