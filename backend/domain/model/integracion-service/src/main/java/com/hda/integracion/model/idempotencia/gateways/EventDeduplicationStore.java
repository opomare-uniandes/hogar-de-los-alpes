package com.hda.integracion.model.idempotencia.gateways;

import reactor.core.publisher.Mono;

/**
 * Puerto de idempotencia (arquitectura hexagonal): permite a un consumidor detectar
 * eventos ya procesados. Es una preocupacion de INFRAESTRUCTURA, no de dominio: se
 * modela como puerto porque las suscripciones Shared de Pulsar entregan al-menos-una-vez
 * (un mismo evento puede reentregarse tras un negativeAck o el reinicio de una instancia).
 * Se identifica cada ocurrencia por el id del evento (ver el campo `id` de TrabajoCreado).
 */
public interface EventDeduplicationStore {

    /**
     * Registra de forma atomica que el evento con este id fue visto.
     *
     * @return {@code true} si es la PRIMERA vez que se ve (el consumidor debe procesarlo);
     *         {@code false} si ya habia sido registrado antes (es un duplicado: saltar).
     */
    Mono<Boolean> registrarSiNoVisto(String eventId);
}
