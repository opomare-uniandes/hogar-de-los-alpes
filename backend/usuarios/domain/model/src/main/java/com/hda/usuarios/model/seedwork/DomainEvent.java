package com.hda.usuarios.model.seedwork;

import java.time.Instant;
import java.util.UUID;

/** Todos los eventos de dominio del agregado Usuario implementan este contrato minimo. */
public interface DomainEvent {
    UUID id();

    Instant ocurridoEn();
}
