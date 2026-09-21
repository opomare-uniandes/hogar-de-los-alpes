package com.hda.trabajosaga.model.sagatrabajo;

/** Los 5 pasos posibles de la saga "Asignacion de trabajo con proveedor" (seccion 2.2 del plan).
 * Iniciar la saga (INICIADA) no es un paso propio - es la fila de saga_trabajo misma. */
public enum PasoSagaTipo {
    RESERVAR_PROVEEDOR, ASIGNAR_TRABAJO, NOTIFICAR, COMPENSAR_CANCELAR_TRABAJO, NOTIFICAR_FALLO
}
