package com.hda.trabajosaga.model.seedwork;

import java.time.Instant;
import java.util.UUID;

/** Todos los eventos de dominio del agregado SagaTrabajo implementan este contrato minimo. */
public interface DomainEvent {
    UUID id();

    Instant ocurridoEn();
}
