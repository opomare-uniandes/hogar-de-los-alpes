package com.hda.proveedor.model.proveedor;

import com.hda.proveedor.model.seedwork.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProveedorNoDisponibleDomainEvent(
        UUID id,
        UUID sagaId,
        UUID trabajoId,
        String motivo,
        Instant ocurridoEn
) implements DomainEvent {

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Instant ocurridoEn() {
        return ocurridoEn;
    }
}
