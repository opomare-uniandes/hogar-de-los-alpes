package com.hda.trabajosaga.model.sagatrabajo;

import com.hda.trabajosaga.model.seedwork.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Se traduce a ReservarProveedorCommandV1 hacia proveedor-service (ver PulsarEventPublisherAdapter). */
public record ReservarProveedorSolicitadoEvent(
        UUID id,
        UUID sagaId,
        UUID trabajoId,
        String categoriaServicio,
        String ciudad,
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
