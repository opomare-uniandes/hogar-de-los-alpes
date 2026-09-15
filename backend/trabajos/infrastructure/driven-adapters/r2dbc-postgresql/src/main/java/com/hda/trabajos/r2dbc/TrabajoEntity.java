package com.hda.trabajos.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("trabajo")
public class TrabajoEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID clienteId;
    private String categoriaServicio;
    private String ciudad;
    private String urgencia;
    private String origen;
    private UUID partnerId;
    private String moneda;
    private String estado;
    private Instant fechaCreacion;

    public TrabajoEntity() {
    }

    public TrabajoEntity(UUID id, UUID clienteId, String categoriaServicio, String ciudad,
                          String urgencia, String origen, UUID partnerId, String moneda,
                          String estado, Instant fechaCreacion) {
        this.id = id;
        this.clienteId = clienteId;
        this.categoriaServicio = categoriaServicio;
        this.ciudad = ciudad;
        this.urgencia = urgencia;
        this.origen = origen;
        this.partnerId = partnerId;
        this.moneda = moneda;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Override
    public boolean isNew() { return true; }
    public UUID getClienteId() { return clienteId; }
    public void setClienteId(UUID clienteId) { this.clienteId = clienteId; }
    public String getCategoriaServicio() { return categoriaServicio; }
    public void setCategoriaServicio(String categoriaServicio) { this.categoriaServicio = categoriaServicio; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getUrgencia() { return urgencia; }
    public void setUrgencia(String urgencia) { this.urgencia = urgencia; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
