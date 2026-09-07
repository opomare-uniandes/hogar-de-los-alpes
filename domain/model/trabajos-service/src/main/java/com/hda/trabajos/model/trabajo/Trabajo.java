package com.hda.trabajos.model.trabajo;

import com.hda.trabajos.model.seedwork.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

/**
 * Agregado raiz Trabajo (el "corazon del negocio", ver Entrega 2 - Vista de Informacion).
 * Esta entrega solo implementa lo minimo para satisfacer el comando CrearTrabajo y
 * dejar la infraestructura lista para los escenarios de calidad 2.1 (pico de carga) y
 * 3.3 (evento de integracion hacia Seguros de los Alpes). No incluye todavia
 * SubTrabajo/HitoFlujo/asignacion de proveedor - eso es una entrega futura.
 */
public class Trabajo extends AggregateRoot<UUID> {

    private final UUID clienteId;
    private final CategoriaServicio categoriaServicio;
    private final Urgencia urgencia;
    private final String ciudad;
    private final OrigenTrabajo origen;
    private final UUID partnerId; // nulo si el origen es MARKETPLACE
    private final Moneda moneda;
    private EstadoTrabajo estado;
    private final Instant fechaCreacion;

    private Trabajo(UUID id, UUID clienteId, CategoriaServicio categoriaServicio, Urgencia urgencia,
                     String ciudad, OrigenTrabajo origen, UUID partnerId, Moneda moneda,
                     EstadoTrabajo estado, Instant fechaCreacion) {
        super(id);
        this.clienteId = clienteId;
        this.categoriaServicio = categoriaServicio;
        this.urgencia = urgencia;
        this.ciudad = ciudad;
        this.origen = origen;
        this.partnerId = partnerId;
        this.moneda = moneda;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    /** Factory (patron Factory de DDD tactico): unico punto de creacion valido de un Trabajo. */
    public static Trabajo crear(UUID clienteId, CategoriaServicio categoriaServicio, Urgencia urgencia,
                                 String ciudad, OrigenTrabajo origen, UUID partnerId, Moneda moneda) {

        if (origen != OrigenTrabajo.MARKETPLACE && partnerId == null) {
            throw new IllegalArgumentException("Un trabajo de siniestro/suscripcion debe tener partnerId");
        }

        UUID id = UUID.randomUUID();
        Instant ahora = Instant.now();

        Trabajo trabajo = new Trabajo(id, clienteId, categoriaServicio, urgencia, ciudad, origen,
                partnerId, moneda, EstadoTrabajo.CREADO, ahora);

        trabajo.registrarEvento(new TrabajoCreadoDomainEvent(
                id, clienteId, categoriaServicio.nombre(), urgencia, ciudad, origen,
                partnerId, moneda.codigoIso4217(), ahora));

        return trabajo;
    }

    /** Reconstruccion desde persistencia (no dispara eventos de dominio). */
    public static Trabajo reconstruir(UUID id, UUID clienteId, CategoriaServicio categoriaServicio,
                                       Urgencia urgencia, String ciudad, OrigenTrabajo origen,
                                       UUID partnerId, Moneda moneda, EstadoTrabajo estado,
                                       Instant fechaCreacion) {
        return new Trabajo(id, clienteId, categoriaServicio, urgencia, ciudad, origen, partnerId,
                moneda, estado, fechaCreacion);
    }

    public UUID getClienteId() { return clienteId; }
    public CategoriaServicio getCategoriaServicio() { return categoriaServicio; }
    public Urgencia getUrgencia() { return urgencia; }
    public String getCiudad() { return ciudad; }
    public OrigenTrabajo getOrigen() { return origen; }
    public UUID getPartnerId() { return partnerId; }
    public Moneda getMoneda() { return moneda; }
    public EstadoTrabajo getEstado() { return estado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
