package com.hda.trabajosaga.model.sagatrabajo;

import com.hda.trabajosaga.model.seedwork.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Se traduce a NotificarFalloCommandV1 hacia notificaciones-service (camino de compensacion, ultimo paso). */
public record NotificarFalloSolicitadoEvent(
        UUID id,
        UUID sagaId,
        UUID trabajoId,
        UUID clienteId,
        String mensaje,
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
