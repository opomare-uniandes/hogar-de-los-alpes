package com.hda.trabajosaga.model.sagatrabajo;

import com.hda.trabajosaga.model.seedwork.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Se traduce a CancelarTrabajoCommandV1 hacia trabajos-service (compensacion). */
public record CancelarTrabajoSolicitadoEvent(
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
