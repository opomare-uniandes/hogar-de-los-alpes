package com.hda.trabajos.model.trabajo;

import com.hda.trabajos.model.seedwork.ValueObject;

/** Objeto de valor compartido (igual que en la Vista de Informacion de la Entrega 2). */
public record CategoriaServicio(String nombre) implements ValueObject {

    public CategoriaServicio {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La categoria de servicio no puede estar vacia");
        }
    }
}
