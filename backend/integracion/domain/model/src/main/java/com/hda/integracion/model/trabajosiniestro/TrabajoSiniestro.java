package com.hda.integracion.model.trabajosiniestro;

import java.time.Instant;

public record TrabajoSiniestro(
        String trabajoId,
        String partnerId,
        String categoria,
        String ciudad,
        boolean esSiniestro,
        String moneda,
        Instant fechaEvento
) {
}
