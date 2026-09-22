package com.hda.proveedor.usecase.reservarproveedor;

import com.hda.proveedor.model.proveedor.Proveedor;
import com.hda.proveedor.model.proveedor.ProveedorNoDisponibleDomainEvent;
import com.hda.proveedor.model.proveedor.ProveedorReservadoDomainEvent;
import com.hda.proveedor.model.proveedor.gateways.ProveedorRepository;
import com.hda.proveedor.model.seedwork.DomainEvent;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservarProveedorUseCaseTest {

    private final UUID sagaId = UUID.randomUUID();
    private final UUID trabajoId = UUID.randomUUID();
    private final ReservarProveedorCommand comando =
            new ReservarProveedorCommand(sagaId, trabajoId, "plomeria", "Bogota");

    @Test
    void cuandoHayProveedorDisponibleEncolaExactamenteUnEventoReservado() {
        FakeRepository repo = new FakeRepository(true);
        ReservarProveedorUseCase useCase = new ReservarProveedorUseCase(repo);

        useCase.ejecutar(comando).block();

        assertEquals(1, repo.encolados.size(),
                "una reserva exitosa debe encolar exactamente un evento, no varios");
        assertInstanceOf(ProveedorReservadoDomainEvent.class, repo.encolados.get(0));
    }

    @Test
    void cuandoNoHayProveedorEncolaExactamenteUnEventoNoDisponible() {
        FakeRepository repo = new FakeRepository(false);
        ReservarProveedorUseCase useCase = new ReservarProveedorUseCase(repo);

        useCase.ejecutar(comando).block();

        assertEquals(1, repo.encolados.size(),
                "una reserva fallida debe encolar exactamente un evento");
        assertInstanceOf(ProveedorNoDisponibleDomainEvent.class, repo.encolados.get(0));
    }

    @Test
    void reservaRepetidaDelMismoSagaIdNoVuelveAReservarNiDuplicaElEvento() {
        FakeRepository repo = new FakeRepository(true);
        ReservarProveedorUseCase useCase = new ReservarProveedorUseCase(repo);

        useCase.ejecutar(comando).block();
        int reservasSqlPrimeraVez = repo.reservasAtomicas;
        useCase.ejecutar(comando).block(); // redelivery del mismo comando

        assertEquals(reservasSqlPrimeraVez, repo.reservasAtomicas,
                "una redelivery no debe volver a ejecutar el UPDATE atomico de reserva");
        assertTrue(repo.encolados.stream().allMatch(e -> e instanceof ProveedorReservadoDomainEvent));
        assertEquals(2, repo.encolados.size(),
                "cada ejecucion reemite el resultado (reservado), pero nunca un NoDisponible espurio");
    }

    /**
     * Doble que emula el comportamiento real del adaptador R2DBC:
     * - reservarSiDisponible: UPDATE atomico; devuelve el proveedor solo la PRIMERA vez que
     *   gana la carrera (si hay disponible), vacio despues.
     * - buscarPorSagaIdReserva: encuentra el proveedor una vez ya reservado por este sagaId.
     * - encolarEventoPendiente: registra el evento (equivale a insertar en outbox_evento).
     */
    private static final class FakeRepository implements ProveedorRepository {

        private final boolean hayDisponible;
        private final UUID proveedorId = UUID.randomUUID();
        private UUID reservadoPor;
        int reservasAtomicas;
        final List<DomainEvent> encolados = new ArrayList<>();

        FakeRepository(boolean hayDisponible) {
            this.hayDisponible = hayDisponible;
        }

        @Override
        public Mono<Proveedor> reservarSiDisponible(String categoriaServicio, String ciudad, UUID sagaId) {
            return Mono.defer(() -> {
                if (hayDisponible && reservadoPor == null) {
                    reservadoPor = sagaId;
                    reservasAtomicas++;
                    return Mono.just(proveedorReservado());
                }
                return Mono.empty();
            });
        }

        @Override
        public Mono<Proveedor> buscarPorSagaIdReserva(UUID sagaId) {
            return Mono.defer(() -> sagaId.equals(reservadoPor)
                    ? Mono.just(proveedorReservado())
                    : Mono.empty());
        }

        @Override
        public Mono<Void> encolarEventoPendiente(DomainEvent evento) {
            return Mono.fromRunnable(() -> encolados.add(evento));
        }

        private Proveedor proveedorReservado() {
            return Proveedor.reconstruir(proveedorId, "Proveedor X", "plomeria", "Bogota", false, reservadoPor);
        }

        @Override
        public Mono<Proveedor> guardar(Proveedor proveedor) {
            return Mono.just(proveedor);
        }

        @Override
        public Mono<Proveedor> buscarPorId(UUID id) {
            return Mono.empty();
        }
    }
}
