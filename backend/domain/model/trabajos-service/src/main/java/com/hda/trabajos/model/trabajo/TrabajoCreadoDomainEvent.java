package com.hda.trabajos.model.trabajo;

import com.hda.trabajos.model.seedwork.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record TrabajoCreadoDomainEvent(
        UUID id,
        UUID trabajoId,
        UUID clienteId,
        String categoriaServicio,
        Urgencia urgencia,
        String ciudad,
        OrigenTrabajo origen,
        UUID partnerId,
        String moneda,
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
