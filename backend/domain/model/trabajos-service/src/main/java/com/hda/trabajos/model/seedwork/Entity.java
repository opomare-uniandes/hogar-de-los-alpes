package com.hda.trabajos.model.seedwork;

import java.util.Objects;

public abstract class Entity<ID> {

    protected final ID id;

    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "El id de una entidad no puede ser nulo");
    }

    public ID getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Entity<?> that)) return false;
        return Objects.equals(this.id, that.id) && this.getClass().equals(that.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
