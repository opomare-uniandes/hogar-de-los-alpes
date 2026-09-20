package com.hda.proveedor.usecase.reservarproveedor;

import com.hda.proveedor.model.proveedor.Proveedor;
import com.hda.proveedor.model.proveedor.ProveedorNoDisponibleDomainEvent;
import com.hda.proveedor.model.proveedor.ProveedorReservadoDomainEvent;
import com.hda.proveedor.model.proveedor.gateways.ProveedorEventPublisher;
import com.hda.proveedor.model.proveedor.gateways.ProveedorRepository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReservarProveedorUseCase {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorEventPublisher eventPublisher;

    public ReservarProveedorUseCase(ProveedorRepository proveedorRepository, ProveedorEventPublisher eventPublisher) {
        this.proveedorRepository = proveedorRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<Void> ejecutar(ReservarProveedorCommand comando) {
        return proveedorRepository.buscarPorSagaIdReserva(comando.sagaId())
                .flatMap(yaReservado -> publicarReservado(comando, yaReservado))
                .switchIfEmpty(reservarNuevo(comando));
    }

    /**
     * Intenta la reserva atomica primero (ver ProveedorRepository.reservarSiDisponible - resuelve
     * la carrera de lectura-decision-escritura entre ejecuciones concurrentes del mismo comando).
     * Si no hay fila (Mono vacio), la ambiguedad se resuelve en resolverSinReservaPropia: puede
     * ser que genuinamente no haya proveedor, o que OTRA ejecucion concurrente de este mismo
     * comando (mismo sagaId) haya ganado la carrera un instante antes - buscarPorSagaIdReserva
     * distingue los dos casos.
     */
    private Mono<Void> reservarNuevo(ReservarProveedorCommand comando) {
        return proveedorRepository.reservarSiDisponible(comando.categoriaServicio(), comando.ciudad(), comando.sagaId())
                .flatMap(proveedor -> publicarReservado(comando, proveedor))
                .switchIfEmpty(resolverSinReservaPropia(comando));
    }

    /**
     * Si la reserva atomica no encontro fila, puede ser que genuinamente no haya proveedor, o
     * que otra ejecucion concurrente de este MISMO comando la haya ganado un instante antes y
     * su escritura todavia no sea visible para esta lectura (ver hallazgo del paso 9: bajo
     * ejecuciones casi simultaneas del mismo comando se observo una ventana de milisegundos
     * donde esta lectura podia no ver todavia la reserva ya comprometida). Un solo reintento
     * corto acota esa ventana antes de concluir "no disponible" - publicar NO-DISPONIBLE por
     * error aqui es mas grave que una comprobacion extra, porque contradice una reserva real
     * y trabajo-saga-service podria quedarse con esa version incorrecta de los hechos.
     */
    private Mono<Void> resolverSinReservaPropia(ReservarProveedorCommand comando) {
        return proveedorRepository.buscarPorSagaIdReserva(comando.sagaId())
                .flatMap(proveedor -> publicarReservado(comando, proveedor))
                .switchIfEmpty(Mono.delay(Duration.ofMillis(300)).then(
                        proveedorRepository.buscarPorSagaIdReserva(comando.sagaId())
                                .flatMap(proveedor -> publicarReservado(comando, proveedor))
                                .switchIfEmpty(publicarNoDisponible(comando))));
    }

    private Mono<Void> publicarReservado(ReservarProveedorCommand comando, Proveedor proveedor) {
        return eventPublisher.publicarTodos(List.of(new ProveedorReservadoDomainEvent(
                UUID.randomUUID(), comando.sagaId(), comando.trabajoId(),
                proveedor.getId(), proveedor.getNombre(), Instant.now())));
    }

    private Mono<Void> publicarNoDisponible(ReservarProveedorCommand comando) {
        return eventPublisher.publicarTodos(List.of(new ProveedorNoDisponibleDomainEvent(
                UUID.randomUUID(), comando.sagaId(), comando.trabajoId(),
                "No hay proveedor disponible para categoriaServicio=" + comando.categoriaServicio()
                        + ", ciudad=" + comando.ciudad(),
                Instant.now())));
    }
}
