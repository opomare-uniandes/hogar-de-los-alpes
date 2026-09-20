package com.hda.proveedor.model.proveedor.gateways;

import com.hda.proveedor.model.proveedor.Proveedor;
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
}
