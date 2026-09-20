package com.hda.trabajosaga.usecase.orquestarsaga;

import java.util.UUID;

public record ProveedorReservadoComando(UUID sagaId, UUID proveedorId, String nombreProveedor) {
}
