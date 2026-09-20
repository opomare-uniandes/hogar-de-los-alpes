package com.hda.trabajosaga.model.sagatrabajo;

import com.hda.trabajosaga.model.seedwork.AggregateRoot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Orquestador de la saga "Asignacion de trabajo con proveedor" (ver seccion 2 del plan).
 * El id de este agregado ES el sagaId. Cada metodo de transicion es idempotente por
 * construccion: se guarda contra yaTienePaso(tipo) antes de mutar o registrar evento,
 * de modo que el propio Saga Log (los pasos ya persistidos) es el store de idempotencia
 * (seccion 3 del plan) sin necesidad de infraestructura adicional.
 */
public class SagaTrabajo extends AggregateRoot<UUID> {

    private final UUID trabajoId;
    private final UUID clienteId;
    private EstadoSaga estado;
    private final Instant fechaInicio;
    private Instant fechaFin;
    private final List<PasoSaga> pasos;
    private final List<PasoSaga> pasosNuevos = new ArrayList<>();

    private SagaTrabajo(UUID id, UUID trabajoId, UUID clienteId, EstadoSaga estado,
                         Instant fechaInicio, Instant fechaFin, List<PasoSaga> pasos) {
        super(id);
        this.trabajoId = trabajoId;
        this.clienteId = clienteId;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.pasos = new ArrayList<>(pasos);
    }

    /** Arranca la saga en respuesta a TrabajoCreado. Unico punto donde se genera un sagaId nuevo. */
    public static SagaTrabajo iniciar(UUID trabajoId, UUID clienteId, String categoriaServicio, String ciudad) {
        UUID sagaId = UUID.randomUUID();
        Instant ahora = Instant.now();
        SagaTrabajo saga = new SagaTrabajo(sagaId, trabajoId, clienteId, EstadoSaga.INICIADA, ahora, null, List.of());
        saga.registrarEvento(new ReservarProveedorSolicitadoEvent(
                UUID.randomUUID(), sagaId, trabajoId, categoriaServicio, ciudad, ahora));
        return saga;
    }

    /** Reconstruccion desde persistencia (no dispara eventos de dominio). pasos debe venir
     * ordenado por ocurridoEn (ver SagaTrabajoRepositoryAdapter). */
    public static SagaTrabajo reconstruir(UUID id, UUID trabajoId, UUID clienteId, EstadoSaga estado,
                                           Instant fechaInicio, Instant fechaFin, List<PasoSaga> pasos) {
        return new SagaTrabajo(id, trabajoId, clienteId, estado, fechaInicio, fechaFin, pasos);
    }

    private boolean yaTienePaso(PasoSagaTipo tipo) {
        return pasos.stream().anyMatch(p -> p.tipo() == tipo);
    }

    private void agregarPaso(PasoSagaTipo tipo, ResultadoPaso resultado, String detalle) {
        PasoSaga paso = new PasoSaga(UUID.randomUUID(), tipo, resultado, detalle, Instant.now());
        pasos.add(paso);
        pasosNuevos.add(paso);
    }

    /** RESERVAR_PROVEEDOR=OK -> pide ASIGNAR_TRABAJO. */
    public void registrarProveedorReservado(UUID proveedorId, String nombreProveedor) {
        if (yaTienePaso(PasoSagaTipo.RESERVAR_PROVEEDOR)) return;
        agregarPaso(PasoSagaTipo.RESERVAR_PROVEEDOR, ResultadoPaso.OK,
                "proveedorId=" + proveedorId + ", nombreProveedor=" + nombreProveedor);
        this.estado = EstadoSaga.PROVEEDOR_RESERVADO;
        registrarEvento(new AsignarTrabajoSolicitadoEvent(
                UUID.randomUUID(), getId(), trabajoId, proveedorId, Instant.now()));
    }

    /** RESERVAR_PROVEEDOR=FALLO -> arranca la compensacion pidiendo CANCELAR_TRABAJO. */
    public void registrarProveedorNoDisponible(String motivo) {
        if (yaTienePaso(PasoSagaTipo.RESERVAR_PROVEEDOR)) return;
        agregarPaso(PasoSagaTipo.RESERVAR_PROVEEDOR, ResultadoPaso.FALLO, motivo);
        this.estado = EstadoSaga.COMPENSANDO;
        registrarEvento(new CancelarTrabajoSolicitadoEvent(
                UUID.randomUUID(), getId(), trabajoId, motivo, Instant.now()));
    }

    /** ASIGNAR_TRABAJO=OK (camino feliz) -> pide NOTIFICAR (asignacion). */
    public void registrarTrabajoAsignado() {
        if (yaTienePaso(PasoSagaTipo.ASIGNAR_TRABAJO)) return;
        agregarPaso(PasoSagaTipo.ASIGNAR_TRABAJO, ResultadoPaso.OK, "estado=ASIGNADO");
        this.estado = EstadoSaga.ASIGNADA;
        registrarEvento(new NotificarAsignacionSolicitadoEvent(
                UUID.randomUUID(), getId(), trabajoId, clienteId,
                "Tu trabajo fue asignado a un proveedor.", Instant.now()));
    }

    /** COMPENSAR_CANCELAR_TRABAJO=OK (compensacion) -> pide NOTIFICAR_FALLO. El estado sigue
     * COMPENSANDO (seccion 4.2 del plan): todavia falta notificar antes de llegar a CANCELADA. */
    public void registrarTrabajoCancelado() {
        if (yaTienePaso(PasoSagaTipo.COMPENSAR_CANCELAR_TRABAJO)) return;
        agregarPaso(PasoSagaTipo.COMPENSAR_CANCELAR_TRABAJO, ResultadoPaso.OK, "estado=CANCELADO");
        registrarEvento(new NotificarFalloSolicitadoEvent(
                UUID.randomUUID(), getId(), trabajoId, clienteId,
                "No encontramos un proveedor disponible para tu trabajo.", Instant.now()));
    }

    /** NOTIFICAR=OK -> cierra el camino feliz. Sin comando saliente: es el ultimo paso. */
    public void completar() {
        if (yaTienePaso(PasoSagaTipo.NOTIFICAR)) return;
        agregarPaso(PasoSagaTipo.NOTIFICAR, ResultadoPaso.OK, "notificacion de asignacion enviada");
        this.estado = EstadoSaga.COMPLETADA;
        this.fechaFin = Instant.now();
    }

    /** NOTIFICAR_FALLO=OK -> cierra el camino de compensacion. Sin comando saliente. */
    public void cancelar() {
        if (yaTienePaso(PasoSagaTipo.NOTIFICAR_FALLO)) return;
        agregarPaso(PasoSagaTipo.NOTIFICAR_FALLO, ResultadoPaso.OK, "notificacion de fallo enviada");
        this.estado = EstadoSaga.CANCELADA;
        this.fechaFin = Instant.now();
    }

    public UUID getTrabajoId() { return trabajoId; }
    public UUID getClienteId() { return clienteId; }
    public EstadoSaga getEstado() { return estado; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public List<PasoSaga> getPasos() { return Collections.unmodifiableList(pasos); }

    /** Pasos agregados en ESTA invocacion (los que SagaTrabajoRepositoryAdapter debe insertar).
     * A diferencia de eventosDeDominio(), no se limpia explicitamente: cada caso de uso carga
     * una instancia fresca por request (ver OrquestarSagaTrabajoUseCase), no hay reuso. */
    public List<PasoSaga> pasosNuevos() { return Collections.unmodifiableList(pasosNuevos); }
}
