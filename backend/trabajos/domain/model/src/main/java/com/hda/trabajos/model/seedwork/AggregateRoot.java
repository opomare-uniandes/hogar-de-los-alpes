package com.hda.trabajos.model.seedwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot<ID> extends Entity<ID> {

    private final List<DomainEvent> eventosDeDominio = new ArrayList<>();

    protected AggregateRoot(ID id) {
        super(id);
    }

    protected void registrarEvento(DomainEvent evento) {
        this.eventosDeDominio.add(evento);
    }

    public List<DomainEvent> eventosDeDominio() {
        return Collections.unmodifiableList(eventosDeDominio);
    }

    public void limpiarEventos() {
        this.eventosDeDominio.clear();
    }
}
