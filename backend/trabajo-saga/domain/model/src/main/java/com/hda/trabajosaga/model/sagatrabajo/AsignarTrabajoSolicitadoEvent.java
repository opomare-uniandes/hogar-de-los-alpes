package com.hda.trabajosaga.model.sagatrabajo;

import com.hda.trabajosaga.model.seedwork.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Se traduce a AsignarTrabajoCommandV1 hacia trabajos-service. */
public record AsignarTrabajoSolicitadoEvent(
        UUID id,
        UUID sagaId,
        UUID trabajoId,
        UUID proveedorId,
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
