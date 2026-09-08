package com.hda.integracion.model.trabajocreado;

import java.time.Instant;

public record TrabajoCreadoEvento(
        String trabajoId,
        String partnerId,
        String categoriaServicio,
        String ciudad,
        String origenTrabajo,
        String moneda,
        Instant fechaCreacion
) {
}
