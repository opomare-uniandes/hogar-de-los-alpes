package com.hda.trabajos.model.trabajo;

import com.hda.trabajos.model.seedwork.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record TrabajoAsignadoDomainEvent(
        UUID id,
        UUID trabajoId,
        UUID sagaId,
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
