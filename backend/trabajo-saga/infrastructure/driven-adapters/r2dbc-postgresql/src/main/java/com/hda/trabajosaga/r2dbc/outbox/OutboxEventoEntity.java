package com.hda.trabajosaga.r2dbc.outbox;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/** Fila de outbox_evento: un DomainEvent serializado, pendiente de publicar a Pulsar.
 * Siempre se inserta nueva (isNew() = true, igual que SagaPasoEntity); OutboxRelay la marca
 * publicada con un UPDATE directo (OutboxEventoR2dbcRepository.marcarPublicado), nunca con
 * save(). */
@Table("outbox_evento")
public class OutboxEventoEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private String tipoEvento;
    private String payload;
    private Instant ocurridoEn;
    private Instant publicadoEn;

    public OutboxEventoEntity() {
    }

    public OutboxEventoEntity(UUID id, String tipoEvento, String payload, Instant ocurridoEn) {
        this.id = id;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
        this.ocurridoEn = ocurridoEn;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public boolean isNew() {
        return true;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getOcurridoEn() {
        return ocurridoEn;
    }

    public void setOcurridoEn(Instant ocurridoEn) {
        this.ocurridoEn = ocurridoEn;
    }

    public Instant getPublicadoEn() {
        return publicadoEn;
    }

    public void setPublicadoEn(Instant publicadoEn) {
        this.publicadoEn = publicadoEn;
    }
}
