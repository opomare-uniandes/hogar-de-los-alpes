package com.hda.proveedor.model.proveedor;

import com.hda.proveedor.model.seedwork.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

/**
 * Agregado raiz Proveedor (ver Entrega 2 - Vista de Informacion; documentado desde
 * entonces y nunca implementado hasta la saga "Asignacion de trabajo con proveedor").
 * No se crea desde la aplicacion: los proveedores son datos semilla deterministas
 * (ver schema.sql).
 *
 * La reserva (paso RESERVAR_PROVEEDOR) NO se modela como un metodo mutador de este
 * agregado: es un UPDATE atomico a nivel de SQL (ver ProveedorRepository.reservarSiDisponible)
 * porque "cargar el agregado, decidir, guardar" en pasos separados es una carrera de
 * lectura-decision-escritura sobre un recurso compartido y contendido (ver seccion 3 del
 * plan e idempotencia verificada en el paso 9) - el invariante ("disponible = true") vive
 * en el WHERE de esa sentencia, no en este objeto en memoria. liberar() si sigue el patron
 * cargar-mutar-guardar porque actua sobre un proveedorId ya conocido (sin contienda por
 * "cual" reservar), donde una doble ejecucion concurrente es, a lo sumo, un evento
 * redundante - no un estado contradictorio.
 */
public class Proveedor extends AggregateRoot<UUID> {

    private final String nombre;
    private final String categoriaServicio;
    private final String ciudad;
    private boolean disponible;
    private UUID sagaIdReserva;

    private Proveedor(UUID id, String nombre, String categoriaServicio, String ciudad,
                       boolean disponible, UUID sagaIdReserva) {
        super(id);
        this.nombre = nombre;
        this.categoriaServicio = categoriaServicio;
        this.ciudad = ciudad;
        this.disponible = disponible;
        this.sagaIdReserva = sagaIdReserva;
    }

    /** Reconstruccion desde persistencia (no dispara eventos de dominio). */
    public static Proveedor reconstruir(UUID id, String nombre, String categoriaServicio, String ciudad,
                                         boolean disponible, UUID sagaIdReserva) {
        return new Proveedor(id, nombre, categoriaServicio, ciudad, disponible, sagaIdReserva);
    }

    /**
     * Compensacion del paso RESERVAR_PROVEEDOR. Idempotente: si el proveedor ya esta
     * disponible no muta nada, pero igual emite el evento resultado (ver LiberarProveedorUseCase).
     */
    public void liberar(UUID sagaId) {
        if (!disponible) {
            this.disponible = true;
            this.sagaIdReserva = null;
        }
        registrarEvento(new ProveedorLiberadoDomainEvent(UUID.randomUUID(), sagaId, getId(), Instant.now()));
    }

    public String getNombre() { return nombre; }
    public String getCategoriaServicio() { return categoriaServicio; }
    public String getCiudad() { return ciudad; }
    public boolean isDisponible() { return disponible; }
    public UUID getSagaIdReserva() { return sagaIdReserva; }
}
