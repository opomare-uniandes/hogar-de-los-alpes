package com.hda.trabajosaga.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/** Una fila de saga_paso. Siempre es un INSERT nuevo (isNew() = true, igual que TrabajoEntity):
 * un paso jamas se actualiza una vez escrito, y SagaTrabajo.pasosNuevos() solo entrega pasos
 * que el agregado confirmo que no existian todavia (ver yaTienePaso()). */
@Table("saga_paso")
public class SagaPasoEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID sagaId;
    private String paso;
    private String resultado;
    private String detalle;
    private Instant ocurridoEn;

    public SagaPasoEntity() {
    }

    public SagaPasoEntity(UUID id, UUID sagaId, String paso, String resultado, String detalle, Instant ocurridoEn) {
        this.id = id;
        this.sagaId = sagaId;
        this.paso = paso;
        this.resultado = resultado;
        this.detalle = detalle;
        this.ocurridoEn = ocurridoEn;
    }

    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Override
    public boolean isNew() { return true; }

    public UUID getSagaId() { return sagaId; }
    public void setSagaId(UUID sagaId) { this.sagaId = sagaId; }
    public String getPaso() { return paso; }
    public void setPaso(String paso) { this.paso = paso; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public Instant getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(Instant ocurridoEn) { this.ocurridoEn = ocurridoEn; }
}
