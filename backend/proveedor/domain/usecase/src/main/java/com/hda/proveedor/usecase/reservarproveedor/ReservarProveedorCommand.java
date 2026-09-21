package com.hda.proveedor.usecase.reservarproveedor;

import java.util.UUID;

/** Comando (CQS): unica entrada valida para reservar un Proveedor. DTO inmutable, sin comportamiento. */
public record ReservarProveedorCommand(
        UUID sagaId,
        UUID trabajoId,
        String categoriaServicio,
        String ciudad
) {
}
