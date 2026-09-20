package com.hda.proveedor.usecase.liberarproveedor;

import java.util.UUID;

/** Comando (CQS): unica entrada valida para liberar un Proveedor. DTO inmutable, sin comportamiento. */
public record LiberarProveedorCommand(
        UUID sagaId,
        UUID proveedorId
) {
}
