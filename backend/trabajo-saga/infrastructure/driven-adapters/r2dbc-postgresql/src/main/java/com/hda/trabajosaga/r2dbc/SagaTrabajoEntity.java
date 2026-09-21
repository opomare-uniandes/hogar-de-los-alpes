package com.hda.trabajosaga.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/** Proyeccion de lectura de saga_trabajo. SagaTrabajoRepositoryAdapter escribe con un
 * upsert manual (INSERT ... ON CONFLICT), no via ReactiveCrudRepository.save(): esta fila
 * necesita tanto INSERT (al iniciar la saga) como UPDATE (en cada transicion posterior)
 * a traves del mismo guardar() - a diferencia de trabajo/proveedor, que solo hacen una
 * de las dos operaciones siempre. */
@Table("saga_trabajo")
public class SagaTrabajoEntity {

    @Id
    private UUID id;
    private UUID trabajoId;
    private UUID clienteId;
    private String estado;
    private Instant fechaInicio;
    private Instant fechaFin;

    public SagaTrabajoEntity() {
    }

    public SagaTrabajoEntity(UUID id, UUID trabajoId, UUID clienteId, String estado,
                              Instant fechaInicio, Instant fechaFin) {
        this.id = id;
        this.trabajoId = trabajoId;
        this.clienteId = clienteId;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTrabajoId() { return trabajoId; }
    public void setTrabajoId(UUID trabajoId) { this.trabajoId = trabajoId; }
    public UUID getClienteId() { return clienteId; }
    public void setClienteId(UUID clienteId) { this.clienteId = clienteId; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public void setFechaFin(Instant fechaFin) { this.fechaFin = fechaFin; }
}
