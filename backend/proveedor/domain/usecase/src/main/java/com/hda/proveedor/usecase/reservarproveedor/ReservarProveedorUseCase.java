package com.hda.proveedor.usecase.reservarproveedor;

import com.hda.proveedor.model.proveedor.Proveedor;
import com.hda.proveedor.model.proveedor.ProveedorNoDisponibleDomainEvent;
import com.hda.proveedor.model.proveedor.ProveedorReservadoDomainEvent;
import com.hda.proveedor.model.proveedor.gateways.ProveedorRepository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class ReservarProveedorUseCase {

    private final ProveedorRepository proveedorRepository;

    public ReservarProveedorUseCase(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public Mono<Void> ejecutar(ReservarProveedorCommand comando) {
        return resolverProveedor(comando)
                .flatMap(proveedor -> publicarReservado(comando, proveedor).thenReturn(proveedor))
                .switchIfEmpty(Mono.defer(() -> publicarNoDisponible(comando).then(Mono.empty())))
                .then();
    }

    /**
     * Resuelve QUE proveedor quedo reservado para este comando (o vacio si ninguno), sin publicar
     * todavia. Separar la decision de la publicacion es intencional: publicarReservado y
     * publicarNoDisponible devuelven un Mono&lt;Void&gt; que completa vacio, asi que encadenar
     * switchIfEmpty directamente sobre ellos hacia que cada paso ya publicado pareciera vacio y
     * volviera a disparar la rama de respaldo. Una sola ejecucion del comando terminaba encolando
     * varios eventos, incluido un ProveedorNoDisponible espurio que contradecia una reserva real
     * (verificado: una reserva exitosa encolaba 4 eventos en vez de 1). Con la decision separada,
     * el switchIfEmpty de ejecutar() solo se activa cuando de verdad no hubo proveedor.
     *
     * El orden refleja la desambiguacion de idempotencia del paso 9:
     * 1. buscarPorSagaIdReserva reutiliza la reserva si este sagaId ya la hizo (redelivery).
     * 2. reservarSiDisponible intenta el UPDATE atomico (WHERE disponible = TRUE ... RETURNING).
     * 3. si aun asi no hay fila, un unico reintento corto acota la ventana en la que otra
     *    ejecucion concurrente del mismo comando pudo ganar la carrera un instante antes sin que
     *    su escritura fuera visible todavia; publicar NO-DISPONIBLE por error es mas grave que la
     *    comprobacion extra, porque contradice una reserva real y trabajo-saga-service podria
     *    quedarse con esa version incorrecta de los hechos.
     */
    private Mono<Proveedor> resolverProveedor(ReservarProveedorCommand comando) {
        return proveedorRepository.buscarPorSagaIdReserva(comando.sagaId())
                .switchIfEmpty(Mono.defer(() -> proveedorRepository.reservarSiDisponible(
                        comando.categoriaServicio(), comando.ciudad(), comando.sagaId())))
                .switchIfEmpty(Mono.defer(() -> Mono.delay(Duration.ofMillis(300))
                        .then(proveedorRepository.buscarPorSagaIdReserva(comando.sagaId()))));
    }

    /**
     * Encola el evento en outbox_evento (no eventPublisher directo): se persiste antes de intentar
     * Pulsar. No hay un guardar() de agregado con el que compartir transaccion aqui, porque la
     * reserva atomica es reservarSiDisponible, un UPDATE aparte, pero el INSERT en outbox_evento es
     * en si mismo una escritura atomica y ya no se pierde el evento si el proceso cae entre
     * construirlo y publicarlo.
     */
    private Mono<Void> publicarReservado(ReservarProveedorCommand comando, Proveedor proveedor) {
        return proveedorRepository.encolarEventoPendiente(new ProveedorReservadoDomainEvent(
                UUID.randomUUID(), comando.sagaId(), comando.trabajoId(),
                proveedor.getId(), proveedor.getNombre(), Instant.now()));
    }

    private Mono<Void> publicarNoDisponible(ReservarProveedorCommand comando) {
        return proveedorRepository.encolarEventoPendiente(new ProveedorNoDisponibleDomainEvent(
                UUID.randomUUID(), comando.sagaId(), comando.trabajoId(),
                "No hay proveedor disponible para categoriaServicio=" + comando.categoriaServicio()
                        + ", ciudad=" + comando.ciudad(),
                Instant.now()));
    }
}
