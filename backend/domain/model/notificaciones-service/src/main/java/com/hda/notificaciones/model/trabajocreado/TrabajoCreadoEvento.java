package com.hda.notificaciones.model.trabajocreado;

import java.time.Instant;

public record TrabajoCreadoEvento(
        String id,
        String trabajoId,
        String clienteId,
        String categoriaServicio,
        String ciudad,
        String urgencia,
        Instant fechaCreacion
) {
}
