package com.hda.trabajos.model.seedwork;

import java.time.Instant;
import java.util.UUID;

/** Todos los eventos de dominio del agregado Trabajo implementa este contrato minimo. */
public interface DomainEvent {
    UUID id();

    Instant ocurridoEn();
}
