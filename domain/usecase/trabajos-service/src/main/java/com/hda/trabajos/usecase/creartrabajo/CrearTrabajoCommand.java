package com.hda.trabajos.usecase.creartrabajo;

import com.hda.trabajos.model.trabajo.OrigenTrabajo;
import com.hda.trabajos.model.trabajo.Urgencia;

import java.util.UUID;

/** Comando (CQS): unica entrada valida para crear un Trabajo. Es un DTO inmutable, sin comportamiento. */
public record CrearTrabajoCommand(
        UUID clienteId,
        String categoriaServicio,
        Urgencia urgencia,
        String ciudad,
        OrigenTrabajo origen,
        UUID partnerId,
        String moneda
) {
}
