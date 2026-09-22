package com.hda.proveedor.model.proveedor.gateways;

import com.hda.proveedor.model.proveedor.Proveedor;
import com.hda.proveedor.model.seedwork.DomainEvent;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProveedorRepository {

    Mono<Proveedor> guardar(Proveedor proveedor);

    Mono<Proveedor> buscarPorId(UUID id);

    /** Clave de idempotencia de la reserva (ver seccion 3 del plan): si ya existe un
     * Proveedor con este sagaIdReserva, ReservarProveedorUseCase no vuelve a tocar la fila. */
    Mono<Proveedor> buscarPorSagaIdReserva(UUID sagaId);

    /**
     * Reserva atomica (compare-and-swap a nivel de SQL - UPDATE ... WHERE disponible = true).
     * Encontrado en la verificacion de idempotencia del paso 9: "buscar disponible, mutar,
     * guardar" en tres pasos separados es una carrera clasica de lectura-decision-escritura -
     * dos ejecuciones concurrentes del MISMO comando (p.ej. una redelivery de Pulsar muy cercana
     * a la original) podian leer "disponible" ambas antes de que cualquiera escribiera, y la
     * segunda terminaba publicando ProveedorNoDisponible con el proveedor YA reservado por la
     * primera - un proveedor quedaba huerfano (reservado para siempre) porque trabajo-saga-service
     * podia quedarse con ese resultado contradictorio. Devuelve el Proveedor ya reservado si esta
     * llamada gano la carrera; vacio si no habia ninguno disponible EN ESE INSTANTE (puede ser que
     * de verdad no haya, o que otra ejecucion concurrente del mismo comando ya se lo haya llevado -
     * ver ReservarProveedorUseCase, que resuelve la ambiguedad revisando buscarPorSagaIdReserva).
     */
    Mono<Proveedor> reservarSiDisponible(String categoriaServicio, String ciudad, UUID sagaId);

    /**
     * Encola un evento en outbox_evento para que OutboxRelay lo publique a Pulsar (patron
     * Transactional Outbox). Se usa cuando el evento no coincide con una escritura de guardar()
     * sobre este agregado (ver ReservarProveedorUseCase: la reserva atomica es un UPDATE aparte
     * y ProveedorNoDisponible no cambia ningun estado) -- igual queda durable antes de intentar
     * Pulsar, en vez de publicarse directo y arriesgarse a perderse si el proceso cae a mitad.
     */
    Mono<Void> encolarEventoPendiente(DomainEvent evento);
}
