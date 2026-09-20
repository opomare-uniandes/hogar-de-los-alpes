package com.hda.trabajosaga.model.sagatrabajo;

/** Estados de saga_trabajo (seccion 4.1 del plan). */
public enum EstadoSaga {
    INICIADA, PROVEEDOR_RESERVADO, ASIGNADA, COMPENSANDO, CANCELADA, COMPLETADA
}
