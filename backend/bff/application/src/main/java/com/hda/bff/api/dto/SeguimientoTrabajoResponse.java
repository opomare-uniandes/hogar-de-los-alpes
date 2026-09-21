package com.hda.bff.api.dto;

public record SeguimientoTrabajoResponse(
        TrabajoResponse trabajo,
        SagaTrabajoResponse saga,
        String estadoSeguimiento
) {
    public static SeguimientoTrabajoResponse conSaga(TrabajoResponse trabajo, SagaTrabajoResponse saga) {
        return new SeguimientoTrabajoResponse(trabajo, saga, saga.estado());
    }

    public static SeguimientoTrabajoResponse sagaPendiente(TrabajoResponse trabajo) {
        return new SeguimientoTrabajoResponse(trabajo, null, "SAGA_PENDIENTE");
    }
}
