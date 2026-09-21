package com.hda.proveedor.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("proveedor")
public class ProveedorEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private String nombre;
    private String categoriaServicio;
    private String ciudad;
    private boolean disponible;
    private UUID sagaIdReserva;

    public ProveedorEntity() {
    }

    public ProveedorEntity(UUID id, String nombre, String categoriaServicio, String ciudad,
                            boolean disponible, UUID sagaIdReserva) {
        this.id = id;
        this.nombre = nombre;
        this.categoriaServicio = categoriaServicio;
        this.ciudad = ciudad;
        this.disponible = disponible;
        this.sagaIdReserva = sagaIdReserva;
    }

    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    /** Siempre false: proveedor-service nunca inserta un Proveedor desde la aplicacion
     * (son datos semilla de schema.sql) - guardar() aqui siempre es un UPDATE sobre
     * una fila que ya existe. */
    @Override
    public boolean isNew() { return false; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoriaServicio() { return categoriaServicio; }
    public void setCategoriaServicio(String categoriaServicio) { this.categoriaServicio = categoriaServicio; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public UUID getSagaIdReserva() { return sagaIdReserva; }
    public void setSagaIdReserva(UUID sagaIdReserva) { this.sagaIdReserva = sagaIdReserva; }
}
