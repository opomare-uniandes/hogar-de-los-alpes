package com.hda.trabajosaga.model.sagatrabajo;

import java.time.Instant;
import java.util.UUID;

/** Entidad interna de SagaTrabajo: una fila de saga_paso. Inmutable una vez escrita
 * (nunca se actualiza, solo se inserta - ver UNIQUE (saga_id, paso) en schema.sql). */
public record PasoSaga(
        UUID id,
        PasoSagaTipo tipo,
        ResultadoPaso resultado,
        String detalle,
        Instant ocurridoEn
) {
}
