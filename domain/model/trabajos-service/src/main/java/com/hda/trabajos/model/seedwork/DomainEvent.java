package com.hda.trabajos.model.seedwork;

import java.time.Instant;

/** Todo evento de dominio del agregado Trabajo implementa este contrato minimo. */
public interface DomainEvent {
    Instant ocurridoEn();
}
